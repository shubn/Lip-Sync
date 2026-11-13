plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.freeavatarstudio"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.freeavatarstudio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
