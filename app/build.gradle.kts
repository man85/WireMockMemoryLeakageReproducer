plugins {
    kotlin("jvm") version "2.2.21"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.5.8")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.8")
    testImplementation("org.wiremock.integrations:wiremock-spring-boot:4.0.9")
    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.6.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    systemProperty("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer\$ClassName")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    maxHeapSize = "1g"
}
