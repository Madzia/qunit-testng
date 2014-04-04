package net.vivin.qunit.reporter;

import net.vivin.qunit.QUnitTest;
import net.vivin.qunit.QUnitTestSuite;
import org.apache.commons.lang3.StringUtils;
import org.testng.*;
import org.testng.collections.Lists;
import org.testng.internal.Utils;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This reporter is for QUnit tests
 */

public class QUnitJUnitXMLReporter implements IReporter {

    private enum JUnitTestReportElement {

        TestSuite("testsuite"),
        TestCase("testcase"),
        Failure("failure");

        private String tag;

        private JUnitTestReportElement(String tag) {
            this.tag = tag;
        }
    }

    private enum JUnitTestReportElementAttribute {

        HostName("hostname"),
        Name("name"),
        Tests("tests"),
        Failures("failures"),
        Skipped("skipped"),
        Errors("errors"),
        TimeStamp("timestamp"),
        Time("time"),
        ClassName("classname"),
        Message("message"),
        Type("type");

        private String attribute;

        private JUnitTestReportElementAttribute(String attribute) {
            this.attribute = attribute;
        }
    }

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String defaultOutputDirectory) {

        try {
            ISuite suite = suites.get(0);
            XmlSuite xmlSuite = suite.getXmlSuite();
            XmlTest xmlTest = xmlSuite.getTests().get(0);
            XmlClass xmlClass = xmlTest.getXmlClasses().get(0);
            Class clazz = xmlClass.getSupportClass();

            if (!clazz.isAnnotationPresent(QUnitTestSuite.class)) {
                throw new TestNGException(this.getClass().getName() + " is only applicable for tests that have been annotated with " + QUnitTestSuite.class.getName() + ".");
            }

            ISuiteResult suiteResult = suite.getResults().values().iterator().next(); //We should only have one result since we only have one suite
            ITestContext testContext = suiteResult.getTestContext();

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            //Start with the root!
            Document document = documentBuilder.newDocument();
            Element testSuiteElement = document.createElement(JUnitTestReportElement.TestSuite.tag);

            testSuiteElement.setAttribute(JUnitTestReportElementAttribute.HostName.attribute, InetAddress.getLocalHost().getHostName());
            testSuiteElement.setAttribute(JUnitTestReportElementAttribute.Name.attribute, StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(clazz.getSimpleName()), " "));

            int passed = testContext.getPassedTests().size();
            int failed = testContext.getFailedTests().size();
            int skipped = testContext.getSkippedTests().size();

            testSuiteElement.setAttribute(JUnitTestReportElementAttribute.Tests.attribute, String.valueOf(passed + failed + skipped));
            testSuiteElement.setAttribute(JUnitTestReportElementAttribute.Failures.attribute, String.valueOf(failed));
            testSuiteElement.setAttribute(JUnitTestReportElementAttribute.Skipped.attribute, String.valueOf(skipped));
            testSuiteElement.setAttribute(JUnitTestReportElementAttribute.Errors.attribute, "0");
            testSuiteElement.setAttribute(JUnitTestReportElementAttribute.TimeStamp.attribute, toGMTString(Calendar.getInstance().getTime()));

            long startTime = Math.min(testContext.getStartDate().getTime(), Long.MAX_VALUE);
            long endTime = Math.max(testContext.getEndDate().getTime(), Long.MIN_VALUE);
            testSuiteElement.setAttribute(JUnitTestReportElementAttribute.Time.attribute, formatTime(endTime - startTime));

            addTestCaseElements(document, testSuiteElement, suite, testContext.getFailedTests());
            addTestCaseElements(document, testSuiteElement, suite, testContext.getPassedTests());
            addTestCaseElements(document, testSuiteElement, suite, testContext.getSkippedTests());

            document.appendChild(testSuiteElement);

            Utils.writeFile(defaultOutputDirectory, "TEST-" + clazz.getName() + ".xml", documentToString(document));

        } catch (ParserConfigurationException e) {
            throw new TestNGException(e);
        } catch (UnknownHostException e) {
            throw new TestNGException(e);
        } catch (TransformerException e) {
            throw new TestNGException(e);
        }
    }

    private String documentToString(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StreamResult streamResult = new StreamResult(new StringWriter());
        Source source = new DOMSource(document);
        transformer.transform(source, streamResult);

        return streamResult.getWriter().toString();
    }

    private void addTestCaseElements(Document document, Element root, ISuite suite, IResultMap tests) {

        if(tests.getAllMethods().size() > 0) {
            for(ITestNGMethod method : getMethodSet(tests, suite)) {
                QUnitTest qUnitTest = (QUnitTest) method.getInstances()[0];
                String moduleName = qUnitTest.getModuleName();
                String testName = qUnitTest.getTestName();

                Set<ITestResult> results = tests.getResults(method);
                long end = Long.MIN_VALUE;
                long start = Long.MAX_VALUE;

                for(ITestResult testResult : results) {
                    if(testResult.getEndMillis() > end) {
                        end = testResult.getEndMillis();
                    }

                    if(testResult.getStartMillis() < start) {
                        start = testResult.getStartMillis();
                    }
                }

                long time = end - start;

                Element testCaseElement = document.createElement(JUnitTestReportElement.TestCase.tag);
                testCaseElement.setAttribute(JUnitTestReportElementAttribute.Name.attribute, testName);
                testCaseElement.setAttribute(JUnitTestReportElementAttribute.ClassName.attribute, moduleName);
                testCaseElement.setAttribute("time", formatTime(time));

                for(ITestResult result : results) {
                    if(result.getThrowable() != null) {
                        Element failureElement = document.createElement(JUnitTestReportElement.Failure.tag);
                        failureElement.setAttribute(JUnitTestReportElementAttribute.Message.attribute, result.getThrowable().getLocalizedMessage());
                        failureElement.setAttribute(JUnitTestReportElementAttribute.Type.attribute, "QUnit");

                        if(qUnitTest.getSource() != null) {
                            failureElement.setTextContent(qUnitTest.getSource());
                        }

                        testCaseElement.appendChild(failureElement);
                    }
                }

                root.appendChild(testCaseElement);
            }
        }
    }

    /**
     * Since the methods will be sorted chronologically, we want to return
     * the ITestNGMethod from the invoked methods.
     */
    private Collection<ITestNGMethod> getMethodSet(IResultMap tests, ISuite suite) {
        List<IInvokedMethod> r = Lists.newArrayList();
        List<IInvokedMethod> invokedMethods = suite.getAllInvokedMethods();

        for (IInvokedMethod im : invokedMethods) {
            if (tests.getAllMethods().contains(im.getTestMethod())) {
                r.add(im);
            }
        }
        Arrays.sort(r.toArray(new IInvokedMethod[r.size()]), new TestSorter());
        List<ITestNGMethod> result = Lists.newArrayList();

        // Add all the invoked methods
        for (IInvokedMethod m : r) {
            result.add(m.getTestMethod());
        }

        // Add all the methods that weren't invoked (e.g. skipped) that we
        // haven't added yet
        for (ITestNGMethod m : tests.getAllMethods()) {
            if (!result.contains(m)) {
                result.add(m);
            }
        }

        return result;
    }

    private String formatTime(float time) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        // JUnitReports wants points here, regardless of the locale
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat("#.###", symbols);
        format.setMinimumFractionDigits(3);
        return format.format(time / 1000.0f);
    }

    private String toGMTString(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
        simpleDateFormat.applyPattern("dd MMM yyyy HH:mm:ss z");
        return simpleDateFormat.format(date);
    }

    /**
     * Arranges methods by classname and method name
     */
    private class TestSorter implements Comparator<IInvokedMethod> {
        @Override
        public int compare(IInvokedMethod o1, IInvokedMethod o2) {
            return (int) (o1.getDate() - o2.getDate());
        }
    }
}

