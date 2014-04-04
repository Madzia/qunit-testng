package net.vivin.qunit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA.
 * User: vivin
 * Date: 8/6/12
 * Time: 9:06 AM
 *
 * This annotation is used to mark a test as a QUnit test suite
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QUnitTestSuite {
}

