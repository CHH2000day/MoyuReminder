/*
 * MoyuReminder
 * Copyright (C)  2021  CHH2000day .
 * Permission is granted to copy, distribute and/or modify this document
 * under the terms of the GNU Free Documentation License, Version 1.3
 * or any later version published by the Free Software Foundation;
 * with no Invariant Sections, no Front-Cover Texts, and no Back-Cover Texts.
 * A copy of the license is included in the section entitled "GNU
 * Free Documentation License".
 */

plugins {
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.8.2"
}

group = "com.chh2000day.mirai.plugins"
version = "0.2.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}
tasks.test {
    useJUnitPlatform()
}
afterEvaluate {
    //写入插件信息
    val pluginConfigFile = File("src/main/resources/plugin.yml")
    //懒得引yml的库了,直接硬编码
    val content = buildString {
        append(
            """
        id: com.chh2000day.mirai.plugins.MoyuReminder
        main: com.chh2000day.mirai.plugins.MoyuReminder
        name: 摸鱼提醒
        """.trimIndent()
        )
        append("\n")
        append("version: $version")
        append("\n")
        append(
            """
            author: CHH2000day
            info: 喵帕斯~都已经这个点了还不坐下来悠哉悠哉地摸鱼吗
            dependencies: [ ]
        """.trimIndent()
        )
        append("\n")
    }
    pluginConfigFile.writeText(content)
}
