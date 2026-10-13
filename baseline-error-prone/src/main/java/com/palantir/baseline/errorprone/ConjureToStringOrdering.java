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
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/**
 * Flags comparators that order Conjure-generated types (and {@code ResourceIdentifier}, the Java type
 * of the Conjure {@code rid} primitive) by their {@code toString()} representation.
 *
 * <p>Conjure-generated classes don't carry a runtime-visible marker ({@code @Generated} has source retention), so they
 * are recognized by the shape of the generated code instead.
 */
@AutoService(BugChecker.class)
@BugPattern(
        summary = "Conjure-generated types make no guarantees about their toString() representation, so it is not a"
                + " stable ordering key. Use natural ordering or compare by a stable field, such as a RID.",
        explanation = "conjure-java does not provide any guarantees on the toString representation of generated"
                + " objects, and it has changed between releases. Ordering by toString() therefore depends on the"
                + " conjure-java version used to generate the type, and renders the whole object on every"
                + " comparison. ResourceIdentifier and Conjure string and rid aliases are Comparable, so they can be"
                + " ordered directly. For other Conjure types, compare by a field that identifies the value.",
        severity = SeverityLevel.WARNING,
        linkType = BugPattern.LinkType.CUSTOM,
        link = "https://github.com/palantir/baseline-error-prone#baseline-error-prone-checks")
