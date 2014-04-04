QUnit.log(function (result) {
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

QUnit.done(function () {
    console.log("__qUnitTestDriver__:" + JSON.stringify({
        type: "done"
    }));
});

Object.defineProperty(Error.prototype, 'toJSON', {
    value: function () {
        var alt = {};

        Object.getOwnPropertyNames(this).forEach(function (key) {
            alt[key] = this[key];
        }, this);

        return alt;
    },
    configurable: true
});

function CustomException (message) {
    this.name = "CustomException";
    this.message = message;
};

CustomException.prototype = new Error();
CustomException.prototype.constructor = CustomException;

module("Module One");

test("Test case one", function () {
    equal("1", "1", "There must only be one result.");
    equal("something", "something", "The return value must equal \"something\".");
    throws(function () {
        throw "Invalid URL";
    }, /Invalid URL/, "Exception message must equal \"Invalid URL\".");
});

test("Test case two", function () {
    equal("2", "2", "There must be two results.");
    equal("nothing", "something", "The return value must equal \"nothing\".");
    throws(function () {
        throw "Invalid Email Address";
    }, /Invalid Email Address/, "Exception message must equal \"Invalid Email Address\".");
});

test("Test case three", function() {
    throws(function () {
        throw new CustomException();
    }, CustomException, "Exception must be instance of CustomException.");
});

test("Test case four", function() {
    throws(function () {
        throw new CustomException("Everything has failed!");
    }, /failed/, "Exception must contain the string \"failed\".");
});

test("Test case five", function() {
    throws(function () {
        throw "Failed";
    }, function(actual) { return actual === "Failed"; }, "Exception message must equal \"Failed\".");
});

module("Module Two");

test("All fail", function () {
    equal("3", "4", "There must be four results.");
    equal("everything", "nothing", "The return value must equal \"everything\".");
    throws(function () {
        throw "Oh yeah";
    }, /Invalid Phone Number/, "Exception message must equal \"Invalid Phone Number\".");
});

module("Module Three");

test("Test ok and deepEquals", function () {
    expect(100);
    ok(false, "Value must be true.");
    ok(true, "Other value must be true.");
    deepEqual({
        one: "1",
        two: "2",
        three: "3",
        stuff: {
            four: 4,
            five: 5
        }
    }, {
        one: "1",
        two: "2",
        three: "4",
        stuff: {
            four: 4,
            five: 6
        }
    }, "deepEqual does not match");
});

