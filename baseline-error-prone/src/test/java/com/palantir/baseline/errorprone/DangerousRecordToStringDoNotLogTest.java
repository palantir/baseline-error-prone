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
class DangerousRecordToStringDoNotLogTest {

    @Test
    void flags_record_with_do_not_log_component() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        // BUG: Diagnostic contains: Record component 'secret' is @DoNotLog
                        public record Test(String name, @DoNotLog String secret) {}
                        """)
                .doTest();
    }

    @Test
    void flags_record_with_do_not_log_type_component() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.tokens.auth.*;
                        // BUG: Diagnostic contains: Record component 'token' is @DoNotLog
                        public record Test(String name, BearerToken token) {}
                        """)
                .doTest();
    }

    @Test
    void flags_record_with_do_not_log_inherited_type() {
        helper().addSourceLines(
                        "Secret.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        @DoNotLog
                        public interface Secret {
                          String value();
                        }
                        """)
                .addSourceLines(
                        "SecretImpl.java",
                        // language=Java
                        """
                        public record SecretImpl(String value) implements Secret {}
                        """)
                .addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        // BUG: Diagnostic contains: Record component 'secret' is @DoNotLog
                        public record Test(String name, SecretImpl secret) {}
                        """)
                .doTest();
    }

    @Test
    void allows_record_with_safe_components() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(@Safe String name, @Safe String value) {}
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void allows_record_with_matching_to_string_override() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(String name, @DoNotLog String secret) {
                            @Override
                            public String toString() {
                                return "Test[name=" + name + ", secret=<redacted>]";
                            }
                        }
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void flags_record_with_non_matching_to_string_override() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        // BUG: Diagnostic contains: Record component 'secret' is @DoNotLog
                        public record Test(String name, @DoNotLog String secret) {
                            @Override
                            public String toString() {
                                return "Test{name=" + name + "}";
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    void allows_record_with_unannotated_components() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        public record Test(String name, int value) {}
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void allows_record_with_unsafe_component() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(@Unsafe String name) {}
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void flags_all_do_not_log_components_when_multiple_exist() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        // BUG: Diagnostic contains: Record component 'secret1' is @DoNotLog
                        // Record component 'secret2' is @DoNotLog
                        public record Test(String name, @DoNotLog String secret1, @DoNotLog String secret2) {}
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
                        @SuppressWarnings("DangerousRecordToStringDoNotLog")
                        public record Test(String name, @DoNotLog String secret) {}
                        """)
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void fix_generates_toString_with_redacted_do_not_log_components() {
        fixHelper()
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(String name, @DoNotLog String secret, int count) {}
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(String name, @DoNotLog String secret, int count) {

                            @Override
                            public String toString() {
                                return "Test[name=" + name + ", secret=<redacted>, count=" + count + "]";
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    void no_fix_when_toString_already_matches_expected_shape() {
        fixHelper()
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(String name, @DoNotLog String secret) {
                            @Override
                            public String toString() {
                                return "Test[name=" + name + ", secret=<redacted>]";
                            }
                        }
                        """)
                .expectUnchanged()
                .doTest();
    }

    @Test
    void fix_replaces_non_matching_to_string() {
        fixHelper()
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(String name, @DoNotLog String secret) {
                            @Override
                            public String toString() {
                                return "Test{name=" + name + "}";
                            }
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(String name, @DoNotLog String secret) {
                            @Override
                            public String toString() {
                                return "Test[name=" + name + ", secret=<redacted>]";
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    void fix_generates_toString_with_only_redacted_components_when_all_are_do_not_log() {
        fixHelper()
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(@DoNotLog String secret) {}
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.*;
                        public record Test(@DoNotLog String secret) {

                            @Override
                            public String toString() {
                                return "Test[secret=<redacted>]";
                            }
                        }
                        """)
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(DangerousRecordToStringDoNotLog.class, getClass());
    }

    private BugCheckerRefactoringTestHelper fixHelper() {
        return BugCheckerRefactoringTestHelper.newInstance(DangerousRecordToStringDoNotLog.class, getClass());
    }
}
