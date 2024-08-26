plugins {
    val kotlinVersion = "1.9.23"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "com.github.hatoyuze"
version = "0.3.0"

repositories {
    if (System.getenv("CI")?.toBoolean() != true) {
        maven("https://repo.huaweicloud.com/repository/maven/")
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    }
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    val ktorVersion = "2.3.10"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.apache.commons:commons-text:1.12.0")

}
