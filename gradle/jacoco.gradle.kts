tasks.register<JacocoReport>("generateCodeCoverageReport") {
    group = "Reporting"
    description = "Execute unit tests, generate and combine Jacoco coverage report"

    reports {
        html.required.set(true)
    }

    // Set source directories to the main source directory
    sourceDirectories.setFrom(layout.projectDirectory.dir("src/main"))
    // Set class directories to compiled Java and Kotlin classes, excluding specified exclusions
    classDirectories.setFrom(files(
        fileTree(layout.buildDirectory.dir("classes/kotlin/main"))
    ))
    // Collect execution data from .exec and .ec files generated during test execution
    executionData.setFrom(files(
        fileTree(layout.buildDirectory) { include(listOf("**/*.exec", "**/*.ec")) }
    ))

    dependsOn("test")
}

tasks.register<JacocoCoverageVerification>("verifyCodeCoverage"){
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.9".toBigDecimal()
            }
        }
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.9".toBigDecimal()
            }
        }
    }
    // Set source directories to the main source directory
    sourceDirectories.setFrom(layout.projectDirectory.dir("src/main"))
    // Set class directories to compiled Java and Kotlin classes, excluding specified exclusions
    classDirectories.setFrom(files(
        fileTree(layout.buildDirectory.dir("classes/kotlin/main"))
    ))
    // Collect execution data from .exec and .ec files generated during test execution
    executionData.setFrom(files(
        fileTree(layout.buildDirectory) { include(listOf("**/*.exec", "**/*.ec")) }
    ))
    dependsOn("generateCodeCoverageReport")
}