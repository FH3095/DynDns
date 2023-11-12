
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
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
	implementation("com.github.spotbugs:spotbugs-annotations:4.8.1")
	implementation("com.google.guava:guava:32.1.3-jre")
	compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    testImplementation("org.assertj:assertj-core:3.23.1")

	testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
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
