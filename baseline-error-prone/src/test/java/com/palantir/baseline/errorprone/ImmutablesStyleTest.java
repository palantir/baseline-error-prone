/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

public class ImmutablesStyleTest {

    @Test
    public void testClass_inlineAnnotation() {
        helper().addSourceLines(
                        "Person.java",
                        "import org.immutables.value.Value;",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "// BUG: Diagnostic contains: ImmutablesStyle",
                        "public interface Person {}")
                .doTest();
    }

    @Test
    public void testClass_metaAnnotation() {
        helper().addSourceLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(RetentionPolicy.SOURCE)\n",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "public @interface ValueStyle {}")
                .addSourceLines(
                        "Person.java",
                        "import org.immutables.value.Value;",
                        "@ValueStyle",
                        "public interface Person {}")
                .doTest();
    }

    @Test
    public void testPackage_inlineAnnotation() {
        helper().addSourceLines(
                        "package-info.java",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "// BUG: Diagnostic contains: ImmutablesStyle",
                        "package com.example;",
                        "import org.immutables.value.Value;")
                .doTest();
    }

    @Test
    public void testPackage_metaAnnotation() {
        helper().addSourceLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(RetentionPolicy.SOURCE)\n",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "public @interface ValueStyle {}")
                .addSourceLines(
                        "package-info.java",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "// BUG: Diagnostic contains: ImmutablesStyle",
                        "package com.example;",
                        "import org.immutables.value.Value;")
                .doTest();
    }

    @Test
    public void testMetaAnnotation_defaultRetention() {
        helper().addSourceLines(
                        "ValueStyle.java",
                        "import org.immutables.value.Value;",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "// BUG: Diagnostic contains: ImmutablesStyle",
                        "public @interface ValueStyle {}")
                .addSourceLines("Person.java", "@ValueStyle", "public interface Person {}")
                .doTest();
    }

    @Test
    public void fixMetaAnnotation_defaultRetention() {
        fix().addInputLines(
                        "ValueStyle.java",
                        "import org.immutables.value.Value;",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "public @interface ValueStyle {}")
                .addOutputLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(RetentionPolicy.SOURCE)",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "public @interface ValueStyle {}")
                .doTest();
    }

    @Test
    public void testMetaAnnotation_classRetention() {
        helper().addSourceLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(RetentionPolicy.CLASS)\n",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "// BUG: Diagnostic contains: ImmutablesStyle",
                        "public @interface ValueStyle {}")
                .addSourceLines("Person.java", "@ValueStyle", "public interface Person {}")
                .doTest();
    }

    @Test
    public void fixMetaAnnotation_classRetention() {
        fix().addInputLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(RetentionPolicy.CLASS)",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)",
                        "public @interface ValueStyle {}")
                .addOutputLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(RetentionPolicy.SOURCE)",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)",
                        "public @interface ValueStyle {}")
                .doTest();
    }

    @Test
    public void testMetaAnnotation_runtimeRetention() {
        helper().addSourceLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(RetentionPolicy.RUNTIME)\n",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "// BUG: Diagnostic contains: ImmutablesStyle",
                        "public @interface ValueStyle {}")
                .addSourceLines("Person.java", "@ValueStyle", "public interface Person {}")
                .doTest();
    }

    @Test
    public void testMetaAnnotation_sourceRetention() {
        helper().addSourceLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(RetentionPolicy.SOURCE)\n",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)\n",
                        "public @interface ValueStyle {}")
                .addSourceLines("Person.java", "@ValueStyle", "public interface Person {}")
                .doTest();
    }

    @Test
    public void fixMetaAnnotation_runtimeRetention() {
        fix().addInputLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(value = RetentionPolicy.RUNTIME)",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)",
                        "public @interface ValueStyle {}")
                .addOutputLines(
                        "ValueStyle.java",
                        "import java.lang.annotation.Retention;",
                        "import java.lang.annotation.RetentionPolicy;",
                        "import org.immutables.value.Value;",
                        "@Retention(value = RetentionPolicy.SOURCE)",
                        "@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)",
                        "public @interface ValueStyle {}")
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(ImmutablesStyle.class, getClass());
    }

    private RefactoringValidator fix() {
        return RefactoringValidator.of(ImmutablesStyle.class, getClass());
    }
}
