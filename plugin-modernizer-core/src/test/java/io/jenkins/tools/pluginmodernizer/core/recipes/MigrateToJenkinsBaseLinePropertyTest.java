package io.jenkins.tools.pluginmodernizer.core.recipes;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

public class MigrateToJenkinsBaseLinePropertyTest implements RewriteTest {

    @DocumentExample
    @Test
    void addComment() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
                    @Override
                    public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                        doAfterVisit(new AddCommentVisitor<>((Xml.Tag) requireNonNull(x.getRoot().getContent()).get(0), "2.0"));
                        return super.visitDocument(x, ctx);
                    }
                })),
                xml(
                        """
                          <dependency>
                              <version/>
                          </dependency>
                          """,
                        """
                          <dependency>
                              <version>2.0</version>
                          </dependency>
                          """
                )
        );
    }

    @Test
    void testNoChanges() {
        rewriteRun(
                spec -> spec.recipe(new MigrateToJenkinsBaseLineProperty()),
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <parent>
                     <groupId>org.jenkins-ci.plugins</groupId>
                     <artifactId>plugin</artifactId>
                     <version>4.88</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                        <jenkins.baseline>2.452</jenkins.baseline>
                        <jenkins.version>${jenkins.baseline}.4</jenkins.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>bom-${jenkins.baseline}.x</artifactId>
                         <version>3814.v9563d972079a_</version>
                         <type>pom</type>
                         <scope>import</scope>
                       </dependency>
                     </dependencies>
                   </dependencyManagement>
                   <repositories>
                     <repository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </repository>
                   </repositories>
                   <pluginRepositories>
                     <pluginRepository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </pluginRepository>
                   </pluginRepositories>
                 </project>
                """));
    }

    @Test
    void testAddBaseline() {
        rewriteRun(
                spec -> spec.recipe(new MigrateToJenkinsBaseLineProperty()),
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <parent>
                     <groupId>org.jenkins-ci.plugins</groupId>
                     <artifactId>plugin</artifactId>
                     <version>4.88</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                     <jenkins.version>2.452.4</jenkins.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>bom-2.452.x</artifactId>
                         <version>3814.v9563d972079a_</version>
                         <type>pom</type>
                         <scope>import</scope>
                       </dependency>
                     </dependencies>
                   </dependencyManagement>
                   <repositories>
                     <repository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </repository>
                   </repositories>
                   <pluginRepositories>
                     <pluginRepository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </pluginRepository>
                   </pluginRepositories>
                 </project>
                """,
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <parent>
                     <groupId>org.jenkins-ci.plugins</groupId>
                     <artifactId>plugin</artifactId>
                     <version>4.88</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                     <jenkins.baseline>2.452</jenkins.baseline>
                     <jenkins.version>${jenkins.baseline}.4</jenkins.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>bom-${jenkins.baseline}.x</artifactId>
                         <version>3814.v9563d972079a_</version>
                         <type>pom</type>
                         <scope>import</scope>
                       </dependency>
                     </dependencies>
                   </dependencyManagement>
                   <repositories>
                     <repository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </repository>
                   </repositories>
                   <pluginRepositories>
                     <pluginRepository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </pluginRepository>
                   </pluginRepositories>
                 </project>
                """));
    }

    @Test
    void testFixBom() {
        rewriteRun(
                spec -> spec.recipe(new MigrateToJenkinsBaseLineProperty()),
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <parent>
                     <groupId>org.jenkins-ci.plugins</groupId>
                     <artifactId>plugin</artifactId>
                     <version>4.88</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                     <jenkins.version>2.479.1</jenkins.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>bom-2.452.x</artifactId>
                         <version>3814.v9563d972079a_</version>
                         <type>pom</type>
                         <scope>import</scope>
                       </dependency>
                     </dependencies>
                   </dependencyManagement>
                   <repositories>
                     <repository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </repository>
                   </repositories>
                   <pluginRepositories>
                     <pluginRepository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </pluginRepository>
                   </pluginRepositories>
                 </project>
                """,
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <parent>
                     <groupId>org.jenkins-ci.plugins</groupId>
                     <artifactId>plugin</artifactId>
                     <version>4.88</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                     <jenkins.baseline>2.479</jenkins.baseline>
                     <jenkins.version>${jenkins.baseline}.1</jenkins.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>bom-${jenkins.baseline}.x</artifactId>
                         <version>3814.v9563d972079a_</version>
                         <type>pom</type>
                         <scope>import</scope>
                       </dependency>
                     </dependencies>
                   </dependencyManagement>
                   <repositories>
                     <repository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </repository>
                   </repositories>
                   <pluginRepositories>
                     <pluginRepository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </pluginRepository>
                   </pluginRepositories>
                 </project>
                """));
    }

    @Test
    void testNoChangesWithWeekly() {
        rewriteRun(
                spec -> spec.recipe(new MigrateToJenkinsBaseLineProperty()),
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <parent>
                     <groupId>org.jenkins-ci.plugins</groupId>
                     <artifactId>plugin</artifactId>
                     <version>4.88</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                        <jenkins.baseline>2.452</jenkins.baseline>
                        <jenkins.version>${jenkins.baseline}</jenkins.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>bom-weekly</artifactId>
                         <version>3814.v9563d972079a_</version>
                         <type>pom</type>
                         <scope>import</scope>
                       </dependency>
                     </dependencies>
                   </dependencyManagement>
                   <repositories>
                     <repository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </repository>
                   </repositories>
                   <pluginRepositories>
                     <pluginRepository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </pluginRepository>
                   </pluginRepositories>
                 </project>
                """));
    }
}
