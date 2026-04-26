import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    jvm()
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "GhostSample"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("ghost-sample-wasm")
        browser {
            val projectDir = project.projectDir
            commonWebpackConfig {
                outputFileName = "ghost-sample.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(projectDir.path)
                }
            }
        }
        binaries.executable()
    }

    js(IR) {
        outputModuleName.set("ghost-sample")
        browser()
        nodejs()
        binaries.executable()
    }
    
    // KSP automatically adds generated sources to the corresponding source sets.
    // Manual srcDir configuration is removed to prevent redeclaration errors.

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            
            // Ghost (Remote 1.1.14)
            implementation(libs.ghost.api)
            api(libs.ghost.serialization)
            implementation(libs.ghost.ktor)
            
            implementation(libs.ktorfit.lib)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.kotlinx.serialization.json)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.metrics)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.bundles.serialization.engines)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.bundles.serialization.engines)
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.ktor.client.core)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.ktor.client.core)
            }
        }
    }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

android {
    namespace = "com.ghost.serialization.sample"
    compileSdk = 36

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    defaultConfig {
        applicationId = "com.ghost.serialization.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    
    buildFeatures {
        compose = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res")
            resources.srcDirs("src/androidMain/resources")
        }
    }
}

ksp {
    arg("ghost.moduleName", "serialization_sample")
}

ktorfit {
    compilerPluginVersion.set("2.3.3")
}

configurations.all {
    resolutionStrategy {
        // Force JetBrains version of Lifecycle & SavedState to avoid KLIB conflicts with Google's version
        force(libs.androidx.lifecycle.common)
        force(libs.androidx.lifecycle.runtime)
        force(libs.androidx.lifecycle.runtime.compose)
        force(libs.androidx.lifecycle.viewmodel)
        force(libs.androidx.lifecycle.viewmodel.compose)
        force(libs.androidx.lifecycle.viewmodel.savedstate)
        force(libs.androidx.savedstate)
        force(libs.androidx.savedstate.compose)
    }
}

dependencies {
    // Ghost KSP (Remote 1.1.14)
    add("kspCommonMainMetadata", libs.ghost.compiler)
    add("kspJvm", libs.ghost.compiler)
    add("kspAndroid", libs.ghost.compiler)
    add("kspIosArm64", libs.ghost.compiler)
    add("kspIosSimulatorArm64", libs.ghost.compiler)
    add("kspWasmJs", libs.ghost.compiler)
    add("kspJs", libs.ghost.compiler)

    // Ktorfit KSP
    add("kspJvm", libs.ktorfit.ksp)
    add("kspAndroid", libs.ktorfit.ksp)
    add("kspIosArm64", libs.ktorfit.ksp)
    add("kspIosSimulatorArm64", libs.ktorfit.ksp)
    add("kspWasmJs", libs.ktorfit.ksp)
    add("kspJs", libs.ktorfit.ksp)
}

compose.desktop {
    application {
        mainClass = "com.ghost.serialization.sample.MainKt"
    }
}