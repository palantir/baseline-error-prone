<p align="right">
<a href="https://autorelease.general.dmz.palantir.tech/palantir/baseline-error-prone"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

## com.palantir.baseline-error-prone
The `com.palantir.baseline-error-prone` plugin brings in the `net.ltgt.errorprone-javacplugin` plugin. The minimal setup is as follows:

```groovy
apply plugin: 'com.palantir.baseline-error-prone'
```

Error-prone rules can be suppressed on a per-line or per-block basis just like Checkstyle rules:

```Java
@SuppressWarnings("Slf4jConstantLogMessage")
```

Rules can be suppressed at the project level, or have their severity modified, by adding the following to the project's `build.gradle`:

```gradle
tasks.withType(JavaCompile).configureEach(new Action<Task>() {
    public void execute(Task task) {
        task.options.errorprone.disable 'Slf4jLogsafeArgs'
    }
})
```

To turn all of error-prone's warnings into errors:

```gradle
allprojects {
    tasks.withType(JavaCompile) {
        options.compilerArgs += ['-Werror']
    }
}
```

More information on error-prone severity handling can be found at [errorprone.info/docs/flags](http://errorprone.info/docs/flags).

#### Baseline error-prone checks
Baseline configures the following checks in addition to the [error-prone's out-of-the-box
checks](https://errorprone.info):

- `DangerousParallelStreamUsage`: Discourage the use of Java parallel streams.
- `Slf4jConstantLogMessage`: Allow only compile-time constant slf4j log message strings.
- `Slf4jLevelCheck`: Slf4j level checks (`if (log.isInfoEnabled()) {`) must match the most severe level in the containing block.
- `Slf4jLogsafeArgs`: Allow only com.palantir.logsafe.Arg types as parameter inputs to slf4j log messages. More information on
  Safe Logging can be found at [github.com/palantir/safe-logging](https://github.com/palantir/safe-logging).
- `LogsafeArrayArg`: Arrays should not be passed as logsafe arguments. Arrays implement `toString()` by returning their class name and hashcode rather than their contents, so logging an array produces an unhelpful string. Convert the array to a list first, e.g., `Arrays.asList(arr)` for object arrays or `Ints.asList(arr)` for primitive arrays.
- `PreferCollectionTransform`: Prefer Guava's Lists.transform or Collections2.transform instead of Iterables.transform when first argument's declared type is a List or Collection type for performance reasons.
- `PreferListsPartition`: Prefer Guava's `Lists.partition(List, int)` instead of `Iterables.partition(Iterable, int)` when first argument's declared type is a list for performance reasons.
- `PreferSafeLoggableExceptions`: Users should throw `SafeRuntimeException` instead of `RuntimeException` so that messages will not be needlessly redacted when logs are collected:
    ```diff
    -throw new RuntimeException("explanation", e); // this message will be redacted when logs are collected
    +throw new SafeRuntimeException("explanation", e); // this message will be preserved (allowing easier debugging)
    ```
- `PreferSafeLoggingPreconditions`: Users should use the safe-logging versions of Precondition checks for standardization when there is equivalent functionality
    ```diff
    -com.google.common.base.Preconditions.checkNotNull(variable, "message");
    +com.palantir.logsafe.Preconditions.checkNotNull(variable, "message"); // equivalent functionality is available in the safe-logging variant
    ```
- `PreferUncheckedIoException`: Prefer UncheckedIOException or SafeUncheckedIoException when wrapping IOException.
- `ShutdownHook`: Applications should not use `Runtime#addShutdownHook`.
- `GradleCacheableTaskAction`: Gradle plugins should not call `Task.doFirst` or `Task.doLast` with a lambda, as that is not cacheable. See [gradle/gradle#5510](https://github.com/gradle/gradle/issues/5510) for more details.
- `PreferBuiltInConcurrentKeySet`: Discourage relying on Guava's `com.google.common.collect.Sets.newConcurrentHashSet()`, when Java's `java.util.concurrent.ConcurrentHashMap.newKeySet()` serves the same purpose.
- `JUnit5RuleUsage`: Prevent accidental usage of `org.junit.Rule`/`org.junit.ClassRule` within Junit5 tests
- `DangerousCompletableFutureUsage`: Disallow CompletableFuture asynchronous operations without an Executor.
- `NonComparableStreamSort`: Stream.sorted() should only be called on streams of Comparable types.
- `DangerousStringInternUsage`: Disallow String.intern() invocations in favor of more predictable, scalable alternatives.
- `OptionalOrElseThrowThrows`: Optional.orElseThrow argument must return an exception, not throw one.
- `OptionalOrElseGetValue`: Prefer `Optional.orElse(value)` over `Optional.orElseGet(() -> value)` for trivial expressions.
- `OptionalOrElseMethodInvocation`: Prefer `Optional.orElseGet(() -> methodInvocation())` over `Optional.orElse(methodInvocation())`.
- `LambdaMethodReference`: Lambda should use a method reference.
- `SafeLoggingExceptionMessageFormat`: SafeLoggable exceptions do not interpolate parameters.
- `StrictUnusedVariable`: Functions shouldn't have unused parameters.
- `StringBuilderConstantParameters`: StringBuilder with a constant number of parameters should be replaced by simple concatenation.
- `JUnit5SuiteMisuse`: When migrating from JUnit4 -> JUnit5, classes annotated with `@RunWith(Suite.class)` are dangerous because if they reference any JUnit5 test classes, these tests will silently not run!
- `ThrowError`: Prefer throwing a RuntimeException rather than Error.
- `DnsLookup`: Calling `new InetSocketAddress(host, port)` results in a DNS lookup which prevents the address from following DNS changes.
- `ReverseDnsLookup`: Calling address.getHostName may result in an unexpected DNS lookup.
- `ReadReturnValueIgnored`: The result of a read call must be checked to know if EOF has been reached or the expected number of bytes have been consumed.
- `FinalClass`: A class should be declared final if all of its constructors are private.
- `RedundantModifier`: Avoid using redundant modifiers.
- `StrictCollectionIncompatibleType`: Likely programming error due to using the wrong type in a method that accepts Object.
- `InvocationHandlerDelegation`: InvocationHandlers which delegate to another object must catch and unwrap InvocationTargetException.
- `Slf4jThrowable`: Slf4j loggers require throwables to be the last parameter otherwise a stack trace is not produced.
- `JooqResultStreamLeak`: Autocloseable streams and cursors from jOOQ results should be obtained in a try-with-resources statement.
- `StreamOfEmpty`: Stream.of() should be replaced with Stream.empty() to avoid unnecessary varargs allocation.
- `RedundantMethodReference`: Redundant method reference to the same type.
- `ExceptionSpecificity`: Prefer more specific catch types than Exception and Throwable.
- `ThrowSpecificity`: Prefer to declare more specific `throws` types than Exception and Throwable.
- `UnsafeGaugeRegistration`: Use TaggedMetricRegistry.registerWithReplacement over TaggedMetricRegistry.gauge.
- `CollectionStreamForEach`: Collection.forEach is more efficient than Collection.stream().forEach.
- `LoggerEnclosingClass`: Loggers created using getLogger(Class<?>) must reference their enclosing class.
- `UnnecessaryLambdaArgumentParentheses`: Lambdas with a single parameter do not require argument parentheses.
- `RawTypes`: Avoid raw types; add appropriate type parameters if possible.
- `VisibleForTestingPackagePrivate`: `@VisibleForTesting` members should be package-private.
- `OptionalFlatMapOfNullable`: Optional.map functions may return null to safely produce an empty result.
- `ExtendsErrorOrThrowable`: Avoid extending Error (or subclasses of it) or Throwable directly.
- `ImmutablesStyle`: Disallow the use of inline immutables style annotations to avoid forcing compile dependencies on consumers.
- `ImmutablesReferenceEquality`: Comparison of Immutables value using reference equality instead of value equality.
- `TooManyArguments`: Prefer Interface that take few arguments rather than many.
- `ObjectsHashCodeUnnecessaryVarargs`: java.util.Objects.hash(non-varargs) should be replaced with java.util.Objects.hashCode(value) to avoid unnecessary varargs array allocations.
- `PreferStaticLoggers`: Prefer static loggers over instance loggers.
- `LogsafeArgName`: Prevent certain named arguments as being logged as safe. Specify unsafe argument names using `LogsafeArgName:UnsafeArgNames` errorProne flag.
- `ImplicitPublicBuilderConstructor`: Prevent builders from unintentionally leaking public constructors.
- `ImmutablesBuilderMissingInitialization`: Prevent building Immutables.org builders when not all fields have been populated.
- `UnnecessarilyQualified`: Types should not be qualified if they are also imported.
- `DeprecatedGuavaObjects`: `com.google.common.base.Objects` has been obviated by `java.util.Objects`.
- `JavaTimeSystemDefaultTimeZone`: Avoid using the system default time zone.
- `ZoneIdConstant`: Prefer `ZoneId` constants.
- `IncubatingMethod`: Prevents calling Conjure incubating APIs unless you explicitly opt-out of the check on a per-use or per-project basis.
- `CompileTimeConstantViolatesLiskovSubstitution`: Requires consistent application of the `@CompileTimeConstant` annotation to resolve inconsistent validation based on the reference type on which the met is invoked.
- `ConsistentLoggerName`: Ensure Loggers are named consistently.
- `PreferImmutableStreamExCollections`: It's common to use toMap/toSet/toList() as the terminal operation on a stream, but would be extremely surprising to rely on the mutability of these collections. Prefer `toImmutableMap`, `toImmutableSet` and `toImmutableList`. (If the performance overhead of a stream is already acceptable, then the `UnmodifiableFoo` wrapper is likely tolerable).
- `DangerousIdentityKey`: Key type does not override equals() and hashCode, so comparisons will be done on reference equality only.
- `DangerousRecordArrayField`: Array fields in records perform reference equality when comparing records.
- `ConsistentOverrides`: Ensure values are bound to the correct variables when overriding methods
- `FilterOutputStreamSlowMultibyteWrite`: Subclasses of FilterOutputStream should provide a more efficient implementation of `write(byte[], int, int)` to avoid slow writes.
- `BugCheckerAutoService`: Concrete BugChecker implementations should be annotated `@AutoService(BugChecker.class)` for auto registration with error-prone.
- `DangerousCollapseKeysUsage`: Disallow usage of `EntryStream#collapseKeys()`.
- `JooqBatchWithoutBindArgs`: jOOQ batch methods that execute without bind args can cause performance problems.
- `InvocationTargetExceptionGetTargetException`: InvocationTargetException.getTargetException() predates the general-purpose exception chaining facility. The Throwable.getCause() method is now the preferred means of obtaining this information. [(source)](https://docs.oracle.com/en/java/javase/17/docs/api//java.base/java/lang/reflect/InvocationTargetException.html#getTargetException())
- `PreferInputStreamTransferTo`: Prefer JDK `InputStream.transferTo(OutputStream)` over utility methods such as `com.google.common.io.ByteStreams.copy(InputStream, OutputStream)`, `org.apache.commons.io.IOUtils.copy(InputStream, OutputStream)`, `org.apache.commons.io.IOUtils.copyLong(InputStream, OutputStream)`.
- `ConjureEndpointDeprecatedForRemoval`: Conjure endpoints marked with Deprecated and `forRemoval = true` should not be used as they are scheduled to be removed.
- `ResourceIdentifierGetEqualsUsage`: Use `ResourceIdentifier#has{Instance,Locator,Service,Type}` to avoid allocation when checking RID component equality.
- `DeprecatedApiUsage` and `DeprecatedForRemovalApiUsage`: Usage of APIs marked as `@Deprecated` or `@Deprecated(forRemoval = true)` is discouraged. These checks replace the build-in compiler checks, that are enabled with the `-Xlint:deprecation` and `-Xlint:removal` (default on) flags, to allow for auto-suppression upon library upgrades that newly deprecate APIs.

### Programmatic Application

There exist a number of programmatic code modifications available via [error-prone](https://errorprone.info). You can run these on your code to apply some refactorings automatically:

```bash
./gradlew compileJava compileTestJava -PerrorProneApply
```

You may apply specific error-prone refactors including those which are not enabled by default by providing a comma
delimited list of check names to the `-PerrorProneApply` option.

```bash
./gradlew compileJava compileTestJava -PerrorProneApply=ThrowSpecificity
```