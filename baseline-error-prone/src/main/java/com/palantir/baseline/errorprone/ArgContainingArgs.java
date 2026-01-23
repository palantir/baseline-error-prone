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

    private static final long serialVersionUID = 1L;

    private static final String ARG_CLASS = "com.palantir.logsafe.Arg";
    private static final String COLLECTION_CLASS = "java.util.Collection";

    private static final Matcher<ExpressionTree> SAFEARG_FACTORY_METHOD = MethodMatchers.staticMethod()
            .onClassAny("com.palantir.logsafe.SafeArg", "com.palantir.logsafe.UnsafeArg")
            .named("of")
            .withParameters(String.class.getName(), Object.class.getName());

    private static final Matcher<ExpressionTree> ARG_MATCHER = MoreMatchers.isSubtypeOf(ARG_CLASS);

    private static final Matcher<ExpressionTree> COLLECTION_MATCHER = MoreMatchers.isSubtypeOf(COLLECTION_CLASS);

    private static final Supplier<Type> JAVA_UTIL_COLLECTION =
            VisitorState.memoize(state -> state.getTypeFromString(COLLECTION_CLASS));

    private static final Supplier<Type> LOGSAFE_ARG = VisitorState.memoize(state -> state.getTypeFromString(ARG_CLASS));

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!SAFEARG_FACTORY_METHOD.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        List<? extends ExpressionTree> args = tree.getArguments();
        ExpressionTree valueArgument = args.get(1);

        // Check if the value is an Arg<?> directly
        if (ARG_MATCHER.matches(valueArgument, state)) {
            return buildDescription(tree)
                    .setMessage("Do not wrap an Arg inside a SafeArg. "
                            + "Args should be passed directly to logging methods.")
                    .build();
        }

        // Check if the value is a Collection<? extends Arg<?>>
        if (COLLECTION_MATCHER.matches(valueArgument, state)) {
            if (isCollectionOfArgs(valueArgument, state)) {
                return buildDescription(tree)
                        .setMessage("Do not wrap a Collection of Args inside a SafeArg. "
                                + "Args should be passed directly to logging methods.")
                        .build();
            }
        }

        return Description.NO_MATCH;
    }

    private static boolean isCollectionOfArgs(ExpressionTree expressionTree, VisitorState state) {
        Type collectionType = JAVA_UTIL_COLLECTION.get(state);
        Type argType = LOGSAFE_ARG.get(state);
        if (collectionType == null || argType == null) {
            return false;
        }

        Type expressionType = ASTHelpers.getType(expressionTree);
        if (expressionType == null) {
            return false;
        }

        // Get the Collection<?> type with the actual type argument
        Symbol collectionSymbol = collectionType.tsym;
        Type asCollection = state.getTypes().asSuper(expressionType, collectionSymbol);
        if (asCollection == null || asCollection.getTypeArguments().isEmpty()) {
            return false;
        }

        // Check if the type argument is a subtype of Arg<?>
        Type typeArgument = asCollection.getTypeArguments().get(0);
        return ASTHelpers.isSubtype(
                state.getTypes().erasure(typeArgument), state.getTypes().erasure(argType), state);
    }
}
