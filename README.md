# KAPT to KSP Migration Guide

Practical guide and working examples for migrating Android projects from **KAPT** (Kotlin Annotation Processing Tool) to **KSP** (Kotlin Symbol Processing).

KAPT is in maintenance mode. KSP reads Kotlin source directly — no stub generation, incremental by default, and typically **25-50% faster** annotation processing.

## Repository Structure

```
kapt-to-ksp/
├── examples/
│   ├── hilt-only/              # Hilt DI — KAPT baseline
│   ├── hilt-only-migrated/     # Hilt DI — KSP migrated
│   ├── room-only/              # Room database — KAPT baseline (with gotchas)
│   ├── room-only-migrated/     # Room database — KSP migrated (gotchas fixed)
│   ├── hilt-room/              # Hilt + Room — KAPT baseline (with gotchas)
│   └── hilt-room-migrated/     # Hilt + Room — KSP migrated (gotchas fixed)
└── skill/
    ├── SKILL.md                # Full 6-step migration workflow
    └── references/
        ├── hilt.md             # Hilt-specific migration guide
        └── room.md             # Room-specific migration guide + gotchas
```

## Examples

Each example has a **KAPT baseline** and a **KSP migrated** copy side-by-side, so you can diff them to see exactly what changed.

All examples are standalone, buildable Android projects using:

| Dependency | Version |
|------------|---------|
| Kotlin | 2.0.0 |
| KSP | 2.0.0-1.0.24 |
| AGP | 8.5.0 |
| Hilt/Dagger | 2.51 |
| Room | 2.6.1 |
| Target SDK | 34 |

### hilt-only / hilt-only-migrated

Hilt dependency injection. **Build config changes only** — no source code modifications needed. All annotations (`@HiltAndroidApp`, `@HiltViewModel`, `@Inject`, `@Module`) remain identical.

### room-only / room-only-migrated

Room database. The KAPT baseline contains intentional gotchas (nullable collections, abstract properties, missing nullability) that KSP's stricter type checking catches. The migrated version shows the fixes.

### hilt-room / hilt-room-migrated

Combined Hilt + Room project. Both processors are swapped in the same module — you cannot mix KAPT and KSP within a single module. Includes DAO gotcha fixes and updated ViewModel signatures.

## Quick Reference

### Build File Migration

```kotlin
// BEFORE (KAPT)
plugins {
    kotlin("kapt")
}
dependencies {
    kapt("com.google.dagger:hilt-android-compiler:2.51")
    kapt("androidx.room:room-compiler:2.6.1")
}
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

// AFTER (KSP)
plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    ksp("androidx.room:room-compiler:2.6.1")
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

### Room DAO Gotchas

KSP is stricter than KAPT — it catches real nullability bugs that KAPT silently ignored:

```kotlin
// BEFORE (compiles with KAPT)         // AFTER (compiles with KSP)
fun getAll(): List<User>?              fun getAll(): List<User>           // collections never null
@get:Query("...") val count: Int       fun getCount(): Int                // property -> function
fun getById(id: Int): User             fun getById(id: Int): User?        // nullable optional row
fun observe(): Flow<List<User>?>       fun observe(): Flow<List<User>>    // non-null Flow wrapper
```

## Building

Each example is an independent Gradle project. Build from its directory:

```bash
cd examples/hilt-only && ./gradlew clean assembleDebug
cd examples/hilt-only-migrated && ./gradlew clean assembleDebug
cd examples/room-only && ./gradlew clean assembleDebug
cd examples/room-only-migrated && ./gradlew clean assembleDebug
cd examples/hilt-room && ./gradlew clean assembleDebug
cd examples/hilt-room-migrated && ./gradlew clean assembleDebug
```

## Migration Workflow

See [skill/SKILL.md](skill/SKILL.md) for the full 6-step migration process:

1. **Analyze** current KAPT usage
2. **Add** KSP plugin (version must match Kotlin version)
3. **Swap** `kapt()` dependencies to `ksp()`
4. **Migrate** processor arguments (`kapt {}` to `ksp {}`)
5. **Clean up** — remove `kotlin("kapt")` plugin
6. **Verify** — clean build and test

## Library-Specific Guides

- [Hilt/Dagger Migration](skill/references/hilt.md) — build config changes only, no source changes
- [Room Migration](skill/references/room.md) — build config changes + DAO source fixes for nullability
