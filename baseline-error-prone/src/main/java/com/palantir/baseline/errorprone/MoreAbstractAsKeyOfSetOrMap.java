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

// Portions adapted from
// https://github.com/google/error-prone/blob/d802cff975/core/src/main/java/com/google/errorprone/bugpatterns/AbstractAsKeyOfSetOrMap.java
// Copyright 2018 The Error Prone Authors.

package com.palantir.baseline.errorprone;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.function.IntPredicate;

/** Checks for bad key types in hash-based sets, maps, caches and collectors. */
abstract class MoreAbstractAsKeyOfSetOrMap extends BugChecker
        implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

    private static final Matcher<ExpressionTree> CONSTRUCTS_SET = Matchers.anyOf(
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.Sets")
                    .named("newHashSet"),
            Matchers.constructor().forClass("java.util.HashSet"));

    private static final Matcher<ExpressionTree> CONSTRUCTS_MULTISET = Matchers.anyOf(
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.HashMultiset")
                    .named("create")
                    .withNoParameters(),
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.LinkedHashMultiset")
                    .named("create"));

    private static final Matcher<ExpressionTree> CONSTRUCTS_MAP = Matchers.anyOf(
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.Maps")
                    .named("newHashMap"),
            Matchers.constructor().forClass("java.util.HashMap"),
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.Maps")
                    .named("newLinkedHashMap"),
            Matchers.constructor().forClass("java.util.LinkedHashMap"),
            Matchers.constructor().forClass("java.util.concurrent.ConcurrentHashMap"),
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.HashBiMap")
                    .named("create"));

    private static final Matcher<ExpressionTree> CONSTRUCTS_MULTIMAP = Matchers.anyOf(
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.HashMultimap")
                    .named("create"),
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.LinkedHashMultimap")
                    .named("create"),
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.ArrayListMultimap")
                    .named("create"),
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.collect.LinkedListMultimap")
                    .named("create"));

    private static final Matcher<ExpressionTree> CONSTRUCTS_SET_OR_MAP =
            Matchers.anyOf(CONSTRUCTS_SET, CONSTRUCTS_MAP, CONSTRUCTS_MULTIMAP, CONSTRUCTS_MULTISET);

    private static final Matcher<ExpressionTree> GUAVA_CACHE_BUILDER = MethodMatchers.instanceMethod()
            .onDescendantOf("com.google.common.cache.CacheBuilder")
            .named("build");

    private static final Matcher<ExpressionTree> CAFFEINE_CACHE_BUILDER = MethodMatchers.instanceMethod()
            .onDescendantOf("com.github.benmanes.caffeine.cache.Caffeine")
            .namedAnyOf("build", "buildAsync");

    private static final Matcher<ExpressionTree> SET_COLLECTOR = MethodMatchers.staticMethod()
            .onClass("java.util.stream.Collectors")
            .named("toSet")
            .withNoParameters();

    private static final Matcher<ExpressionTree> UNMODIFIABLE_SET_COLLECTOR = MethodMatchers.staticMethod()
            .onClass("java.util.stream.Collectors")
            .named("toUnmodifiableSet")
            .withNoParameters();

    private static final Matcher<ExpressionTree> IMMUTABLE_SET_COLLECTOR = MethodMatchers.staticMethod()
            .onClass("com.google.common.collect.ImmutableSet")
            .named("toImmutableSet")
            .withNoParameters();

    private static final Matcher<MethodInvocationTree> MAP_COLLECTOR = Matchers.allOf(
            // We could inspect the fourth argument for hash-based maps in the future. That method
            // is relatively uncommon, and the other overloads use HashMap.
            argumentCount(count -> count < 4),
            MethodMatchers.staticMethod().onClass("java.util.stream.Collectors").named("toMap"));

    private static final Matcher<MethodInvocationTree> CONCURRENT_MAP_COLLECTOR = Matchers.allOf(
            // We could inspect the fourth argument for hash-based maps in the future. That method
            // is relatively uncommon, and the other overloads use HashMap.
            argumentCount(count -> count < 4),
            MethodMatchers.staticMethod().onClass("java.util.stream.Collectors").named("toConcurrentMap"));

    // Unlike map and concurrentMap collectors, 'toUnmodifiableMap' provides no overload to specify the map
    // implementation.
    private static final Matcher<ExpressionTree> UNMODIFIABLE_MAP_COLLECTOR =
            MethodMatchers.staticMethod().onClass("java.util.stream.Collectors").named("toUnmodifiableMap");

    private static final Matcher<ExpressionTree> IMMUTABLE_MAP_COLLECTOR = MethodMatchers.staticMethod()
            .onClass("com.google.common.collect.ImmutableMap")
            .named("toImmutableMap");

    private static final Matcher<ExpressionTree> JAVA_UTIL_MAP_OF =
            MethodMatchers.staticMethod().onClass("java.util.Map").named("of");

    private static final Matcher<ExpressionTree> JAVA_UTIL_SET_OF =
            MethodMatchers.staticMethod().onClass("java.util.Set").named("of");

    private static final Matcher<ExpressionTree> IMMUTABLE_MAP_OF = MethodMatchers.staticMethod()
            .onClass("com.google.common.collect.ImmutableMap")
            .namedAnyOf("of", "copyOf");

    private static final Matcher<ExpressionTree> IMMUTABLE_MAP_BUILDER = MethodMatchers.instanceMethod()
            .onDescendantOf("com.google.common.collect.ImmutableMap.Builder")
            .named("build");

    private static final Matcher<ExpressionTree> IMMUTABLE_SET_OF = MethodMatchers.staticMethod()
            .onClass("com.google.common.collect.ImmutableSet")
            .namedAnyOf("of", "copyOf");

    private static final Matcher<ExpressionTree> IMMUTABLE_SET_BUILDER = MethodMatchers.instanceMethod()
            .onDescendantOf("com.google.common.collect.ImmutableSet.Builder")
            .named("build");

    private static final Matcher<ExpressionTree> STREAMEX_TO_MAP = MethodMatchers.instanceMethod()
            .onDescendantOf("one.util.streamex.EntryStream")
            .namedAnyOf("toMap", "toImmutableMap");

    private static final Matcher<ExpressionTree> STREAMEX_TO_SET = MethodMatchers.instanceMethod()
            .onDescendantOf("one.util.streamex.AbstractStreamEx")
            .namedAnyOf("toSet", "toImmutableSet");

    private static final Matcher<MethodInvocationTree> HASH_KEYED_METHODS = Matchers.anyOf(
            GUAVA_CACHE_BUILDER,
            CAFFEINE_CACHE_BUILDER,
            JAVA_UTIL_MAP_OF,
            JAVA_UTIL_SET_OF,
            IMMUTABLE_MAP_OF,
            IMMUTABLE_MAP_BUILDER,
            IMMUTABLE_SET_OF,
            IMMUTABLE_SET_BUILDER,
            STREAMEX_TO_MAP,
            STREAMEX_TO_SET);

    private static final Matcher<MethodInvocationTree> HASH_KEYED_COLLECTOR_METHODS = Matchers.anyOf(
            SET_COLLECTOR,
            UNMODIFIABLE_SET_COLLECTOR,
            IMMUTABLE_SET_COLLECTOR,
            MAP_COLLECTOR,
            CONCURRENT_MAP_COLLECTOR,
            UNMODIFIABLE_MAP_COLLECTOR,
            IMMUTABLE_MAP_COLLECTOR);

    private static final Supplier<Symbol> JAVA_UTIL_STREAM_COLLECTOR =
            VisitorState.memoize(state -> state.getSymbolFromString("java.util.stream.Collector"));

    @Override
    public final Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (CONSTRUCTS_SET_OR_MAP.matches(tree, state) || HASH_KEYED_METHODS.matches(tree, state)) {
            return checkType(tree, ASTHelpers.getResultType(tree), state);
        }
        if (HASH_KEYED_COLLECTOR_METHODS.matches(tree, state)) {
            Type collectorType = ASTHelpers.getResultType(tree);
            if (collectorType == null) {
                return Description.NO_MATCH;
            }
            Symbol collectorSymbol = JAVA_UTIL_STREAM_COLLECTOR.get(state);
            if (collectorSymbol == null) {
                return Description.NO_MATCH;
            }
            Type asParameterizedCollector = state.getTypes().asSuper(collectorType, collectorSymbol);
            if (asParameterizedCollector == null) {
                return Description.NO_MATCH;
            }
            // Collector<T, A, R>
            // [0] T: input
            // [1] A: accumulator
            // [2] R: result type
            Type collectorResultType =
                    asParameterizedCollector.getTypeArguments().get(2);
            return checkType(tree, collectorResultType, state);
        }

        return Description.NO_MATCH;
    }

    @Override
    public final Description matchNewClass(NewClassTree tree, VisitorState state) {
        if (CONSTRUCTS_SET_OR_MAP.matches(tree, state)) {
            return checkType(tree, ASTHelpers.getResultType(tree), state);
        }
        return Description.NO_MATCH;
    }

    protected abstract boolean isBadType(Type type, VisitorState state);

    private Description checkType(Tree tree, Type type, VisitorState state) {
        List<Type> typeArguments = type.getTypeArguments();
        if (!typeArguments.isEmpty() && isBadType(typeArguments.get(0), state)) {
            return describeMatch(tree);
        }
        return Description.NO_MATCH;
    }

    private static Matcher<MethodInvocationTree> argumentCount(IntPredicate intPredicate) {
        return (methodInvocationTree, state) ->
                intPredicate.test(methodInvocationTree.getArguments().size());
    }
}
