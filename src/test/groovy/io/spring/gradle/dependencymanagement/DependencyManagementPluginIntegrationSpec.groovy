/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Integration tests for {@link DependencyManagementPlugin}.
 *
 * @author Andy Wilkinson
 */
class DependencyManagementPluginIntegrationSpec extends Specification {

    @Rule
    final TemporaryFolder projectFolder = new TemporaryFolder()

    private File buildFile

    def setup() {
        buildFile = projectFolder.newFile('build.gradle')
    }


    def "Poms generated by the maven plugin are customized"() {
        given: 'A project with the maven plugin applied and some dependency management'

        buildFile << """
            buildscript {
                dependencies {
                    classpath files('${new File("build/classes/main").getAbsolutePath()}',
                            '${new File("build/resources/main").getAbsolutePath()}',
                            '${new File("build/libs/maven-repack-3.0.4.jar").getAbsolutePath()}')
                }
            }

            apply plugin: 'io.spring.dependency-management'
            apply plugin: 'java'
            apply plugin: 'maven'

            repositories {
                mavenCentral()
            }

            dependencyManagement {
                imports {
                    mavenBom 'org.springframework.boot:spring-boot-dependencies:1.4.2.RELEASE'
                }
            }
        """

        when: 'The install task is run'

        GradleRunner.create().withProjectDir(projectFolder.root).withArguments("install").build()

        then: 'The generated pom contains dependency management'

        new File(projectFolder.root, "build/poms/pom-default.xml").text.contains("<dependencyManagement>")

    }

    def "Poms generated by the maven publish plugin are customized"() {
        given: 'A project with the maven-publish plugin applied and some dependency management'

        buildFile << """
            buildscript {
                dependencies {
                    classpath files('${new File("build/classes/main").getAbsolutePath()}',
                            '${new File("build/resources/main").getAbsolutePath()}',
                            '${new File("build/libs/maven-repack-3.0.4.jar").getAbsolutePath()}')
                }
            }

            apply plugin: 'io.spring.dependency-management'
            apply plugin: 'java'
            apply plugin: 'maven-publish'

            repositories {
                mavenCentral()
            }

            publishing {
               publications {
                    mavenJava(MavenPublication) {
                        from components.java
                    }
                }
            }

            dependencyManagement {
                imports {
                    mavenBom 'org.springframework.boot:spring-boot-dependencies:1.4.2.RELEASE'
                }
            }
        """

        when: 'The generate pom task is run'

        GradleRunner.create().withProjectDir(projectFolder.root).withArguments("generatePomFileForMavenJavaPublication").build()

        then: 'The generated pom contains dependency management'

        new File(projectFolder.root, "build/publications/mavenJava/pom-default.xml").text.contains("<dependencyManagement>")
    }

    def "Pom customization doesn't stop custom conf-to-scope mappings from working"() {
        given: 'A project with a custom configuration and scope mapping'

        buildFile << """
            buildscript {
                dependencies {
                    classpath files('${new File("build/classes/main").getAbsolutePath()}',
                            '${new File("build/resources/main").getAbsolutePath()}',
                            '${new File("build/libs/maven-repack-3.0.4.jar").getAbsolutePath()}')
                }
            }

            apply plugin: 'io.spring.dependency-management'
            apply plugin: 'java'
            apply plugin: 'maven'

            repositories {
                mavenCentral()
            }

            configurations {
                another
            }

            conf2ScopeMappings.addMapping(1000, project.configurations.another, "compile")

            dependencies {
                another "commons-logging:commons-logging:1.2"
            }
        """

        when: 'The install task is run'

        GradleRunner.create().withProjectDir(projectFolder.root).withArguments("install").build()

        then: 'The generated pom contains the dependency in the custom configuration mapped to the right scope'

        new File(projectFolder.root, "build/poms/pom-default.xml").text.contains("commons-logging")
    }

    def "Using importedProperties does not prevent further configuration of the publishing extension"() {
        when: 'A project the uses importedProperties and configures the publishing extension'

        buildFile << """
            buildscript {
                dependencies {
                    classpath files('${new File("build/classes/main").getAbsolutePath()}',
                            '${new File("build/resources/main").getAbsolutePath()}',
                            '${new File("build/libs/maven-repack-3.0.4.jar").getAbsolutePath()}')
                }
            }

            apply plugin: 'io.spring.dependency-management'
            apply plugin: 'java'
            apply plugin: 'maven-publish'

            repositories {
                mavenCentral()
            }

            dependencyManagement {
                dependencies {
                    dependency "com.foo:bar:\${importedProperties['bar.version']}"
                }
            }

            publishing {
               publications {
                    mavenJava(MavenPublication) {
                        from components.java
                    }
                }
            }
        """

        then: 'The project builds successfully'

        GradleRunner.create().withProjectDir(projectFolder.root).build()
    }

}
