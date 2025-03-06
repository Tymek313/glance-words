plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":logging"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":testCommon"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
}