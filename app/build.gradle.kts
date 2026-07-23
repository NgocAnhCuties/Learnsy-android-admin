import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.learnsy.admin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.learnsy.admin"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "SUPA_URL", "\"${localProps.getProperty("SUPA_URL", "")}\"")
        buildConfigField("String", "SUPA_KEY", "\"${localProps.getProperty("SUPA_KEY", "")}\"")
        buildConfigField("String", "ADMIN_API_KEY", "\"${localProps.getProperty("ADMIN_API_KEY", "")}\"")
        buildConfigField("String", "UPSTASH_URL", "\"${localProps.getProperty("UPSTASH_URL", "")}\"")
        buildConfigField("String", "UPSTASH_TOKEN", "\"${localProps.getProperty("UPSTASH_TOKEN", "")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Dùng tạm debug keystore mặc định của Android để cài trực tiếp APK release
            // lên máy thử nghiệm mà không cần tạo keystore thật. Khi phát hành chính thức
            // (Play Store hoặc chia sẻ ngoài), đổi sang keystore riêng và ký lại.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    val supabaseVersion = "2.5.4"
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:functions-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:storage-kt:$supabaseVersion")
    implementation("io.ktor:ktor-client-android:2.3.11")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
