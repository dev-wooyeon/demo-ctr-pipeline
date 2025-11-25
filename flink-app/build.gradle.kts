plugins {
    java
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

val flinkVersion = "1.18.1"

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Flink (provided by cluster, but needed for compile)
    // Use 'implementation' for local run, 'compileOnly' for cluster deployment if strictly managed
    // Here we use 'implementation' but exclude them in shadowJar if needed, or rely on shadow plugin's provided scope handling
    implementation("org.apache.flink:flink-java:${flinkVersion}")
    implementation("org.apache.flink:flink-streaming-java:${flinkVersion}")
    implementation("org.apache.flink:flink-clients:${flinkVersion}")
    
    // Connectors
    implementation("org.apache.flink:flink-connector-kafka:${flinkVersion}")
    implementation("org.apache.flink:flink-connector-jdbc:3.1.2-1.18")
    
    // Drivers
    implementation("com.clickhouse:clickhouse-jdbc:0.4.6")
    implementation("org.duckdb:duckdb_jdbc:0.9.2")
    implementation("redis.clients:jedis:5.1.0") // Using Jedis directly or Flink Redis connector if available

    // Utils
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Metrics
    implementation("org.apache.flink:flink-metrics-prometheus:${flinkVersion}")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.apache.flink:flink-test-utils:${flinkVersion}")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("") // Remove '-all' suffix
    
    // Flink dependencies are usually provided by the cluster
    // But for simplicity in Docker, we often bundle everything or exclude core flink libs
    // If we run 'java -jar', we need everything. If we run 'flink run', we should exclude flink-dist.
    // For this setup, we will create a fat jar that works.
    
    mergeServiceFiles()
    
    manifest {
        attributes(
            "Main-Class" to "com.example.ctr.CtrApplication"
        )
    }
}

// Disable standard jar task to avoid confusion with shadowJar
tasks.getByName<Jar>("jar") {
    enabled = false
}
