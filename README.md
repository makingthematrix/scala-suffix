# scala-suffix Maven Plugin

A Maven plugin which fixes Scala dependencies incompatible with Java 9+. 

### A bit of context

First of all, you need this plugin only in a corner case situation when:

1. You have Scala dependencies in your project.
2. You use Maven.
3. You use JDK 9 or newer.
4. At least one of your Scala dependencies lacks the `Automatic-Module-Name` property in its `MANIFEST.MF`.

Using Maven with Scala libraries is rare, so there's not much demand to solve this issue. But if a project makes extensive use of Java libraries, it might make sense to build it with Maven instead - or if it uses a Maven plugin which has to sbt alternative, [as in my case](https://github.com/makingthematrix/scalaonandroid). So, if any of those points is not true, you will either not need `scala-suffix-maven-plugin` or it will not help you. But if they are, and you're here, it's possible that it's because you have already been bitten by a problem similar to the following:

You add a dependency to your `pom.xml` which looks somehow like this
```
<dependency>
   <groupId>your.scala.dependency</groupId>
   <artifactId>your-scala-dependency_2.13</artifactId>
   <version>1.0.0</version>
</dependency>
```
You compile the project and you see this warning:
```
[WARNING] There are 1 pathException(s). The related dependencies will be ignored.
[WARNING] Dependency: <user home>/.m2/repository/<path to jar>/your-scala-dependency_2.13/1.0.0/your-scala-dependency_2.13-1.0.0.jar
   - exception: Unable to derive module descriptor for <user home>/.m2/repository/<path to jar>/your-scala-dependency_2.13/1.0.0/your-scala-dependency_2.13-1.0.0.jar
   - cause: your.scala.dependency.2.13: Invalid module name: '2' is not a Java identifier
```

... and then when you run your program, it crashes when it tries to access the given Scala dependency.
Paraphrasing [this answer on Stack Overflow](https://stackoverflow.com/questions/48714633/automatic-module-name-containing-number/48714979#48714979), since Java 9, Java does not recognize suffixes in modules names like `_2.13` as version numbers and treat them as integral parts of modules names. So, when your project tries to use a class from the Scala dependency, it will look for `your.scala.dependency.2.13` instead of just `your.scala.dependency`, it will fail to do it, and it will crash.


### Usage

Add this to the `<plugins>` section of your `pom.xml`:
```
<plugin>
  <groupId>io.github.makingthematrix</groupId>
  <artifactId>scala-suffix-maven-plugin</artifactId>
  <version>0.2.1</version>
  <configuration>
    <libraries>
      <param>your-scala-dependency</param>
    </libraries>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>suffix</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```
where `your-scala-dependency` is a name of your Scala dependency **without** the version suffix (if there are more than one, just add them with more `<param>` tags). This should be the same as `artifactId` in your `<dependency>` section.
Or you can use:
```
<param>groupId:artifactId</param>
```
if there are more than one dependency with the same `artifactId` in your project (I noticed that `core` is a popular name).

The plugin modifies the dependency's JAR file in your local Maven repository. It opens the jar, reads `META-INF/MANIFEST.MF` and adds to it a line:
```
Automatic-Module-Name: your-scala-dependency
```
If the property `Automatic-Module-Name` already exists, the plugin does nothing - we assume that in that case the dependency should already work. This prevents the plugin from modifying the same JAR file more than once. 

If you find any problems or if you think some kind of an extended functionality would be valuable, feel free to open a ticket under the **Issues** tab here on GitHub. I will se what I can do.

### Tests

Here's [a small Scala+JavaFX project, built with Maven](https://github.com/makingthematrix/scalaonandroid/tree/main/HelloFxml2), which you can use to test how the plugin works. 

### Potential problems

1. If `Automatic-Module-Name` already exists but is set to a value that is still invalid for Java 9+, the plugin won't fix this.
2. The plugin changes the contents of the JAR file, but it does not update the checksum. If you check it later on, it won't match.
3. The plugin relies on that you don't need two versions of the same Scala library for different Scala versions. If you do (but... why?) it will modify only one of them and ignore the other.

### Links to more discussion on this issue

* https://dzone.com/articles/automatic-module-name-calling-all-java-library-maintainers
* https://users.scala-lang.org/t/scala-jdk-11-and-jpms/6102
* https://stackoverflow.com/questions/48714633/automatic-module-name-containing-number/48714979#48714979
* https://stackoverflow.com/questions/46683561/how-to-resolve-a-maven-dependency-with-a-name-that-is-not-compliant-with-the-jav/46685325#46685325
* https://stackoverflow.com/questions/59844195/how-to-add-spark-dependencies-in-spring-boot-multi-module-java-11-project/59844858#59844858
* https://stackoverflow.com/questions/46501388/unable-to-derive-module-descriptor-for-auto-generated-module-names-in-java-9

### But wait! That's not all!

If you are a creator of a Scala library, you can simply add the `Automatic-Module-Name` property to `META-INF/MANIFEST.MF` by yourself. In most cases, people will use your library with sbt and then this won't be necessary, but it won't hurt them either, and while doing this you will help people who use Maven. In case you use sbt for building your library, all you need to do is at these two lines somewhere in your `build.sbt`:
```
Compile / packageBin / packageOptions +=
  Package.ManifestAttributes("Automatic-Module-Name" -> NAME)
```
(`NAME` here is the name of the library, without the version suffix).
