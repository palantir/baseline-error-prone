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
