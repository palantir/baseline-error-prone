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
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.RecordComponent;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Flags Java records with {@code @DoNotLog} components that don't override {@code toString()},
 * since the auto-generated toString includes all components.
 *
 * @see DangerousToStringDoNotLog
 * @see DangerousImmutablesToStringDoNotLog
 */
@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.ERROR,
        summary = "Records with @DoNotLog components must override toString() to exclude sensitive data.")
public final class DangerousRecordToStringDoNotLog extends BugChecker implements BugChecker.ClassTreeMatcher {

    @Override
    public Description matchClass(ClassTree classTree, VisitorState state) {
        ClassSymbol classSymbol = ASTHelpers.getSymbol(classTree);
        if (classSymbol == null || !classSymbol.isRecord()) {
            return Description.NO_MATCH;
        }
        if (MoreMatchers.getToString(classTree, state).isPresent()) {
            return Description.NO_MATCH;
        }
        if (TestCheckUtils.isTestCode(state)) {
            return Description.NO_MATCH;
        }
        List<? extends RecordComponent> violators = classSymbol.getRecordComponents().stream()
                .filter(component -> SafetyAnnotations.getVariableSafety(component, state) == Safety.DO_NOT_LOG)
                .toList();
        if (violators.isEmpty()) {
            return Description.NO_MATCH;
        }
        // Report on classTree so that @SuppressWarnings on the class is recognized by error-prone's
        // suppression mechanism for ClassTreeMatchers. Attach the fix only to the first report so that
        // batch application doesn't insert the toString() method multiple times.
        SuggestedFix fix = buildToStringFix(classTree, classSymbol, state);
        state.reportMatch(describe(classTree, violators.get(0)).addFix(fix).build());
        violators
                .subList(1, violators.size())
                .forEach(component ->
                        state.reportMatch(describe(classTree, component).build()));
        return Description.NO_MATCH;
    }

    private Description.Builder describe(ClassTree classTree, RecordComponent component) {
        return buildDescription(classTree)
                .setMessage(String.format(
                        "Record component '%s' is @DoNotLog but will be included in the"
                                + " auto-generated toString(). Override toString() to exclude sensitive data.",
                        component.getSimpleName()));
    }

    private static SuggestedFix buildToStringFix(ClassTree classTree, ClassSymbol classSymbol, VisitorState state) {
        String recordName = classSymbol.getSimpleName().toString();
        String components = classSymbol.getRecordComponents().stream()
                .filter(component -> SafetyAnnotations.getVariableSafety(component, state) != Safety.DO_NOT_LOG)
                .map(component -> component.getSimpleName().toString())
                .map(name -> "\"" + name + "=\" + " + name)
                .collect(Collectors.joining(" + \", \" + "));
        String returnExpression = components.isEmpty()
                ? "\"" + recordName + "[]\""
                : "\"" + recordName + "[\" + " + components + " + \"]\"";
        String toStringMethod = """
            @Override
            public String toString() {
                return %s;
            }
            """.formatted(returnExpression);
        return SuggestedFixes.addMembers(classTree, state, toStringMethod);
    }
}
