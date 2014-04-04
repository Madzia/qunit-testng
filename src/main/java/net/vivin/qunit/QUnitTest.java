package net.vivin.qunit;

import org.testng.ITest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


/**
 * Created with IntelliJ IDEA.
 * User: vivin
 * Date: 8/2/12
 * Time: 4:53 PM
 *
 * This is not actually a test. All this test does is report QUnit test result. It does this by looking at the expected
 * and actual properties of the QUnitTestResult object.
 */

@QUnitTestSuite
public class QUnitTest implements ITest {

    String moduleName;
    String testName;
    private QUnitTestResult result;

    public QUnitTest(String moduleName, String testName, QUnitTestResult result) {
        this.moduleName = moduleName;
        this.testName = testName;
        this.result = result;
    }

    @Test
    public void assertion() {
        assertFalse(result.isFailure(), result.getMessage() + " (From QUnit: [Expected: " + result.getExpected() + ", Actual: " + result.getActual() + "]). TestNG");
    }

    @Override
    public String getTestName() {
        return testName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getSource() {
        return result.getSource();
    }
}

