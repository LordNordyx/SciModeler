import java.time.Duration
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "si.lordnordyx.scimodeler"
val appVersion = providers.gradleProperty("appVersion").get()
version = appVersion

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("dev.chrisbanes.haze:haze:1.2.2")

    testImplementation(kotlin("test-junit"))
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(compose.desktop.currentOs)
}

tasks.test {
    useJUnit()
    timeout.set(Duration.ofMinutes(10))
    // UI tests render offscreen; screenshots of the main flows land in build/screenshots.
    systemProperty("scimodeler.screenshots", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
    testLogging { events("passed", "failed"); showStandardStreams = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

compose.desktop {
    application {
        mainClass = "scimodeler.MainKt"
        jvmArgs += listOf("-Xmx1g")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SciModeler"
            packageVersion = appVersion
            description = "Editor for scientific models and diagrams"
            vendor = "LordNordyx"
            modules("java.desktop", "java.datatransfer")

            windows {
                iconFile.set(project.file("packaging/icon.ico"))
                menu = true
                shortcut = true
                perUserInstall = true
                dirChooser = true
                menuGroup = "SciModeler"
                upgradeUuid = "8a0c6f2e-4b7d-4d3e-9a51-5c2f1e7b9d40"
            }
            linux { iconFile.set(project.file("packaging/icon.png")) }
            macOS {
                iconFile.set(project.file("packaging/icon.icns"))
                bundleID = "si.lordnordyx.scimodeler"
                appCategory = "public.app-category.productivity"
            }
        }
    }
}
