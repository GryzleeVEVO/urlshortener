plugins {
    id("kotlin")
}

group = "es.unizar.urlshortener.gateways"
version = "0.2023.1-SNAPSHOT"

dependencies {
    implementation(project(":gateways"))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}