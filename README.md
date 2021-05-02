# scala-suffix Maven Plugin

A Maven plugin which fixes Scala jars incompatibility with Java 9+. 

### A bit of context

First of all, you need this plugin only in a corner case situation when:
1. You have Scala dependencies in your project.
2. You use Maven.
3. You use JDK 9 or newer.
4. At least one of your Scala dependencies lacks the `Automatic-Module-Name` property in its `MANIFEST.MF`.
 
If any of those points is not true, `scala-suffix-maven-plugin` will not help you. But if they are, and you're here, it's possible that it's because you have already been bitten by a problem similar to the following:

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
  <version>0.0.3</version>
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

The plugin modifies the dependency's JAR file in your local Maven repository. It opens the jar, reads `META-INF/MANIFEST.MF` and adds to it a line:
```
Automatic-Module-Name: your-scala-dependency
```
if it is missing. If the property `Automatic-Module-Name` already exists, the plugin does nothing - we assume that in that case the dependency should already work. This prevents the plugin from modifying the same JAR file more than once. 

#### Potential problems

1. If `Automatic-Module-Name` already exists but is set to a value that is still invalid for Java 9+, the plugin won't fix this.
2. The plugin changes the contents of the JAR file, but it does not update the checksum. If you check it later on, it won't match.
3. The plugin relies on that you don't need two versions of the same Scala library for different Scala versions. If you do (but... why?) it will modify only one of them and ignore the other.
