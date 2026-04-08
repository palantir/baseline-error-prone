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
class DangerousToStringDoNotLogTest {

    @Test
    void flags_string_concatenating_do_not_log_field() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public final class Test {
                          @DoNotLog private String secret;
                          @Override
                          // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data
                          public String toString() {
                            return "Test{secret=" + secret + "}";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_string_format_with_do_not_log_field() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public final class Test {
                          @DoNotLog private String secret;
                          @Override
                          // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data
                          public String toString() {
                            return String.format("Test{secret=%s}", secret);
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_calling_do_not_log_method() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public abstract class Test {
                          @DoNotLog abstract String token();
                          @Override
                          // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data
                          public String toString() {
                            return "Test" + token();
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_string_builder_with_do_not_log_field() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public final class Test {
                          @DoNotLog private String secret;
                          @Override
                          // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data
                          public String toString() {
                            return new StringBuilder("Test{").append(secret).append("}").toString();
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_do_not_log_type() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.tokens.auth.*;
                        public final class Test {
                          private BearerToken token;
                          @Override
                          // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data
                          public String toString() {
                            return "Test{token=" + token + "}";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_to_string_calling_interface_default_method_returning_do_not_log() {
        helper().addSourceLines(
                        "Secret.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public interface Secret {
                          @DoNotLog
                          default String secret() {
                            return "secret";
                          }
                        }
                        """)
                .addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        public final class Test implements Secret {
                          @Override
                          // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data
                          public String toString() {
                            return "Test{secret=" + secret() + "}";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void flags_do_not_log_annotated_to_string_without_do_not_log_data() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public final class Test {
                          private String name;
                          @DoNotLog
                          @Override
                          // BUG: Diagnostic contains: toString() should not return DoNotLog data
                          public String toString() {
                            return "Test{name=" + name + "}";
                          }
                        }
                        """)
                .doTest();
    }

    @Test
    void allows_safe_fields_only() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public final class Test {
                          @Safe private String name;
                          @Override
                          public String toString() {
                            return "Test{name=" + name + "}";
                          }
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void allows_constant_return() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        public final class Test {
                          @Override
                          public String toString() {
                            return "Test{}";
                          }
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void allows_unsafe_field() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public final class Test {
                          @Unsafe private String name;
                          @Override
                          public String toString() {
                            return "Test{name=" + name + "}";
                          }
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void allows_abstract_to_string() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        public abstract class Test {
                          @Override
                          public abstract String toString();
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void flags_record_with_do_not_log_component_and_to_string_override_including_secret() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(String name, @DoNotLog String secret) {
                          @Override
                          // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data
                          public String toString() {
                            return "Test{name=" + name + ", secret=" + secret + "}";
                          }
                        }
                        """)
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(DangerousToStringDoNotLog.class, getClass());
    }
}
