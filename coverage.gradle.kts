val moduleClassesBuildPaths = listOf(
    "tmp/kotlin-classes/debug",
    "classes/kotlin/main"
)

val inclusions = mapOf(
    "app" to listOf("**/WidgetConfigurationViewModel.*")
)

val exclusions = mapOf(
    "data" to listOf(
        "com/pt/glancewords/data/di",
        "com/pt/glancewords/data/database"
    )
)

tasks.register<JacocoReport>("jacocoTestReport") {
    val projects = listOf(":app", ":data", ":domain").map(::project)

    dependsOn(projects.map { it.tasks.withType<JacocoReport>() })

    val compiledClasses = projects.map { subproject ->
        val inclusions = inclusions[subproject.name]
        val exclusions = exclusions[subproject.name]

        moduleClassesBuildPaths.map { buildPath ->
            fileTree(subproject.layout.buildDirectory.dir(buildPath)) {
                if (inclusions != null) include(inclusions)
                if (exclusions != null) exclude(exclusions)
            }
        }
    }

    classDirectories.setFrom(files(compiledClasses))
    sourceDirectories.setFrom(files(projects.map { it.layout.projectDirectory.dir("src/main/kotlin") }))

    executionData.setFrom(
        projects.map { subproject ->
            fileTree(subproject.layout.buildDirectory.get()).apply {
                include(
                    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                    "jacoco/test.exec"
                )
            }
        }
    )
}