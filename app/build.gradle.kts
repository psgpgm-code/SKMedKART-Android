import java.util.Base64

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace="com.srikrishnamedicals.skmedkart"; compileSdk=35
 defaultConfig { applicationId="com.srikrishnamedicals.skmedkart"; minSdk=24; targetSdk=35; versionCode=2; versionName="2.0.0" }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies {
 implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
 implementation("com.google.firebase:firebase-auth")
 implementation("com.google.firebase:firebase-firestore")
 implementation("com.google.firebase:firebase-storage")
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.appcompat:appcompat:1.7.0")
 implementation("androidx.webkit:webkit:1.18.0")
 implementation("com.google.android.material:material:1.12.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}

tasks.register("prepareSkmedkartIcon") {
    doLast {
        val source = project.file("src/main/icon-source/skmedkart_icon.jpg.b64")
        val target = project.file("src/main/res/drawable/skmedkart_icon.jpg")
        target.parentFile.mkdirs()
        target.writeBytes(Base64.getDecoder().decode(source.readText().trim()))
    }
}
tasks.named("preBuild") {
    dependsOn("prepareSkmedkartIcon")
}
