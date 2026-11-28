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
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;

/**
 * Detects Immutables {@code @Value.Default} accessors whose default value would not survive Jackson deserialization.
 *
 * <p>Jackson deserializes a property using its declared type's {@code @JsonDeserialize(as = ...)} target, so a
 * default which returns a subtype of the declared type, or a customised instance of the declared type, only applies
 * when the property is absent. As soon as the property appears in the input, the subtype's (or customised) defaults
 * are silently replaced by the declared type's defaults.
 *
 * <p>Only direct returns of factory, constructor and builder expressions are analysed.
 */
@AutoService(BugChecker.class)
@BugPattern(
        linkType = LinkType.CUSTOM,
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks",
        severity = SeverityLevel.WARNING,
        summary = "Immutables @Value.Default values which return a subtype or customised instance of the declared "
                + "type are lost whenever the property is present in deserialized input")
public final class ImmutablesDefaultLostOnDeserialization extends BugChecker implements MethodTreeMatcher {

    private static final String VALUE_IMMUTABLE = "org.immutables.value.Value.Immutable";
    private static final String VALUE_DEFAULT = "org.immutables.value.Value.Default";
    private static final String JSON_DESERIALIZE = "com.fasterxml.jackson.databind.annotation.JsonDeserialize";
    private static final String JSON_IGNORE = "com.fasterxml.jackson.annotation.JsonIgnore";
    private static final String JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo";
    private static final String JSON_SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes";

    private static final Supplier<Name> AS = VisitorState.memoize(state -> state.getName("as"));

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
        MethodSymbol method = ASTHelpers.getSymbol(tree);
        if (!isDeserializedDefault(tree, method, state)) {
            return Description.NO_MATCH;
        }
        Type declaredType = state.getTypes().erasure(method.getReturnType());
        if (declaredType.getKind() != TypeKind.DECLARED || isPolymorphic(declaredType, state)) {
            return Description.NO_MATCH;
        }
        Optional<Type> maybeDeclaredAs = getDeserializeAs(declaredType.tsym, state);
        if (maybeDeclaredAs.isEmpty()) {
            return Description.NO_MATCH;
        }
        Type declaredAs = maybeDeclaredAs.get();

