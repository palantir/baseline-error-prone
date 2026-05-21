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
                        // BUG: Diagnostic contains: Attribute 'secret()' is @DoNotLog
                        interface Test {
                          String name();
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
                        // BUG: Diagnostic contains: Attribute 'token()' is @DoNotLog
                        interface Test {
                          String name();
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
                        // BUG: Diagnostic contains: Attribute 'secret()' is @DoNotLog
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          @Value.Default
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
                        // BUG: Diagnostic contains: Attribute 'secret()' is @DoNotLog
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          @Value.Derived
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
    void allows_immutables_with_do_not_log_lazy_attribute() {
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
                          String secret() {
                            return name();
                          }
                        }
                        """)
                .expectNoDiagnostics()
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
                        // BUG: Diagnostic contains: Attribute 'secret()' is @DoNotLog
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
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
                        // BUG: Diagnostic contains: Attribute 'secret()' is @DoNotLog
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          @JsonProperty
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void allows_immutables_with_do_not_log_auxiliary_attribute() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @DoNotLog @Value.Auxiliary String secret();
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void flags_all_do_not_log_attributes_when_multiple_exist() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        // BUG: Diagnostic contains: Attribute 'secret1()' is @DoNotLog
                        // Attribute 'secret2()' is @DoNotLog
                        interface Test {
                          String name();
                          @DoNotLog String secret1();
                          @DoNotLog String secret2();
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_immutables_json_value_annotated_field_with_do_not_log() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.annotation.*;
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        // BUG: Diagnostic contains: Attribute 'secret()' is @DoNotLog
                        abstract class Test {
                          abstract String name();
                          @DoNotLog
                          @JsonValue
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void suppression_on_class_suppresses_check() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @SuppressWarnings("DangerousImmutablesToStringDoNotLog")
                        @Value.Immutable
                        interface Test {
                          String name();
                          @DoNotLog String secret();
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void reports_one_diagnostic_per_do_not_log_attribute() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        import org.immutables.value.Value.Redacted;
                        @Value.Immutable
                        // BUG: Diagnostic contains: Attribute 'secret1()' is @DoNotLog
                        // Attribute 'secret3()' is @DoNotLog
                        interface Test {
                          String name();
                          @DoNotLog String secret1();
                          @Redacted @DoNotLog String secret2();
                          @DoNotLog String secret3();
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_adds_redacted_to_do_not_log_attribute() {
        fixHelper()
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @DoNotLog String secret();
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @Value.Redacted
                          @DoNotLog String secret();
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_adds_redacted_to_do_not_log_type_attribute() {
        fixHelper()
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.tokens.auth.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          BearerToken token();
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.tokens.auth.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @Value.Redacted
                          BearerToken token();
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_adds_redacted_to_value_default_method() {
        fixHelper()
                .addInputLines(
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
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class Test {
                          abstract String name();
                          @Value.Redacted
                          @DoNotLog
                          @Value.Default
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_adds_redacted_to_value_derived_method() {
        fixHelper()
                .addInputLines(
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
                          String secret() {
                            return name();
                          }
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class Test {
                          abstract String name();
                          @Value.Redacted
                          @DoNotLog
                          @Value.Derived
                          String secret() {
                            return name();
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_adds_redacted_to_jackson_annotated_method() {
        fixHelper()
                .addInputLines(
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
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.annotation.*;
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class Test {
                          abstract String name();
                          @Value.Redacted
                          @DoNotLog
                          @JsonProperty
                          String secret() {
                            return "default";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_adds_redacted_to_each_do_not_log_attribute() {
        fixHelper()
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @DoNotLog String secret1();
                          @DoNotLog String secret2();
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @Value.Redacted
                          @DoNotLog String secret1();
                          @Value.Redacted
                          @DoNotLog String secret2();
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_leaves_safe_attributes_unchanged() {
        fixHelper()
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @DoNotLog String secret();
                          int count();
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface Test {
                          String name();
                          @Value.Redacted
                          @DoNotLog String secret();
                          int count();
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_no_change_when_already_redacted() {
        fixHelper()
                .addInputLines(
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
                .expectUnchanged()
                .doTest();
    }

    @Test
    void fix_no_change_when_custom_to_string_present() {
        fixHelper()
                .addInputLines(
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
                .expectUnchanged()
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(DangerousImmutablesToStringDoNotLog.class, getClass());
    }

    private BugCheckerRefactoringTestHelper fixHelper() {
        return BugCheckerRefactoringTestHelper.newInstance(DangerousImmutablesToStringDoNotLog.class, getClass());
    }
}
