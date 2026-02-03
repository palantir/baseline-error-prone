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
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.List;

@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = LinkType.CUSTOM,
        severity = SeverityLevel.ERROR,
        summary = "SafeArg.of or UnsafeArg.of should not be called with an Arg or a Collection of Args as the value. "
                + "Args should be passed directly to logging methods, not wrapped in another Arg.")
public final class ArgContainingArgs extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> ARG_FACTORY_METHOD = MethodMatchers.staticMethod()
            .onClassAny("com.palantir.logsafe.SafeArg", "com.palantir.logsafe.UnsafeArg")
            .named("of")
            .withParameters(String.class.getName(), Object.class.getName());
    private static final Matcher<ExpressionTree> ARG_MATCHER = MoreMatchers.isSubtypeOf("com.palantir.logsafe.Arg");
    private static final Matcher<ExpressionTree> ITERABLE_MATCHER = MoreMatchers.isSubtypeOf(Iterable.class);

    private static final Supplier<Type> ITERABLE_TYPE =
            VisitorState.memoize(state -> state.getTypeFromString("java.lang.Iterable"));
    private static final Supplier<Type> ARG_TYPE =
            VisitorState.memoize(state -> state.getTypeFromString("com.palantir.logsafe.Arg"));

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!ARG_FACTORY_METHOD.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        List<? extends ExpressionTree> args = tree.getArguments();
        ExpressionTree valueArgument = args.get(1);

        if (ARG_MATCHER.matches(valueArgument, state)) {
            return buildDescription(tree)
                    .setMessage("Do not wrap an Arg inside a SafeArg/UnsafeArg. "
                            + "Args should be passed directly to logging methods.")
                    .build();
        }

        if (ITERABLE_MATCHER.matches(valueArgument, state)) {
            if (isIterableOfArgs(valueArgument, state)) {
                return buildDescription(tree)
                        .setMessage("Do not wrap a Collection/Iterable of Args inside a SafeArg/UnsafeArg. "
                                + "Args should be passed directly to logging methods.")
                        .build();
            }
        }

        return Description.NO_MATCH;
    }

    private static boolean isIterableOfArgs(ExpressionTree expressionTree, VisitorState state) {
        Type iterableType = ITERABLE_TYPE.get(state);
        Type argType = ARG_TYPE.get(state);
        if (iterableType == null || argType == null) {
            return false;
        }

        Type expressionType = ASTHelpers.getType(expressionTree);
        if (expressionType == null) {
            return false;
        }

        // Get the Iterable<?> type with the actual type argument
        Symbol iterableSymbol = iterableType.tsym;
        Type asIterable = state.getTypes().asSuper(expressionType, iterableSymbol);
        if (asIterable == null || asIterable.getTypeArguments().isEmpty()) {
            return false;
        }

        // Check if the type argument is a subtype of Arg<?>
        Type typeArgument = asIterable.getTypeArguments().get(0);
        return ASTHelpers.isSubtype(
                state.getTypes().erasure(typeArgument), state.getTypes().erasure(argType), state);
    }
}
