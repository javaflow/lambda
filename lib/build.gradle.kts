plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.slf4j:slf4j-api:2.0.7")
    api("io.github.resilience4j:resilience4j-retry:2.0.2")
    implementation("io.vavr:vavr:0.10.4")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.9.1")
        }
    }
}
