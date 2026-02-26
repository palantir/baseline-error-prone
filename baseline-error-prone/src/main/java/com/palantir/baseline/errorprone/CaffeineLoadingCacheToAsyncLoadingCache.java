/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.google.common.base.Ascii;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.WARNING,
        summary = "Caffeine LoadingCache can be converted to Palantir AsyncLoadingCache.")
public final class CaffeineLoadingCacheToAsyncLoadingCache extends BugChecker
        implements BugChecker.MethodInvocationTreeMatcher {

    private static final String CAFFEINE_CLASS = "com.github.benmanes.caffeine.cache.Caffeine";
    private static final String CACHE_STATS_CLASS = "com.palantir.tritium.metrics.caffeine.CacheStats";

    private static final Matcher<ExpressionTree> CAFFEINE_NEW_BUILDER =
            Matchers.staticMethod().onClass(CAFFEINE_CLASS).named("newBuilder");

    private static final Matcher<ExpressionTree> CAFFEINE_BUILD = MethodMatchers.instanceMethod()
            .onDescendantOf(CAFFEINE_CLASS)
            .named("build");

    private static final Matcher<ExpressionTree> CACHE_STATS_REGISTER = MethodMatchers.instanceMethod()
            .onDescendantOf(CACHE_STATS_CLASS)
            .named("register");

    private static final Matcher<ExpressionTree> CACHE_STATS_OF =
            Matchers.staticMethod().onClass(CACHE_STATS_CLASS).named("of");

    private static final String EXECUTOR_FACTORY_CLASS = "com.palantir.cache.ExecutorFactory";
    private static final String WITCHCRAFT_CLASS = "com.palantir.witchcraft.Witchcraft";

    private static final ImmutableSet<String> ALLOWED_BUILDER_METHODS = ImmutableSet.of(
            "maximumSize",
            "expireAfterWrite",
            "expireAfterAccess",
            "expireAfter",
            "ticker",
            "recordStats",
            "build");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!CAFFEINE_BUILD.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        // Must be a LoadingCache: build() must have exactly one argument (the CacheLoader)
        if (tree.getArguments().size() != 1) {
            return Description.NO_MATCH;
        }

        // Walk up the receiver chain collecting builder method invocations
        Map<String, MethodInvocationTree> builderCalls = new HashMap<>();
        ExpressionTree current = ASTHelpers.getReceiver(tree);
        boolean foundNewBuilder = false;

        while (current instanceof MethodInvocationTree) {
            MethodInvocationTree invocation = (MethodInvocationTree) current;
            String methodName =
                    ASTHelpers.getSymbol(invocation).getSimpleName().toString();

            if (CAFFEINE_NEW_BUILDER.matches(invocation, state)) {
                foundNewBuilder = true;
                break;
            }

            if (!ALLOWED_BUILDER_METHODS.contains(methodName)) {
                return Description.NO_MATCH;
            }

            builderCalls.put(methodName, invocation);
            current = ASTHelpers.getReceiver(invocation);
        }

        if (!foundNewBuilder) {
            return Description.NO_MATCH;
        }

        if (!builderCalls.containsKey("maximumSize")) {
            return Description.NO_MATCH;
        }

        SuggestedFix fix = buildFix(tree, builderCalls, state);
        if (fix == null) {
            return Description.NO_MATCH;
        }

        return buildDescription(tree)
                .setMessage("This Caffeine LoadingCache is a candidate for conversion to"
                        + " com.palantir.cache.AsyncLoadingCache.")
                .addFix(fix)
                .build();
    }

    @Nullable
    private SuggestedFix buildFix(
            MethodInvocationTree buildCall, Map<String, MethodInvocationTree> builderCalls, VisitorState state) {
        SuggestedFix.Builder fix = SuggestedFix.builder();

        // Determine K, V type params from the assignment target
        String typeParams = inferTypeParameters(state);

        // Determine the name
        String cacheName = deriveCacheName(buildCall, state);

        // Build the maximumSize argument
        String maxSizeArg = state.getSourceForNode(
                builderCalls.get("maximumSize").getArguments().get(0));

        // Build expiry
        String expiryCall = buildExpiryCall(builderCalls, state, fix);

        // Build metrics
        String metricsCall = buildMetricsCall(buildCall, state);

        // Build ticker
        String tickerCall = buildTickerCall(builderCalls, state, fix);

        // Build executor — if not in scope, skip the fix entirely
        String executorExpr = resolveExecutorExpression(state);
        if (executorExpr == null) {
            return null;
        }

        // Build the loader argument
        String loaderArg = state.getSourceForNode(buildCall.getArguments().get(0));

        // Construct the replacement expression
        String cacheType = SuggestedFixes.qualifyType(state, fix, "com.palantir.cache.Cache");
        StringBuilder replacement = new StringBuilder();
        replacement.append(cacheType).append('.').append(typeParams).append("builder()");
        replacement.append(".name(\"").append(cacheName).append("\")");
        replacement.append(".maximumSize(").append(maxSizeArg).append(')');
        replacement.append('.').append(expiryCall);
        replacement.append('.').append(metricsCall);
        replacement.append(".executor(").append(executorExpr).append(')');
        if (tickerCall != null) {
            replacement.append('.').append(tickerCall);
        }
        replacement.append(".buildAsyncWithLoader(").append(loaderArg).append(')');

        // Determine the tree to replace — may include CacheStats wrapper
        Tree replaceTarget = findCacheStatsWrapper(buildCall, state);
        if (replaceTarget == null) {
            replaceTarget = buildCall;
        }

        fix.replace(replaceTarget, replacement.toString());

        // Add imports
        fix.addImport("com.palantir.cache.Cache");
        fix.addImport("com.palantir.cache.AsyncLoadingCache");
        fix.removeImport("com.github.benmanes.caffeine.cache.Caffeine");
        fix.removeImport("com.github.benmanes.caffeine.cache.LoadingCache");

        // Change the variable type
        addVariableTypeFix(state, fix);
        fix.removeImport("com.palantir.tritium.metrics.caffeine.CacheStats");

        return fix.build();
    }

    private String inferTypeParameters(VisitorState state) {
        Tree parent = state.getPath().getParentPath().getLeaf();
        if (parent instanceof VariableTree) {
            VariableTree varTree = (VariableTree) parent;
            String typeSource = state.getSourceForNode(varTree.getType());
            if (typeSource != null) {
                int start = typeSource.indexOf('<');
                int end = typeSource.lastIndexOf('>');
                if (start >= 0 && end > start) {
                    return "<" + typeSource.substring(start + 1, end) + ">";
                }
            }
        }
        return "";
    }

    private String deriveCacheName(MethodInvocationTree buildCall, VisitorState state) {
        // Check for CacheStats.of(registry, "name") wrapper
        String cacheStatsName = extractCacheStatsName(buildCall, state);
        if (cacheStatsName != null) {
            return toKebabCase(cacheStatsName);
        }

        // Fall back to enclosing class name from the source tree
        com.sun.source.tree.ClassTree enclosingClass =
                ASTHelpers.findEnclosingNode(state.getPath(), com.sun.source.tree.ClassTree.class);
        if (enclosingClass != null) {
            Symbol classSymbol = ASTHelpers.getSymbol(enclosingClass);
            if (classSymbol != null) {
                return toKebabCase(classSymbol.getQualifiedName().toString());
            }
        }
        return "unknown-cache";
    }

    @Nullable
    private String extractCacheStatsName(MethodInvocationTree buildCall, VisitorState state) {
        Tree parent = state.getPath().getParentPath().getLeaf();
        // The CacheStats wrapper appears as: CacheStats.of(registry, "name").register(stats -> ...)
        // The buildCall is inside a lambda, so we need to walk up
        // Check grandparent for CacheStats.of().register() pattern
        Tree registerCall = findCacheStatsRegisterCall(state);
        if (registerCall instanceof MethodInvocationTree) {
            MethodInvocationTree register = (MethodInvocationTree) registerCall;
            if (CACHE_STATS_REGISTER.matches(register, state)) {
                ExpressionTree receiver = ASTHelpers.getReceiver(register);
                if (receiver instanceof MethodInvocationTree) {
                    MethodInvocationTree ofCall = (MethodInvocationTree) receiver;
                    if (CACHE_STATS_OF.matches(ofCall, state) && ofCall.getArguments().size() >= 2) {
                        return ASTHelpers.constValue(ofCall.getArguments().get(1), String.class);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private Tree findCacheStatsRegisterCall(VisitorState state) {
        // Walk up the tree path to find CacheStats.of().register()
        TreePath path = state.getPath();
        while (path != null) {
            Tree leaf = path.getLeaf();
            if (leaf instanceof MethodInvocationTree) {
                MethodInvocationTree mit = (MethodInvocationTree) leaf;
                if (CACHE_STATS_REGISTER.matches(mit, state)) {
                    return mit;
                }
            }
            path = path.getParentPath();
        }
        return null;
    }

    @Nullable
    private Tree findCacheStatsWrapper(MethodInvocationTree buildCall, VisitorState state) {
        Tree registerCall = findCacheStatsRegisterCall(state);
        if (registerCall != null) {
            return registerCall;
        }
        return null;
    }

    private String buildExpiryCall(
            Map<String, MethodInvocationTree> builderCalls, VisitorState state, SuggestedFix.Builder fix) {
        if (builderCalls.containsKey("expireAfterWrite")) {
            MethodInvocationTree expiry = builderCalls.get("expireAfterWrite");
            fix.addImport("com.palantir.cache.Expiry");
            if (expiry.getArguments().size() == 1) {
                // expireAfterWrite(Duration)
                String durationArg = state.getSourceForNode(expiry.getArguments().get(0));
                return "expiry(Expiry.afterWrite(" + durationArg + "))";
            } else if (expiry.getArguments().size() == 2) {
                // expireAfterWrite(long, TimeUnit)
                String amount = state.getSourceForNode(expiry.getArguments().get(0));
                String unit = state.getSourceForNode(expiry.getArguments().get(1));
                fix.addImport("java.time.Duration");
                return "expiry(Expiry.afterWrite(Duration.of(" + amount + ", " + unit + ".toChronoUnit())))";
            }
        }

        if (builderCalls.containsKey("expireAfterAccess")) {
            MethodInvocationTree expiry = builderCalls.get("expireAfterAccess");
            fix.addImport("com.palantir.cache.Expiry");
            if (expiry.getArguments().size() == 1) {
                String durationArg = state.getSourceForNode(expiry.getArguments().get(0));
                return "expiry(Expiry.afterAccess(" + durationArg + "))";
            } else if (expiry.getArguments().size() == 2) {
                String amount = state.getSourceForNode(expiry.getArguments().get(0));
                String unit = state.getSourceForNode(expiry.getArguments().get(1));
                fix.addImport("java.time.Duration");
                return "expiry(Expiry.afterAccess(Duration.of(" + amount + ", " + unit + ".toChronoUnit())))";
            }
        }

        if (builderCalls.containsKey("expireAfter")) {
            MethodInvocationTree expiry = builderCalls.get("expireAfter");
            String expiryArg = state.getSourceForNode(expiry.getArguments().get(0));
            fix.addImport("com.palantir.cache.Expiry");
            return "expiry(" + expiryArg + ")";
        }

        return "noExpiry()";
    }

    private String buildMetricsCall(MethodInvocationTree buildCall, VisitorState state) {
        // Check if CacheStats wrapper provides a TaggedMetricRegistry
        Tree registerCall = findCacheStatsRegisterCall(state);
        if (registerCall instanceof MethodInvocationTree) {
            MethodInvocationTree register = (MethodInvocationTree) registerCall;
            ExpressionTree receiver = ASTHelpers.getReceiver(register);
            if (receiver instanceof MethodInvocationTree) {
                MethodInvocationTree ofCall = (MethodInvocationTree) receiver;
                if (CACHE_STATS_OF.matches(ofCall, state) && !ofCall.getArguments().isEmpty()) {
                    String registryArg = state.getSourceForNode(ofCall.getArguments().get(0));
                    return "metrics(" + registryArg + ")";
                }
            }
        }
        return "noMetrics()";
    }

    @Nullable
    private String buildTickerCall(
            Map<String, MethodInvocationTree> builderCalls, VisitorState state, SuggestedFix.Builder fix) {
        if (builderCalls.containsKey("ticker")) {
            MethodInvocationTree ticker = builderCalls.get("ticker");
            String tickerArg = state.getSourceForNode(ticker.getArguments().get(0));
            fix.addImport("com.palantir.cache.Ticker");
            fix.removeImport("com.github.benmanes.caffeine.cache.Ticker");
            return "ticker(" + tickerArg + ")";
        }
        return null;
    }

    private String resolveExecutorExpression(VisitorState state) {
        // Search enclosing method parameters and enclosing class fields for ExecutorFactory or Witchcraft
        // Priority: ExecutorFactory first, then Witchcraft

        // Check enclosing method parameters
        MethodTree enclosingMethod = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
        if (enclosingMethod != null) {
            for (VariableTree param : enclosingMethod.getParameters()) {
                Symbol paramSymbol = ASTHelpers.getSymbol(param);
                if (paramSymbol != null && isType(paramSymbol, EXECUTOR_FACTORY_CLASS)) {
                    return param.getName().toString();
                }
            }
        }

        // Check enclosing class fields
        ClassTree enclosingClass = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
        if (enclosingClass != null) {
            for (Tree member : enclosingClass.getMembers()) {
                if (member instanceof VariableTree) {
                    VariableTree field = (VariableTree) member;
                    Symbol fieldSymbol = ASTHelpers.getSymbol(field);
                    if (fieldSymbol != null && isType(fieldSymbol, EXECUTOR_FACTORY_CLASS)) {
                        return field.getName().toString();
                    }
                }
            }
        }

        // Check method params for Witchcraft
        if (enclosingMethod != null) {
            for (VariableTree param : enclosingMethod.getParameters()) {
                Symbol paramSymbol = ASTHelpers.getSymbol(param);
                if (paramSymbol != null && isType(paramSymbol, WITCHCRAFT_CLASS)) {
                    return param.getName() + ".executors().cacheExecutorFactory()";
                }
            }
        }

        // Check class fields for Witchcraft
        if (enclosingClass != null) {
            for (Tree member : enclosingClass.getMembers()) {
                if (member instanceof VariableTree) {
                    VariableTree field = (VariableTree) member;
                    Symbol fieldSymbol = ASTHelpers.getSymbol(field);
                    if (fieldSymbol != null && isType(fieldSymbol, WITCHCRAFT_CLASS)) {
                        return field.getName() + ".executors().cacheExecutorFactory()";
                    }
                }
            }
        }

        // No executor in scope — cannot produce a fix
        return null;
    }

    private void addVariableTypeFix(VisitorState state, SuggestedFix.Builder fix) {
        TreePath path = state.getPath().getParentPath();
        while (path != null) {
            Tree leaf = path.getLeaf();
            if (leaf instanceof VariableTree) {
                VariableTree varTree = (VariableTree) leaf;
                String oldType = state.getSourceForNode(varTree.getType());
                if (oldType != null) {
                    int start = oldType.indexOf('<');
                    String typeParams = start >= 0 ? oldType.substring(start) : "";
                    fix.replace(varTree.getType(), "AsyncLoadingCache" + typeParams);
                }
                break;
            }
            path = path.getParentPath();
        }
    }

    private static boolean isType(Symbol symbol, String qualifiedName) {
        return symbol.type != null && symbol.type.tsym != null
                && symbol.type.tsym.getQualifiedName().contentEquals(qualifiedName);
    }

    private static String toKebabCase(String input) {
        // Handle fully-qualified class names: com.foo.BarBaz -> com-foo-bar-baz
        return Ascii.toLowerCase(input.replace('.', '-')
                .replaceAll("([a-z])([A-Z])", "$1-$2"));
    }
}
