import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
	base
	id("net.fabricmc.fabric-loom")
	`maven-publish`
}

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
}

fabricApi {
	configureDataGeneration {
		client = true
		modId = "calc-commands"
	}
}

// Bundle Symja's runtime dependencies as separate nested jars, preserving their licenses.
val symjaLibraries = configurations.create("symjaLibraries") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.implementation {
    extendsFrom(symjaLibraries)
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

	add(symjaLibraries.name, "org.matheclipse:matheclipse-core:${providers.gradleProperty("symja_version").get()}") {
		// These APIs are already provided by Minecraft; use the game's versions.
		exclude(group = "com.google.guava")
		exclude(group = "org.apache.logging.log4j")
		exclude(group = "org.slf4j")
		exclude(group = "it.unimi.dsi", module = "fastutil-core")
	}

	testImplementation("junit:junit:4.13.2")
}

// Loom deliberately makes include non-transitive. Include every resolved runtime module,
// retaining Maven identities so Loom can generate valid metadata for each nested jar.
symjaLibraries.incoming.artifacts.artifacts.forEach { artifact ->
    val module = artifact.id.componentIdentifier as ModuleComponentIdentifier
    dependencies.add("include", "${module.group}:${module.module}:${module.version}")
}

tasks.test {
	useJUnit()
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
