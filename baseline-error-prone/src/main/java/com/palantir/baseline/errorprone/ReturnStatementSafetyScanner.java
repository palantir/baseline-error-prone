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

package com.palantir.baseline.errorprone;

import com.google.errorprone.VisitorState;
import com.palantir.baseline.errorprone.safety.Safety;
import com.palantir.baseline.errorprone.safety.SafetyAnalysis;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import org.checkerframework.errorprone.javacutil.TreePathUtil;

/**
 * Scans return statements within a method and computes the combined {@link Safety} of all returned expressions.
 * Stops at scope boundaries (nested classes, lambdas) to avoid analyzing returns from inner scopes.
 */
final class ReturnStatementSafetyScanner extends TreeScanner<Safety, VisitorState> {

    private final MethodTree target;

    ReturnStatementSafetyScanner(MethodTree target) {
        this.target = target;
    }

    @Override
    public Safety visitReturn(ReturnTree node, VisitorState visitorState) {
        ExpressionTree expression = node.getExpression();
        if (expression == null) {
            return null;
        }
        // Validate that the discovered ReturnTree is from the same scope as the 'target' method.
        TreePath path = TreePath.getPath(visitorState.getPath().getCompilationUnit(), expression);
        if (target.equals(TreePathUtil.enclosingMethodOrLambda(path))) {
            return SafetyAnalysis.of(visitorState.withPath(path));
        } else {
            // Unclear what's happening in this case, so we definitely don't want to claim SAFE
            return Safety.UNKNOWN;
        }
    }

    // Don't search beyond the scope of the method
    @Override
    public Safety visitClass(ClassTree _node, VisitorState _obj) {
        return null;
    }

    @Override
    public Safety visitNewClass(NewClassTree node, VisitorState _state) {
        return null;
    }

    @Override
    public Safety visitLambdaExpression(LambdaExpressionTree node, VisitorState _state) {
        return null;
    }

    @Override
    public Safety reduce(Safety lhs, Safety rhs) {
        if (lhs == null) {
            return rhs;
        }
        if (rhs == null) {
            return lhs;
        }
        return lhs.leastUpperBound(rhs);
    }
}
