plugins {
    id("org.jetbrains.kotlin.jvm")
}

val gdxVersion = "1.13.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
