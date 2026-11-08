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

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DuplicateMapKeyInBuilderChainTest {

    private CompilationTestHelper compilationHelper;

    @BeforeEach
    void before() {
        compilationHelper = CompilationTestHelper.newInstance(DuplicateMapKeyInBuilderChain.class, getClass());
    }

    @Test
    void detectsDuplicateStringKey() {
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(String key, String value) { myMap.put(key, value); return this; }",
                        "    TestBuilder putOther(String value) { return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(\"key\", \"value1\")",
                        "            // BUG: Diagnostic contains: Duplicate map key 'key'",
                        "            .putMyMap(\"key\", \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void detectsDuplicateKeyWithInterveningMethods() {
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(String key, String value) { myMap.put(key, value); return this; }",
                        "    TestBuilder putOther(String value) { return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(\"key\", \"value1\")",
                        "            .putOther(\"other\")",
                        "            // BUG: Diagnostic contains: Duplicate map key 'key'",
                        "            .putMyMap(\"key\", \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void detectsDuplicateStaticFinalKey() {
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(String key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    private static final String KEY = \"myKey\";",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(KEY, \"value1\")",
                        "            // BUG: Diagnostic contains: Duplicate map key 'myKey'",
                        "            .putMyMap(KEY, \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void detectsDuplicateStaticFinalObjectKey() {
        // Static final fields of non-primitive object types should also be detected as duplicate keys
        compilationHelper
                .addSourceLines(
                        "KeyType.java",
                        "class KeyType {",
                        "    private final String value;",
                        "    private KeyType(String value) { this.value = value; }",
                        "    static KeyType of(String value) { return new KeyType(value); }",
                        "}")
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<KeyType, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(KeyType key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    private static final KeyType MY_KEY = KeyType.of(\"key\");",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(MY_KEY, \"value1\")",
                        "            // BUG: Diagnostic contains: Duplicate map key",
                        "            .putMyMap(MY_KEY, \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void detectsDuplicateKeyImmutablesNestedBuilder() {
        // Tests Immutables-style nested Builder class pattern
        compilationHelper
                .addSourceLines(
                        "KeyType.java",
                        "class KeyType {",
                        "    private final String value;",
                        "    private KeyType(String value) { this.value = value; }",
                        "    static KeyType of(String value) { return new KeyType(value); }",
                        "}")
                .addSourceLines(
                        "Constants.java",
                        "class Constants {",
                        "    public static final KeyType MY_KEY = KeyType.of(\"myKey\");",
                        "}")
                .addSourceLines(
                        "ImmutableConfig.java",
                        "class ImmutableConfig {",
                        "    public static Builder builder() { return new Builder(); }",
                        "    public static final class Builder {",
                        "        private java.util.Map<KeyType, String> map = new java.util.HashMap<>();",
                        "        public final Builder putItems(KeyType key, String value) {",
                        "            map.put(key, value);",
                        "            return this;",
                        "        }",
                        "        public ImmutableConfig build() { return new ImmutableConfig(); }",
                        "    }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        ImmutableConfig.builder()",
                        "            .putItems(Constants.MY_KEY, \"v1\")",
                        "            // BUG: Diagnostic contains: Duplicate map key",
                        "            .putItems(Constants.MY_KEY, \"v2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void detectsDuplicateEnumKey() {
        compilationHelper
                .addSourceLines("MyEnum.java", "enum MyEnum {", "    FOO,", "    BAR", "}")
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<MyEnum, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(MyEnum key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(MyEnum.FOO, \"value1\")",
                        "            // BUG: Diagnostic contains: Duplicate map key",
                        "            .putMyMap(MyEnum.FOO, \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void detectsDuplicateNumericKey() {
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<Integer, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(Integer key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(42, \"value1\")",
                        "            // BUG: Diagnostic contains: Duplicate map key '42'",
                        "            .putMyMap(42, \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsDifferentKeysForSameMap() {
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(String key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(\"key1\", \"value1\")",
                        "            .putMyMap(\"key2\", \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsSameKeyForDifferentMaps() {
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> mapA = new java.util.HashMap<>();",
                        "    private java.util.Map<String, String> mapB = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMapA(String key, String value) { mapA.put(key, value); return this; }",
                        "    TestBuilder putMapB(String key, String value) { mapB.put(key, value); return this; }",
                        "    Object build() { return mapA; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMapA(\"key\", \"value1\")",
                        "            .putMapB(\"key\", \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsNonConstantKeys() {
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(String key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    String getKey() { return \"key\"; }",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(getKey(), \"value1\")",
                        "            .putMyMap(getKey(), \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsPutAllMethods() {
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(String key, String value) { myMap.put(key, value); return this; }",
                        "    TestBuilder putAllMyMap(java.util.Map<String, String> values) {",
                        "        myMap.putAll(values); return this;",
                        "    }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(\"key\", \"value1\")",
                        "            .putAllMyMap(java.util.Collections.singletonMap(\"key\", \"value2\"))",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsExpressionKeys() {
        // Expression keys like i++ are not compile-time constants and should be ignored
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<Integer, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(int key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        int i = 0;",
                        "        TestBuilder.builder()",
                        "            .putMyMap(i++, \"value1\")",
                        "            .putMyMap(i++, \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsSimplePutMethod() {
        // Simple put(key, value) without uppercase after "put" should not be flagged
        // as it follows a different pattern (e.g., ImmutableMap.Builder.put)
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder put(String key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .put(\"key\", \"value1\")",
                        "            .put(\"key\", \"value2\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsBuilderStoredInVariable() {
        // Builders stored in variables are out of scope - too complex to track
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(String key, String value) { myMap.put(key, value); return this; }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder builder = TestBuilder.builder().putMyMap(\"key\", \"value1\");",
                        "        builder.putMyMap(\"key\", \"value2\").build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsMethodWithWrongParameterCount() {
        // Methods with != 2 parameters should be ignored
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    TestBuilder putMyMap(String key) { return this; }",
                        "    Object build() { return null; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder.builder()",
                        "            .putMyMap(\"key\")",
                        "            .putMyMap(\"key\")",
                        "            .build();",
                        "    }",
                        "}")
                .doTest();
    }

    @Test
    void allowsNonFluentMethods() {
        // Methods that don't return the builder type should be ignored
        compilationHelper
                .addSourceLines(
                        "TestBuilder.java",
                        "class TestBuilder {",
                        "    private java.util.Map<String, String> myMap = new java.util.HashMap<>();",
                        "    static TestBuilder builder() { return new TestBuilder(); }",
                        "    void putMyMap(String key, String value) { myMap.put(key, value); }",
                        "    Object build() { return myMap; }",
                        "}")
                .addSourceLines(
                        "Test.java",
                        "class Test {",
                        "    void test() {",
                        "        TestBuilder builder = TestBuilder.builder();",
                        "        builder.putMyMap(\"key\", \"value1\");",
                        "        builder.putMyMap(\"key\", \"value2\");",
                        "        builder.build();",
                        "    }",
                        "}")
                .doTest();
    }
}
