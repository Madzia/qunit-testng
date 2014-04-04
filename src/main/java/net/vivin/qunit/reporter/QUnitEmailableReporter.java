package net.vivin.qunit.reporter;

import net.vivin.qunit.QUnitTestSuite;
import net.vivin.qunit.QUnitTest;
import org.testng.*;
import org.testng.collections.Lists;
import org.testng.internal.Utils;
import org.testng.log4testng.Logger;
import org.testng.reporters.EmailableReporter;
import org.testng.reporters.util.StackTraceTools;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * This code (unfortunately) duplicates most of what exists in EmailableReporter. I would have extended, except for the fact that most of the presentation
 * logic exists in private methods. This way is a little better since we have more control over the presentation anyway. I've renamed some of the variables
 * and cleaned up some of the code for clarity.
 */
public class QUnitEmailableReporter implements IReporter {

    private static final Logger logger = Logger.getLogger(EmailableReporter.class);

    private PrintWriter out;
    private int row;
    private int methodIndex;
    private int m_rowTotal;

    /**
     * Creates summary of the run
     */
    @Override
    public void generateReport(List<XmlSuite> xml, List<ISuite> suites, String outdir) {
        try {
            out = createWriter(outdir);
        } catch (IOException e) {
            logger.error("output file", e);
            return;
        }

        startHtml(out);
        generateSuiteSummaryReport(suites);
        generateMethodSummaryReport(suites);
        generateMethodDetailReport(suites);
        endHtml(out);

        out.flush();
        out.close();
    }

    protected PrintWriter createWriter(String outdir) throws IOException {
        new File(outdir).mkdirs();
        return new PrintWriter(new BufferedWriter(new FileWriter(new File(outdir,
                "emailable-report.html"))));
    }

    /**
     * Creates a table showing the highlights of each test method with links to the method details
     */
    protected void generateMethodSummaryReport(List<ISuite> suites) {
        methodIndex = 0;
        out.println("<a id=\"summary\"></a>");

        startResultSummaryTable("passed");
        for (ISuite suite : suites) {

            boolean isQUnitTestSuite = isQUnitTestSuite(suite);

            if (suites.size() > 1) {
                titleRow(suite.getName(), 4);
            }

            Map<String, ISuiteResult> results = suite.getResults();
            for (ISuiteResult suiteResult : results.values()) {

                ITestContext testContext = suiteResult.getTestContext();
                String testName = testContext.getName();

                if (isQUnitTestSuite) {
                    testName = "QUnit Test Suite";

                    qUnitResultSummary(suite, testContext.getFailedConfigurations(), testName, "failed", " (configuration methods)");
                    qUnitResultSummary(suite, testContext.getFailedTests(), testName, "failed", "");
                    qUnitResultSummary(suite, testContext.getSkippedConfigurations(), testName, "skipped", " (configuration methods)");
                    qUnitResultSummary(suite, testContext.getSkippedTests(), testName, "skipped", "");
                    qUnitResultSummary(suite, testContext.getPassedTests(), testName, "passed", "");
                } else {
                    resultSummary(suite, testContext.getFailedConfigurations(), testName, "failed", " (configuration methods)");
                    resultSummary(suite, testContext.getFailedTests(), testName, "failed", "");
                    resultSummary(suite, testContext.getSkippedConfigurations(), testName, "skipped", " (configuration methods)");
                    resultSummary(suite, testContext.getSkippedTests(), testName, "skipped", "");
                    resultSummary(suite, testContext.getPassedTests(), testName, "passed", "");
                }
            }
        }

        out.println("</table>");
    }

    private boolean isQUnitTestSuite(ISuite suite) {
        XmlSuite xmlSuite = suite.getXmlSuite();
        XmlTest xmlTest = xmlSuite.getTests().get(0);
        XmlClass xmlClass = xmlTest.getXmlClasses().get(0);
        Class clazz = xmlClass.getSupportClass();

        return clazz.isAnnotationPresent(QUnitTestSuite.class);
    }

