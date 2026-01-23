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

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class ArgContainingArgsTest {

    private CompilationTestHelper compilationHelper;

    @BeforeEach
    public void before() {
        compilationHelper = CompilationTestHelper.newInstance(ArgContainingArgs.class, getClass());
    }

    @Test
    public void fails_when_safearg_wraps_safearg() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "class Bean {",
                        "  public SafeArg<?> foo() {",
                        "    // BUG: Diagnostic contains: Do not wrap an Arg inside a SafeArg",
                        "    return SafeArg.of(\"outer\", SafeArg.of(\"inner\", \"value\"));",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void fails_when_safearg_wraps_unsafearg() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "import com.palantir.logsafe.UnsafeArg;",
                        "class Bean {",
                        "  public SafeArg<?> foo() {",
                        "    // BUG: Diagnostic contains: Do not wrap an Arg inside a SafeArg",
                        "    return SafeArg.of(\"outer\", UnsafeArg.of(\"inner\", \"value\"));",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void fails_when_safearg_wraps_arg_variable() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.Arg;",
                        "import com.palantir.logsafe.SafeArg;",
                        "class Bean {",
                        "  public SafeArg<?> foo(Arg<?> arg) {",
                        "    // BUG: Diagnostic contains: Do not wrap an Arg inside a SafeArg",
                        "    return SafeArg.of(\"outer\", arg);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void fails_when_safearg_wraps_list_of_args() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "import java.util.List;",
                        "class Bean {",
                        "  public SafeArg<?> foo(List<SafeArg<?>> args) {",
                        "    // BUG: Diagnostic contains: Do not wrap a Collection of Args inside a SafeArg",
                        "    return SafeArg.of(\"outer\", args);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void fails_when_safearg_wraps_collection_of_args() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.Arg;",
                        "import com.palantir.logsafe.SafeArg;",
                        "import java.util.Collection;",
                        "class Bean {",
                        "  public SafeArg<?> foo(Collection<Arg<?>> args) {",
                        "    // BUG: Diagnostic contains: Do not wrap a Collection of Args inside a SafeArg",
                        "    return SafeArg.of(\"outer\", args);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void fails_when_safearg_wraps_set_of_args() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "import java.util.Set;",
                        "class Bean {",
                        "  public SafeArg<?> foo(Set<SafeArg<String>> args) {",
                        "    // BUG: Diagnostic contains: Do not wrap a Collection of Args inside a SafeArg",
                        "    return SafeArg.of(\"outer\", args);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void allows_safearg_with_string_value() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "class Bean {",
                        "  public SafeArg<?> foo() {",
                        "    return SafeArg.of(\"key\", \"value\");",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void allows_safearg_with_integer_value() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "class Bean {",
                        "  public SafeArg<?> foo() {",
                        "    return SafeArg.of(\"count\", 42);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void allows_safearg_with_null_value() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "class Bean {",
                        "  public SafeArg<?> foo() {",
                        "    return SafeArg.of(\"key\", null);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void allows_safearg_with_object_value() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "class Bean {",
                        "  public SafeArg<?> foo(Object obj) {",
                        "    return SafeArg.of(\"key\", obj);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void allows_safearg_with_list_of_strings() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "import java.util.List;",
                        "class Bean {",
                        "  public SafeArg<?> foo(List<String> items) {",
                        "    return SafeArg.of(\"items\", items);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void allows_safearg_with_collection_of_integers() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "import java.util.Collection;",
                        "class Bean {",
                        "  public SafeArg<?> foo(Collection<Integer> items) {",
                        "    return SafeArg.of(\"items\", items);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void allows_safearg_with_raw_collection() {
        compilationHelper
                .addSourceLines(
                        "Bean.java",
                        "import com.palantir.logsafe.SafeArg;",
                        "import java.util.Collection;",
                        "class Bean {",
                        "  @SuppressWarnings(\"rawtypes\")",
                        "  public SafeArg<?> foo(Collection items) {",
                        "    return SafeArg.of(\"items\", items);",
                        "  }",
                        "}")
                .doTest();
    }
}
