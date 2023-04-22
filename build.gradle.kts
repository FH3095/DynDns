
plugins {
	eclipse
    java
    war
}

repositories {
    mavenCentral()
}

allprojects {
    group = "eu.4fh"
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
	implementation("com.github.spotbugs:spotbugs-annotations:4.7.3")
	implementation("com.google.guava:guava:31.1-jre")
	compileOnly("javax.servlet:javax.servlet-api:4.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    testImplementation("org.assertj:assertj-core:3.23.1")

	testImplementation("javax.servlet:javax.servlet-api:4.0.0")
}

val test by tasks.getting(Test::class) {
    // Use junit platform for unit tests
    useJUnitPlatform()
}

tasks.register<Copy>("warToTomcat") {
	dependsOn("war")
	from(layout.buildDirectory.dir("libs"))
	include("*.war")
	into(layout.buildDirectory.dir("../../../Tomcat/webapps"))
}
