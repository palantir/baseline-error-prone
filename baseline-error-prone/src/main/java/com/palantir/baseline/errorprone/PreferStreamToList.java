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
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.List;

@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = SeverityLevel.WARNING,
        summary = "Prefer Stream.toList() over .collect(Collectors.toList()). Stream.toList() returns an unmodifiable"
                + " list and avoids the indirection of a Collector. Collectors.toList() provides no guarantees on type,"
                + " mutability, serializability, or thread-safety of the returned list.")
public final class PreferStreamToList extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final long serialVersionUID = 1L;

    private static final Matcher<ExpressionTree> STREAM_COLLECT = MethodMatchers.instanceMethod()
            .onDescendantOf("java.util.stream.Stream")
            .named("collect")
            .withParameters("java.util.stream.Collector");

    private static final Matcher<ExpressionTree> COLLECTORS_TO_LIST = MethodMatchers.staticMethod()
            .onClass("java.util.stream.Collectors")
            .named("toList")
            .withNoParameters();

    private static final Matcher<ExpressionTree> STREAM_ELEMENT_TRANSFORM = Matchers.anyOf(
            MethodMatchers.instanceMethod()
                    .onDescendantOf("java.util.stream.Stream")
                    .namedAnyOf("map", "flatMap")
                    .withParameters("java.util.function.Function"),
            MethodMatchers.instanceMethod()
                    .onDescendantOf("java.util.stream.Stream")
                    .named("mapMulti")
                    .withParameters("java.util.function.BiConsumer"),
            MethodMatchers.instanceMethod()
                    .onDescendantOf("java.util.stream.IntStream")
                    .named("mapToObj")
                    .withParameters("java.util.function.IntFunction"),
            MethodMatchers.instanceMethod()
                    .onDescendantOf("java.util.stream.LongStream")
                    .named("mapToObj")
                    .withParameters("java.util.function.LongFunction"),
            MethodMatchers.instanceMethod()
                    .onDescendantOf("java.util.stream.DoubleStream")
                    .named("mapToObj")
                    .withParameters("java.util.function.DoubleFunction"));

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!STREAM_COLLECT.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        ExpressionTree argument = tree.getArguments().get(0);
        if (!COLLECTORS_TO_LIST.matches(argument, state)) {
            return Description.NO_MATCH;
        }

        SuggestedFix replacement = replaceCollector(tree, argument, state);
        if (SuggestedFixes.compilesWithFix(replacement, state)) {
            return buildDescription(tree).addFix(replacement).build();
        }

        SuggestedFix widenedStreamFix =
                widenStreamElementType(tree, replacement, getResultElementType(tree, state), state);
        if (widenedStreamFix != null) {
            return buildDescription(tree).addFix(widenedStreamFix).build();
        }

        return Description.NO_MATCH;
    }

    private static SuggestedFix replaceCollector(
            MethodInvocationTree tree, ExpressionTree argument, VisitorState state) {
        return SuggestedFix.builder()
                .delete(argument)
                .merge(SuggestedFixes.renameMethodInvocation(tree, "toList", state))
                .build();
    }

    private static SuggestedFix widenStreamElementType(
            MethodInvocationTree tree, SuggestedFix replacement, Type resultElementType, VisitorState state) {
        if (resultElementType == null) {
            return null;
        }

        ExpressionTree current = ASTHelpers.getReceiver(tree);
        while (current instanceof MethodInvocationTree invocation) {
            if (invocation.getTypeArguments().size() <= 1 && STREAM_ELEMENT_TRANSFORM.matches(invocation, state)) {
                SuggestedFix.Builder fix = SuggestedFix.builder().merge(replacement);
                String elementType = SuggestedFixes.prettyType(state, fix, resultElementType);
                if (invocation.getTypeArguments().isEmpty()) {
                    String methodName =
                            ASTHelpers.getSymbol(invocation).getSimpleName().toString();
                    fix.merge(SuggestedFixes.renameMethodInvocation(
                            invocation, '<' + elementType + '>' + methodName, state));
                } else {
                    fix.replace(invocation.getTypeArguments().get(0), elementType);
                }
                SuggestedFix candidate = fix.build();
                if (SuggestedFixes.compilesWithFix(candidate, state)) {
                    return candidate;
                }
            }
            current = ASTHelpers.getReceiver(invocation);
        }
        return null;
    }

    private static Type getResultElementType(MethodInvocationTree tree, VisitorState state) {
        Type resultType = ASTHelpers.getType(tree);
        Type listType = resultType == null
                ? null
                : state.getTypes().asSuper(resultType, state.getSymbolFromString(List.class.getName()));
        return listType == null || listType.getTypeArguments().size() != 1
                ? null
                : listType.getTypeArguments().get(0);
    }
}