        ImmutableList<ExpressionTree> returned = returnExpressions(tree);
        for (ExpressionTree expression : returned) {
            Type returnedType = returnedSubtype(expression, declaredType, declaredAs, state);
            if (returnedType != null) {
                return describeSubtype(tree, returnedType, declaredType, declaredAs, state);
            }
        }
        if (returned.stream().anyMatch(expression -> isCustomisedBuilder(expression, declaredType, declaredAs))) {
            return buildDescription(tree)
                    .setMessage(String.format(
                            "This default customises %1$s using its builder, but the customised values only apply "
                                    + "when the property is absent. Jackson deserializes any explicit value as a "
                                    + "fresh %1$s (via @JsonDeserialize(as = %2$s.class)), which resets them to "
                                    + "%1$s's defaults. Move the customised defaults into a subtype of %1$s which "
                                    + "overrides the relevant @Value.Default methods, then annotate this method with "
                                    + "@JsonDeserialize(as = <the subtype>.class).",
                            simpleName(declaredType), simpleName(declaredAs)))
                    .build();
        }
        return Description.NO_MATCH;
    }

    /** Matches {@code @Value.Default} accessors of Immutables types which are deserialized by Jackson. */
    private static boolean isDeserializedDefault(MethodTree tree, MethodSymbol method, VisitorState state) {
        if (tree.getBody() == null
                || !method.getParameters().isEmpty()
                || method.isStatic()
                || !ASTHelpers.hasAnnotation(method, VALUE_DEFAULT, state)
                || ASTHelpers.hasAnnotation(method, JSON_DESERIALIZE, state)
                || ASTHelpers.hasAnnotation(method, JSON_IGNORE, state)) {
            return false;
        }
        ClassSymbol enclosing = ASTHelpers.enclosingClass(method);
        return enclosing != null
                && ASTHelpers.hasAnnotation(enclosing, VALUE_IMMUTABLE, state)
                && ASTHelpers.hasAnnotation(enclosing, JSON_DESERIALIZE, state);
    }

    private Description describeSubtype(
            MethodTree tree, Type returnedType, Type declaredType, Type declaredAs, VisitorState state) {
        // Suggest the nearest supertype which declares how it should be deserialized, typically the abstract value type
        // the returned ImmutableSubtype implements, rather than its generated implementation: Immutables copies
        // attribute annotations into generated code, which fails to compile when they reference classes generated in
        // the same compilation. Jackson applies the subtype's own @JsonDeserialize(as = ...) in turn.
        for (Type subtype : state.getTypes().closure(returnedType)) {
            if (ASTHelpers.isSameType(subtype, declaredType, state)
                    || !state.getTypes().isSubtype(subtype, declaredType)
                    || getDeserializeAs(subtype.tsym, state).isEmpty()) {
                continue;
            }
            SuggestedFix.Builder fix = SuggestedFix.builder();
            String annotation = String.format(
                    "@%s(as = %s.class)",
                    SuggestedFixes.qualifyType(state, fix, JSON_DESERIALIZE),
                    SuggestedFixes.qualifyType(state, fix, subtype.tsym));
            List<? extends AnnotationTree> annotations = tree.getModifiers().getAnnotations();
            if (annotations.isEmpty()) {
                fix.prefixWith(tree, annotation + " ");
            } else {
                fix.postfixWith(annotations.get(annotations.size() - 1), " " + annotation);
            }
            return buildDescription(tree)
                    .setMessage(subtypeProblem(subtype, declaredType, declaredAs)
                            + String.format(
                                    " Annotate the method with @JsonDeserialize(as = %s.class).", simpleName(subtype)))
                    .addFix(fix.build())
                    .build();
        }
        return buildDescription(tree)
                .setMessage(subtypeProblem(returnedType, declaredType, declaredAs)
                        + String.format(
                                " Annotate %1$s with @JsonDeserialize(as = <its implementation>.class), then annotate "
                                        + "the method with @JsonDeserialize(as = %1$s.class).",
                                simpleName(returnedType)))
                .build();
    }

    private static String subtypeProblem(Type subtype, Type declaredType, Type declaredAs) {
        return String.format(
                "This default returns %1$s, but Jackson deserializes the property as %2$s (via "
                        + "@JsonDeserialize(as = %3$s.class)), so %1$s's defaults are lost whenever the property "
                        + "is present.",
                simpleName(subtype), simpleName(declaredType), simpleName(declaredAs));
    }

    /** Returns the erased type of the expression if it is a subtype which Jackson would not deserialize. */
    @Nullable
    private static Type returnedSubtype(
            ExpressionTree expression, Type declaredType, Type declaredAs, VisitorState state) {
        Type type = ASTHelpers.getType(expression);
        if (type == null || type.getKind() != TypeKind.DECLARED || type.tsym.isAnonymous()) {
            return null;
        }
        Type erased = state.getTypes().erasure(type);
        if (!state.getTypes().isSubtype(erased, declaredType)
                || ASTHelpers.isSameType(erased, declaredType, state)
                || ASTHelpers.isSameType(erased, declaredAs, state)) {
            return null;
        }
        return erased;
    }

    /**
     * Matches builder chains for the declared type, for example {@code T.builder().foo(1).build()}, which call at least
     * one method other than {@code from}.
     */
    private static boolean isCustomisedBuilder(ExpressionTree expression, Type declaredType, Type declaredAs) {
        if (!isNoArgCall(expression, "build")) {
            return false;
        }
        boolean customised = false;
        ExpressionTree receiver = ASTHelpers.getReceiver(expression);
        while (receiver instanceof MethodInvocationTree call
                && !ASTHelpers.getSymbol(call).isStatic()) {
            customised |= !ASTHelpers.getSymbol(call).getSimpleName().contentEquals("from");
            receiver = ASTHelpers.getReceiver(call);
        }
        return customised && isNewBuilder(receiver, declaredType, declaredAs);
    }

    /** Matches {@code T.builder()}, {@code ImmutableT.builder()} and {@code new T.Builder()}. */
    private static boolean isNewBuilder(@Nullable ExpressionTree tree, Type declaredType, Type declaredAs) {
        if (tree instanceof MethodInvocationTree call) {
            return isNoArgCall(call, "builder")
                    && isDeclaredTypeOrImplementation(ASTHelpers.getSymbol(call).owner, declaredType, declaredAs);
        }
        if (tree instanceof NewClassTree newClass) {
            Symbol builderClass = ASTHelpers.getSymbol(newClass.getIdentifier());
            return builderClass != null
                    && builderClass.getSimpleName().contentEquals("Builder")
                    && isDeclaredTypeOrImplementation(builderClass.owner, declaredType, declaredAs);
        }
        return false;
    }

    private static boolean isNoArgCall(ExpressionTree tree, String name) {
        return tree instanceof MethodInvocationTree call
                && call.getArguments().isEmpty()
                && ASTHelpers.getSymbol(call).getSimpleName().contentEquals(name);
    }

    private static boolean isDeclaredTypeOrImplementation(Symbol symbol, Type declaredType, Type declaredAs) {
        return symbol.equals(declaredType.tsym) || symbol.equals(declaredAs.tsym);
    }

    private static boolean isPolymorphic(Type type, VisitorState state) {
        for (Type supertype : state.getTypes().closure(type)) {
            if (ASTHelpers.hasAnnotation(supertype.tsym, JSON_TYPE_INFO, state)
                    || ASTHelpers.hasAnnotation(supertype.tsym, JSON_SUB_TYPES, state)) {
                return true;
            }
        }
        return false;
    }

    /** Returns the {@code as} value of the symbol's {@code @JsonDeserialize} annotation, if present. */
    private static Optional<Type> getDeserializeAs(Symbol symbol, VisitorState state) {
        for (Attribute.Compound annotation : symbol.getAnnotationMirrors()) {
            if (!annotation.type.tsym.getQualifiedName().contentEquals(JSON_DESERIALIZE)) {
                continue;
            }
            Attribute value = annotation.member(AS.get(state));
            if (value instanceof Attribute.Class classValue
                    && classValue.classType.getKind() == TypeKind.DECLARED
                    && !classValue.classType.tsym.getQualifiedName().contentEquals(Void.class.getName())) {
                return Optional.of(classValue.classType);
            }
        }
        return Optional.empty();
    }

    /** Collects returned expressions, excluding those in lambdas and nested or anonymous classes. */
    private static ImmutableList<ExpressionTree> returnExpressions(MethodTree tree) {
        ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();
        tree.getBody()
                .accept(
                        new TreeScanner<Void, Void>() {
                            @Override
                            public Void visitReturn(ReturnTree node, Void unused) {
                                if (node.getExpression() != null) {
                                    expressions.add(node.getExpression());
                                }
                                return super.visitReturn(node, null);
                            }

                            @Override
                            public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
                                return null;
                            }

                            @Override
                            public Void visitClass(ClassTree node, Void unused) {
                                return null;
                            }
                        },
                        null);
        return expressions.build();
    }

    private static String simpleName(Type type) {
        return type.tsym.getSimpleName().toString();
    }
}
