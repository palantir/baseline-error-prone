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
import org.junit.jupiter.api.Test;

public class PreferStreamToListTest {

    @Test
    void collectorsToList_qualified() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  List<String> list = Stream.of(\"hello\").collect(Collectors.toList());",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  List<String> list = Stream.of(\"hello\").toList();",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_staticImport() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Stream;",
                        "import static java.util.stream.Collectors.toList;",
                        "public class Test {",
                        "  List<String> list = Stream.of(\"hello\").collect(toList());",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Stream;",
                        "import static java.util.stream.Collectors.toList;",
                        "public class Test {",
                        "  List<String> list = Stream.of(\"hello\").toList();",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_chained() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "public class Test {",
                        "  List<String> list = List.of(\"hello\").stream()",
                        "      .filter(s -> !s.isEmpty())",
                        "      .collect(Collectors.toList());",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "public class Test {",
                        "  List<String> list = List.of(\"hello\").stream()",
                        "      .filter(s -> !s.isEmpty())",
                        "      .toList();",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_sameGenericType() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  <T> List<T> collect(Stream<T> stream) {",
                        "    return stream.collect(Collectors.toList());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  <T> List<T> collect(Stream<T> stream) {",
                        "    return stream.toList();",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedGenericMapReturnType() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.function.Function;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  <T> List<T> collect(Stream<String> stream, Function<String, ? extends T> mapper) {",
                        "    return stream.map(mapper).collect(Collectors.toList());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.function.Function;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  <T> List<T> collect(Stream<String> stream, Function<String, ? extends T> mapper) {",
                        "    return stream.<T>map(mapper).toList();",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedGenericType_noMatch() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  <T> List<T> collect(Stream<? extends T> stream) {",
                        "    return stream.collect(Collectors.toList());",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedAssignmentType_noMatch() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  List<CharSequence> values = Stream.of(\"hello\").collect(Collectors.toList());",
                        "}")
                .expectUnchanged()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedMapAssignmentType() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values = Stream.of(\"hello\")",
                        "      .map(value -> new Dog())",
                        "      .collect(Collectors.toList());",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values = Stream.of(\"hello\")",
                        "      .<Animal>map(value -> new Dog())",
                        "      .toList();",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedReturnType_noMatch() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  List<CharSequence> values() {",
                        "    return Stream.of(\"hello\").collect(Collectors.toList());",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedMapReturnType() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values(Stream<String> stream) {",
                        "    return stream.map(value -> new Dog()).collect(Collectors.toList());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values(Stream<String> stream) {",
                        "    return stream.<Animal>map(value -> new Dog()).toList();",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedFlatMapReturnType() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values(Stream<String> stream) {",
                        "    return stream.flatMap(value -> Stream.of(new Dog())).collect(Collectors.toList());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values(Stream<String> stream) {",
                        "    return stream.<Animal>flatMap(value -> Stream.of(new Dog())).toList();",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widensTransformBeforeTypePreservingOperations() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values(Stream<String> stream) {",
                        "    return stream.map(value -> new Dog())",
                        "        .filter(value -> true)",
                        "        .limit(10)",
                        "        .collect(Collectors.toList());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values(Stream<String> stream) {",
                        "    return stream.<Animal>map(value -> new Dog())",
                        "        .filter(value -> true)",
                        "        .limit(10)",
                        "        .toList();",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedMapToObjAssignmentType() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.IntStream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values = IntStream.range(0, 10)",
                        "      .mapToObj(value -> new Dog())",
                        "      .collect(Collectors.toList());",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.IntStream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values = IntStream.range(0, 10)",
                        "      .<Animal>mapToObj(value -> new Dog())",
                        "      .toList();",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_widenedMapMultiReturnType() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values(Stream<String> stream) {",
                        "    return stream",
                        "        .<Dog>mapMulti((value, downstream) -> downstream.accept(new Dog()))",
                        "        .collect(Collectors.toList());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  interface Animal {}",
                        "  static final class Dog implements Animal {}",
                        "  List<Animal> values(Stream<String> stream) {",
                        "    return stream",
                        "        .<Animal>mapMulti((value, downstream) -> downstream.accept(new Dog()))",
                        "        .toList();",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToList_covariantReturnType() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  List<? extends CharSequence> values() {",
                        "    return Stream.of(\"hello\").collect(Collectors.toList());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  List<? extends CharSequence> values() {",
                        "    return Stream.of(\"hello\").toList();",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToUnmodifiableList_noMatch() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.List;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  List<String> list = Stream.of(\"hello\").collect(Collectors.toUnmodifiableList());",
                        "}")
                .expectUnchanged()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void collectorsToSet_noMatch() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.Set;",
                        "import java.util.stream.Collectors;",
                        "import java.util.stream.Stream;",
                        "public class Test {",
                        "  Set<String> set = Stream.of(\"hello\").collect(Collectors.toSet());",
                        "}")
                .expectUnchanged()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    private RefactoringValidator fix() {
        return RefactoringValidator.of(PreferStreamToList.class, getClass());
    }
}
