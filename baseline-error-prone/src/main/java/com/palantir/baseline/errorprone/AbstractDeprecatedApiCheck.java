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

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.FileObject;

/**
 * This is an abstract base class for checks meant to replace the `-Xlint:deprecation` and `-Xlint:removal` compiler
 *   flags.
 *
 * See {@link DeprecatedApiUsage} and {@link DeprecatedForRemovalApiUsage} for concrete implementations.
 */
public abstract class AbstractDeprecatedApiCheck extends BugChecker
        implements BugChecker.MethodInvocationTreeMatcher,
                BugChecker.MemberReferenceTreeMatcher,
                BugChecker.MemberSelectTreeMatcher,
                BugChecker.IdentifierTreeMatcher {

    private static final Logger log = Logger.getLogger(AbstractDeprecatedApiCheck.class.getName());

    /**
     * Returns true if the given symbol is deprecated in a way that should trigger this check.
     */
    protected abstract boolean isDeprecationWarning(Symbol symbol);

    /**
     * Returns true if the enclosing context should suppress this warning.
     */
    protected abstract boolean isEnclosingDeprecatedForSuppression(Symbol symbol);

    /**
     * Returns the error description to show in the diagnostic, using the qualified name of the deprecated symbol.
     */
    protected abstract String getErrorDescription(String qualifiedName);

    @Override
    public final Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        return checkTree(tree, state);
    }

    @Override
    public final Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
        return checkTree(tree, state);
    }

    @Override
    public final Description matchIdentifier(IdentifierTree tree, VisitorState state) {
        return checkTree(tree, state);
    }

    @Override
    public final Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
        return checkTree(tree, state);
    }

    private Description checkTree(Tree tree, VisitorState state) {
        if (isImportStatement(state)) {
            // We don't want to flag import statements, as those cannot be suppressed.
            return Description.NO_MATCH;
        }

        Symbol symbol = ASTHelpers.getSymbol(tree);

        if (symbol == null) {
            return Description.NO_MATCH;
        }

        if (!isDeprecationWarning(symbol)) {
            return Description.NO_MATCH;
        }

        if (isEnclosingDeprecated(state)) {
            // Suppress this warning if the enclosing method or class is deprecated.
            return Description.NO_MATCH;
        }

        // Note: Symbol#enclClass() returns the class itself if symbol is a class, rather than
        //   the potentially enclosing class (for nested classes). This is what we want here.
        Optional<ClassSymbol> owningClass = Optional.ofNullable(symbol.enclClass());
        Optional<URI> sourceFileUri = owningClass.map(c -> c.sourcefile).map(FileObject::toUri);
        if (sourceFileUri.isPresent() && isRegularFileOnSystem(sourceFileUri.get())) {
            // If the source file is a regular file on the local file system, this means we're calling a deprecated API
            //   within the same project. We don't want to flag these usages, as they don't have any impact, and any
            //   ABI break would have to be fixed immediately anyway.
            // Note: This isn't triggered by files within the same repo for error-prone tests, because these use
            //   in-memory file systems.
            return Description.NO_MATCH;
        }

        Optional<URI> classFileUri = owningClass.map(c -> c.classfile).map(FileObject::toUri);
        if (classFileUri.isPresent()
                && (isClassInBuildOutputDirectory(classFileUri.get()) || isLocalBuildJar(classFileUri.get()))) {
            return Description.NO_MATCH;
        }

        String qualifiedName = symbol.owner.getQualifiedName() + "#"
                + symbol.getQualifiedName().toString();
        String description = getErrorDescription(qualifiedName);
        return buildDescription(tree).setMessage(description).build();
    }

    private boolean isImportStatement(VisitorState state) {
        return ASTHelpers.findEnclosingNode(state.getPath(), ImportTree.class) != null;
    }

    /**
     * Returns true if any of the enclosing nodes (methods/classes/etc) is deprecated
     *   (in a way that should suppress this warning).
     */
    private boolean isEnclosingDeprecated(VisitorState state) {
        for (Tree parent : state.getPath()) {
            if (!(parent instanceof MethodTree || parent instanceof ClassTree)) {
                // Only check for deprecation on methods and classes/interfaces/records/etc
                continue;
            }
            Symbol symbol = ASTHelpers.getSymbol(parent);
            if (symbol == null) {
                continue;
            }
            if (isEnclosingDeprecatedForSuppression(symbol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given URI points to a regular file on the local file system, as opposed to e.g.
     *   not an actual file, or a file within a zip/jar file system.
     */
    private boolean isRegularFileOnSystem(URI uri) {
        if (!"file".equals(uri.getScheme())) {
            return false;
        }

        try {
            Path path = Paths.get(uri);

            // Ensure we're using the default file system (not a zip file system, etc.)
            FileSystem fileSystem = path.getFileSystem();
            if (!fileSystem.equals(FileSystems.getDefault())) {
                return false;
            }

            // Check if it exists and is a regular file
            return Files.exists(path) && Files.isRegularFile(path);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to check if URI is a regular file on the system: " + uri, e);
            return false;
        }
    }

    /**
     * Returns true if the given URI points to a class file within a /build/classes/ directory on the local file system,
     *   which likely indicates it's from a local build of the same repository.
     * We don't need to flag these usages, as breaks would be caught at compile time anyway.
     */
    private boolean isClassInBuildOutputDirectory(URI uri) {
        return isRegularFileOnSystem(uri) && uri.getPath().contains("/build/classes/");
    }

    /**
     * Returns true if the given URI points to a class file within a JAR file that is likely from a
     *   local build of the same repository, as opposed to a dependency JAR from e.g. Gradle's cache.
     * This is a best-effort heuristic, and may have false positives or false negatives, but should be good enough for
     *   well-behaved repositories that don't have unusual build setups or directory structures.
     *
     * This step is important for cases like setting org.gradle.java.compile-classpath-packaging to true,
     *   which is done by IntelliJ when using the JUnit runner (as opposed to the Gradle runner). This can happen for
     *   instance when running a test from a class that lives in a main or testFixtures source set (for instance,
     *   when in a test-only subproject, and running tests from a base abstract class).
     *
     * This can also happen for repositories that apply the "java" plugin rather than the "java-library" plugin.
     */
    private boolean isLocalBuildJar(URI uri) {
        String uriStr = uri.toString();
        if (!uriStr.startsWith("jar:file:")) {
            return false;
        }
        try {
            // JAR URIs have the form: jar:file:/path/to/foo.jar!/com/example/Foo.class
            int separatorIndex = uriStr.indexOf("!/");
            if (separatorIndex < 0) {
                return false;
            }
            Path jarPath = Paths.get(uriStr.substring("jar:file:".length(), separatorIndex));

            if (jarPath.toString().contains("/.gradle/caches/")) {
                // The file is in the Gradle cache directory, so it shouldn't be a local build JAR
                return false;
            }

            // It's possible that there could be false positives here, but if the JAR file is within a /build/libs/ or
            //   /build/artifacts/ directory, it's likely a local build JAR from the current repository.
            return jarPath.toString().contains("/build/libs/")
                    || jarPath.toString().contains("/build/artifacts/");
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to check JAR URI for local build: " + uri, e);
            return false;
        }
    }
}
