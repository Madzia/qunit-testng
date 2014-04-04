package net.vivin.qunit;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.List;
import java.util.Map;

/**
 * Tests the QUNitTestSuiteRunner so that we can be sure that it's reporting the expected failures and passes
 */

@Test
public class TestQUnitTestSuiteRunner {

    @Test
    public void testQUnitTestSuiteRunnerReturnsExpectedResults() {
        List<QUnitTestSuiteResult> results = QUnitTestSuiteRunner.getTestResults();
        assertEquals(results.size(), 2, "There should be two test suite results");

        //We are only going to look at one result because both sample tests are exactly the same. The reason there are two is because we want
        //to make sure the runner picks up multiple tests HTML files.
        QUnitTestSuiteResult qUnitTestSuiteResult = results.get(0);
        assertEquals(qUnitTestSuiteResult.getResults().keySet().size(), 3, "There should be three modules");

        int totalPasses = 0;
        int totalFailures = 0;

        //[Module 1]
        Map<String, List<QUnitTestResult>> moduleOne = qUnitTestSuiteResult.getResults().get("Module One");
        assertEquals(moduleOne.keySet().size(), 5, "There should be five test cases");

        int passes = 0;
        int failures = 0;

        //[Module 1: Test Case One]
        List<QUnitTestResult> testCaseOneResults = moduleOne.get("Test case one");
        assertEquals(testCaseOneResults.size(), 3, "There should be three results");

        for(QUnitTestResult qUnitTestResult : testCaseOneResults) {
            if(qUnitTestResult.isFailure()) {
                totalFailures++;
                failures++;
            } else {
                totalPasses++;
                passes++;
            }
        }

        assertEquals(failures, 0, "There must be no failures in Module 1, Test case one");
        assertEquals(passes, 3, "There must be three passes in Module 1, Test case one");

        QUnitTestResult result = testCaseOneResults.get(0);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(1)
                .message("There must only be one result.")
                .expected("1")
                .actual("1")
                .failure(false)
        ));

        result = testCaseOneResults.get(1);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(2)
                .message("The return value must equal \"something\".")
                .expected("something")
                .actual("something")
                .failure(false)
        ));

        result = testCaseOneResults.get(2);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(3)
                .message("Exception message must equal \"Invalid URL\".")
                .expected("/Invalid URL/")
                .actual("Invalid URL")
                .failure(false)
        ));

        passes = 0;
        failures = 0;

        //[Module 1: Test Case Two]
        List<QUnitTestResult> testCaseTwoResults = moduleOne.get("Test case two");
        assertEquals(testCaseTwoResults.size(), 3, "There should be three results");

        for(QUnitTestResult qUnitTestResult : testCaseTwoResults) {
            if(qUnitTestResult.isFailure()) {
                totalFailures++;
                failures++;
            } else {
                totalPasses++;
                passes++;
            }
        }

        assertEquals(failures, 1, "There must be one failure in Module 1, Test case two");
        assertEquals(passes, 2, "There must be two passes in Module 1, Test case two");

        result = testCaseTwoResults.get(0);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(4)
                .message("There must be two results.")
                .expected("2")
                .actual("2")
                .failure(false)
        ));

        result = testCaseTwoResults.get(1);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(5)
                .message("The return value must equal \"nothing\".")
                .expected("something")
                .actual("nothing")
                .failure(true)
        ));

        result = testCaseTwoResults.get(2);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(6)
                .message("Exception message must equal \"Invalid Email Address\".")
                .expected("/Invalid Email Address/")
                .actual("Invalid Email Address")
                .failure(false)
        ));

        //[Module 1: Test Case Three]
        List<QUnitTestResult> testCaseThreeResults = moduleOne.get("Test case three");
        assertEquals(testCaseThreeResults.size(), 1, "There should be one result");

        result = testCaseThreeResults.get(0);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(7)
                .message("Exception must be instance of CustomException.")
                .expected("CustomException")
                .actual("CustomException: undefined")
                .failure(false)
        ));

        //[Module 1: Test Case Four]
        List<QUnitTestResult> testCaseFourResults = moduleOne.get("Test case four");
        assertEquals(testCaseFourResults.size(), 1, "There should be one result");

        result = testCaseFourResults.get(0);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(8)
                .message("Exception must contain the string \"failed\".")
                .expected("/failed/")
                .actual("CustomException: Everything has failed!")
                .failure(false)
        ));

        //[Module 1: Test Case Five]
        List<QUnitTestResult> testCaseFiveResults = moduleOne.get("Test case five");
        assertEquals(testCaseFiveResults.size(), 1, "There should be one result");

        result = testCaseFiveResults.get(0);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(9)
                .message("Exception message must equal \"Failed\".")
                .expected("null (not set by QUnit)")
                .actual("Failed")
                .failure(false)
        ));


        //[Module 2]
        Map<String, List<QUnitTestResult>> moduleTwo = qUnitTestSuiteResult.getResults().get("Module Two");
        assertEquals(moduleTwo.keySet().size(), 1, "There should be one test cases");

        passes = 0;
        failures = 0;

        //[Module 2: All fail]
        List<QUnitTestResult> allFailResults = moduleTwo.get("All fail");
        assertEquals(allFailResults.size(), 3, "There should be three results");

        for(QUnitTestResult qUnitTestResult : allFailResults) {
            if(qUnitTestResult.isFailure()) {
                totalFailures++;
                failures++;
            } else {
                totalPasses++;
                passes++;
            }
        }

        assertEquals(failures, 3, "There must be three failures in Module 2, All fail");
        assertEquals(passes, 0, "There must be no passes in Module 2, All fail");

        result = allFailResults.get(0);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(10)
                .message("There must be four results.")
                .expected("4")
                .actual("3")
                .failure(true)
        ));

        result = allFailResults.get(1);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(11)
                .message("The return value must equal \"everything\".")
                .expected("nothing")
                .actual("everything")
                .failure(true)
        ));

        result = allFailResults.get(2);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(12)
                .message("Exception message must equal \"Invalid Phone Number\".")
                .expected("/Invalid Phone Number/")
                .actual("Oh yeah")
                .failure(true)
        ));

        //[Module 3]
        Map<String, List<QUnitTestResult>> moduleThree = qUnitTestSuiteResult.getResults().get("Module Three");
        assertEquals(moduleThree.keySet().size(), 1, "There should be one test cases");

        passes = 0;
        failures = 0;

        //[Module 3: Test ok and deepEquals]
        List<QUnitTestResult> okDeepEqualsResults = moduleThree.get("Test ok and deepEquals");
        assertEquals(okDeepEqualsResults.size(), 4, "There should be four results");

        for(QUnitTestResult qUnitTestResult : okDeepEqualsResults) {
            if(qUnitTestResult.isFailure()) {
                totalFailures++;
                failures++;
            } else {
                totalPasses++;
                passes++;
            }
        }

        assertEquals(failures, 3, "There must be three failures in Module 3, Test ok and deepEquals");
        assertEquals(passes, 1, "There must be no passes in Module 3, Test ok and deepEquals");

        result = okDeepEqualsResults.get(0);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(13)
                .message("Value must be true.")
                .failure(true)
        ));

        result = okDeepEqualsResults.get(1);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(14)
                .message("Other value must be true.")
                .failure(false)
        ));

        result = okDeepEqualsResults.get(2);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(15)
                .message("deepEqual does not match")
                .expected("{\"one\":\"1\",\"two\":\"2\",\"three\":\"4\",\"stuff\":{\"four\":4,\"five\":6}}")
                .actual("{\"one\":\"1\",\"two\":\"2\",\"three\":\"3\",\"stuff\":{\"four\":4,\"five\":5}}")
                .failure(true)
        ));

        result = okDeepEqualsResults.get(3);
        testTestResult(result, new ExpectedQUnitTestResult(new ExpectedQUnitTestResult.Builder()
                .testNumber(16)
                .message("Expected 100 assertions, but 3 were run")
                .failure(true)
        ));

        assertEquals(totalFailures, 7, "There must be 7 failures in total");
        assertEquals(totalPasses, 6, "There must be 6 passes in total");
        assertEquals(totalFailures + totalPasses, 13, "There must be 13 results in total");
    }

    private void testTestResult(QUnitTestResult result, ExpectedQUnitTestResult expectedResult) {
        assertEquals(result.getTestNumber(), expectedResult.testNumber, "Test number must match");
        assertEquals(result.getMessage(), expectedResult.message, "Message must match");
        assertEquals(result.getActual(), expectedResult.actual, "Actual value must match");
        assertEquals(result.getExpected(), expectedResult.expected, "Expected value must match");
        assertEquals(result.isFailure(), expectedResult.failure, "Failure value must match");
    }

    private static class ExpectedQUnitTestResult {
        private int testNumber;
        private String message;
        private String actual;
        private String expected;
        private boolean failure;

        public static class Builder {

            private int testNumber;
            private String message;
            private String actual;
            private String expected;
            private boolean failure;

            public Builder() {
                testNumber = 0;
                message = "";
                actual = null;
                expected = null;
                failure = false;
            }

            public Builder testNumber(int testNumber) {
                this.testNumber = testNumber;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder actual(String actual) {
                this.actual = actual;
                return this;
            }

            public Builder expected(String expected) {
                this.expected = expected;
                return this;
            }

            public Builder failure(boolean failure) {
                this.failure = failure;
                return this;
            }
        }

        public ExpectedQUnitTestResult(Builder builder) {
            this.testNumber = builder.testNumber;
            this.message = builder.message;
            this.actual = builder.actual;
            this.expected = builder.expected;
            this.failure = builder.failure;
        }
    }
}
