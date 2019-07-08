/*
 * Copyright (c) 2019 Ian Bondoc
 *
 * This file is part of project "kargo"
 *
 * Project kargo is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or(at your option)
 * any later version.
 *
 * Project kargo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.3.41" apply false
    jacoco
    id("com.github.kt3k.coveralls") version "2.8.4"
}

allprojects {

    group = "org.chiknrice"
    version = "1.0-SNAPSHOT"

    repositories {
        jcenter()
    }

}

subprojects {

    apply(plugin = "kotlin")
    apply(plugin = "jacoco")

    dependencies {
        "implementation"(kotlin("stdlib-jdk8"))
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.5.0")
        "testImplementation"("org.assertj:assertj-core:3.12.2")
        "testImplementation"("io.mockk:mockk:1.9.3")
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            suppressWarnings = true
            jvmTarget = "1.8"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<Jar> {
        archiveBaseName.set("${rootProject.name}-${archiveBaseName.get()}")
    }

}

tasks.withType<Wrapper> {
    gradleVersion = "5.5"
}

tasks.register<JacocoReport>("jacocoRootTestReport") {
    sourceDirectories.from(subprojects.flatMap { it.the<SourceSetContainer>()["main"].allSource.srcDirs })
    classDirectories.from(subprojects.flatMap { it.the<SourceSetContainer>()["main"].output })
    executionData.from(subprojects.flatMap { files(it.tasks.withType<Test>()) }
            .filter { it.exists() && it.name.endsWith(".exec") })
    reports {
        xml.isEnabled = true
    }
}

coveralls {
    sourceDirs = subprojects.flatMap { it.the<SourceSetContainer>()["main"].allSource.srcDirs }.map { it.absolutePath }
    jacocoReportPath = "build/reports/jacoco/jacocoRootTestReport/jacocoRootTestReport.xml"
}