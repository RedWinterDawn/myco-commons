# Myco Commons BOM

This module provide a 
[bill of materials (BOM) POM](http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Importing_Dependencies)  
to ease the use of Myco Commons within other projects.

* **[Usage](#usage)**
* **[Change Log](../README.md#changes)**

### <a name="usage"></a>Usage

Add the BOM artifact into the dependencies management section of your POM with scope "import" and 
type "pom".  In the dependencies section of your project, you can now specify any of the Myco 
Commons artifacts without the need to specify a version number.  The BOM POM ensures that all of 
the artifacts supplied from the Myco Commons BOM POM have consistent versioning and that developers 
do not need to repeat dependency management boilerplate code across projects.

```xml
  ...

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.jive.myco.commons</groupId>
        <artifactId>myco-commons-bom</artifactId>
        <version><MYCO_COMMONS_VERSION></version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  ...
```