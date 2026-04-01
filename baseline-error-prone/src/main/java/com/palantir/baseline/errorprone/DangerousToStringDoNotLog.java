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
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.palantir.baseline.errorprone.safety.Safety;
import com.palantir.baseline.errorprone.safety.SafetyAnnotations;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.RecordComponent;

/**
 * Flags {@code toString()} methods whose return value includes {@code @DoNotLog} data.
 * Including do-not-log data in a toString representation is dangerous because toString is
 * frequently used in logging in third-party libraries. The fix is to remove the sensitive data from the string
 * representation, or redact it.
 *
 * <p>Also flags Java records and {@code @Value.Immutable} types with {@code @DoNotLog} attributes
 * that don't override {@code toString()}, since the auto-generated toString includes all attributes.
 * For Immutables, attributes annotated with {@code @Value.Redacted} are excluded from the generated
 * toString and are therefore not flagged.
 */
@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.ERROR,
        summary = "toString() methods must not include @DoNotLog data in their return value."
                + "Remove the @DoNotLog data from the string representation, or redact it.")
public final class DangerousToStringDoNotLog extends BugChecker
        implements BugChecker.MethodTreeMatcher, BugChecker.ClassTreeMatcher {

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

    @Override
    public Description matchClass(ClassTree classTree, VisitorState state) {
        ClassSymbol classSymbol = ASTHelpers.getSymbol(classTree);
        if (classSymbol == null) {
            return Description.NO_MATCH;
        }
        if (TestCheckUtils.isTestCode(state)) {
            return Description.NO_MATCH;
        }
        if (classSymbol.isRecord()) {
            return matchRecord(classTree, classSymbol, state);
        }
        if (ASTHelpers.hasAnnotation(classSymbol, "org.immutables.value.Value.Immutable", state)) {
            return matchImmutables(classTree, classSymbol, state);
        }
        return Description.NO_MATCH;
    }

    private Description matchRecord(ClassTree classTree, ClassSymbol classSymbol, VisitorState state) {
        if (hasToStringOverride(classTree, state)) {
            return Description.NO_MATCH;
        }
        for (RecordComponent recordComponent : classSymbol.getRecordComponents()) {
            if (SafetyAnnotations.getVariableSafety(recordComponent, state) == Safety.DO_NOT_LOG) {
                return buildDescription(classTree).build();
            }
        }
        return Description.NO_MATCH;
    }

    @SuppressWarnings("CyclomaticComplexity")
    private Description matchImmutables(ClassTree classTree, ClassSymbol classSymbol, VisitorState state) {
        // If the source type provides its own toString, the generated class won't override it,
        // so the MethodTreeMatcher handles that case instead.
        if (hasToStringOverride(classTree, state)) {
            return Description.NO_MATCH;
        }
        for (Tree member : classTree.getMembers()) {
            if (!(member instanceof MethodTree methodTree)) {
                continue;
            }
            MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodTree);
            if (methodSymbol == null
                    // Constructors and initializers are not Immutables attributes
                    || methodSymbol.isConstructor()
                    || methodSymbol.isStaticOrInstanceInit()
                    // Immutables attributes are no-arg getters; methods with parameters are not attributes
                    || !methodSymbol.getParameters().isEmpty()
                    // void methods cannot be attributes
                    || state.getTypes().isSameType(methodSymbol.getReturnType(), state.getSymtab().voidType)
                    // Delegate to SafeLoggingPropagation's logic which handles abstract methods,
                    // @Value.Default, @Value.Derived, @Value.Lazy, defaultAsDefault style, and Jackson
                    || !SafeLoggingPropagation.isImmutablesField(classSymbol, methodSymbol, state)) {
                continue;
            }
            // @Value.Redacted excludes the attribute from the generated toString, so it's safe
            if (ASTHelpers.hasAnnotation(methodSymbol, "org.immutables.value.Value.Redacted", state)) {
                continue;
            }
            if (SafetyAnnotations.getMethodReturnSafety(methodSymbol, state) == Safety.DO_NOT_LOG) {
                SuggestedFix.Builder fix = SuggestedFix.builder();
                String annotation = SuggestedFixes.qualifyType(state, fix, "org.immutables.value.Value.Redacted");
                fix.prefixWith(methodTree, String.format("@%s ", annotation));
                return buildDescription(methodTree).addFix(fix.build()).build();
            }
        }
        return Description.NO_MATCH;
    }

    private static boolean hasToStringOverride(ClassTree classTree, VisitorState state) {
        for (Tree member : classTree.getMembers()) {
            if (member instanceof MethodTree methodTree && TO_STRING.matches(methodTree, state)) {
                return true;
            }
        }
        return false;
    }
}
