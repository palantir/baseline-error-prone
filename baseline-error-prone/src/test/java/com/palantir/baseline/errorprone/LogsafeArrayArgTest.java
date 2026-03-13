/*
 * (c) Copyright 2022 Palantir Technologies Inc. All rights reserved.
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

class LogsafeArrayArgTest {

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

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(LogsafeArrayArg.class, getClass());
    }
}
