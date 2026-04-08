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
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.palantir.baseline.errorprone.safety.Safety;
import com.palantir.baseline.errorprone.safety.SafetyAnnotations;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.RecordComponent;

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
        // Report on classTree so that @SuppressWarnings on the class is recognized by error-prone's
        // suppression mechanism for ClassTreeMatchers. Use state.reportMatch rather than returning
        // early so that all offending components are flagged in a single pass.
        for (RecordComponent recordComponent : classSymbol.getRecordComponents()) {
            if (SafetyAnnotations.getVariableSafety(recordComponent, state) == Safety.DO_NOT_LOG) {
                state.reportMatch(buildDescription(classTree)
                        .setMessage(String.format(
                                "Record component '%s' is @DoNotLog but will be included in the"
                                        + " auto-generated toString(). Override toString() to exclude sensitive data.",
                                recordComponent.getSimpleName()))
                        .build());
            }
        }
        return Description.NO_MATCH;
    }
}
