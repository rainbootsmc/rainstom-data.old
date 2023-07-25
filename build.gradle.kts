plugins {
    `java-library`
    alias(libs.plugins.blossom)

    `maven-publish`
}

val publishPath = System.getenv()["PUBLISH_PATH"]
val branch = System.getenv()["GITHUB_REF_NAME"] ?: "unknown"
val buildNumber = System.getenv()["BUILD_NUMBER"] ?: "local-SNAPSHOT"
val outputDirectory = (findProperty("output") ?: rootDir.resolve("MinestomData").absolutePath) as String

group = "dev.uten2c"
version = "$branch+build.$buildNumber"
description = "Generator for Minecraft game data values"

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

blossom {
    val gitFile = "src/main/java/net/minestom/data/MinestomData.java"

    val gitCommit = System.getenv("GIT_COMMIT")
    val gitBranch = System.getenv("GIT_BRANCH")

    replaceToken("\"&COMMIT\"", if (gitCommit == null) "null" else "\"${gitCommit}\"", gitFile)
    replaceToken("\"&BRANCH\"", if (gitBranch == null) "null" else "\"${gitBranch}\"", gitFile)
}

tasks {
    register("generateData") {
        logger.warn("Mojang requires all source-code and mappings used to be governed by the Minecraft EULA.")
        logger.warn("Please read the Minecraft EULA located at https://account.mojang.com/documents/minecraft_eula.")
        logger.warn("In order to agree to the EULA you must create a file called eula.txt with the text 'eula=true'.")
        val eulaTxt = File("${rootProject.projectDir}/eula.txt")
        logger.warn("The file must be located at '${eulaTxt.absolutePath}'.")
        if ((eulaTxt.exists() && eulaTxt.readText(Charsets.UTF_8).equals("eula=true", true))
            || project.properties["eula"].toString().toBoolean()
            || System.getenv("EULA")?.toBoolean() == true
        ) {
            logger.warn("")
            logger.warn("The EULA has been accepted and signed.")
            logger.warn("")
        } else {
            throw GradleException("Data generation has been halted as the EULA has not been signed.")
        }
        logger.warn("It is unclear if the data from the data generator also adhere to the Minecraft EULA.")
        logger.warn("Please consult your own legal team!")
        logger.warn("All data is given independently without warranty, guarantee or liability of any kind.")
        logger.warn("The data may or may not be the intellectual property of Mojang Studios.")
        logger.warn("")

        // Simplified by Sponge's VanillaGradle
        dependsOn(
            project(":DataGenerator").tasks.getByName<JavaExec>("run") {
                args = arrayListOf(outputDirectory)
            }
        )

    }
    register<Jar>("dataJar") {
        dependsOn("generateData")

        archiveBaseName.set("minestom-data")
        archiveVersion.set(libs.versions.minecraft)
        destinationDirectory.set(layout.buildDirectory.dir("dist"))
        from(outputDirectory)
    }
}

publishing {
    publications {
        create<MavenPublication>("MinestomData") {
            groupId = "dev.uten2c"
            artifactId = "rainstom-data"
            version = project.version.toString()

            artifact(tasks.getByName("dataJar"))
        }
    }
    if (publishPath != null) {
        repositories {
            maven {
                url = uri(publishPath)
            }
        }
    }
}