plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.sqldelight)
    jacoco
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.pt.glancewords.data.database")
        }
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

dependencies {
    implementation(project(":logging"))
    implementation(project(":domain"))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.google.api.client)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.api.services.sheets)
    implementation(libs.okio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.sqldelight.primitive.adapters)
    testImplementation(project(":testCommon"))
    testImplementation(libs.sqldelight.driver.jvm)
    testImplementation(libs.okio.fakefilesystem)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}