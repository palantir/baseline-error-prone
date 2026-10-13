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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConjureToStringOrderingTest {

    /** Trimmed-down copies of conjure-java's generated output, keeping only the parts the check inspects. */
    private static final Map<String, String[]> CONJURE_TYPES = Map.ofEntries(
            Map.entry("ComparableRid.java", alias("ComparableRid", "com.palantir.ri.ResourceIdentifier", true)),
            Map.entry("LegacyRid.java", alias("LegacyRid", "com.palantir.ri.ResourceIdentifier", false)),
            Map.entry("StringAlias.java", alias("StringAlias", "String", true)),
            Map.entry("LegacyStringAlias.java", alias("LegacyStringAlias", "String", false)),
            Map.entry("NestedStringAlias.java", alias("NestedStringAlias", "StringAlias", true)),
            Map.entry("LegacyNestedStringAlias.java", alias("LegacyNestedStringAlias", "LegacyStringAlias", false)),
            Map.entry("UuidAlias.java", alias("UuidAlias", "java.util.UUID", true)),
            Map.entry("Bean.java", new String[] {
                "package com.palantir.product;",
                "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;",
                "@JsonDeserialize(builder = Bean.Builder.class)",
                "public final class Bean {",
                "  public static final class Builder {}",
                "}"
            }),
            Map.entry("ConjureEnum.java", new String[] {
                "package com.palantir.product;",
                "import com.fasterxml.jackson.annotation.JsonCreator;",
                "public final class ConjureEnum {",
                "  public enum Value { FOO, UNKNOWN }",
                "  public interface Visitor<T> {}",
                "  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)",
                "  public static ConjureEnum valueOf(String value) { return new ConjureEnum(); }",
                "}"
            }),
            Map.entry("SealedUnion.java", new String[] {
                "package com.palantir.product;",
                "public abstract sealed class SealedUnion permits SealedUnion.Foo {",
                "  public sealed interface Known permits Foo {}",
                "  public static final class Foo extends SealedUnion implements Known {}",
                "}"
            }),
            Map.entry("NotConjure.java", new String[] {
                "package com.palantir.product;",
                "public final class NotConjure {",
                "  public String get() { return \"\"; }",
                "  public static NotConjure of(String value) { return new NotConjure(); }",
                "}"
            }));

    @Test
    void sortedComparableAliasUsesNaturalOrdering() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.product.*;",
                        "import java.util.Comparator;",
                        "import java.util.List;",
                        "class Test {",
                        "  List<ComparableRid> f(List<ComparableRid> in) {",
                        "    return in.stream().sorted(Comparator.comparing(ComparableRid::toString)).toList();",
                        "  }",
                        "  List<StringAlias> g(List<StringAlias> in) {",
                        "    return in.stream().sorted(Comparator.comparing(Object::toString)).toList();",
                        "  }",
                        "  List<NestedStringAlias> h(List<NestedStringAlias> in) {",
                        "    return in.stream().sorted(Comparator.comparing(x -> x.toString())).toList();",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.product.*;",
                        "import java.util.Comparator;",
                        "import java.util.List;",
                        "class Test {",
                        "  List<ComparableRid> f(List<ComparableRid> in) {",
                        "    return in.stream().sorted().toList();",
                        "  }",
                        "  List<StringAlias> g(List<StringAlias> in) {",
                        "    return in.stream().sorted().toList();",
                        "  }",
                        "  List<NestedStringAlias> h(List<NestedStringAlias> in) {",
                        "    return in.stream().sorted().toList();",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void comparatorsOutsideSortedUseTypedNaturalOrder() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.product.*;",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Comparator;",
                        "import java.util.Objects;",
                        "import java.util.TreeSet;",
                        "class Test {",
                        "  void f() {",
                        "    new TreeSet<>(Comparator.comparing(ResourceIdentifier::toString));",
                        "    Comparator<ComparableRid> a = Comparator.comparing(ComparableRid::toString).reversed();",
                        "    Comparator<StringAlias> b = Comparator.comparing(String::valueOf);",
                        "    Comparator<StringAlias> c = Comparator.comparing(Objects::toString);",
                        "    Comparator<StringAlias> d = Comparator.comparing(x -> String.valueOf(x));",
                        "    Comparator<StringAlias> e =",
                        "        Comparator.comparing(StringAlias::get, String.CASE_INSENSITIVE_ORDER)",
                        "            .thenComparing(StringAlias::toString);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.product.*;",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Comparator;",
                        "import java.util.Objects;",
                        "import java.util.TreeSet;",
                        "class Test {",
                        "  void f() {",
                        "    new TreeSet<>(Comparator.<ResourceIdentifier>naturalOrder());",
                        "    Comparator<ComparableRid> a = Comparator.<ComparableRid>naturalOrder().reversed();",
                        "    Comparator<StringAlias> b = Comparator.<StringAlias>naturalOrder();",
                        "    Comparator<StringAlias> c = Comparator.<StringAlias>naturalOrder();",
                        "    Comparator<StringAlias> d = Comparator.<StringAlias>naturalOrder();",
                        "    Comparator<StringAlias> e =",
                        "        Comparator.comparing(StringAlias::get, String.CASE_INSENSITIVE_ORDER)",
                        "            .thenComparing(Comparator.<StringAlias>naturalOrder());",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void aliasesWithoutNaturalOrderingCompareByValue() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.product.*;",
                        "import java.util.Comparator;",
                        "import java.util.List;",
                        "class Test {",
                        "  List<LegacyRid> f(List<LegacyRid> in) {",
                        "    return in.stream().sorted(Comparator.comparing(LegacyRid::toString)).toList();",
                        "  }",
                        "  Comparator<LegacyStringAlias> g() {",
                        "    return Comparator.comparing(Object::toString);",
                        "  }",
                        "  Comparator<StringAlias> h() {",
                        "    return Comparator.comparing(StringAlias::toString, String.CASE_INSENSITIVE_ORDER);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.product.*;",
                        "import java.util.Comparator;",
                        "import java.util.List;",
                        "class Test {",
                        "  List<LegacyRid> f(List<LegacyRid> in) {",
                        "    return in.stream().sorted(Comparator.comparing(LegacyRid::get)).toList();",
                        "  }",
                        "  Comparator<LegacyStringAlias> g() {",
                        "    return Comparator.comparing(LegacyStringAlias::get);",
                        "  }",
                        "  Comparator<StringAlias> h() {",
                        "    return Comparator.comparing(StringAlias::get, String.CASE_INSENSITIVE_ORDER);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void warnsWithoutFixWhenOrderWouldChange() {
        compile(
                        "Test.java",
                        "import com.palantir.product.*;",
                        "import java.util.Comparator;",
                        "class Test {",
                        "  void f() {",
                        "    // BUG: Diagnostic contains: Compare UuidAlias by a field that identifies it",
                        "    Comparator<UuidAlias> a = Comparator.comparing(UuidAlias::toString);",
                        "    // BUG: Diagnostic contains: Compare Bean by a field that identifies it",
                        "    Comparator<Bean> b = Comparator.comparing(Bean::toString);",
                        "    // BUG: Diagnostic contains: Compare ConjureEnum by a field that identifies it",
                        "    Comparator<ConjureEnum> c = Comparator.comparing(ConjureEnum::toString);",
                        "    // BUG: Diagnostic contains: Compare SealedUnion by a field that identifies it",
                        "    Comparator<SealedUnion> d = Comparator.comparing(SealedUnion::toString);",
                        "    // BUG: Diagnostic contains: Compare LegacyNestedStringAlias by a field",
                        "    Comparator<LegacyNestedStringAlias> e = Comparator.comparing(Object::toString);",
                        "    // BUG: Diagnostic contains: Compare ComparableRid by a field that identifies it",
                        "    Comparator<ComparableRid> f = Comparator.comparing(ComparableRid::toString,"
                                + " String.CASE_INSENSITIVE_ORDER);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void ignoresOtherComparators() {
        compile(
                        "Test.java",
                        "import com.palantir.product.*;",
                        "import java.util.Comparator;",
                        "import java.util.List;",
                        "class Test {",
                        "  void f(List<String> strings, List<Object> objects, List<ComparableRid> rids) {",
                        "    strings.stream().sorted(Comparator.comparing(String::toString));",
                        "    objects.stream().sorted(Comparator.comparing(Object::toString));",
                        "    rids.stream().sorted(Comparator.comparing(ComparableRid::get));",
                        "    rids.stream().sorted(Comparator.comparing(rid -> rid.get().toString()));",
                        "    Comparator<NotConjure> a = Comparator.comparing(NotConjure::toString);",
                        "  }",
                        "  <T> Comparator<T> generic() {",
                        "    return Comparator.comparing(Object::toString);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void detectsConjureTypesFromClassFiles() {
        BugCheckerRefactoringTestHelper.newInstance(ConjureToStringOrdering.class, getClass())
                .addInputLines(
                        "Test.java",
                        "import com.palantir.baseline.errorprone.conjuretypes.CompiledStringAlias;",
                        "import java.util.Comparator;",
                        "import java.util.List;",
                        "class Test {",
                        "  List<CompiledStringAlias> f(List<CompiledStringAlias> in) {",
                        "    return in.stream().sorted(Comparator.comparing(CompiledStringAlias::toString)).toList();",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.baseline.errorprone.conjuretypes.CompiledStringAlias;",
                        "import java.util.Comparator;",
                        "import java.util.List;",
                        "class Test {",
                        "  List<CompiledStringAlias> f(List<CompiledStringAlias> in) {",
                        "    return in.stream().sorted().toList();",
                        "  }",
                        "}")
                .doTest();
        CompilationTestHelper.newInstance(ConjureToStringOrdering.class, getClass())
                .addSourceLines(
                        "Test.java",
                        "import com.palantir.baseline.errorprone.conjuretypes.CompiledBean;",
                        "import java.util.Comparator;",
                        "class Test {",
                        "  // BUG: Diagnostic contains: Compare CompiledBean by a field that identifies it",
                        "  Comparator<CompiledBean> c = Comparator.comparing(CompiledBean::toString);",
                        "}")
                .doTest();
    }

    @Test
    void ignoresTestCode() {
        compile(
                        "FooTest.java",
                        "import com.palantir.product.*;",
                        "import java.util.Comparator;",
                        "import org.junit.jupiter.api.Test;",
                        "class FooTest {",
                        "  @Test",
                        "  void f() {",
                        "    Comparator<Bean> b = Comparator.comparing(Bean::toString);",
                        "  }",
                        "}")
                .doTest();
    }

    private static String[] alias(String name, String valueType, boolean comparable) {
        return new String[] {
            "package com.palantir.product;",
            "import com.fasterxml.jackson.annotation.JsonCreator;",
            "import com.fasterxml.jackson.annotation.JsonValue;",
            "public final class " + name + (comparable ? " implements Comparable<" + name + ">" : "") + " {",
            "  private final " + valueType + " value;",
            "  private " + name + "(" + valueType + " value) { this.value = value; }",
            "  @JsonValue",
            "  public " + valueType + " get() { return value; }",
            "  @Override",
            "  public String toString() { return value.toString(); }",
            comparable
                    ? "  @Override public int compareTo(" + name + " other) { return value.compareTo(other.get()); }"
                    : "",
            "  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)",
            "  public static " + name + " of(" + valueType + " value) { return new " + name + "(value); }",
            "}"
        };
    }

    private CompilationTestHelper compile(String path, String... lines) {
        CompilationTestHelper helper = CompilationTestHelper.newInstance(ConjureToStringOrdering.class, getClass());
        for (Map.Entry<String, String[]> type : CONJURE_TYPES.entrySet()) {
            helper = helper.addSourceLines("com/palantir/product/" + type.getKey(), type.getValue());
        }
        return helper.addSourceLines(path, lines);
    }

    private FixStage fix() {
        return new FixStage(BugCheckerRefactoringTestHelper.newInstance(ConjureToStringOrdering.class, getClass()));
    }

    private record FixStage(BugCheckerRefactoringTestHelper helper) {
        BugCheckerRefactoringTestHelper.ExpectOutput addInputLines(String path, String... lines) {
            BugCheckerRefactoringTestHelper withTypes = helper;
            for (Map.Entry<String, String[]> type : CONJURE_TYPES.entrySet()) {
                withTypes = withTypes
                        .addInputLines("com/palantir/product/" + type.getKey(), type.getValue())
                        .expectUnchanged();
            }
            return withTypes.addInputLines(path, lines);
        }
    }
}