    /**
     * Creates a section showing known results for each method
     */
    protected void generateMethodDetailReport(List<ISuite> suites) {
        methodIndex = 0;
        for (ISuite suite : suites) {

            boolean isQUnitTestSuite = isQUnitTestSuite(suite);

            Map<String, ISuiteResult> results = suite.getResults();

            for (ISuiteResult suiteResult : results.values()) {

                ITestContext testContext = suiteResult.getTestContext();

                if (results.values().size() > 0) {
                    if (isQUnitTestSuite) {
                        out.println("<h1>QUnit Test Suite</h1>");
                    } else {
                        out.println("<h1>" + testContext.getName() + "</h1>");
                    }
                }

                if (isQUnitTestSuite) {
                    qUnitResultDetail(testContext.getFailedConfigurations());
                    qUnitResultDetail(testContext.getFailedTests());
                    qUnitResultDetail(testContext.getSkippedConfigurations());
                    qUnitResultDetail(testContext.getSkippedTests());
                    qUnitResultDetail(testContext.getPassedTests());
                } else {
                    resultDetail(testContext.getFailedConfigurations());
                    resultDetail(testContext.getFailedTests());
                    resultDetail(testContext.getSkippedConfigurations());
                    resultDetail(testContext.getSkippedTests());
                    resultDetail(testContext.getPassedTests());
                }
            }
        }
    }

    private void qUnitResultSummary(ISuite suite, IResultMap tests, String testName, String style, String details) {

        if (tests.getAllResults().size() > 0) {
            StringBuffer buffer = new StringBuffer();
            String lastModuleName = "";

            int methodCount = 0;
            int moduleCount = 0;

            //Let's build a map that's keyed by module name so that we only display the tests for the next module once we're
            //done with the tests for the current module
            Map<String, List<ITestNGMethod>> classMethodMap = new LinkedHashMap<String, List<ITestNGMethod>>();
            for (ITestNGMethod method : getMethodSet(tests, suite)) {
                QUnitTest qUnitTest = (QUnitTest) method.getInstances()[0];

                if (classMethodMap.get(qUnitTest.getModuleName()) == null) {
                    classMethodMap.put(qUnitTest.getModuleName(), new ArrayList<ITestNGMethod>());
                }

                classMethodMap.get(qUnitTest.getModuleName()).add(method);
            }

            for (Map.Entry<String, List<ITestNGMethod>> entry : classMethodMap.entrySet()) {
                String moduleName = entry.getKey();
                List<ITestNGMethod> methods = entry.getValue();

                for (ITestNGMethod method : methods) {
                    row += 1;
                    methodIndex += 1;

                    QUnitTest qUnitTest = (QUnitTest) method.getInstances()[0];

                    if (methodCount == 0) {
                        titleRow(testName + " &#8212; " + style + details, 4);
                    }

                    if (!moduleName.equalsIgnoreCase(lastModuleName)) {
                        if (methodCount > 0) {
                            moduleCount++;
                            out.println("<tr class=\"" + style + (moduleCount % 2 == 0 ? "even" : "odd") + "\">" + "<td rowspan=\"" + methodCount + "\">" + lastModuleName + buffer);
                        }

                        methodCount = 0;
                        buffer.setLength(0);
                        lastModuleName = moduleName;
                    }

                    Set<ITestResult> results = tests.getResults(method);
                    long end = Long.MIN_VALUE;
                    long start = Long.MAX_VALUE;

                    for (ITestResult testResult : results) {
                        if (testResult.getEndMillis() > end) {
                            end = testResult.getEndMillis();
                        }

                        if (testResult.getStartMillis() < start) {
                            start = testResult.getStartMillis();
                        }
                    }

                    methodCount++;

                    if (methodCount > 1) {
                        buffer.append("<tr class=\"").append(style).append(moduleCount % 2 == 0 ? "odd" : "even").append("\">");
                    }

                    String description = method.getDescription();
                    String testInstanceName = qUnitTest.getTestName();

                    buffer.append("<td><a href=\"#m" + methodIndex + "\">").append(testInstanceName).append(" ").append(description != null && description.length() > 0 ? "(\"" + description + "\")" : "").append("</a></td>")
                            .append("<td class=\"numi\">").append(results.size()).append("</td>")
                            .append("<td>" + start + "</td>")
                            .append("<td class=\"numi\">").append(end - start).append("</td>")
                            .append("</tr>");
                }


            }


            if (methodCount > 0) {
                moduleCount++;
                out.println("<tr class=\"" + style + (moduleCount % 2 == 0 ? "even" : "odd") + "\">" + "<td rowspan=\"" + methodCount + "\">" + lastModuleName + buffer);
            }
        }
    }

