import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "si.lordnordyx.scimodeler"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

compose.desktop {
    application {
        mainClass = "scimodeler.MainKt"
        jvmArgs += listOf("-Xmx1g")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SciModeler"
            packageVersion = "1.0.0"
            description = "Editor for scientific models and diagrams"
            vendor = "LordNordyx"
            modules("java.desktop", "java.datatransfer")

            windows {
                menu = true
                shortcut = true
                perUserInstall = true
                dirChooser = true
                menuGroup = "SciModeler"
                upgradeUuid = "8a0c6f2e-4b7d-4d3e-9a51-5c2f1e7b9d40"
            }
            macOS {
                bundleID = "si.lordnordyx.scimodeler"
                appCategory = "public.app-category.productivity"
            }
        }
    }
}
