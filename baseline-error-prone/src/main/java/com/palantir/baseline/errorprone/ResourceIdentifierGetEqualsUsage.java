/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import javax.lang.model.element.Name;

@AutoService(BugChecker.class)
@BugPattern(
        summary = "Disallowed usage of ResourceIdentifier#get{Instance,Locator,Service,Type} with equals",
        explanation = "ResourceIdentifier internally stores a single string for the entire RID. Each of the getX "
                + "methods allocates a new string for that specific part of the RID. Use "
                + "ResourceIdentifier#has{Instance,Locator,Service,Type} instead of String#equals or "
                + "Objects#equals on the result, which does not allocate any memory.",
        severity = SeverityLevel.WARNING,
        linkType = BugPattern.LinkType.CUSTOM,
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks")
public final class ResourceIdentifierGetEqualsUsage extends BugChecker
        implements BugChecker.MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> EQUALS_MATCHER = MethodMatchers.instanceMethod()
            .onDescendantOf(String.class.getName())
            .named("equals");
    private static final Matcher<ExpressionTree> OBJECTS_EQUALS_MATCHER =
            MethodMatchers.staticMethod().onClass("java.util.Objects").named("equals");
    private static final Matcher<ExpressionTree> GET_MATCHER = MethodMatchers.instanceMethod()
            .onExactClass("com.palantir.ri.ResourceIdentifier")
            .namedAnyOf("getInstance", "getLocator", "getService", "getType");

    private static final Matcher<MethodInvocationTree> GET_RECEIVER_MATCHER =
            Matchers.receiverOfInvocation(GET_MATCHER);
    private static final Matcher<MethodInvocationTree> GET_ARGUMENT_MATCHER = Matchers.argument(0, GET_MATCHER);

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (EQUALS_MATCHER.matches(tree, state)) {
            if (GET_RECEIVER_MATCHER.matches(tree, state)) {
                ExpressionTree ridTree = ASTHelpers.getReceiver(tree);
                ExpressionTree valueTree = tree.getArguments().get(0);
                return fix(tree, state, ridTree, valueTree);
            } else if (GET_ARGUMENT_MATCHER.matches(tree, state)) {
                ExpressionTree ridTree = tree.getArguments().get(0);
                ExpressionTree valueTree = ASTHelpers.getReceiver(tree);
                return fix(tree, state, ridTree, valueTree);
            }
            return Description.NO_MATCH;
        }

        if (OBJECTS_EQUALS_MATCHER.matches(tree, state) && tree.getArguments().size() == 2) {
            ExpressionTree first = tree.getArguments().get(0);
            ExpressionTree second = tree.getArguments().get(1);
            boolean firstIsGet = GET_MATCHER.matches(first, state);
            boolean secondIsGet = GET_MATCHER.matches(second, state);
            if (firstIsGet && !secondIsGet) {
                return fix(tree, state, first, second);
            } else if (secondIsGet && !firstIsGet) {
                return fix(tree, state, second, first);
            }
        }
        return Description.NO_MATCH;
    }

    private Description fix(
            MethodInvocationTree tree, VisitorState state, ExpressionTree getTree, ExpressionTree valueTree) {
        ExpressionTree ridTree = ASTHelpers.getReceiver(getTree);
        Name methodName = ASTHelpers.getSymbol(getTree).getSimpleName();
        String replacement = state.getSourceForNode(ridTree) + ".has"
                + methodName.subSequence(3, methodName.length()) + "("
                + state.getSourceForNode(valueTree) + ")";
        return buildDescription(tree)
                .addFix(SuggestedFix.builder().replace(tree, replacement).build())
                .build();
    }
}
