import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
}


val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.isa.cuidadocompartidomayor"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.isa.cuidadocompartidomayor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProperties.getProperty("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${localProperties.getProperty("CLOUDINARY_UPLOAD_PRESET")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true // Habilita la generación de la clase BuildConfig
    }
}

dependencies {
    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Glide para cargar imágenes (recomendado)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // View Model
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // Live Data
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Fragments
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.swiperefreshlayout)
    //ubicacion google
    implementation(libs.play.services.location)
    //Generacion QR
    implementation(libs.zxing.android.embedded)
    implementation(libs.zxing.core)

    // WorkManager (para tareas en segundo plano)
    implementation(libs.androidx.work.runtime.ktx)

    // MPAndroidChart para gráficas
    implementation(libs.mpandroidchart)

    implementation(libs.gson)
    implementation(libs.okhttp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
