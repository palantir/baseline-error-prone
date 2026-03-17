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
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;

@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = SeverityLevel.ERROR,
        summary = "Arrays should not be logged. Logged args are converted to strings using toString() "
                + "and arrays implement toString() by returning their class name and hashcode, not by "
                + "calling toString() on their contents. An array can be usefully logged by converting "
                + "it to a list with Arrays.asList (primitive arrays can be converted to lists with "
                + "guava, e.g., Ints.asList).")
public final class LogsafeArrayArg extends BugChecker implements MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> MATCHER = Matchers.staticMethod()
            .onClassAny("com.palantir.logsafe.SafeArg", "com.palantir.logsafe.UnsafeArg")
            .named("of")
            .withParameters(String.class.getName(), Object.class.getName());
    private static final Matcher<ExpressionTree> ARRAY = Matchers.isArrayType();
    private static final Matcher<ExpressionTree> PRIMITIVE_ARRAY = Matchers.isPrimitiveArrayType();

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (MATCHER.matches(tree, state)) {
            List<? extends ExpressionTree> args = tree.getArguments();
            ExpressionTree argValue = args.get(1);
            if (ARRAY.matches(argValue, state)) {
                if (PRIMITIVE_ARRAY.matches(argValue, state)) {
                    return describeMatch(tree);
                }
                SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
                String newType = SuggestedFixes.qualifyType(state, fixBuilder, "java.util.Arrays");
                String arg = state.getSourceForNode(argValue);
                String replacement = newType + ".asList(" + arg + ")";
                return buildDescription(tree)
                        .addFix(fixBuilder.replace(argValue, replacement).build())
                        .build();
            }
        }
        return Description.NO_MATCH;
    }
}
