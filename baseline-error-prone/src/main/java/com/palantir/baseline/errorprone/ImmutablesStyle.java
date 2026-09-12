/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PackageTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.AnnotationMatcherUtils;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import javax.annotation.Nullable;

@AutoService(BugChecker.class)
@BugPattern(
        linkType = LinkType.CUSTOM,
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        severity = SeverityLevel.WARNING,
        summary = "Using an inline Immutables @Value.Style annotation or meta-annotation with non-SOURCE "
                + "retention forces consumers to add a Immutables annotations to their compile classpath."
                + "Instead use a meta-annotation with SOURCE retention."
                + "See https://github.com/immutables/immutables/issues/291.")
public final class ImmutablesStyle extends BugChecker implements ClassTreeMatcher, PackageTreeMatcher {

    private static final Matcher<Tree> STYLE_ANNOTATION = Matchers.hasAnnotation("org.immutables.value.Value$Style");

    @Override
    public Description matchPackage(PackageTree tree, VisitorState state) {
        if (STYLE_ANNOTATION.matches(tree, state)) {
            return buildDescription(tree).build();
        }

        return Description.NO_MATCH;
    }

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        if (STYLE_ANNOTATION.matches(tree, state)) {
            switch (tree.getKind()) {
                case CLASS, INTERFACE -> {
                    return buildDescription(tree).build();
                }
                case ANNOTATION_TYPE -> {
                    return matchStyleMetaAnnotation(tree, state);
                }
                default -> {}
            }
        }

        return Description.NO_MATCH;
    }

    private Description matchStyleMetaAnnotation(ClassTree tree, VisitorState state) {
        AnnotationTree retention = getAnnotation(tree, Retention.class.getName(), state);
        if (retention == null) {
            SuggestedFix.Builder fix = SuggestedFix.builder();
            fix.prefixWith(
                    tree,
                    String.format(
                            "@%s(%s.SOURCE)",
                            SuggestedFixes.qualifyType(state, fix, Retention.class.getName()),
                            SuggestedFixes.qualifyType(state, fix, RetentionPolicy.class.getName())));
            return buildDescription(tree).addFix(fix.build()).build();
        }

        ExpressionTree retentionValue = AnnotationMatcherUtils.getArgument(retention, "value");
        Symbol retentionValueSymbol = ASTHelpers.getSymbol(retentionValue);
        if (retentionValueSymbol == null
                || !retentionValueSymbol.getSimpleName().contentEquals("SOURCE")) {
            SuggestedFix.Builder fix = SuggestedFix.builder();
            fix.merge(SuggestedFixes.updateAnnotationArgumentValues(
                    retention,
                    state,
                    "value",
                    ImmutableList.of(String.format(
                            "%s.SOURCE", SuggestedFixes.qualifyType(state, fix, RetentionPolicy.class.getName())))));
            return buildDescription(tree).addFix(fix.build()).build();
        }

        return Description.NO_MATCH;
    }

    @Nullable
    private static AnnotationTree getAnnotation(ClassTree tree, String annotationType, VisitorState state) {
        List<? extends AnnotationTree> annotations = tree.getModifiers().getAnnotations();
        Type retention = state.getTypeFromString(annotationType);
        if (retention != null) {
            for (AnnotationTree annotation : annotations) {
                if (ASTHelpers.isSameType(ASTHelpers.getType(annotation.getAnnotationType()), retention, state)) {
                    return annotation;
                }
            }
        }
        return null;
    }
}
