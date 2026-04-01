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
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.palantir.baseline.errorprone.safety.Safety;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * Flags {@code toString()} methods whose return value includes {@code @DoNotLog} data.
 * Including do-not-log data in a toString representation is dangerous because toString is
 * frequently used in logging in third-party libraries. The fix is to remove the sensitive data from the string
 * representation, or redact it.
 */
@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.ERROR,
        summary = "toString() methods must not include @DoNotLog data in their return value."
                + "Remove the @DoNotLog data from the string representation, or redact it.")
public final class DangerousToStringDoNotLog extends BugChecker implements BugChecker.MethodTreeMatcher {

    private static final Matcher<MethodTree> TO_STRING = Matchers.allOf(
            Matchers.methodIsNamed("toString"),
            Matchers.methodHasNoParameters(),
            Matchers.not(Matchers.isStatic()),
            Matchers.methodReturns(Matchers.isSameType(String.class)));

    @Override
    public Description matchMethod(MethodTree method, VisitorState state) {
        if (!TO_STRING.matches(method, state)) {
            return Description.NO_MATCH;
        }
        MethodSymbol methodSymbol = ASTHelpers.getSymbol(method);
        if ((methodSymbol.flags() & Flags.ABSTRACT) != 0) {
            return Description.NO_MATCH;
        }
        if (TestCheckUtils.isTestCode(state)) {
            return Description.NO_MATCH;
        }
        Safety combinedReturnSafety = method.accept(new ReturnStatementSafetyScanner(method), state);
        if (combinedReturnSafety == Safety.DO_NOT_LOG) {
            return buildDescription(method).build();
        }
        return Description.NO_MATCH;
    }
}
