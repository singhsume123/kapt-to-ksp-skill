---
name: kapt-to-ksp
description: >
  Migrate Android projects from KAPT to KSP annotation processing.
  Triggers on: kapt, ksp, annotation processing, build speed, Hilt to KSP,
  Room to KSP, or build.gradle files containing kapt dependencies.
---

# KAPT to KSP Migration

## Background

### Why KAPT is Slow

KAPT (Kotlin Annotation Processing Tool) works by generating Java stubs from Kotlin code, then running standard Java annotation processors against those stubs. This means:

- **Double symbol resolution**: Kotlin compiler resolves symbols once to generate stubs, then the Java annotation processor resolves them again.
- **Stub generation overhead**: Every Kotlin file with annotations gets a corresponding Java stub file generated, even if no processor needs it.
- **No incremental processing by default**: Most KAPT processors don't support incremental processing, so the full stub generation and processing runs on every build.

### Why KSP is Faster

KSP (Kotlin Symbol Processing) reads Kotlin source code directly through a Kotlin compiler plugin. This means:

- **No stub generation**: KSP sees Kotlin symbols directly — no intermediate Java stubs.
- **Kotlin-native types**: KSP sees actual Kotlin types including nullability, suspend functions, and default parameters.
- **Incremental by default**: KSP tracks which files each processor depends on and only reprocesses what changed.
- **Typical speedup**: 25-50% faster annotation processing vs KAPT.

### KAPT Status

KAPT is in **maintenance mode**. Google recommends migrating to KSP for all annotation processing. Major libraries (Hilt/Dagger, Room, Moshi, Glide) all support KSP.

---

## Migration Workflow

Follow these 6 steps to migrate from KAPT to KSP:

### Step 1: Analyze Current KAPT Usage

Scan your project for:
- `kotlin("kapt")` or `id("org.jetbrains.kotlin.kapt")` plugin declarations
- `kapt(...)` dependency declarations
- `kaptTest(...)` and `kaptAndroidTest(...)` test dependencies
- `kapt { arguments { ... } }` configuration blocks
- Any `kapt.incremental.apt` or other kapt settings in `gradle.properties`

List every library that uses KAPT and check if it supports KSP. See [references/hilt.md](references/hilt.md) and [references/room.md](references/room.md) for library-specific details.

### Step 2: Add KSP Plugin

Add the KSP Gradle plugin. **The KSP version must align with your Kotlin version.**

```kotlin
// Project-level build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "<KOTLIN_VERSION>-<KSP_RELEASE>" apply false
}
```

KSP version format: `<kotlin-version>-<ksp-release>`. For example:
- Kotlin 2.0.0 → KSP `2.0.0-1.0.24`
- Kotlin 2.0.21 → KSP `2.0.21-1.0.28`
- Kotlin 2.1.0 → KSP `2.1.0-1.0.29`

Check [KSP releases](https://github.com/google/ksp/releases) for the latest version matching your Kotlin version.

### Step 3: Migrate Dependencies Library-by-Library

For each module, apply the KSP plugin and swap dependencies:

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    // Before:
    // kapt("com.example:processor:1.0")
    // After:
    ksp("com.example:processor:1.0")

    // Test dependencies too:
    // kaptTest("com.example:processor:1.0")
    kspTest("com.example:processor:1.0")

    // Android test dependencies:
    // kaptAndroidTest("com.example:processor:1.0")
    kspAndroidTest("com.example:processor:1.0")
}
```

Migrate one library at a time. Build and test after each migration.

### Step 4: Migrate Processor Arguments

KAPT and KSP use different syntax for passing arguments to processors:

```kotlin
// KAPT syntax:
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

// KSP syntax:
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
```

### Step 5: Clean Up

Once all processors are migrated to KSP:

- **Remove** `kotlin("kapt")` plugin from every module
- **Remove** any `kapt { ... }` configuration blocks
- **Remove** kapt-related settings from `gradle.properties`
- **Exception**: Keep `kotlin("kapt")` if you still use Data Binding with `kapt`. Data Binding does not yet support KSP.

### Step 6: Verify

1. **Clean build**: `./gradlew clean assembleDebug`
2. **Run tests**: `./gradlew testDebugUnitTest`
3. **Run instrumented tests**: `./gradlew connectedDebugAndroidTest`
4. **Compare build times**: Run a clean build before and after migration to measure improvement.

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `Unresolved reference: ksp` | KSP plugin not applied to the module | Add `id("com.google.devtools.ksp")` to the module's plugins block |
| `KSP version X does not match Kotlin version Y` | KSP/Kotlin version mismatch | Use KSP version that matches your Kotlin version exactly |
| `Could not resolve com.example:processor` | Library doesn't have a KSP artifact, or wrong artifact ID | Check library docs — some use a different artifact for KSP |
| Nullability errors in generated code | KSP sees actual Kotlin types (stricter than KAPT) | Fix source code nullability — see [references/room.md](references/room.md) |
| `DefaultImpls` errors | Room < 2.6.0 with default interface methods | Update Room to 2.6.0+ — see [references/room.md](references/room.md) |
| Build succeeds but app crashes at runtime | Generated code differences between KAPT and KSP | Clean build, check for mixed KAPT/KSP in same module |

---

## Library-Specific References

- **[Hilt/Dagger Migration](references/hilt.md)** — Build file changes only, no source code changes needed.
- **[Room Migration](references/room.md)** — Build file changes AND possible source code fixes for nullability and DAO patterns.
