package li.earth.urchin.twic.test.pending;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class PendingTestTests {

    private static abstract class TestBase {
        public final PendingTest pendingTest = new PendingTest();

        public abstract void test();
    }

    private final AssertionError failure = new AssertionError("broken");

    @Test
    public void doesNothingToANormalPassingTest() {
        class NormalPassingTest extends TestBase {
            @Override
            public void test() {
            }
        }

        assertThat(testFor(new NormalPassingTest()), passes());
    }

    @Test
    public void doesNothingToANormalFailingTest() {
        class NormalFailingTest extends TestBase {
            @Override
            public void test() {
                throw failure;
            }
        }

        assertThat(testFor(new NormalFailingTest()), failsWith(equalTo(failure)));
    }

    @Test
    public void improperlyFailingPendingTestFails() {
        class ImproperlyFailingPendingTest extends TestBase {
            @Override
            public void test() {
                if (true) throw failure;
                pendingTest.shouldFailAfterThis();
                pendingTest.shouldFailBeforeThis();
            }
        }

        assertThat(testFor(new ImproperlyFailingPendingTest()), failsWith(equalTo(failure)));
    }

    @Test
    public void properlyFailingPendingTestPasses() {
        class ProperlyFailingPendingTest extends TestBase {
            @Override
            public void test() {
                pendingTest.shouldFailAfterThis();
                if (true) throw failure;
                pendingTest.shouldFailBeforeThis();
            }
        }

        assertThat(testFor(new ProperlyFailingPendingTest()), passes());
    }

    @Test
    public void improperlyPassingPendingTestFails() {
        class ImproperlyPassingPendingTest extends TestBase {
            @Override
            public void test() {
                pendingTest.shouldFailAfterThis();
                pendingTest.shouldFailBeforeThis();
            }
        }

        assertThat(testFor(new ImproperlyPassingPendingTest()), failsWith(instanceOf(AssertionError.class)));
    }

    @Test
    public void testWithUnfinishedPendFails() throws Exception {
        class UnfinishedPend extends TestBase {
            @Override
            public void test() {
                pendingTest.shouldFailAfterThis();
            }
        }

        assertThat(testFor(new UnfinishedPend()), failsWith(instanceOf(IllegalStateException.class)));
    }

    @Test
    public void testWithRestartedPendFails() throws Exception {
        class RestartedPend extends TestBase {
            @Override
            public void test() {
                pendingTest.shouldFailAfterThis();
                pendingTest.shouldFailAfterThis();
                pendingTest.shouldFailBeforeThis();
            }
        }

        assertThat(testFor(new RestartedPend()), failsWith(instanceOf(IllegalStateException.class)));
    }

    @Test
    public void testWithUnstartedPendFails() throws Exception {
        class UnstartedPend extends TestBase {
            @Override
            public void test() {
                pendingTest.shouldFailBeforeThis();
            }
        }

        assertThat(testFor(new UnstartedPend()), failsWith(instanceOf(IllegalStateException.class)));
    }

    private Statement testFor(TestBase test) {
        Description description = Description.createTestDescription(test.getClass(), "test");

        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                test.test();
            }
        };

        return test.pendingTest.apply(statement, description);
    }

    private Matcher<Statement> passes() {
        return new TypeSafeDiagnosingMatcher<Statement>() {
            @Override
            protected boolean matchesSafely(Statement statement, org.hamcrest.Description mismatchDescription) {
                try {
                    statement.evaluate();
                } catch (Throwable failure) {
                    mismatchDescription.appendText("fails with ").appendValue(failure);
                    return false;
                }

                return true;
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("passes");
            }
        };
    }

    private Matcher<Statement> failsWith(Matcher<Throwable> failureMatcher) {
        return new TypeSafeDiagnosingMatcher<Statement>() {
            @Override
            protected boolean matchesSafely(Statement statement, org.hamcrest.Description mismatchDescription) {
                try {
                    statement.evaluate();
                } catch (Throwable actualFailure) {
                    if (!failureMatcher.matches(actualFailure)) {
                        mismatchDescription.appendText("fails, but ");
                        failureMatcher.describeMismatch(actualFailure, mismatchDescription);
                        return false;
                    }

                    return true;
                }

                mismatchDescription.appendText("passes");
                return false;
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("fails with ");
                failureMatcher.describeTo(description);
            }
        };
    }
}
