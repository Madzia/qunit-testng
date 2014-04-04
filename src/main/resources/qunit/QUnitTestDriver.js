/**
 * QUnitTestDriver.js: A test-driver for QUnit tests. The PhantomJS script loads a QUnit HTML file and reports test results. The script does this by
 * looking for console messages that the QUnit test is expected to send. The result of the test suite is printed out to STDOUT in JSON.
 *
 * Author: Vivin Paliath
 *
 */

var QUnitTestDriver = (function () {

    //If we have any errors in our PhantomJS script, we want to fail
    phantom.onError = function(message, trace) {
        var suite = {results: {}};
        suite.results["global"] = {};
        suite.results["global"]["global failure"] = [];

        var source = "";
        trace.forEach(function (item) {
            source += "    at " + item.file + ":" + item.line + "\n";
        });

        suite.results["global"]["global failure"].push({
            testNumber: 1,
            message: message,
            expected: null,
            actual: null,
            source: source,
            failure: true
        });

        console.log(JSON.stringify(suite));
        phantom.exit(1);
    };

    var args = {
        SCRIPT_NAME: 0,
        QUNIT_TEST_FILE: 1,
        TIMEOUT: 2
    };

    var DEFAULT_TIMEOUT = 15;
    var MILLISECONDS_IN_A_SECOND = 1000;

    var timeout = DEFAULT_TIMEOUT;

    function start() {
        var system = require("system");
        var fs = require("fs");

        if (system.args.length === 1) {
            console.log("Syntax phantomjs", system.args[args.SCRIPT_NAME], "</path/to/qunit-test.html>", "[<timeout-in-seconds>]");
            phantom.exit(1);
        }

        //If we haven't been provided an absolute path, let's convert it
        var path_to_test = system.args[args.QUNIT_TEST_FILE];
        if ((!/^\//.test(path_to_test) && system.os.name !== "windows") ||
            ((!/^[A-Z]:\\/.test(path_to_test) && !/\\\\/.test(path_to_test)) && system.os.name === "windows")) {
            path_to_test = fs.workingDirectory + "/" + path_to_test
        }

        if (typeof system.args[args.TIMEOUT] !== "undefined") {
            if (!/^[0-9]+$/.test(system.args[args.TIMEOUT])) {
                console.log("Timeout value must be a number, if supplied!");
                phantom.exit(1);
            } else {
                timeout = system.args[args.TIMEOUT];
            }
        }

        if (!fs.exists(path_to_test)) {
            console.log("Unable to open test at", path_to_test);
            phantom.exit(1);
        }

        runTest(path_to_test, function () {
            if (this.message) {
                console.log(this.message);
            }

            phantom.exit(this.failed ? 1 : 0);
        });
    }

    function runTest(path, callback) {
        var page = require("webpage").create();

        var timeoutId;
        var failed = false;
        var testNumber = 1;
        var suite = {
            results: {}
        };

        //Although we try to catch all sorts of errors on the page, including Javascript errors, it is possible for certain pages to stop responding.
        //This usually means that something is wrong on the page. We don't want our testing process to hang indefinitely, so we start up a
        //timeout that will stop the driver if the script hasn't responded for 15 seconds (this is the default).
        var timeoutHandler = function () {
            if (!suite.results["global"]) {
                suite.results["global"] = {};
            }

            if (!suite.results["global"]["global failure"]) {
                suite.results["global"]["global failure"] = [];
            }

            suite.results["global"]["global failure"].push({
                testNumber: testNumber++,
                message: "file://" + path + " timed out. No activity for " + timeout + " seconds.",
                expected: null,
                actual: null,
                source: "    at " + path,
                failure: true
            });

            callback.apply({
                failed: true,
                message: JSON.stringify(suite)
            });
        };

        //On error we need to record the error and exit immediately
        page.onError = function (message, trace) {
            suite.results["global"] = {};
            suite.results["global"]["global failure"] = [];

            var source = "";
            trace.forEach(function (item) {
                source += "    at " + item.file + ":" + item.line + "\n";
            });

            suite.results["global"]["global failure"].push({
                testNumber: testNumber++,
                message: message,
                expected: null,
                actual: null,
                source: source,
                failure: true
            });

            callback.apply({
                failed: true,
                message: JSON.stringify(suite)
            });
        };

        page.onConsoleMessage = function (message) {

            if (/^__qUnitTestDriver__:/.test(message)) {

                //We got some activity, so let's clear the timeout! We don't want to erroneously timeout!
                clearTimeout(timeoutId);

                message = JSON.parse(message.replace(/^__qUnitTestDriver__:/, ""));
                var result = message.result;

                //log(JSON.stringify(result, null, 3));

                if (message.type === "log") {

                    if (typeof result.module === "undefined") {
                        result.module = "global";
                    }

                    var globalFailure = result.name === "global failure";

                    if (!suite.results[result.module]) {
                        suite.results[result.module] = {};
                    }

                    if (!suite.results[result.module][result.name]) {
                        suite.results[result.module][result.name] = [];
                    }

                    if (!result.result) {
                        failed = true;
                    }

                    var expected = result.expected;
                    var actual = result.actual;

                    //If both actual and expected are objects, then deepEquals was used. We need to stringify these
                    //objects, otherwise Gson will assume they are actual objects that need to be deserialized
                    if (typeof expected !== "undefined" && typeof actual !== "undefined" && typeof expected === "object" && typeof actual === "object") {
                        expected = JSON.stringify(expected);
                        actual = JSON.stringify(actual);
                    }

                    suite.results[result.module][result.name].push({
                        testNumber: testNumber++,
                        message: result.message,
                        expected: expected,
                        actual: actual,
                        source: result.source,
                        failure: !result.result
                    });
                }

                if (message.type === "done" || globalFailure) {
                    callback.apply({
                        failed: failed,
                        message: JSON.stringify(suite)
                    });
                }
                //Start up a new timeout
                timeoutId = setTimeout(timeoutHandler, timeout * MILLISECONDS_IN_A_SECOND);
            }
        };

        page.open("file://" + path, function (status) {
            if (status !== "success") {
                callback.apply({
                    failed: true,
                    message: "Unable to run test at file://" + path
                });
            }

            timeoutId = setTimeout(timeoutHandler, timeout * MILLISECONDS_IN_A_SECOND);
        });
    }

    return {
        start: start
    }
})();

QUnitTestDriver.start();