    /**
     * @param tests
     */
    private void resultSummary(ISuite suite, IResultMap tests, String testName, String style, String details) {

        if (tests.getAllResults().size() > 0) {
            StringBuffer buff = new StringBuffer();
            String lastClassName = "";
            int mq = 0;
            int cq = 0;

            for (ITestNGMethod method : getMethodSet(tests, suite)) {
                row += 1;
                methodIndex += 1;
                ITestClass testClass = method.getTestClass();
                String className = testClass.getName();
                if (mq == 0) {
                    titleRow(testName + " &#8212; " + style + details, 4);
                }
                if (!className.equalsIgnoreCase(lastClassName)) {
                    if (mq > 0) {
                        cq += 1;
                        out.println("<tr class=\"" + style
                                + (cq % 2 == 0 ? "even" : "odd") + "\">" + "<td rowspan=\""
                                + mq + "\">" + lastClassName + buff);
                    }
                    mq = 0;
                    buff.setLength(0);
                    lastClassName = className;
                }
                Set<ITestResult> resultSet = tests.getResults(method);
                long end = Long.MIN_VALUE;
                long start = Long.MAX_VALUE;
                for (ITestResult testResult : tests.getResults(method)) {
                    if (testResult.getEndMillis() > end) {
                        end = testResult.getEndMillis();
                    }
                    if (testResult.getStartMillis() < start) {
                        start = testResult.getStartMillis();
                    }
                }
                mq += 1;
                if (mq > 1) {
                    buff.append("<tr class=\"" + style + (cq % 2 == 0 ? "odd" : "even")
                            + "\">");
                }
                String description = method.getDescription();
                String testInstanceName = resultSet.toArray(new ITestResult[]{})[0].getTestName();
                buff.append("<td><a href=\"#m" + methodIndex + "\">"
                        + qualifiedName(method)
                        + " " + (description != null && description.length() > 0
                        ? "(\"" + description + "\")"
                        : "")
                        + "</a>" + (null == testInstanceName ? "" : "<br>(" + testInstanceName + ")")
                        + "</td>"
                        + "<td class=\"numi\">" + resultSet.size() + "</td>"
                        + "<td>" + start + "</td>"
                        + "<td class=\"numi\">" + (end - start) + "</td>"
                        + "</tr>");
            }
            if (mq > 0) {
                cq += 1;
                out.println("<tr class=\"" + style + (cq % 2 == 0 ? "even" : "odd")
                        + "\">" + "<td rowspan=\"" + mq + "\">" + lastClassName + buff);
            }
        }
    }

    /**
     * Starts and defines columns result summary table
     */
    private void startResultSummaryTable(String style) {
        tableStart(style);
        out.println("<tr><th>Class</th>"
                + "<th>Method</th><th># of<br/>Scenarios</th><th>Start</th><th>Time<br/>(ms)</th></tr>");
        row = 0;
    }

