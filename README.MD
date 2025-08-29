# Compose Multiplatform Application

### Desktop
Run the desktop application: `./gradlew :composeApp:run`  
Run the desktop **hot reload** application: `./gradlew :composeApp:hotRunJvm`  
Run desktop UI tests: `./gradlew :composeApp:jvmTest`  

### Web Distribution
Build web distribution: `./gradlew :composeApp:composeCompatibilityBrowserDistribution`  
Deploy a dir `composeApp/build/dist/composeWebCompatibility/productionExecutable` to a web server  

### JS Browser
Run the browser application: `./gradlew :composeApp:jsBrowserDevelopmentRun --continue`  
Run browser UI tests: `./gradlew :composeApp:jsBrowserTest`  

### Wasm Browser
Run the browser application: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continue`  
Run browser UI tests: `./gradlew :composeApp:wasmJsBrowserTest`  

