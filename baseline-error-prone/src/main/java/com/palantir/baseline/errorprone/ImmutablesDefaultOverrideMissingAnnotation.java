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
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.List;

/**
 * Immutables only treats a non-abstract accessor as an attribute when it is annotated, so overriding an inherited
 * {@code @Value.Default} accessor without re-applying the annotation turns the attribute into a plain method which
 * can no longer be set through the builder or deserialized.
 */
@AutoService(BugChecker.class)
@BugPattern(
        linkType = LinkType.CUSTOM,
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        severity = SeverityLevel.WARNING,
        summary = "Overrides of Immutables @Value.Default accessors must also be annotated @Value.Default, "
                + "otherwise the attribute can no longer be set")
public final class ImmutablesDefaultOverrideMissingAnnotation extends BugChecker implements MethodTreeMatcher {

    private static final String VALUE_IMMUTABLE = "org.immutables.value.Value.Immutable";
    private static final String VALUE_DEFAULT = "org.immutables.value.Value.Default";
    private static final String VALUE_DERIVED = "org.immutables.value.Value.Derived";
    private static final String VALUE_LAZY = "org.immutables.value.Value.Lazy";

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
        MethodSymbol method = ASTHelpers.getSymbol(tree);
        if (!isUnannotatedAccessor(tree, method, state)) {
            return Description.NO_MATCH;
        }
        ClassSymbol enclosing = ASTHelpers.enclosingClass(method);
        if (enclosing == null || !ASTHelpers.hasAnnotation(enclosing, VALUE_IMMUTABLE, state)) {
            return Description.NO_MATCH;
        }
        boolean overridesDefault = ASTHelpers.findSuperMethods(method, state.getTypes()).stream()
                .anyMatch(superMethod -> ASTHelpers.hasAnnotation(superMethod, VALUE_DEFAULT, state));
        if (!overridesDefault) {
            return Description.NO_MATCH;
        }
        SuggestedFix.Builder fix = SuggestedFix.builder();
        String annotation = "@" + SuggestedFixes.qualifyType(state, fix, VALUE_DEFAULT);
        List<? extends AnnotationTree> annotations = tree.getModifiers().getAnnotations();
        if (annotations.isEmpty()) {
            fix.prefixWith(tree, annotation + " ");
        } else {
            fix.postfixWith(annotations.get(annotations.size() - 1), " " + annotation);
        }
        return buildDescription(tree)
                .setMessage("This method overrides a @Value.Default accessor but is not annotated @Value.Default, "
                        + "so Immutables no longer treats it as an attribute: it cannot be set through the builder "
                        + "or deserialized, and always returns this value. Annotate it with @Value.Default, or "
                        + "suppress this warning if the value is intentionally fixed.")
                .addFix(fix.build())
                .build();
    }

    private static boolean isUnannotatedAccessor(MethodTree tree, MethodSymbol method, VisitorState state) {
        return tree.getBody() != null
                && method.getParameters().isEmpty()
                && !method.isStatic()
                && !ASTHelpers.hasAnnotation(method, VALUE_DEFAULT, state)
                && !ASTHelpers.hasAnnotation(method, VALUE_DERIVED, state)
                && !ASTHelpers.hasAnnotation(method, VALUE_LAZY, state);
    }
}
