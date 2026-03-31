/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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

class DangerousToStringDoNotLogTest {

    @Test
    void testToStringConcatenatingDoNotLogField() {
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.logsafe.*;",
                        "public final class Test {",
                        "  @DoNotLog private String secret;",
                        "  @Override",
                        "  // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data",
                        "  public String toString() {",
                        "    return \"Test{secret=\" + secret + \"}\";",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testToStringWithStringFormat() {
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.logsafe.*;",
                        "public final class Test {",
                        "  @DoNotLog private String secret;",
                        "  @Override",
                        "  // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data",
                        "  public String toString() {",
                        "    return String.format(\"Test{secret=%s}\", secret);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testToStringCallingDoNotLogMethod() {
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.logsafe.*;",
                        "public abstract class Test {",
                        "  @DoNotLog abstract String token();",
                        "  @Override",
                        "  // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data",
                        "  public String toString() {",
                        "    return \"Test\" + token();",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testToStringWithStringBuilder() {
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.logsafe.*;",
                        "public final class Test {",
                        "  @DoNotLog private String secret;",
                        "  @Override",
                        "  // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data",
                        "  public String toString() {",
                        "    return new StringBuilder(\"Test{\").append(secret).append(\"}\").toString();",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testToStringWithDoNotLogType() {
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.tokens.auth.*;",
                        "public final class Test {",
                        "  private BearerToken token;",
                        "  @Override",
                        "  // BUG: Diagnostic contains: toString() methods must not include @DoNotLog data",
                        "  public String toString() {",
                        "    return \"Test{token=\" + token + \"}\";",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testToStringWithSafeFieldsOnly() {
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.logsafe.*;",
                        "public final class Test {",
                        "  @Safe private String name;",
                        "  @Override",
                        "  public String toString() {",
                        "    return \"Test{name=\" + name + \"}\";",
                        "  }",
                        "}")
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void testToStringReturningConstant() {
        helper().addSourceLines(
                        "Test.java",
                        "public final class Test {",
                        "  @Override",
                        "  public String toString() {",
                        "    return \"Test{}\";",
                        "  }",
                        "}")
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void testToStringWithUnsafeField() {
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.logsafe.*;",
                        "public final class Test {",
                        "  @Unsafe private String name;",
                        "  @Override",
                        "  public String toString() {",
                        "    return \"Test{name=\" + name + \"}\";",
                        "  }",
                        "}")
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void testNonToStringMethodReturningDoNotLog() {
        // This is handled by SafeLoggingPropagation, not this check
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.logsafe.*;",
                        "public final class Test {",
                        "  @DoNotLog private String secret;",
                        "  public String getSecret() {",
                        "    return secret;",
                        "  }",
                        "}")
                .expectNoDiagnostics()
                .doTest();
    }

    @Test
    void testAbstractToString() {
        helper().addSourceLines(
                        "Test.java",
                        "public abstract class Test {",
                        "  @Override",
                        "  public abstract String toString();",
                        "}")
                .expectNoDiagnostics()
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(DangerousToStringDoNotLog.class, getClass());
    }
}
