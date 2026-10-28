/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Detects duplicate constant keys in fluent builder chains where the same key is used multiple times
 * with putXxx(key, value) methods. This is almost always a bug where the earlier value gets silently
 * overwritten by the later one.
 *
 * <p>Example of flagged code:
 * <pre>{@code
 * SomeType.builder()
 *     .putMyMap("key", value1)
 *     .putOther(x)
 *     .putMyMap("key", value2)  // ERROR: duplicate key "key"
 *     .build();
 * }</pre>
 */
@AutoService(BugChecker.class)
@BugPattern(
        linkType = LinkType.CUSTOM,
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        severity = BugPattern.SeverityLevel.ERROR,
        summary = "Duplicate map key in fluent builder chain. The same constant key is used multiple times "
                + "with putXxx() calls, which silently overwrites earlier values.")
public final class DuplicateMapKeyInBuilderChain extends BugChecker implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!isMapPutMethod(tree, state)) {
            return Description.NO_MATCH;
        }

        String mapFieldName = extractMapFieldName(tree);
        Object key = extractConstantKey(tree.getArguments().get(0), state);

        if (key == null) {
            return Description.NO_MATCH;
        }

        // Walk the fluent chain looking for earlier calls with the same key
        Map<String, Set<Object>> seenKeys = new HashMap<>();
        seenKeys.computeIfAbsent(mapFieldName, k -> new HashSet<>()).add(key);

        ExpressionTree receiver = ASTHelpers.getReceiver(tree);
        while (receiver instanceof MethodInvocationTree) {
            MethodInvocationTree receiverInvocation = (MethodInvocationTree) receiver;

            if (isMapPutMethod(receiverInvocation, state)) {
                String receiverMapField = extractMapFieldName(receiverInvocation);
                Object receiverKey =
                        extractConstantKey(receiverInvocation.getArguments().get(0), state);

                if (receiverKey != null) {
                    Set<Object> keysForField = seenKeys.computeIfAbsent(receiverMapField, k -> new HashSet<>());
                    if (!keysForField.add(receiverKey)) {
                        // Found duplicate - receiverKey was already seen, so the current chain position
                        // (represented by `tree`) is the later call that overwrites the earlier value
                        // The duplicate is for receiverMapField (not necessarily mapFieldName)
                        String fieldNameFormatted =
                                receiverMapField.substring(0, 1).toLowerCase(Locale.ROOT)
                                        + receiverMapField.substring(1);
                        return buildDescription(tree)
                                .setMessage(String.format(
                                        "Duplicate map key '%s' in fluent builder chain for map field '%s'. "
                                                + "The earlier value will be silently overwritten.",
                                        receiverKey, fieldNameFormatted))
                                .build();
                    }
                }
            }

            receiver = ASTHelpers.getReceiver(receiverInvocation);
        }

        return Description.NO_MATCH;
    }

    /**
     * Checks if the method invocation is a map put method on a builder:
     * - Method name starts with "put" followed by an uppercase letter (e.g., putMyMap)
     * - Method has exactly 2 parameters (key, value)
     * - Method returns the receiver type (fluent builder pattern)
     */
    private static boolean isMapPutMethod(MethodInvocationTree tree, VisitorState state) {
        MethodSymbol methodSymbol = ASTHelpers.getSymbol(tree);
        if (methodSymbol == null) {
            return false;
        }

        String methodName = methodSymbol.getSimpleName().toString();

        // Must be putXxx where X is uppercase (not just "put" or "putAll")
        if (!methodName.startsWith("put") || methodName.length() <= 3) {
            return false;
        }
        char charAfterPut = methodName.charAt(3);
        if (!Character.isUpperCase(charAfterPut)) {
            return false;
        }

        // Exclude putAllXxx methods
        if (methodName.startsWith("putAll")) {
            return false;
        }

        // Must have exactly 2 parameters (key, value)
        if (methodSymbol.getParameters().size() != 2) {
            return false;
        }

        // Must return the receiver type (fluent builder pattern)
        Type receiverType = ASTHelpers.getReceiverType(tree);
        Type returnType = ASTHelpers.getResultType(tree);
        if (receiverType == null || returnType == null) {
            return false;
        }

        return state.getTypes().isSameType(receiverType, returnType);
    }

    /**
     * Extracts the map field name from a putXxx method call.
     * E.g., "putMyMap" -> "MyMap"
     */
    private static String extractMapFieldName(MethodInvocationTree tree) {
        MethodSymbol methodSymbol = ASTHelpers.getSymbol(tree);
        String methodName = methodSymbol.getSimpleName().toString();
        return methodName.substring(3); // Remove "put" prefix
    }

    /**
     * Extracts a constant key value from an expression tree. Returns null if the key
     * cannot be determined to be a stable reference.
     *
     * <p>Handles:
     * - String/numeric literals (compile-time constants)
     * - Static final fields (including non-primitive types)
     * - Enum constants
     */
    private static Object extractConstantKey(ExpressionTree keyExpr, VisitorState state) {
        // Try compile-time constant first (handles strings, primitives, static final primitives/Strings)
        Object constValue = ASTHelpers.constValue(keyExpr);
        if (constValue != null) {
            return constValue;
        }

        // Try static final field reference or enum constant
        Symbol symbol = ASTHelpers.getSymbol(keyExpr);
        if (symbol instanceof VarSymbol varSymbol) {
            if (varSymbol.isEnum()) {
                // Use fully-qualified name as unique identifier for enum constants
                return varSymbol.owner.getQualifiedName() + "." + varSymbol.getSimpleName();
            }
            // Check for static final fields (even non-primitive object types)
            if (varSymbol.isStatic() && (varSymbol.flags() & com.sun.tools.javac.code.Flags.FINAL) != 0) {
                // Use fully-qualified name as unique identifier
                return varSymbol.owner.getQualifiedName() + "." + varSymbol.getSimpleName();
            }
        }

        return null;
    }
}
