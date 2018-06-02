This project was re-written the the [fat-aar gradle plugin](https://github.com/Vigi0303/fat-aar-plugin) using kotlin and currently support AGP 2.3.3.

# Usage 
### Step 1: Apply plugin
Change the `gradle.properties` in the `fataar` module and upload to your maven center or just publish to maven local use `publishToMavenLocal`, then add the maven repositories in your root `build.gradle`:
```groovy
buildscript {
    repositories {
        maven {
            url  "your maven url"
        }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:xxx'
        classpath 'your groupId:your artifactId:your version'
    }
}
```

or if you use maven local:

```groovy
buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath 'your groupId:your artifactId:your version'
    }
}
```

Add snippet below to the build.gradle of your android library:
```groovy
apply plugin: 'com.littlegnal.fataar'
```

or you can modify the plugin name in the `META-INF`.

### Step 2: Embed dependencies
change compile to embed while you want to embed the dependency in the library. Like this:
```groovy
dependencies {
    // aar project
    embedded project(':aar-lib')
    // java project
    embedded project(':java-lib')
    // java dependency
    embedded 'com.google.guava:guava:20.0'
    // aar dependency
    embedded 'com.android.volley:volley:1.0.0'
  
    // other dependencies you don't want to embed in
    compile 'com.squareup.okhttp3:okhttp:3.6.0'
}
```

