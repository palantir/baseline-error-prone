/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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
import com.sun.source.tree.Tree;
import java.io.Serial;
import java.util.List;

/**
 * Error Prone check that suggests migrating from {@code com.google.common.collect.Iterables.partition}
 * to {@code com.google.common.collect.Lists.partition} or {@code com.palantir.common.collect.IterableUtils.partition}.
 *
 * <p>The Palantir implementation is more efficient as it:
 * <ul>
 *   <li>Avoids excess allocations by delegating to {@code Lists.partition} where possible</li>
 *   <li>For small collections that don't need partitioning, avoids pre-allocating array of full partition size</li>
 *   <li>Handles {@code ImmutableCollection} efficiently via {@code asList()}</li>
 * </ul>
 */
@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = SeverityLevel.WARNING,
        summary = "Prefer Guava's Lists.partition(List, int) instead of Iterables.partition(Iterable, int) when the "
                + "first argument's declared type is a List for performance reasons, or "
                + "com.palantir.common.streams.MoreIterables.partition(List, int) when the first argument's declared "
                + "type is an Iterable cf. https://github.com/palantir/gradle-baseline/issues/621 "
                + " and https://github.com/palantir/baseline-error-prone/pull/68")
public final class PreferListsPartition extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Matcher<ExpressionTree> ITERABLES_PARTITION_MATCHER = MethodMatchers.staticMethod()
            .onClass("com.google.common.collect.Iterables")
            .named("partition")
            .withParameters("java.lang.Iterable", "int");

    private static final Matcher<Tree> LIST_MATCHER = MoreMatchers.isSubtypeOf("java.util.List");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (ITERABLES_PARTITION_MATCHER.matches(tree, state)) {
            List<? extends ExpressionTree> args = tree.getArguments();
            if (args.size() <= 1) {
                return Description.NO_MATCH;
            }

            if (LIST_MATCHER.matches(args.getFirst(), state)) {
                // Fail on any 'Iterables.partition(List, int) invocation
                return fix(tree, state, "com.google.common.collect.Lists");
            }
            return fix(tree, state, "com.palantir.common.streams.MoreIterables");
        }

        return Description.NO_MATCH;
    }

    private Description fix(MethodInvocationTree tree, VisitorState state, String type) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        String qualifiedType = SuggestedFixes.qualifyType(state, fix, type);
        String method = qualifiedType + ".partition";
        fix.replace(tree.getMethodSelect(), method);
        return buildDescription(tree)
                .setMessage("Prefer " + type + ".partition")
                .addFix(fix.build())
                .build();
    }
}
