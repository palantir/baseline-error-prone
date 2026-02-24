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

package com.palantir.gradle.baselineerrorprone;

import static com.palantir.gradle.testing.assertion.GradlePluginTestAssertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.files.gradle.GradleFile;
import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * This test depends on ./gradlew :baseline-error-prone:publishToMavenLocal
 */
@GradlePluginTests
@DisabledConfigurationCache
class BaselineErrorProneIntegrationTest {

    // ***DELINEATOR FOR REVIEW: standardBuildFile
    GradleFile standardBuildFile(RootProject project) {
        project.buildGradle().plugins().add("java");
        project.buildGradle().plugins().add("com.palantir.baseline-error-prone");

        return project.buildGradle().append("""
            repositories {
                mavenLocal()
                // TODO(forozco): figure out why pTML no longer works - same below
                mavenCentral()
            }
            """);
    }

    GradleFile javaProjectBuildFile(SubProject project) {
        project.buildGradle().plugins().add("java");
        project.buildGradle().plugins().add("com.palantir.baseline-error-prone");

        return project.buildGradle().append("""
            repositories {
                mavenLocal()
                mavenCentral()
            }
            """);
    }

    GradleFile javaLibraryProjectBuildFile(SubProject project) {
        project.buildGradle().plugins().add("java-library");
        project.buildGradle().plugins().add("com.palantir.baseline-error-prone");

        return project.buildGradle().append("""
            repositories {
                mavenLocal()
                mavenCentral()
            }
            """);
    }

    // ***DELINEATOR FOR REVIEW: validJavaFile
    private static final String validJavaFile = """
        package test;
        public class Test { void test() {} }
        """;

    // ***DELINEATOR FOR REVIEW: invalidJavaFile
    private static final String invalidJavaFile = """
        package test;
        import java.util.Optional;
        public class Test {
            void test() {
                int[] a = {1, 2, 3};
                int[] b = {1, 2, 3};
                if (a.equals(b)) {
                  System.out.println("arrays are equal!");
                  Optional.of("hello").orElse(System.getProperty("world"));
                }
            }
        }
        """;

    @Language("Java")
    private static final String javaFileWithDeprecations = """
        package test;
        @Deprecated(forRemoval = true)
        public class DeprecatedClass {
            @Deprecated(forRemoval = true)
            static void deprecated() {}

            // Testing nested classes too
            @Deprecated(forRemoval = true)
            public static class Inner {}
        }
        """;

    @Language("Java")
    private static final String javaFileUsingDeprecatedApi = """
        package test;
        public class Test {
            // The object parameter is to ensure that we also notice classes
            //   marked as deprecated in the same project/repo
            void test(DeprecatedClass obj) {
                obj.deprecated();
            }

            void testInner(DeprecatedClass.Inner _obj) {}
        }
        """;

    // ***DELINEATOR FOR REVIEW: can_apply_plugin
    @Test
    void can_apply_plugin(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);

