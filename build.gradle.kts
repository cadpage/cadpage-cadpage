plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://dl.bintray.com/blockchainds/bds") }
    }
}
android {
    signingConfigs {
        getByName("release") {
            storeFile = file("~/.android/cadpage.keystore")
            keyAlias = "cadpage"
        }
    }
    defaultConfig {
        applicationId = "net.anei.cadpage"
//        versionName = project.versionName
//        versionCode = project.versionCode
        compileSdkVersion = "34"
        targetSdk = 34
        minSdk = 21
        multiDexEnabled = true
    }
    buildTypes {
        release {
//            minifyEnabled = false
//            proguardFiles getDefaultProguardFile("proguard-android.txt")
//            signingConfig(android.signingConfigs.release)
        }
    }

    flavorDimensions("receive", "send")
    productFlavors {

        rec {
            dimension("receive")
            versionNameSuffix("M")
            versionCode = 2
            buildConfigField("boolean", "REC_SMS_ALLOWED", "true")
            buildConfigField( "boolean", "REC_MMS_ALLOWED", "true")
        }

        smsrec {
            dimension("receive")
            versionNameSuffix("S")
            versionCode = 1
            buildConfigField("boolean", "REC_SMS_ALLOWED", "true")
            buildConfigField("boolean", "REC_MMS_ALLOWED", "false")
        }

        norec {
            dimension("receive")
            versionNameSuffix("R")
            versionCode = 0
            buildConfigField("boolean", "REC_SMS_ALLOWED", "false")
            buildConfigField("boolean", "REC_MMS_ALLOWED", "false")
        }

        send {
            dimension("send")
            versionNameSuffix("S")
            versionCode = 2
            buildConfigFiel("boolean", "SEND_ALLOWED", "true")
        }

        nosend {
            dimensio("send")
            versionNameSuffix("")
            versionCode = 2
            buildConfigField("boolean", "SEND_ALLOWED", "false")
        }
    }

    applicationVariants.all { variant ->
        var typeVersion = variant.productFlavors.get(1).versionCode


        var version = defaultConfig.versionCode + typeVersion // * 10000000
        variant.outputs.each { output ->
            output.versionCodeOverride = version
        }
    }

    variantFilter { variant ->
        var names = variant.flavors.name
        if (names.contains("aptoide") ||
            names.contains("send") && !names.contains("rec")) {
            setIgnore(true)
        }
    }

    compileSdkVersion(34)

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

//    buildToolsVersion "28.0.3"
}

dependencies {
    implementation platform("com.google.firebase:firebase-bom:28.4.2")
    implementation project(":cadpage-parsers")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.appcompat:appcompat:1.6.1")   // Upgrade to 1.7.0 fails
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-iid:21.1.0")
    implementation("com.google.firebase:firebase-messaging:24.0.3")
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")  // Upgraade to 2.8.6 fails
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
//    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("com.klinkerapps:android-smsmms:5.2.5") {
        exclude(group = "com.squareup.okhttp", module = "okhttp")
        exclude(group = "com.squareup.okhttp", module ="okhttp-urlconnection")
    }
    googleImplementation("com.android.billingclient:billing:7.1.1")
//    aptoideImplementation "com.blockchainds:appcoins-ads:0.5.1.32b"
//    aptoideImplementation "com.blockchainds:android-appcoins-billing:0.5.1.32b"
//    aptoideImplementation "com.blockchainds:appcoins-contract-proxy:0.5.1.32b"
}

