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

class LogsafeArrayArgTest {

    @SuppressWarnings("for-rollout:MisformattedTestData")
    @Test
    void testNormalUsage() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.SafeArg;
                        class Test {
                            void f() {
                                SafeArg.of("name", "string");
                            }
                        }
                        """)
                .doTest();
    }

    @SuppressWarnings("for-rollout:MisformattedTestData")
    @Test
    void testFixedUsage() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.SafeArg;
                        import java.util.Arrays;
                        class Test {
                            void f() {
                                String[] array = new String[] {"string"};
                                SafeArg.of("name", Arrays.asList(array));
                            }
                        }
                        """)
                .doTest();
    }

    @SuppressWarnings("for-rollout:MisformattedTestData")
    @Test
    void testSafe() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.SafeArg;
                        class Test {
                            void f() {
                                String[] arg = new String[] {"string"};
                                // BUG: Diagnostic contains: Arrays should not be logged
                                SafeArg.of("name", arg);
                            }
                        }
                        """)
                .doTest();
    }

    @SuppressWarnings("for-rollout:MisformattedTestData")
    @Test
    void testUnsafe() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.UnsafeArg;
                        class Test {
                            void f() {
                                String[] arg = new String[] {"string"};
                                // BUG: Diagnostic contains: Arrays should not be logged
                                UnsafeArg.of("name", arg);
                            }
                        }
                        """)
                .doTest();
    }

    @SuppressWarnings("for-rollout:MisformattedTestData")
    @Test
    void testPrimitive() {
        helper().addSourceLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.UnsafeArg;
                        class Test {
                            void f() {
                                int[] arg = new int[] {1};
                                // BUG: Diagnostic contains: Arrays should not be logged
                                UnsafeArg.of("name", arg);
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    void testRewrite() {
        RefactoringValidator.of(LogsafeArrayArg.class, getClass())
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.SafeArg;
                        class Test {
                            void f() {
                                String[] arg = new String[] {"string"};
                                SafeArg.of("name", arg);
                            }
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.SafeArg;
                        import java.util.Arrays;
                        class Test {
                            void f() {
                                String[] arg = new String[] {"string"};
                                SafeArg.of("name", Arrays.asList(arg));
                            }
                        }
                        """)
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testRewriteInline() {
        RefactoringValidator.of(LogsafeArrayArg.class, getClass())
                .addInputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.SafeArg;
                        class Test {
                            void f() {
                                SafeArg.of("name", new String[] {"string"});
                            }
                        }
                        """)
                .addOutputLines(
                        "Test.java",
                        // language=Java
                        """
                        import com.palantir.logsafe.SafeArg;
                        import java.util.Arrays;
                        class Test {
                            void f() {
                                SafeArg.of("name", Arrays.asList(new String[] {"string"}));
                            }
                        }
                        """)
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(LogsafeArrayArg.class, getClass());
    }
}
