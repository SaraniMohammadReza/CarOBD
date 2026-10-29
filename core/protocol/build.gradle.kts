import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    id("org.jetbrains.kotlin.jvm")
}

// AGP supplies KGP on the shared classpath. Reuse it without a second version
// request, while rejecting any drift from the committed compiler version.
check(project.getKotlinPluginVersion() == libs.versions.kotlin.get()) {
    "AGP-provided Kotlin plugin ${project.getKotlinPluginVersion()} does not match ${libs.versions.kotlin.get()}"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
