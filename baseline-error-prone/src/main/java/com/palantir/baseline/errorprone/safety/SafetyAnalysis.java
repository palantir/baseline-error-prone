/*
 * (c) Copyright 2022 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.baseline.errorprone.safety;

import com.google.errorprone.VisitorState;
import com.google.errorprone.dataflow.AccessPathStore;
import com.palantir.baseline.errorprone.safety.SafetyPropagationTransfer.ClearVisitorState;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.errorprone.dataflow.analysis.Analysis;
import org.checkerframework.errorprone.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.errorprone.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.errorprone.dataflow.cfg.UnderlyingAST;
import org.checkerframework.errorprone.dataflow.cfg.builder.CFGBuilder;

public final class SafetyAnalysis {
    private static final Context.Key<SafetyPropagationTransfer> SAFETY_PROPAGATION = new Context.Key<>();
    private static final Context.Key<AnalysisCache> ANALYSIS_CACHE = new Context.Key<>();

    /**
     * Returns the safety of the item at the current path.
     * Callers may need to use {@link VisitorState#withPath(TreePath)} to provide a more specific path.
     */
    public static Safety of(VisitorState state) {
        SafetyPropagationTransfer propagation = instance(state.context);
        try (ClearVisitorState ignored = propagation.setVisitorState(state)) {
            return Safety.nullToUnknown(
                    analysisCache(state.context).expressionDataflow(state.getPath(), state.context, propagation));
        }
    }

    private static SafetyPropagationTransfer instance(Context context) {
        SafetyPropagationTransfer instance = context.get(SAFETY_PROPAGATION);
        if (instance == null) {
            instance = new SafetyPropagationTransfer();
            context.put(SAFETY_PROPAGATION, instance);
        }
        return instance;
    }

    private static AnalysisCache analysisCache(Context context) {
        AnalysisCache cache = context.get(ANALYSIS_CACHE);
        if (cache == null) {
            cache = new AnalysisCache();
            context.put(ANALYSIS_CACHE, cache);
        }
        return cache;
    }

    /**
     * Replaces {@code DataFlow.expressionDataflow}, which retains only a single control flow graph keyed on
     * {@link TreePath} identity. Safety checks alternate between a method and the lambdas it contains, so with a
     * single entry the method is rebuilt and re-analyzed after every lambda. This retains several analyses for the
     * current compilation unit, keyed on the analyzed tree.
     */
    private static final class AnalysisCache {
        private static final int MAX_ENTRIES = 32;

        @Nullable
        private CompilationUnitTree compilationUnit;

        private final Map<Tree, Analysis<Safety, AccessPathStore<Safety>, SafetyPropagationTransfer>> analyses =
                new LinkedHashMap<>(16, 0.75f, /* accessOrder= */ true) {
                    @Override
                    protected boolean removeEldestEntry(
                            Map.Entry<Tree, Analysis<Safety, AccessPathStore<Safety>, SafetyPropagationTransfer>>
                                    _eldest) {
                        return size() > MAX_ENTRIES;
                    }
                };

        @Nullable
        @SuppressWarnings("ReferenceEquality")
        Safety expressionDataflow(TreePath exprPath, Context context, SafetyPropagationTransfer transfer) {
            TreePath enclosingPath = findEnclosingMethodOrLambdaOrInitializer(exprPath);
            if (enclosingPath == null) {
                return null;
            }
            if (enclosingPath.getLeaf() instanceof MethodTree method && method.getBody() == null) {
                return null;
            }
            CompilationUnitTree unit = exprPath.getCompilationUnit();
            if (unit != compilationUnit) {
                analyses.clear();
                compilationUnit = unit;
            }
            Tree analyzed = enclosingPath.getLeaf();
            Analysis<Safety, AccessPathStore<Safety>, SafetyPropagationTransfer> analysis = analyses.get(analyzed);
            if (analysis == null) {
                analysis = analyze(enclosingPath, context, transfer);
                analyses.put(analyzed, analysis);
            }
            return analysis.getValue(exprPath.getLeaf());
        }

        private static Analysis<Safety, AccessPathStore<Safety>, SafetyPropagationTransfer> analyze(
                TreePath path, Context context, SafetyPropagationTransfer transfer) {
            ClassTree enclosingClass = null;
            MethodTree enclosingMethod = null;
            for (Tree tree : path) {
                if (tree instanceof MethodTree methodTree) {
                    enclosingMethod = methodTree;
                }
                if (tree instanceof ClassTree classTree) {
                    enclosingClass = classTree;
                    break;
                }
            }
            Tree leaf = path.getLeaf();
            UnderlyingAST ast;
            if (leaf instanceof LambdaExpressionTree lambda) {
                ast = new UnderlyingAST.CFGLambda(lambda, enclosingClass, enclosingMethod);
            } else if (leaf instanceof MethodTree method) {
                ast = new UnderlyingAST.CFGMethod(method, enclosingClass);
            } else {
                ast = new UnderlyingAST.CFGStatement(leaf, enclosingClass);
            }
            ControlFlowGraph cfg = buildCfg(path, ast, JavacProcessingEnvironment.instance(context));
            Analysis<Safety, AccessPathStore<Safety>, SafetyPropagationTransfer> analysis =
                    new ForwardAnalysisImpl<>(transfer);
            analysis.performAnalysis(cfg);
            return analysis;
        }

        /** Matches the lookup in {@code DataFlow.findEnclosingMethodOrLambdaOrInitializer}. */
        @Nullable
        private static TreePath findEnclosingMethodOrLambdaOrInitializer(TreePath start) {
            TreePath path = start;
            while (path != null) {
                if (path.getLeaf() instanceof MethodTree) {
                    return path;
                }
                TreePath parent = path.getParentPath();
                if (parent != null) {
                    if (parent.getLeaf() instanceof ClassTree) {
                        if (path.getLeaf() instanceof BlockTree) {
                            return path;
                        }
                        if (path.getLeaf() instanceof VariableTree variable && variable.getInitializer() != null) {
                            return path;
                        }
                    }
                    if (parent.getLeaf() instanceof LambdaExpressionTree) {
                        return parent;
                    }
                }
                path = parent;
            }
            return null;
        }
    }

    /**
     * Builds the control flow graph for {@code ast}, whose code is {@code enclosingPath} or a direct child of it.
     * The {@link CompilationUnitTree} overload of {@link CFGBuilder#build} searches the entire compilation unit
     * for the code's path, which is significantly more expensive than constructing it.
     */
    @SuppressWarnings("ReferenceEquality")
    static ControlFlowGraph buildCfg(TreePath enclosingPath, UnderlyingAST ast, ProcessingEnvironment env) {
        Tree code = ast.getCode();
        TreePath codePath = code == enclosingPath.getLeaf() ? enclosingPath : new TreePath(enclosingPath, code);
        return CFGBuilder.build(
                codePath, ast, /* assumeAssertionsEnabled= */ false, /* assumeAssertionsDisabled= */ false, env);
    }

    private SafetyAnalysis() {}
}
