import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation ("org.jetbrains.kotlin:kotlin-stdlib")
    implementation ("commons-io:commons-io:2.16.1")
    implementation ("com.google.guava:guava:26.0-jre")
    implementation ("org.dom4j:dom4j:2.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

// repositories, dependencies, etc...

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("ktextrd")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "MainKt"))
        }
        destinationDirectory.set(file("${rootProject.rootDir}/dist"))
    }
}

tasks.register<JavaExec>("runJar"){
    dependsOn("shadowJar")
    //println("hi ")
    val testfolder = "${rootProject.rootDir}/testfiles/"
    val jarFilePath = "${rootProject.rootDir}/dist/ktextrd-all.jar"
    mainClass.set("MainKt")
    classpath = files(jarFilePath)
    args("-f", "${testfolder}ext2img.ext4")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    test {
        useJUnitPlatform() // Enable JUnit 5 support of Gradle
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

