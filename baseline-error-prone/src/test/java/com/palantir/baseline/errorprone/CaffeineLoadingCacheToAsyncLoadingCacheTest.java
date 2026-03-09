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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class CaffeineLoadingCacheToAsyncLoadingCacheTest {

    private final CompilationTestHelper testHelper =
            CompilationTestHelper.newInstance(CaffeineLoadingCacheToAsyncLoadingCache.class, getClass());

    private RefactoringValidator fix() {
        return RefactoringValidator.of(CaffeineLoadingCacheToAsyncLoadingCache.class, getClass());
    }

    @Nested
    class Refactoring {

        @Test
        void simple_loading_cache_with_executor_field() {
            fix().addInputLines(
                            "Test.java",
                            "import com.github.benmanes.caffeine.cache.Caffeine;",
                            "import com.github.benmanes.caffeine.cache.LoadingCache;",
                            "import com.palantir.cache.ExecutorFactory;",
                            "class Test {",
                            "  private final ExecutorFactory executorFactory = null;",
                            "  LoadingCache<String, String> cache = Caffeine.newBuilder()",
                            "      .maximumSize(100)",
                            "      .build(this::load);",
                            "  private String load(String key) { return key; }",
                            "}")
                    .addOutputLines(
                            "Test.java",
                            "import com.palantir.cache.AsyncLoadingCache;",
                            "import com.palantir.cache.Cache;",
                            "import com.palantir.cache.ExecutorFactory;",
                            "class Test {",
                            "  private final ExecutorFactory executorFactory = null;",
                            "  AsyncLoadingCache<String, String> cache ="
                                + " Cache.<String, String>builder().name(\"test\").maximumSize(100).noExpiry()"
                                + ".noMetrics().executor(executorFactory).buildAsyncWithLoader(this::load);",
                            "  private String load(String key) { return key; }",
                            "}")
                    .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
        }

        @Test
        void with_expire_after_write_duration() {
            fix().addInputLines(
                            "Test.java",
                            "import com.github.benmanes.caffeine.cache.Caffeine;",
                            "import com.github.benmanes.caffeine.cache.LoadingCache;",
                            "import com.palantir.cache.ExecutorFactory;",
                            "import java.time.Duration;",
                            "class Test {",
                            "  private final ExecutorFactory executorFactory = null;",
                            "  LoadingCache<String, String> cache = Caffeine.newBuilder()",
                            "      .maximumSize(100)",
                            "      .expireAfterWrite(Duration.ofMinutes(5))",
                            "      .build(this::load);",
                            "  private String load(String key) { return key; }",
                            "}")
                    .addOutputLines(
                            "Test.java",
                            "import com.palantir.cache.AsyncLoadingCache;",
                            "import com.palantir.cache.Cache;",
                            "import com.palantir.cache.ExecutorFactory;",
                            "import com.palantir.cache.Expiry;",
                            "import java.time.Duration;",
                            "class Test {",
                            "  private final ExecutorFactory executorFactory = null;",
                            "  AsyncLoadingCache<String, String> cache ="
                                + " Cache.<String, String>builder().name(\"test\").maximumSize(100)"
                                + ".expiry(Expiry.afterWrite(Duration.ofMinutes(5))).noMetrics()"
                                + ".executor(executorFactory).buildAsyncWithLoader(this::load);",
                            "  private String load(String key) { return key; }",
                            "}")
                    .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
        }

        @Test
        void adds_maximum_size_when_not_present() {
            fix().addInputLines(
                            "Test.java",
                            "import com.github.benmanes.caffeine.cache.Caffeine;",
                            "import com.github.benmanes.caffeine.cache.LoadingCache;",
                            "import com.palantir.cache.ExecutorFactory;",
                            "class Test {",
                            "  private final ExecutorFactory executorFactory = null;",
                            "  LoadingCache<String, String> cache = Caffeine.newBuilder()",
                            "      .build(this::load);",
                            "  private String load(String key) { return key; }",
                            "}")
                    .addOutputLines(
                            "Test.java",
                            "import com.palantir.cache.AsyncLoadingCache;",
                            "import com.palantir.cache.Cache;",
                            "import com.palantir.cache.ExecutorFactory;",
                            "class Test {",
                            "  private final ExecutorFactory executorFactory = null;",
                            "  AsyncLoadingCache<String, String> cache ="
                                    + " Cache.<String, String>builder().name(\"test\").maximumSize(Long.MAX_VALUE).noExpiry()"
                                    + ".noMetrics().executor(executorFactory).buildAsyncWithLoader(this::load);",
                            "  private String load(String key) { return key; }",
                            "}")
                    .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
        }
    }

    @Nested
    class Negative {

        @Test
        void no_executor_in_scope() {
            testHelper
                    .addSourceLines(
                            "Test.java",
                            "import com.github.benmanes.caffeine.cache.Caffeine;",
                            "import com.github.benmanes.caffeine.cache.LoadingCache;",
                            "class Test {",
                            "  LoadingCache<String, String> cache = Caffeine.newBuilder()",
                            "      .maximumSize(100)",
                            "      .build(this::load);",
                            "  private String load(String key) { return key; }",
                            "}")
                    .doTest();
        }

        @Test
        void no_maximum_size() {
            testHelper
                    .addSourceLines(
                            "Test.java",
                            "import com.github.benmanes.caffeine.cache.Caffeine;",
                            "import com.github.benmanes.caffeine.cache.LoadingCache;",
                            "class Test {",
                            "  private LoadingCache<String, String> cache;",
                            "  void setup() {",
                            "    this.cache = Caffeine.newBuilder().build(this::load);",
                            "  }",
                            "  private String load(String key) { return key; }",
                            "}")
                    .doTest();
        }

        @Test
        void not_a_loading_cache() {
            testHelper
                    .addSourceLines(
                            "Test.java",
                            "import com.github.benmanes.caffeine.cache.Cache;",
                            "import com.github.benmanes.caffeine.cache.Caffeine;",
                            "class Test {",
                            "  private Cache<String, String> cache;",
                            "  void setup() {",
                            "    this.cache = Caffeine.newBuilder().maximumSize(100).build();",
                            "  }",
                            "}")
                    .doTest();
        }

        @Test
        void disallowed_builder_method() {
            testHelper
                    .addSourceLines(
                            "Test.java",
                            "import com.github.benmanes.caffeine.cache.Caffeine;",
                            "import com.github.benmanes.caffeine.cache.LoadingCache;",
                            "import com.github.benmanes.caffeine.cache.RemovalListener;",
                            "class Test {",
                            "  private LoadingCache<String, String> cache;",
                            "  void setup() {",
                            "    this.cache = Caffeine.newBuilder()",
                            "        .maximumSize(100)",
                            "        .removalListener((k, v, cause) -> {})",
                            "        .build(this::load);",
                            "  }",
                            "  private String load(String key) { return key; }",
                            "}")
                    .doTest();
        }
    }
}
