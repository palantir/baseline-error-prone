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
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.palantir.baseline.errorprone.safety.Safety;
import com.palantir.baseline.errorprone.safety.SafetyAnnotations;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.ArrayList;
import java.util.List;

/**
 * Flags {@code @Value.Immutable} types with {@code @DoNotLog} attributes that don't override
 * {@code toString()}, since the auto-generated toString includes all attributes.
 * Attributes annotated with {@code @Value.Redacted} are excluded from the generated toString
 * and are therefore not flagged.
 *
 * @see DangerousToStringDoNotLog
 * @see DangerousRecordToStringDoNotLog
 */
@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.ERROR,
        summary = "Immutables types with @DoNotLog attributes must either annotate the attribute"
                + " with @Value.Redacted or implement custom toString().")
public final class DangerousImmutablesToStringDoNotLog extends BugChecker implements BugChecker.ClassTreeMatcher {

    @Override
    @SuppressWarnings("CyclomaticComplexity")
    public Description matchClass(ClassTree classTree, VisitorState state) {
        ClassSymbol classSymbol = ASTHelpers.getSymbol(classTree);
        if (classSymbol == null
                || !ASTHelpers.hasAnnotation(classSymbol, "org.immutables.value.Value.Immutable", state)) {
            return Description.NO_MATCH;
        }
        // If the source type provides its own toString, the generated class won't override it,
        // so the DangerousToStringDoNotLog MethodTreeMatcher handles that case instead.
        if (MoreMatchers.getToString(classTree, state).isPresent()) {
            return Description.NO_MATCH;
        }
        if (TestCheckUtils.isTestCode(state)) {
            return Description.NO_MATCH;
        }
        List<MethodTree> violators = new ArrayList<>();
        for (Tree member : classTree.getMembers()) {
            if (!(member instanceof MethodTree methodTree)) {
                continue;
            }
            MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodTree);
            if (methodSymbol == null || !SafeLoggingPropagation.isImmutablesField(classSymbol, methodSymbol, state)) {
                continue;
            }
            // @Value.Redacted, @Value.Auxiliary, and @Value.Lazy exclude the attribute from the
            // generated toString (lazy attributes act as auxiliary per Immutables docs)
            if (ASTHelpers.hasAnnotation(methodSymbol, "org.immutables.value.Value.Redacted", state)
                    || ASTHelpers.hasAnnotation(methodSymbol, "org.immutables.value.Value.Auxiliary", state)
                    || ASTHelpers.hasAnnotation(methodSymbol, "org.immutables.value.Value.Lazy", state)) {
                continue;
            }
            if (SafetyAnnotations.getMethodReturnSafety(methodSymbol, state) == Safety.DO_NOT_LOG) {
                violators.add(methodTree);
            }
        }
        if (violators.isEmpty()) {
            return Description.NO_MATCH;
        }
        SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
        String redacted = SuggestedFixes.qualifyType(state, fixBuilder, "org.immutables.value.Value.Redacted");
        for (MethodTree methodTree : violators) {
            fixBuilder.prefixWith(methodTree, "@" + redacted + "\n    ");
        }
        SuggestedFix fix = fixBuilder.build();
        // Report on classTree so that @SuppressWarnings on the class is recognized by error-prone's
        // suppression mechanism for ClassTreeMatchers. Attach the fix only to the first report so that
        // batch application doesn't insert @Value.Redacted multiple times per attribute.
        state.reportMatch(describe(classTree, violators.get(0)).addFix(fix).build());
        for (int i = 1; i < violators.size(); i++) {
            state.reportMatch(describe(classTree, violators.get(i)).build());
        }
        return Description.NO_MATCH;
    }

    private Description.Builder describe(ClassTree classTree, MethodTree methodTree) {
        MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodTree);
        return buildDescription(classTree)
                .setMessage(String.format(
                        "Attribute '%s()' is @DoNotLog but will be included in the auto-generated toString()."
                                + " Annotate with @Value.Redacted, or implement a custom toString().",
                        methodSymbol.getSimpleName()));
    }
}
