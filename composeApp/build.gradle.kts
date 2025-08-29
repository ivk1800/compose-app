import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.hotReload)
}

kotlin {
    jvm()

    js {
        browser()
        binaries.executable()
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Multiplatform App"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "org.company.app.desktopApp"
            }
        }
    }
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass = "MainKt"
}

tasks.register("buildApp") {
    dependsOn("jsBrowserDistribution")

    val distDir = layout.buildDirectory.dir("dist/js/productionExecutable")
    val buildTimestamp: String by extra { System.currentTimeMillis().toString() }
    val metrikaIdProvider = project.providers.gradleProperty("metrikaId").orElse("")

    doLast {
        val dist = distDir.get().asFile
        if (!dist.exists()) {
            throw GradleException("Distribution directory not found: $distDir")
        }

        val composeResourcesDir = File(dist, "composeResources")
        if (!composeResourcesDir.exists()) {
            throw GradleException("composeResources not found in $dist")
        }
        val newComposeResourcesDirName = "composeResources-${buildTimestamp}"
        composeResourcesDir.renameTo(File(dist, newComposeResourcesDirName))

        val targetDir = File(dist, buildTimestamp)
        targetDir.mkdirs()

        dist.listFiles()
            ?.filter {
                it.isFile && (it.name.startsWith("composeApp") || it.name.startsWith("skiko")) ||
                        it.name.endsWith(".wasm")
            }
            ?.forEach { file ->
                file.copyTo(File(targetDir, file.name), overwrite = true)
                file.delete()
            }

        val index = File(dist, "index.html")
        if (!index.exists()) {
            throw GradleException("index.html not found in $dist")
        }

        val metrikaCounter = """
<!-- Yandex.Metrika counter -->
<script type="text/javascript">
    (function(m,e,t,r,i,k,a){
        m[i]=m[i]||function(){(m[i].a=m[i].a||[]).push(arguments)};
        m[i].l=1*new Date();
        for (var j = 0; j < document.scripts.length; j++) {if (document.scripts[j].src === r) { return; }}
        k=e.createElement(t),a=e.getElementsByTagName(t)[0],k.async=1,k.src=r,a.parentNode.insertBefore(k,a)
    })(window, document,'script','https://mc.yandex.ru/metrika/tag.js?id=103948613', 'ym');

    ym(103948613, 'init', {ssr:true, webvisor:true, clickmap:true, ecommerce:"dataLayer", accurateTrackBounce:true, trackLinks:true});
</script>
<noscript><div><img src="https://mc.yandex.ru/watch/103948613" style="position:absolute; left:-9999px;" alt="" /></div></noscript>
<!-- /Yandex.Metrika counter -->
    """.trimIndent()

        val updatedIndex = index.readText()
            .replace(
                """<script\s+src=["']composeApp\.js["']></script>""".toRegex(),
                """<script src="$buildTimestamp/composeApp.js"></script>""",
            )
            .replace(
                """<script\s+src=["']skiko\.js["']></script>""".toRegex(),
                """<script src="$buildTimestamp/skiko.js"></script>""",
            )
            .replace("<!--METRIKA_PLACEHODLER-->", metrikaCounter)

        index.writeText(updatedIndex)

        val composeAppFile = File(targetDir, "composeApp.js")
        if (!composeAppFile.exists()) {
            throw GradleException("composeApp.js not found in $dist")
        }

        val newComposeAppContent = composeAppFile.readText()
            .replace(
                "composeResources/multiplatform_app.composeapp.generated.resources/",
                "$newComposeResourcesDirName/multiplatform_app.composeapp.generated.resources/",
            )
        composeAppFile.writeText(newComposeAppContent)
    }
}
