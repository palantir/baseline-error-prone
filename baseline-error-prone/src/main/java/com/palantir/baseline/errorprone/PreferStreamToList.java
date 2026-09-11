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
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Collections;

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
            .named("collect");

    private static final Matcher<ExpressionTree> COLLECTORS_TO_LIST = MethodMatchers.staticMethod()
            .onClass("java.util.stream.Collectors")
            .named("toList")
            .withParameters(Collections.emptyList());

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!STREAM_COLLECT.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        if (tree.getArguments().size() != 1) {
            return Description.NO_MATCH;
        }

        ExpressionTree argument = tree.getArguments().get(0);
        if (!COLLECTORS_TO_LIST.matches(argument, state)) {
            return Description.NO_MATCH;
        }

        return buildDescription(tree)
                .addFix(SuggestedFix.builder()
                        .delete(argument)
                        .merge(SuggestedFixes.renameMethodInvocation(tree, "toList", state))
                        .build())
                .build();
    }
}
