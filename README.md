##QUnit and TestNG Integration in Maven using PhantomJS

### What is this?

This is a way to let you run your QUnit tests along with your regular tests in Maven. Test failures are reports are generated just as you would expect, with TestNG. This basically lets you integrate your QUnit tests into your continuous-integration process.

### How do I use this?

To see a sample project, check out the [`using-qunit-testng`](https://github.com/vivin/using-qunit-testng) project. That should give you a general idea. There are a few things you will need to do, to use this project. First, you will have to add it as a dependency. I haven't uploaded this to Maven's central repository, so for now you will have to check out the source, build it, and include it as a snapshot dependency:

    <dependency>
        <groupId>net.vivin</groupId>
        <artifactId>qunit-testng</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

Then, you will need to add a maven plugin that instructs maven to unpack this project into your test output-directory, so that you can actually use the JavaScript files that come with this project:

    <build>

        ...

        <plugins>

            ...

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>unpack-qunit-testng</id>
                        <phase>generate-test-resources</phase>
                        <goals>
                            <goal>unpack</goal>
                        </goals>
                        <configuration>
                            <artifactItems>
                                <artifactItem>
                                    <groupId>net.vivin</groupId>
                                    <artifactId>qunit-testng</artifactId>
                                    <type>jar</type>
                                    <outputDirectory>
                                        ${project.build.testOutputDirectory}
                                    </outputDirectory>
                                    <overWrite>true</overWrite>
                                </artifactItem>
                            </artifactItems>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            ...

        </plugins>

        ...

    </build>

After this, you will also have to instruct maven to copy over your QUnit tests and JavaScript files into the test output-directory. This way you have access to all your JavaScript libraries, and more importantly, your JavaScript file that you actually want to test. My convention is to put my QUnit tests in `src/test/javascript`, and my JavaScript and CSS files in `src/main/webapp`. You can tell maven to copy your test resources over like so:

    <build>

        ...

        <testResources>
            <testResource>
                <directory>src/test/javascript</directory>
            </testResource>
            <testResource>
                <directory>src/main/webapp</directory>
                <includes>
                    <include>js/**/*.js</include>
                    <include>css/**/*.css</include>
                </includes>
            </testResource>
        </testResources>

        ...

    </build>

Having done this, maven is properly set up to run your tests. But you're not done yet! There are a few things you need to do so that your QUnit tests will communicate properly with PhantomJS. The changes aren't that drastic, and are minimal.

The first thing you will need to do, is modify your QUnit test file. You will have to wrap your entire test suite with a call to `QUnitTestSuite.create`:

    QUnitTestSuite.create(function() {

        //QUnit test code here

    });

Then in your HTML file (**which must end in `Test.html`. So for example, a file like `complexTest.html`**), you will need to explicitly run your test suite. Assuming that your QUnit test HTML file is in the same directory as your test JavaScript file, you will have to include a reference to `QUnitTestSuite.js` and then run your test using `QUnitTestSuite#run(String)`: 

    <html>
    <head>
        <meta charset="utf-8">
        <title>QUnit Example</title>
        <link rel="stylesheet" type="text/css" href="../css/qunit/qunit.css" />
    </head>
    <body>
    <div id="qunit"></div>
    <div id="qunit-fixture"></div>
    <script src="../js/qunit/qunit.js"></script>
    <script src="../js/jquery/jquery.js"></script>

    <!-- QUnitTestSuite.js will always be inside /qunit -->
    <script src="../qunit/QUnitTestSuite.js"></script>
    <script src="../js/complex/complex.js"></script>
    <script type="text/javascript">
        <!-- Instruct QUnitTestSuite to run the test. This sets up some bootstrapping that allows 
             QUnit to talk to PhantomJS -->
        QUnitTestSuite.run("./complexTest.js");
    </script>
    </body>
    </html>

You will also notice that you can reference your JavaScript library files as if you were inside `src/main/webapp`. This is because, as I mentioned before, we've instructed maven to copy all the JavaScript resources over to the test output-directory.

That's all there is to it! The maven changes will only need to be made once in your project, and subsequent QUnit tests only need to implement the minimal changes described above!
