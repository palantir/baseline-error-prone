package com.palantir.baseline.errorprone;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;

@AutoService(BugChecker.class)
@BugPattern(
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = SeverityLevel.ERROR,
        summary = "Arrays should not be logged. Logged args are converted to strings using toString() "
                + "and arrays implement toString() by returning their class name and hashcode, not by "
                + "calling toString() on their contents. An array can be usefully logged by converting "
                + "it to a list with Arrays.asList.")
public final class LogsafeArrayArg extends BugChecker implements MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> MATCHER = Matchers.staticMethod()
            .onClassAny("com.palantir.logsafe.SafeArg", "com.palantir.logsafe.UnsafeArg")
            .named("of")
            .withParameters(String.class.getName(), Object.class.getName());

    private static final Matcher<ExpressionTree> ARRAY = Matchers.isArrayType();

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (MATCHER.matches(tree, state)) {
            List<? extends ExpressionTree> args = tree.getArguments();
            if (ARRAY.matches(args.get(1), state)) {
                return describeMatch(tree);
            }
        }
        return Description.NO_MATCH;
    }
}
