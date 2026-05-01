/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

import org.junit.jupiter.api.Test;

final class ResourceIdentifierGetEqualsUsageTest {
    @Test
    void testHasService() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasService(\"test\");",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.getService().equals(\"test\");",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasService(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return \"test\".equals(rid.getService());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasService(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.getService().equals(CONSTANT);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasService(CONSTANT);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testHasInstance() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasInstance(\"test\");",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.getInstance().equals(\"test\");",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasInstance(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return \"test\".equals(rid.getInstance());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasInstance(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.getInstance().equals(CONSTANT);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasInstance(CONSTANT);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testHasType() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasType(\"test\");",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.getType().equals(\"test\");",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasType(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return \"test\".equals(rid.getType());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasType(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.getType().equals(CONSTANT);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasType(CONSTANT);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testHasLocator() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasLocator(\"test\");",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.getLocator().equals(\"test\");",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasLocator(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return \"test\".equals(rid.getLocator());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasLocator(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.getLocator().equals(CONSTANT);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasLocator(CONSTANT);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testObjectsEqualsHasService() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(rid.getService(), \"test\");",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasService(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(\"test\", rid.getService());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasService(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(rid.getService(), CONSTANT);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasService(CONSTANT);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testObjectsEqualsHasInstance() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(rid.getInstance(), \"test\");",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasInstance(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(\"test\", rid.getInstance());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasInstance(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(rid.getInstance(), CONSTANT);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasInstance(CONSTANT);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testObjectsEqualsHasType() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(rid.getType(), \"test\");",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasType(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(\"test\", rid.getType());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasType(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(rid.getType(), CONSTANT);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasType(CONSTANT);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testObjectsEqualsHasLocator() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(rid.getLocator(), \"test\");",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasLocator(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(\"test\", rid.getLocator());",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasLocator(\"test\");",
                        "  }",
                        "}")
                .doTest();

        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return Objects.equals(rid.getLocator(), CONSTANT);",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  private static final String CONSTANT = \"FOO\";",
                        "  boolean f(ResourceIdentifier rid) {",
                        "    return rid.hasLocator(CONSTANT);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    void testObjectsEqualsBothRids() {
        fix().addInputLines(
                        "Test.java",
                        "import com.palantir.ri.ResourceIdentifier;",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(ResourceIdentifier a, ResourceIdentifier b) {",
                        "    return Objects.equals(a.getService(), b.getService());",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest();
    }

    @Test
    void testObjectsEqualsUnrelated() {
        fix().addInputLines(
                        "Test.java",
                        "import java.util.Objects;",
                        "public class Test {",
                        "  boolean f(String a, String b) {",
                        "    return Objects.equals(a, b);",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest();
    }

    private RefactoringValidator fix() {
        return RefactoringValidator.of(ResourceIdentifierGetEqualsUsage.class, getClass());
    }
}
