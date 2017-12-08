package li.earth.urchin.twic.test.pending;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class PendingTest implements TestRule {

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

    private boolean shouldFail;

    public void shouldFailAfterThis() {
        if (shouldFail) throw new SetupError("shouldFailAfterThis was called twice");
        shouldFail = true;
    }

    public void shouldFailBeforeThis() {
        if (!shouldFail) throw new SetupError("shouldFailBeforeThis was called, but not shouldFailAfterThis");
        throw new Failure("should have failed");
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                shouldFail = false;

                try {
                    base.evaluate();
                } catch (Failure | SetupError e) {
                    throw e;
                } catch (Throwable e) {
                    if (shouldFail) return;
                    else throw e;
                }

                if (shouldFail) throw new SetupError("shouldFailAfterThis was called, but not shouldFailBeforeThis");
            }
        };
    }

}