        // ***DELINEATOR FOR REVIEW: then
        gradle.withArgs("compileJava", "--info").buildsSuccessfully();
    }

    // ***DELINEATOR FOR REVIEW: compileJava_fails_when_there_is_an_unclosed_stream_of_files
    @Test
    void compilejava_fails_when_there_is_an_unclosed_stream_of_files(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass("""
            package test;
            public class Test {
                void test() throws java.io.IOException {
                    java.nio.file.Files.list(java.nio.file.Paths.get("/"))
                        .collect(java.util.stream.Collectors.toList());
                }
            }
            """);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result = gradle.withArgs("compileJava").buildsWithFailure();
        assertThat(result).task(":compileJava").failed();
        assertThat(result)
                .output()
                .contains("[StreamResourceLeak] Streams that encapsulate a closeable resource should be closed using"
                        + " try-with-resources");
    }

    // ***DELINEATOR FOR REVIEW: compileJava_fails_when_error_prone_finds_errors
    @Test
    void compilejava_fails_when_error_prone_finds_errors(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass(invalidJavaFile);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result = gradle.withArgs("compileJava").buildsWithFailure();
        assertThat(result).task(":compileJava").failed();
        assertThat(result).output().contains("[ArrayEquals] Reference equality used to compare arrays");
    }

    @Test
    void compilejava_fails_when_using_deprecated_apis(GradleInvoker gradle, RootProject project) {
        standardBuildFile(project);
        project.buildGradle().append("""
            dependencies {
                // CheckedServiceException constructors are deprecated for removal in this version
                implementation 'com.palantir.conjure.java.api:errors:2.65.0'
            }
            """);

        project.mainSourceSet().java().writeClass("""
            package test;

            import com.palantir.conjure.java.api.errors.CheckedServiceException;
            import com.palantir.conjure.java.api.errors.ErrorType;

            public class Test extends CheckedServiceException {
                public Test() {
                    super(ErrorType.CONFLICT);
                }
            }
            """);

        InvocationResult result = gradle.withArgs("compileJava").buildsWithFailure();
        assertThat(result).task(":compileJava").failed();
        assertThat(result).output().contains("CheckedServiceException#<init> is deprecated for removal");
    }

    @Test
    void compilejava_succeeds_when_using_deprecated_for_removal_apis_even_with_werror_if_check_is_disabled(
            GradleInvoker gradle, RootProject project) {
        standardBuildFile(project);
        project.buildGradle().append("""
            dependencies {
                // CheckedServiceException constructors are deprecated for removal in this version
                implementation 'com.palantir.conjure.java.api:errors:2.65.0'
            }
            tasks.withType(JavaCompile) {
                options.compilerArgs += ['-Werror']
                options.errorprone {
                    check 'DeprecatedForRemovalApiUsage', net.ltgt.gradle.errorprone.CheckSeverity.OFF
                }
            }
            """);

        project.mainSourceSet().java().writeClass("""
            package test;

            import com.palantir.conjure.java.api.errors.CheckedServiceException;
            import com.palantir.conjure.java.api.errors.ErrorType;

            public class Test extends CheckedServiceException {
                public Test() {
                    super(ErrorType.CONFLICT);
                }
            }
            """);

        InvocationResult result = gradle.withArgs("compileJava").buildsSuccessfully();
        assertThat(result).task(":compileJava").succeeded();
    }

    @Test
    void compilejava_succeeds_when_using_deprecated_if_deprecated_api_is_in_the_same_project(
            GradleInvoker gradle, RootProject project) {
        standardBuildFile(project);
        project.buildGradle().append("""
            tasks.withType(JavaCompile) {
                options.compilerArgs += ['-Werror']
            }
            """);

        project.mainSourceSet().java().writeClass(javaFileWithDeprecations);

        project.mainSourceSet().java().writeClass(javaFileUsingDeprecatedApi);

        InvocationResult result = gradle.withArgs("compileJava").buildsSuccessfully();
        assertThat(result).task(":compileJava").succeeded();
    }

    enum DeprecatedMultiProjectConfiguration {
        DEFAULT,
        COMPILATION_CLASSPATH_PACKAGING,
        JAVA_PLUGIN_IN_LIBRARY
    }

    @ParameterizedTest
    @EnumSource(DeprecatedMultiProjectConfiguration.class)
    void compilejava_succeeds_when_using_deprecated_if_deprecated_api_is_in_the_same_repo_in_different_subprojects(
            DeprecatedMultiProjectConfiguration config,
            GradleInvoker gradle,
            RootProject project,
            SubProject lib,
            SubProject app) {
        standardBuildFile(project);
        project.buildGradle().append("""
            tasks.withType(JavaCompile) {
                options.compilerArgs += ['-Werror']
            }
            """);

        if (config == DeprecatedMultiProjectConfiguration.COMPILATION_CLASSPATH_PACKAGING) {
            // Set org.gradle.java.compile-classpath-packaging to true
            // This makes the app project use jars for the lib sub project, rather than class files directly
            project.gradlePropertiesFile()
                    .setProperty("systemProp.org.gradle.java.compile-classpath-packaging", "true");
        }

        // Set up lib sub-project
        if (config == DeprecatedMultiProjectConfiguration.JAVA_PLUGIN_IN_LIBRARY) {
            // Mistakenly use the java plugin rather than java-library
            javaProjectBuildFile(lib);
        } else {
            javaLibraryProjectBuildFile(lib);
        }

        lib.mainSourceSet().java().writeClass(javaFileWithDeprecations);

        // Set up app sub-project, depending on lib
        javaProjectBuildFile(app);
        app.buildGradle().append("""
            dependencies {
                implementation project(':lib')
            }
            """);

        app.mainSourceSet().java().writeClass(javaFileUsingDeprecatedApi);

        InvocationResult result = gradle.withArgs("compileJava").buildsSuccessfully();
        assertThat(result).task(":lib:compileJava").succeeded();
        assertThat(result).task(":app:compileJava").succeeded();
    }

    // ***DELINEATOR FOR REVIEW: compileJava_fails_when_StrictUnusedVariable_finds_errors
    @Test
    void compilejava_fails_when_strictunusedvariable_finds_errors(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass("""
            package test;
            public class Test {
                void test() {
                    int a = 5;
                }
            }
            """);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result = gradle.withArgs("compileJava").buildsWithFailure();
        assertThat(result).task(":compileJava").failed();
        assertThat(result).output().contains("[StrictUnusedVariable]");
    }

    // ***DELINEATOR FOR REVIEW: error_prone_can_be_disabled_using_property
    @Test
    void error_prone_can_be_disabled_using_property(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass(invalidJavaFile);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result = gradle.withArgs("compileJava", "-Pcom.palantir.baseline-error-prone.disable")
                .buildsSuccessfully();
        assertThat(result).task(":compileJava").succeeded();
    }

    // ***DELINEATOR FOR REVIEW: error_prone_is_not_disabled_in_IntelliJ
    @Test
    void error_prone_is_not_disabled_in_intellij(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass(invalidJavaFile);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result =
                gradle.withArgs("compileJava", "-Didea.active=true").buildsWithFailure();
        assertThat(result).task(":compileJava").failed();
        assertThat(result).output().contains("[ArrayEquals] Reference equality used to compare arrays");
    }

    // ***DELINEATOR FOR REVIEW: error_prone_can_be_enabled_using_property
    @Test
    void error_prone_can_be_enabled_using_property(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass(invalidJavaFile);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result = gradle.withArgs(
                        "compileJava", "-Pcom.palantir.baseline-error-prone.disable=false", "-Didea.active=true")
                .buildsWithFailure();
        assertThat(result).task(":compileJava").failed();
        assertThat(result).output().contains("[ArrayEquals] Reference equality used to compare arrays");
    }

    // ***DELINEATOR FOR REVIEW: compileJava_succeeds_when_error_prone_finds_no_errors
    @Test
    void compilejava_succeeds_when_error_prone_finds_no_errors(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass(validJavaFile);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result = gradle.withArgs("compileJava").buildsSuccessfully();
        assertThat(result).task(":compileJava").succeeded();
    }

    // ***DELINEATOR FOR REVIEW: compileJava_applies_patches_when_error_prone_finds_errors
    @Test
    void compilejava_applies_patches_when_error_prone_finds_errors(GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass(invalidJavaFile);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result =
                gradle.withArgs("compileJava", "-PerrorProneApply").buildsSuccessfully();
        assertThat(result).task(":compileJava").succeeded();
        project.mainSourceSet().java().fileByClassName("test.Test").assertThat().hasContent("""
            package test;
            import java.util.Arrays;
            import java.util.Optional;
            public class Test {
                void test() {
                    int[] a = {1, 2, 3};
                    int[] b = {1, 2, 3};
                    if (Arrays.equals(a, b)) {
                      System.out.println("arrays are equal!");
                      Optional.of("hello").orElseGet(() -> System.getProperty("world"));
                    }
                }
            }
            """);
    }

    // ***DELINEATOR FOR REVIEW: compileJava_applies_patches_when_errorProneApply_contains_specific_checks
    @Test
    void compilejava_applies_patches_when_errorproneapply_contains_specific_checks(
            GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: when
        standardBuildFile(project);
        project.mainSourceSet().java().writeClass(invalidJavaFile);

        // ***DELINEATOR FOR REVIEW: then
        InvocationResult result = gradle.withArgs("compileJava", "-PerrorProneApply=OptionalOrElseMethodInvocation")
                .buildsSuccessfully();
        assertThat(result).task(":compileJava").succeeded();
        project.mainSourceSet().java().fileByClassName("test.Test").assertThat().hasContent("""
            package test;
            import java.util.Optional;
            public class Test {
                void test() {
                    int[] a = {1, 2, 3};
                    int[] b = {1, 2, 3};
                    if (a.equals(b)) {
                      System.out.println("arrays are equal!");
                      Optional.of("hello").orElseGet(() -> System.getProperty("world"));
                    }
                }
            }
            """);
    }

    enum CheckConfigurationMethod {
        ARG,
        DSL
    }

    // ***DELINEATOR FOR REVIEW:
    // compileJava_does_not_apply_patches_for_error_prone_checks_that_were_turned_OFF_via_checkConfigurationMethod
    @ParameterizedTest
    @EnumSource(CheckConfigurationMethod.class)
    void compilejava_does_not_apply_patches_for_error_prone_checks_that_were_turned_off_via(
            CheckConfigurationMethod checkConfigurationMethod, GradleInvoker gradle, RootProject project) {
        // ***DELINEATOR FOR REVIEW: setup
        String checkName = "Slf4jLogsafeArgs";
        String turnOffCheck =
                switch (checkConfigurationMethod) {
                    case ARG -> "options.errorprone.disable '" + checkName + "'";
                    case DSL -> """
                        options.errorprone {
                            check '%s', net.ltgt.gradle.errorprone.CheckSeverity.OFF
                        }
                        """.formatted(checkName);
                };

        standardBuildFile(project);
        project.buildGradle().append("""
            tasks.withType(JavaCompile) {
                %s
            }
            dependencies {
                implementation 'org.slf4j:slf4j-api:1.7.25'
            }
            """, turnOffCheck);

        String correctJavaFile = """
            package test;
            import org.slf4j.LoggerFactory;
            import org.slf4j.Logger;
            public class Test {
                void test() {
                    Logger log = LoggerFactory.getLogger("foo");
                    log.info("Hi there {}", "non safe arg");
                }
            }
            """;
        project.mainSourceSet().java().writeClass(correctJavaFile);

        // ***DELINEATOR FOR REVIEW: expect
        InvocationResult result =
                gradle.withArgs("compileJava", "-PerrorProneApply").buildsSuccessfully();
        assertThat(result).task(":compileJava").succeeded();
        project.mainSourceSet().java().fileByClassName("test.Test").assertThat().hasContent(correctJavaFile);
    }
}
