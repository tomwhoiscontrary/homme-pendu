package li.earth.urchin.twic.test.pending;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class PendingTest implements TestRule {

    public static class Pending extends AssumptionViolatedException {
        public Pending(Throwable actual, Matcher<? super Throwable> matcher) {
            super(actual, matcher);
        }
    }

    public static class Failure extends AssertionError {
        public Failure(String message) {
            super(message);
        }
    }

    public static class SetupError extends IllegalStateException {
        public SetupError(String message) {
            super(message);
        }
    }

    private Matcher<? super Throwable> matcher;

    public <T> void shouldFailAfterThis(Matcher<? super Throwable> matcher) {
        if (this.matcher != null) throw new SetupError("shouldFailAfterThis was called twice");
        this.matcher = matcher;
    }

    public void shouldFailAfterThis() {
        shouldFailAfterThis(CoreMatchers.notNullValue());
    }

    public void shouldFailBeforeThis() {
        if (matcher == null) throw new SetupError("shouldFailBeforeThis was called, but not shouldFailAfterThis");
        throw new Failure("should have failed");
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                matcher = null;

                try {
                    base.evaluate();
                } catch (Failure | SetupError e) {
                    throw e;
                } catch (Throwable e) {
                    if (matcher != null && matcher.matches(e)) throw new Pending(e, matcher);
                    else throw e;
                }

                if (matcher != null) {
                    throw new SetupError("shouldFailAfterThis was called, but not shouldFailBeforeThis");
                }
            }
        };
    }

}
