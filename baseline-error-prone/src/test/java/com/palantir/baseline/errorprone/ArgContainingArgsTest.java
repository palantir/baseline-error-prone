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
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class ArgContainingArgsTest {

    private CompilationTestHelper compilationHelper;

    @BeforeEach
    public void before() {
        compilationHelper = CompilationTestHelper.newInstance(ArgContainingArgs.class, getClass());
    }

    @Test
    public void bans_safe_arg_of_safe_arg() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            class Bean {
              public SafeArg<?> foo() {
                // BUG: Diagnostic contains: Do not wrap an Arg inside a SafeArg
                return SafeArg.of("outer", SafeArg.of("inner", "value"));
              }
            }
            """);
    }

    @Test
    public void bans_safe_arg_of_unsafe_arg() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            import com.palantir.logsafe.UnsafeArg;
            class Bean {
              public SafeArg<?> foo() {
                // BUG: Diagnostic contains: Do not wrap an Arg inside a SafeArg
                return SafeArg.of("outer", UnsafeArg.of("inner", "value"));
              }
            }
            """);
    }

    @Test
    public void bans_unsafe_arg_of_unsafe_arg() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            import com.palantir.logsafe.UnsafeArg;
            class Bean {
              public UnsafeArg<?> foo() {
                // BUG: Diagnostic contains: Do not wrap an Arg inside a SafeArg
                return UnsafeArg.of("outer", UnsafeArg.of("inner", "value"));
              }
            }
            """);
    }

    @Test
    public void bans_safe_arg_of_arg_variable() {
        runTest("""
            import com.palantir.logsafe.Arg;
            import com.palantir.logsafe.SafeArg;
            class Bean {
              public SafeArg<?> foo(Arg<?> arg) {
                // BUG: Diagnostic contains: Do not wrap an Arg inside a SafeArg
                return SafeArg.of("outer", arg);
              }
            }
            """);
    }

    @Test
    public void bans_safe_arg_of_arg_method_return_value() {
        runTest("""
            import com.palantir.logsafe.Arg;
            import com.palantir.logsafe.SafeArg;
            class Bean {
              public SafeArg<?> foo() {
                // BUG: Diagnostic contains: Do not wrap an Arg inside a SafeArg
                return SafeArg.of("outer", wrapWithArg("test"));
              }
              private Arg<String> wrapWithArg(String value) {
                return SafeArg.of("value", value);
              }
            }
            """);
    }

    @Test
    public void bans_safe_arg_wrapping_list_of_safe_args() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            import java.util.List;
            class Bean {
              public SafeArg<?> foo(List<SafeArg<?>> args) {
                // BUG: Diagnostic contains: Do not wrap a Collection/Iterable of Args inside a SafeArg
                return SafeArg.of("outer", args);
              }
            }
            """);
    }

    @Test
    public void bans_safe_arg_wrapping_iterable_of_args() {
        runTest("""
            import com.palantir.logsafe.Arg;
            import com.palantir.logsafe.SafeArg;
            class Bean {
              public SafeArg<?> foo(Iterable<Arg<?>> args) {
                // BUG: Diagnostic contains: Do not wrap a Collection/Iterable of Args inside a SafeArg
                return SafeArg.of("outer", args);
              }
            }
            """);
    }

    @Test
    public void allows_safe_arg_of_string() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            class Bean {
              public SafeArg<?> foo() {
                return SafeArg.of("key", "value");
              }
            }
            """);
    }

    @Test
    public void allows_safe_arg_of_null_literal() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            class Bean {
              public SafeArg<?> foo() {
                return SafeArg.of("key", null);
              }
            }
            """);
    }

    @Test
    public void allows_safe_arg_of_object() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            class Bean {
              public SafeArg<?> foo(Object obj) {
                return SafeArg.of("key", obj);
              }
            }
            """);
    }

    @Test
    public void allows_safe_arg_of_list_of_strings() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            import java.util.List;
            class Bean {
              public SafeArg<?> foo(List<String> items) {
                return SafeArg.of("items", items);
              }
            }
            """);
    }

    @Test
    public void allows_safearg_with_raw_collection() {
        runTest("""
            import com.palantir.logsafe.SafeArg;
            import java.util.Collection;
            class Bean {
              @SuppressWarnings("rawtypes")
              public SafeArg<?> foo(Collection items) {
                return SafeArg.of("items", items);
              }
            }
            """);
    }

    private void runTest(@Language("Java") String code) {
        compilationHelper.addSourceLines("Bean.java", code).doTest();
    }
}