public final class ConjureToStringOrdering extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final String RESOURCE_IDENTIFIER = "com.palantir.ri.ResourceIdentifier";
    private static final String JSON_VALUE = "com.fasterxml.jackson.annotation.JsonValue";
    private static final String JSON_CREATOR = "com.fasterxml.jackson.annotation.JsonCreator";
    private static final String JSON_DESERIALIZE = "com.fasterxml.jackson.databind.annotation.JsonDeserialize";
    private static final String JSON_SERIALIZE = "com.fasterxml.jackson.databind.annotation.JsonSerialize";

    // Bounds alias-of-alias unwrapping; generated aliases are never nested anywhere near this deep.
    private static final int MAX_ALIAS_DEPTH = 8;

    private static final Supplier<Symbol> COMPARATOR =
            VisitorState.memoize(state -> state.getSymbolFromString("java.util.Comparator"));

    private static final Matcher<ExpressionTree> COMPARING =
            MethodMatchers.staticMethod().onClass("java.util.Comparator").named("comparing");
    private static final Matcher<ExpressionTree> THEN_COMPARING = MethodMatchers.instanceMethod()
            .onDescendantOf("java.util.Comparator")
            .named("thenComparing");
    private static final Matcher<ExpressionTree> STREAM_SORTED = MethodMatchers.instanceMethod()
            .onDescendantOf("java.util.stream.Stream")
            .named("sorted");
    private static final Matcher<ExpressionTree> TO_STRING =
            MethodMatchers.instanceMethod().anyClass().named("toString").withNoParameters();
    private static final Matcher<ExpressionTree> STRING_VALUE_OF = MethodMatchers.staticMethod()
            .onClass("java.lang.String")
            .named("valueOf")
            .withParameters("java.lang.Object");
    private static final Matcher<ExpressionTree> OBJECTS_TO_STRING = MethodMatchers.staticMethod()
            .onClass("java.util.Objects")
            .named("toString")
            .withParameters("java.lang.Object");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        boolean isComparing = COMPARING.matches(tree, state);
        if (!isComparing && !THEN_COMPARING.matches(tree, state)) {
            return Description.NO_MATCH;
        }
        List<? extends ExpressionTree> arguments = tree.getArguments();
        if (arguments.isEmpty()
                || !firstParameterIsFunction(tree, state)
                || !isToStringOfElement(arguments.get(0), state)) {
            return Description.NO_MATCH;
        }
        Optional<ClassSymbol> maybeElement = comparedType(tree, state).filter(type -> shouldFlag(type, state));
        if (maybeElement.isEmpty() || TestCheckUtils.isTestCode(state)) {
            return Description.NO_MATCH;
        }
        ClassSymbol element = maybeElement.get();

        Replacement replacement = arguments.size() == 1
                ? replacementFor(element, state)
                : replacementForCustomKeyComparator(element, state);
        return describe(tree, isComparing, element, replacement, state);
    }

    private Description describe(
            MethodInvocationTree tree,
            boolean isComparing,
            ClassSymbol element,
            Replacement replacement,
            VisitorState state) {
        String typeName = element.getSimpleName().toString();
        Description.Builder description = buildDescription(tree);
        switch (replacement) {
            case NATURAL_ORDER -> {
                description.setMessage(
                        element.getQualifiedName().contentEquals(RESOURCE_IDENTIFIER)
                                ? "ResourceIdentifier is Comparable. Use its natural ordering instead of comparing by"
                                        + " toString()."
                                : String.format(
                                        "Conjure-generated types make no guarantees about their toString()"
                                                + " representation. %s is Comparable, so use its natural ordering.",
                                        typeName));
                description.addFix(naturalOrderFix(tree, isComparing, element, state));
            }
            case GETTER -> {
                description.setMessage(String.format(
                        "Conjure-generated types make no guarantees about their toString() representation. Compare"
                                + " by %s::get instead.",
                        typeName));
                SuggestedFix.Builder fix = SuggestedFix.builder();
                fix.replace(tree.getArguments().get(0), SuggestedFixes.qualifyType(state, fix, element) + "::get");
                description.addFix(fix.build());
            }
            case NONE ->
                description.setMessage(String.format(
                        "Conjure-generated types make no guarantees about their toString() representation, so it is"
                                + " not a stable ordering key. Compare %s by a field that identifies it, such as a"
                                + " RID.",
                        typeName));
        }
        return description.build();
    }

    private static boolean shouldFlag(ClassSymbol type, VisitorState state) {
        // ResourceIdentifier#toString is its contract, so there's only something to suggest once it is Comparable.
        return type.getQualifiedName().contentEquals(RESOURCE_IDENTIFIER)
                ? isSelfComparable(type, state)
                : isConjureType(type, state);
    }

    private enum Replacement {
        NATURAL_ORDER,
        GETTER,
        NONE
    }

    /**
     * Returns a replacement that preserves the existing order exactly. Natural ordering and the alias value both
     * compare the same string that {@code toString()} returns, but only when the innermost aliased value is a string
     * or a RID.
     */
    private static Replacement replacementFor(ClassSymbol element, VisitorState state) {
        if (element.getQualifiedName().contentEquals(RESOURCE_IDENTIFIER)) {
            return Replacement.NATURAL_ORDER;
        }
        Optional<Type> aliasedType = aliasGetter(element, state).map(MethodSymbol::getReturnType);
        if (aliasedType.isEmpty()) {
            return Replacement.NONE;
        }
        if (isSelfComparable(element, state) && wrapsStringOrComparableRid(element, state)) {
            return Replacement.NATURAL_ORDER;
        }
        if (isStringOrComparableRid(aliasedType.get(), state)) {
            return Replacement.GETTER;
        }
        return Replacement.NONE;
    }

    /**
     * The two-argument overloads order the rendered strings with a custom comparator, which only accepts the alias
     * value when it is itself a string.
     */
    private static Replacement replacementForCustomKeyComparator(ClassSymbol element, VisitorState state) {
        return aliasGetter(element, state)
                        .map(MethodSymbol::getReturnType)
                        .filter(type -> ASTHelpers.isSameType(type, state.getSymtab().stringType, state))
                        .isPresent()
                ? Replacement.GETTER
                : Replacement.NONE;
    }

    private static SuggestedFix naturalOrderFix(
            MethodInvocationTree tree, boolean isComparing, ClassSymbol element, VisitorState state) {
        if (isComparing) {
            Tree parent = state.getPath().getParentPath().getLeaf();
            if (parent instanceof MethodInvocationTree parentInvocation
                    && STREAM_SORTED.matches(parentInvocation, state)
                    && parentInvocation.getArguments().size() == 1
                    && parentInvocation.getArguments().get(0).equals(tree)) {
                // stream.sorted(Comparator.comparing(X::toString)) -> stream.sorted()
                return SuggestedFix.replace(tree, "");
            }
        }
        SuggestedFix.Builder fix = SuggestedFix.builder();
        // The explicit type witness keeps inference working in chains like naturalOrder().reversed().
        String naturalOrder = SuggestedFixes.qualifyType(state, fix, "java.util.Comparator") + ".<"
                + SuggestedFixes.qualifyType(state, fix, element) + ">naturalOrder()";
        fix.replace(isComparing ? tree : tree.getArguments().get(0), naturalOrder);
        return fix.build();
    }

    private static boolean firstParameterIsFunction(MethodInvocationTree tree, VisitorState state) {
        MethodSymbol symbol = ASTHelpers.getSymbol(tree);
        if (symbol.getParameters().isEmpty()) {
            return false;
        }
        Type parameterType = state.getTypes().erasure(symbol.getParameters().get(0).type);
        return parameterType.tsym.getQualifiedName().contentEquals("java.util.function.Function");
    }

    /** Matches key extractors that render the compared element itself, e.g. {@code X::toString}. */
    private static boolean isToStringOfElement(ExpressionTree keyExtractor, VisitorState state) {
        ExpressionTree extractor = ASTHelpers.stripParentheses(keyExtractor);
        if (extractor instanceof MemberReferenceTree) {
            return TO_STRING.matches(extractor, state)
                    || STRING_VALUE_OF.matches(extractor, state)
                    || OBJECTS_TO_STRING.matches(extractor, state);
        }
        return extractor instanceof LambdaExpressionTree lambda && isToStringOfParameter(lambda, state);
    }

    private static boolean isToStringOfParameter(LambdaExpressionTree lambda, VisitorState state) {
        if (lambda.getParameters().size() != 1 || !(lambda.getBody() instanceof MethodInvocationTree body)) {
            return false;
        }
        Symbol parameter = ASTHelpers.getSymbol(lambda.getParameters().get(0));
        if (TO_STRING.matches(body, state)) {
            ExpressionTree receiver = ASTHelpers.getReceiver(body);
            return receiver != null && parameter.equals(ASTHelpers.getSymbol(receiver));
        }
        return (STRING_VALUE_OF.matches(body, state) || OBJECTS_TO_STRING.matches(body, state))
                && parameter.equals(ASTHelpers.getSymbol(body.getArguments().get(0)));
    }

    private static Optional<ClassSymbol> comparedType(MethodInvocationTree tree, VisitorState state) {
        Type comparator = state.getTypes().asSuper(ASTHelpers.getType(tree), COMPARATOR.get(state));
        if (comparator == null || comparator.getTypeArguments().size() != 1) {
            return Optional.empty();
        }
        Type element = ASTHelpers.getUpperBound(comparator.getTypeArguments().get(0), state.getTypes());
        return element.tsym instanceof ClassSymbol classSymbol ? Optional.of(classSymbol) : Optional.empty();
    }

    private static boolean isSelfComparable(ClassSymbol type, VisitorState state) {
        Type comparable = state.getTypes().asSuper(type.type, state.getSymtab().comparableType.tsym);
        return comparable != null
                && comparable.getTypeArguments().size() == 1
                && ASTHelpers.isSameType(comparable.getTypeArguments().get(0), type.type, state);
    }

    private static boolean isStringOrComparableRid(Type type, VisitorState state) {
        if (ASTHelpers.isSameType(type, state.getSymtab().stringType, state)) {
            return true;
        }
        return type.tsym instanceof ClassSymbol classSymbol
                && classSymbol.getQualifiedName().contentEquals(RESOURCE_IDENTIFIER)
                && isSelfComparable(classSymbol, state);
    }

    private static boolean wrapsStringOrComparableRid(ClassSymbol alias, VisitorState state) {
        ClassSymbol current = alias;
        for (int depth = 0; depth < MAX_ALIAS_DEPTH; depth++) {
            Optional<MethodSymbol> getter = aliasGetter(current, state);
            if (getter.isEmpty()) {
                return false;
            }
            Type aliased = getter.get().getReturnType();
            if (isStringOrComparableRid(aliased, state)) {
                return true;
            }
            if (!(aliased.tsym instanceof ClassSymbol next)) {
                return false;
            }
            current = next;
        }
        return false;
    }

    private static boolean isConjureType(ClassSymbol type, VisitorState state) {
        return aliasGetter(type, state).isPresent()
                || isLegacyUnionOrEnum(type, state)
                || isSealedUnion(type)
                || isBean(type, state);
    }

    /** Aliases expose their value through {@code @JsonValue get()} and are created by {@code @JsonCreator of(value)}. */
    private static Optional<MethodSymbol> aliasGetter(ClassSymbol type, VisitorState state) {
        if (!type.getModifiers().contains(Modifier.FINAL)) {
            return Optional.empty();
        }
        Optional<MethodSymbol> getter = methods(type)
                .filter(method -> !method.isStatic()
                        && method.getSimpleName().contentEquals("get")
                        && method.getParameters().isEmpty()
                        && ASTHelpers.hasAnnotation(method, JSON_VALUE, state))
                .findFirst();
        boolean hasFactory = methods(type)
                .anyMatch(method -> method.isStatic()
                        && method.getSimpleName().contentEquals("of")
                        && method.getParameters().size() == 1
                        && ASTHelpers.hasAnnotation(method, JSON_CREATOR, state));
        return hasFactory ? getter : Optional.empty();
    }

    /** Enums and visitor-based unions both nest a {@code Visitor} interface and deserialize via {@code @JsonCreator}. */
    private static boolean isLegacyUnionOrEnum(ClassSymbol type, VisitorState state) {
        return type.getModifiers().contains(Modifier.FINAL)
                && hasNestedType(type, ElementKind.INTERFACE, "Visitor")
                && ASTHelpers.getEnclosedElements(type).stream()
                        .anyMatch(member -> ASTHelpers.hasAnnotation(member, JSON_CREATOR, state));
    }

    private static boolean isSealedUnion(ClassSymbol type) {
        return type.getModifiers().contains(Modifier.SEALED)
                && ASTHelpers.getEnclosedElements(type).stream()
                        .anyMatch(member -> member.getKind() == ElementKind.INTERFACE
                                && member.getModifiers().contains(Modifier.SEALED)
                                && member.getSimpleName().contentEquals("Known"));
    }

    private static boolean isBean(ClassSymbol type, VisitorState state) {
        if (!type.getModifiers().contains(Modifier.FINAL)) {
            return false;
        }
        if (ASTHelpers.hasAnnotation(type, JSON_DESERIALIZE, state)) {
            return hasNestedType(type, ElementKind.CLASS, "Builder");
        }
        // Objects without fields have no builder, only a static no-arg factory.
        return ASTHelpers.hasAnnotation(type, JSON_SERIALIZE, state)
                && methods(type)
                        .anyMatch(method -> method.isStatic()
                                && method.getSimpleName().contentEquals("of")
                                && method.getParameters().isEmpty()
                                && ASTHelpers.hasAnnotation(method, JSON_CREATOR, state));
    }

    private static boolean hasNestedType(ClassSymbol type, ElementKind kind, String simpleName) {
        return ASTHelpers.getEnclosedElements(type).stream()
                .anyMatch(member ->
                        member.getKind() == kind && member.getSimpleName().contentEquals(simpleName));
    }

    private static Stream<MethodSymbol> methods(ClassSymbol type) {
        return ASTHelpers.getEnclosedElements(type).stream()
                .filter(MethodSymbol.class::isInstance)
                .map(MethodSymbol.class::cast);
    }
}