    private String qualifiedName(ITestNGMethod method) {
        StringBuilder addon = new StringBuilder();
        String[] groups = method.getGroups();
        int length = groups.length;
        if (length > 0 && !"basic".equalsIgnoreCase(groups[0])) {
            addon.append("(");
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    addon.append(", ");
                }
                addon.append(groups[i]);
            }
            addon.append(")");
        }

        return "<b>" + method.getMethodName() + "</b> " + addon;
    }

    private void qUnitResultDetail(IResultMap tests) {
        for (ITestResult result : tests.getAllResults()) {
            ITestNGMethod method = result.getMethod();
            methodIndex++;

            QUnitTest qUnitTest = (QUnitTest) method.getInstances()[0];
            String moduleName = qUnitTest.getModuleName();

            out.println("<a id=\"m" + methodIndex + "\"></a><h2>" + moduleName + ":" + qUnitTest.getTestName() + "</h2>");

            if (result.getThrowable() != null) {
                out.println("<div style=\"padding-left:3em\">");
                out.println("<p>" + result.getThrowable().getLocalizedMessage() + "</p><br />");

                if (qUnitTest.getSource() != null) {
                    out.println(" " + qUnitTest.getSource().replace("\n", "<br />") + "<br />");
                }

                out.println("</div>");
            }

            out.println("<p class=\"totop\"><a href=\"#summary\">back to summary</a></p>");
        }
    }

    private void resultDetail(IResultMap tests) {
        for (ITestResult result : tests.getAllResults()) {
            ITestNGMethod method = result.getMethod();
            methodIndex++;
            String cname = method.getTestClass().getName();
            out.println("<a id=\"m" + methodIndex + "\"></a><h2>" + cname + ":"
                    + method.getMethodName() + "</h2>");
            Set<ITestResult> resultSet = tests.getResults(method);
            generateForResult(result, method, resultSet.size());
            out.println("<p class=\"totop\"><a href=\"#summary\">back to summary</a></p>");

        }
    }

    private void generateForResult(ITestResult ans, ITestNGMethod method, int resultSetSize) {
        int rq = 0;
        rq += 1;
        Object[] parameters = ans.getParameters();
        boolean hasParameters = parameters != null && parameters.length > 0;
        if (hasParameters) {
            if (rq == 1) {
                tableStart("param");
                out.print("<tr>");
                for (int x = 1; x <= parameters.length; x++) {
                    out
                            .print("<th style=\"padding-left:1em;padding-right:1em\">Parameter #"
                                    + x + "</th>");
                }
                out.println("</tr>");
            }
            out.print("<tr" + (rq % 2 == 0 ? " class=\"stripe\"" : "") + ">");
            for (Object p : parameters) {
                out.println("<td style=\"padding-left:.5em;padding-right:2em\">"
                        + (p != null ? Utils.escapeHtml(p.toString()) : "null") + "</td>");
            }
            out.println("</tr>");
        }
        List<String> msgs = Reporter.getOutput(ans);
        boolean hasReporterOutput = msgs.size() > 0;
        Throwable exception = ans.getThrowable();
        boolean hasThrowable = exception != null;
        if (hasReporterOutput || hasThrowable) {
            String indent = " style=\"padding-left:3em\"";
            if (hasParameters) {
                out.println("<tr" + (rq % 2 == 0 ? " class=\"stripe\"" : "")
                        + "><td" + indent + " colspan=\"" + parameters.length + "\">");
            } else {
                out.println("<div" + indent + ">");
            }
            if (hasReporterOutput) {
                if (hasThrowable) {
                    out.println("<h3>Test Messages</h3>");
                }
                for (String line : msgs) {
                    out.println(line + "<br/>");
                }
            }
            if (hasThrowable) {
                boolean wantsMinimalOutput = ans.getStatus() == ITestResult.SUCCESS;
                if (hasReporterOutput) {
                    out.println("<h3>"
                            + (wantsMinimalOutput ? "Expected Exception" : "Failure")
                            + "</h3>");
                }
                generateExceptionReport(exception, method);
            }
            if (hasParameters) {
                out.println("</td></tr>");
            } else {
                out.println("</div>");
            }
        }
        if (hasParameters) {
            if (rq == resultSetSize) {
                out.println("</table>");
            }
        }
    }

    protected void generateExceptionReport(Throwable exception, ITestNGMethod method) {
        generateExceptionReport(exception, method, exception.getLocalizedMessage());
    }

    private void generateExceptionReport(Throwable exception, ITestNGMethod method, String title) {
        out.println("<p>" + Utils.escapeHtml(title) + "</p>");
        StackTraceElement[] s1 = exception.getStackTrace();
        Throwable t2 = exception.getCause();
        if (t2 == exception) {
            t2 = null;
        }
        int maxlines = Math.min(100, StackTraceTools.getTestRoot(s1, method));
        for (int x = 0; x <= maxlines; x++) {
            out.println((x > 0 ? "<br/>at " : "") + Utils.escapeHtml(s1[x].toString()));
        }
        if (maxlines < s1.length) {
            out.println("<br/>" + (s1.length - maxlines) + " lines not shown");
        }
        if (t2 != null) {
            generateExceptionReport(t2, method, "Caused by " + t2.getLocalizedMessage());
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

    public void generateSuiteSummaryReport(List<ISuite> suites) {
        tableStart("param");
        out.print("<tr><th>Test</th>");

        tableColumnStart("Methods<br/>Passed");
        tableColumnStart("Scenarios<br/>Passed");
        tableColumnStart("# skipped");
        tableColumnStart("# failed");
        tableColumnStart("Total<br/>Time");
        tableColumnStart("Included<br/>Groups");
        tableColumnStart("Excluded<br/>Groups");

        out.println("</tr>");
        NumberFormat formatter = new DecimalFormat("#,##0.0");

        int qty_tests = 0;
        int qty_pass_m = 0;
        int qty_pass_s = 0;
        int qty_skip = 0;
        int qty_fail = 0;
        long time_start = Long.MAX_VALUE;
        long time_end = Long.MIN_VALUE;

        for (ISuite suite : suites) {

            boolean isQUnitTestSuite = isQUnitTestSuite(suite);

            if (suites.size() > 1) {
                titleRow(suite.getName(), 7);
            }

            Map<String, ISuiteResult> tests = suite.getResults();
            for (ISuiteResult r : tests.values()) {

                qty_tests += 1;
                ITestContext overview = r.getTestContext();
                startSummaryRow(isQUnitTestSuite ? "QUnit Test Suite" : overview.getName());

                int q = getMethodSet(overview.getPassedTests(), suite).size();
                qty_pass_m += q;
                summaryCell(q, Integer.MAX_VALUE);

                q = overview.getPassedTests().size();
                qty_pass_s += q;
                summaryCell(q, Integer.MAX_VALUE);

                q = getMethodSet(overview.getSkippedTests(), suite).size();
                qty_skip += q;
                summaryCell(q, 0);

                q = getMethodSet(overview.getFailedTests(), suite).size();
                qty_fail += q;
                summaryCell(q, 0);

                time_start = Math.min(overview.getStartDate().getTime(), time_start);
                time_end = Math.max(overview.getEndDate().getTime(), time_end);
                summaryCell(formatter.format(
                        (overview.getEndDate().getTime() - overview.getStartDate().getTime()) / 1000.)
                        + " seconds", true);
                summaryCell(overview.getIncludedGroups());
                summaryCell(overview.getExcludedGroups());

                out.println("</tr>");
            }
        }
        if (qty_tests > 1) {
            out.println("<tr class=\"total\"><td>Total</td>");
            summaryCell(qty_pass_m, Integer.MAX_VALUE);
            summaryCell(qty_pass_s, Integer.MAX_VALUE);
            summaryCell(qty_skip, 0);
            summaryCell(qty_fail, 0);
            summaryCell(formatter.format((time_end - time_start) / 1000.) + " seconds", true);
            out.println("<td colspan=\"2\">&nbsp;</td></tr>");
        }
        out.println("</table>");
    }

    private void summaryCell(String[] val) {
        StringBuffer b = new StringBuffer();
        for (String v : val) {
            b.append(v + " ");
        }
        summaryCell(b.toString(), true);
    }

    private void summaryCell(String v, boolean isgood) {
        out.print("<td class=\"numi" + (isgood ? "" : "_attn") + "\">" + v + "</td>");
    }

    private void startSummaryRow(String label) {
        row += 1;
        out.print("<tr" + (row % 2 == 0 ? " class=\"stripe\"" : "")
                + "><td style=\"text-align:left;padding-right:2em\">" + label
                + "</td>");
    }

    private void summaryCell(int v, int maxexpected) {
        summaryCell(String.valueOf(v), v <= maxexpected);
        m_rowTotal += v;
    }

    /**
     *
     */
    private void tableStart(String cssclass) {
        out.println("<table cellspacing=0 cellpadding=0"
                + (cssclass != null ? " class=\"" + cssclass + "\""
                : " style=\"padding-bottom:2em\"") + ">");
        row = 0;
    }

    private void tableColumnStart(String label) {
        out.print("<th class=\"numi\">" + label + "</th>");
    }

    private void titleRow(String label, int cq) {
        out.println("<tr><th colspan=\"" + cq + "\">" + label + "</th></tr>");
        row = 0;
    }

    protected void writeStyle(String[] formats, String[] targets) {

    }

    /**
     * Starts HTML stream
     */
    protected void startHtml(PrintWriter out) {
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        out.println("<head>");
        out.println("<title>TestNG:  Unit Test</title>");
        out.println("<style type=\"text/css\">");
        out.println("table caption,table.info_table,table.param,table.passed,table.failed {margin-bottom:10px;border:1px solid #000099;border-collapse:collapse;empty-cells:show;}");
        out.println("table.info_table td,table.info_table th,table.param td,table.param th,table.passed td,table.passed th,table.failed td,table.failed th {");
        out.println("border:1px solid #000099;padding:.25em .5em .25em .5em");
        out.println("}");
        out.println("table.param th {vertical-align:bottom}");
        out.println("td.numi,th.numi,td.numi_attn {");
        out.println("text-align:right");
        out.println("}");
        out.println("tr.total td {font-weight:bold}");
        out.println("table caption {");
        out.println("text-align:center;font-weight:bold;");
        out.println("}");
        out.println("table.passed tr.stripe td,table tr.passedodd td {background-color: #00AA00;}");
        out.println("table.passed td,table tr.passedeven td {background-color: #33FF33;}");
        out.println("table.passed tr.stripe td,table tr.skippedodd td {background-color: #cccccc;}");
        out.println("table.passed td,table tr.skippedodd td {background-color: #dddddd;}");
        out.println("table.failed tr.stripe td,table tr.failedodd td,table.param td.numi_attn {background-color: #FF3333;}");
        out.println("table.failed td,table tr.failedeven td,table.param tr.stripe td.numi_attn {background-color: #DD0000;}");
        out.println("tr.stripe td,tr.stripe th {background-color: #E6EBF9;}");
        out.println("p.totop {font-size:85%;text-align:center;border-bottom:2px black solid}");
        out.println("div.shootout {padding:2em;border:3px #4854A8 solid}");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
    }

    /**
     * Finishes HTML stream
     */
    protected void endHtml(PrintWriter out) {
        out.println("</body></html>");
    }

    // ~ Inner Classes --------------------------------------------------------

    /**
     * Arranges methods by classname and method name
     */
    private class TestSorter implements Comparator<IInvokedMethod> {
        // ~ Methods -------------------------------------------------------------

        /**
         * Arranges methods by classname and method name
         */
        @Override
        public int compare(IInvokedMethod o1, IInvokedMethod o2) {
            return (int) (o1.getDate() - o2.getDate());
        }
    }

}

