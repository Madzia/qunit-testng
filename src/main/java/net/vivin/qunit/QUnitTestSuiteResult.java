package net.vivin.qunit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: vivin
 * Date: 8/2/12
 * Time: 4:23 PM
 *
 * Java Object that represents the JSON returned by QUnitTestDriver.js. We deserialize the JSON into this object.
 */
public class QUnitTestSuiteResult {

    //Using a concrete type on the inner map because I want to preserve order
    private Map<String, LinkedHashMap<String, List<QUnitTestResult>>> results = new LinkedHashMap<String,LinkedHashMap<String, List<QUnitTestResult>>>();

    public Map<String, LinkedHashMap<String, List<QUnitTestResult>>> getResults() {
        return results;
    }

    public void setResults(Map<String, LinkedHashMap<String, List<QUnitTestResult>>> results) {
        this.results = results;
    }
}

