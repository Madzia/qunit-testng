/**
 * This is a simple harness that takes care of setting up a bunch of things; it's essentially bootstrapping. It sets up the QUnit callbacks
 * required to log the test results.
 */
var QUnitTestSuite = (function () {

    function run(pathToSuite) {
        //Load up the test suite
        var head = document.getElementsByTagName("head")[0];
        var script = document.createElement("script");

        script.type = "text/javascript";
        script.src = pathToSuite + "?t=" + new Date().getTime(); //prevent caching

        head.appendChild(script);
    }

    function create(suiteSetupFunction) {
        jQuery(function() {
            //Disable QUnit autostart. We'll start QUnit when we're ready
            QUnit.config.autostart = false;

            //Set up the logging callback. We don't care how early this runs because all this does is report the result of a test to the console.
            //This is how we communicate the results of our tests to PhantomJS
            QUnit.log(function (result) {

                //The following bits of code translates the expected and actual values from QUnit into a stringified
                //form that makes sense in a test-failure message. The expected and actual values can be regular
                //expressions, Error objects, or functions, so we need to convert them into a form that can be
                //easily understood. This method is not foolproof; there are some cases where we don't really have
                //any meaningful information. However, all we care about here is that we are reporting out failures
                //properly.
                function isFunction(func) {
                    return func && {}.toString.call(func) === '[object Function]';
                }

                var actual = result.actual;
                if(actual != null && typeof actual !== "string" && typeof actual !== "undefined") {
                    if(actual instanceof Error || actual.prototype instanceof Error) {
                        if(actual instanceof Error) {
                            result.actual = actual.name + ": " + actual.message;
                        } else {
                            result.actual = actual.name;
                        }

                    } else if(isFunction(actual)) {
                        result.actual = actual().toString();
                    } else if(actual instanceof RegExp) {
                        result.actual = actual.toString();
                    } else if(typeof actual === "object") {
                        result.actual = JSON.stringify(actual);
                    } else {
                        result.actual = actual.toString();
                    }
                } else if(actual === null) {
                    result.actual = "null (not set by QUnit)";
                }

                var expected = result.expected;
                if(expected != null && typeof expected !== "string" && typeof expected !== "undefined") {
                    if(expected instanceof Error || expected.prototype instanceof Error) {
                        if(expected instanceof Error) {
                            result.expected = expected.name + ": " + expected.message;
                        } else {
                            result.expected = expected.name;
                        }

                    } else if(isFunction(expected)) {
                        result.expected = expected().toString();
                    } else if(expected instanceof RegExp) {
                        result.expected = expected.toString();
                    } else if(typeof expected === "object") {
                        result.expected = JSON.stringify(expected);
                    } else {
                        result.expected = expected.toString();
                    }
                } else if(expected === null) {
                    result.expected = "null (not set by QUnit)";
                }

                console.log("__qUnitTestDriver__:" + JSON.stringify({
                    type: "log",
                    result: result
                }));
            });

            //Run the suite-setup function. What this does is set up the test suite, but does NOT run it. The reason is that there are weird
            //timing issues when running the tests in PhantomJS, that causes the QUnit.done event to misfire (i.e., it fires early, after the
            //first test function has been defined). To prevent this from happening, we will set up the test suite, then set up the "done"
            //callback, and finally start the tests when we are ready
            suiteSetupFunction.apply();

            //Setup the "done" callback after we initialize the suite so that it doesn't misfire
            QUnit.done(function () {
                console.log("__qUnitTestDriver__:" + JSON.stringify({
                    type: "done"
                }));
            });

            //Start the test suite
            QUnit.start();
        });
    }

    return {
        run: run,
        create: create
    }
})();
