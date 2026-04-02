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
import org.junit.jupiter.api.Test;

@SuppressWarnings("MisformattedTestData")
class DangerousImmutablesToStringDoNotLogTest {

    @Test
    void flags_immutables_with_do_not_log_attribute() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          // BUG: Diagnostic contains: Immutables types with @DoNotLog attributes must either override toString()
                          @DoNotLog String secret();
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_immutables_with_do_not_log_type_attribute() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.tokens.auth.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          // BUG: Diagnostic contains: Immutables types with @DoNotLog attributes must either override toString()
                          BearerToken token();
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_immutables_value_default_with_do_not_log() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          @Value.Default
                          // BUG: Diagnostic contains: Immutables types with @DoNotLog attributes must either override toString()
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_immutables_value_derived_with_do_not_log() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          @Value.Derived
                          // BUG: Diagnostic contains: Immutables types with @DoNotLog attributes must either override toString()
                          String secret() {
                            return name();
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void allows_immutables_with_do_not_log_redacted_attribute() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @DoNotLog @Value.Redacted String secret();
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void allows_immutables_with_safe_attributes() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          @Safe String name();
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void allows_immutables_with_do_not_log_and_to_string_override() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class Test {
                          abstract String name();
                          @DoNotLog abstract String secret();
                          @Override
                          public String toString() {
                            return "Test{name=" + name() + "}";
                          }
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void flags_immutables_value_lazy_with_do_not_log() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          @Value.Lazy
                          // BUG: Diagnostic contains: Immutables types with @DoNotLog attributes must either override toString()
                          String secret() {
                            return name();
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_immutables_default_as_default_style_with_do_not_log() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @Value.Style(defaultAsDefault = true)
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          // BUG: Diagnostic contains: Immutables types with @DoNotLog attributes must either override toString()
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_immutables_jackson_annotated_field_with_do_not_log() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.annotation.*;
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          @JsonProperty
                          // BUG: Diagnostic contains: Immutables types with @DoNotLog attributes must either override toString()
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(DangerousImmutablesToStringDoNotLog.class, getClass());
    }
}
