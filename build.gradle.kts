plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    maven(
        url = "https://central.sonatype.com/repository/maven-snapshots/",
    )
    maven(
        url = "https://oss.sonatype.org/service/local/staging/deploy/maven2",
    )
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
//    implementation("net.codecrete.usb:java-does-usb:1.2.1")
    implementation("org.hid4java:hid4java:0.8.0")
    implementation("com.pi4j:pi4j-core:5.0.0-SNAPSHOT")
  //  implementation("com.pi4j:pi4j-plugin-ffm:4.0.1")
    implementation("com.pi4j:pi4j-drivers:1.0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}