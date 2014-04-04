package net.vivin.qunit;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.SystemUtils;
import org.testng.TestNGException;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class does all of the heavy lifting. Here we actually run QUnitTestDriver.js using PhantomJS against all the
 * QUnit HTML test files that we can find. Using these results, we create instances of QUnitTest for each result.
 * TestNG will later run these tests and report the results.
 */
public class QUnitTestSuiteRunner {

    public static Object[] run() {
        List<QUnitTest> tests = new ArrayList<QUnitTest>();

        List<QUnitTestSuiteResult> suiteResults = getTestResults();

        for (QUnitTestSuiteResult suiteResult : suiteResults) {

            for (Map.Entry<String, LinkedHashMap<String, List<QUnitTestResult>>> moduleEntry : suiteResult.getResults().entrySet()) {
                String moduleName = moduleEntry.getKey();

                for (Map.Entry<String, List<QUnitTestResult>> testEntry : moduleEntry.getValue().entrySet()) {
                    String testName = testEntry.getKey();

                    for (QUnitTestResult result : testEntry.getValue()) {
                        tests.add(new QUnitTest(moduleName, testName, result));
                    }
                }
            }
        }

        return tests.toArray();
    }

    //Package private so that it's accessible from tests
    static List<QUnitTestSuiteResult> getTestResults() {

        try {
            List<QUnitTestSuiteResult> testSuiteResults = new ArrayList<QUnitTestSuiteResult>();

            String currentWorkingDirectory = new File(QUnitTestSuiteRunner.class.getClassLoader().getResource("").getPath()).getPath();
            String basePhantomJsPath = Paths.get(QUnitTestSuiteRunner.class.getResource("/phantomjs").toURI()).toString();
            String pathToTestDriver = Paths.get(QUnitTestSuiteRunner.class.getResource("/qunit/QUnitTestDriver.js").toURI()).toString();

            String osDir = null;
            String extension = "";
            if(SystemUtils.IS_OS_LINUX) {
                if(System.getProperty("os.arch").endsWith("64")) {
                    osDir = "linux_i686";
                } else {
                    osDir = "linux_x64";
                }

            } else if(SystemUtils.IS_OS_MAC_OSX) {
                osDir = "macosx";
            } else if(SystemUtils.IS_OS_WINDOWS) {
                osDir = "win32";
                extension = ".exe";
            }

            String pathToPhantomJS = new StringBuilder(basePhantomJsPath).
                    append(File.separator).
                    append(osDir).
                    append(File.separator).
                    append("phantomjs").
                    append(extension).toString();

            //When copied from resources, the executable bit is unset, so we need to set it here
            File file = new File(pathToPhantomJS);
            file.setExecutable(true);

            IOFileFilter htmlFileFilter = new IOFileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith("Test.html");
                }

                @Override
                public boolean accept(File file, String s) {
                    return file.getName().endsWith("Test.html");
                }
            };

            Collection<File> htmlTestFiles = FileUtils.listFiles(new File(currentWorkingDirectory), htmlFileFilter, TrueFileFilter.INSTANCE);
            for (File htmlTestFile : htmlTestFiles) {
                String absolutePath = htmlTestFile.getAbsolutePath();

                ProcessBuilder builder = new ProcessBuilder(pathToPhantomJS, pathToTestDriver, absolutePath);

                Process qUnitTest = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(qUnitTest.getInputStream()));

                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }

                //Forces the current thread to wait for the process to complete
                int exitValue = qUnitTest.waitFor();
                testSuiteResults.add(new Gson().fromJson(output.toString().trim(), QUnitTestSuiteResult.class));
            }

            return testSuiteResults;
        } catch (IOException e) {
            throw new TestNGException(e);
        } catch (InterruptedException e) {
            throw new TestNGException(e);
        } catch (URISyntaxException e) {
            throw new TestNGException(e);
        }
    }
}

