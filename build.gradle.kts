plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.17.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.xiaohai"
version = "1.0-SNAPSHOT"

repositories {
//    mavenCentral()
    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.2.5")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("java"))
    // URL schemes if needed
//    args.urlSchemes = ["http", "https"]
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        // 设置打包后的 JAR 文件名
        archiveClassifier.set("all")
        // 合并服务文件，如果有服务提供者配置
        // mergeServiceFiles()

        // 选择需要包含的内容
        // 你可以配置需要包含的内容
//         include("com/googlecode/java-diff-utils/**")
    }
}

dependencies {
    implementation("com.googlecode.java-diff-utils:diffutils:1.3.0")
//    implementation("cn.hutool:hutool-all:5.8.23")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("jakarta.mail:jakarta.mail-api:2.1.0")
    implementation("javax.mail:mail:1.4.7")
    implementation("org.apache.commons:commons-email:1.5")
    implementation("org.commonmark:commonmark:0.18.0")
    implementation("net.minidev:json-smart:2.5.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
