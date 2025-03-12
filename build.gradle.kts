import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply true
    jacoco
}

apply("coverage.gradle.kts")

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        verbose = true
        android = true
        reporters {
            reporter(ReporterType.HTML)
        }
        filter {
            exclude { element ->
                element.file.path.contains("${File.separator}generated${File.separator}")
            }
        }
    }
}