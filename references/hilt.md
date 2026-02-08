# Hilt/Dagger: KAPT to KSP Migration

## How Hilt Uses Annotation Processing

### What KAPT Does

1. Kotlin compiler generates Java stubs from your `@HiltAndroidApp`, `@HiltViewModel`, `@Module`, `@Inject` annotations
2. Dagger/Hilt annotation processor reads these Java stubs
3. Processor generates DI component code (factories, injectors, component implementations)
4. Generated Java code is compiled alongside your Kotlin code

### What KSP Does

1. KSP reads your Kotlin source code directly — no Java stubs
2. Dagger/Hilt KSP processor reads Kotlin symbols
3. Processor generates the same DI component code
4. Generated code is compiled alongside your Kotlin code

### Key Insight

**Your source code annotations don't change at all.** `@HiltAndroidApp`, `@HiltViewModel`, `@Module`, `@Provides`, `@Inject` — all remain exactly the same. Only the build configuration changes.

---

## What Changes

| Area | Changes? | Details |
|------|----------|---------|
| Source code annotations | No | All Hilt/Dagger annotations remain the same |
| Kotlin source files | No | No code changes needed |
| build.gradle.kts | Yes | Plugin and dependency configuration changes |
| gradle.properties | No | No kapt-specific Hilt settings to remove |

---

## Step-by-Step Migration

### Prerequisites

- **Dagger/Hilt 2.48+** is required for KSP support
- Recommended: **Dagger/Hilt 2.51+** for best KSP stability
- Know your Kotlin version (needed to pick matching KSP version)

### 1. Add KSP Plugin (Project-Level)

```kotlin
// build.gradle.kts (project-level)
plugins {
    // Existing plugins...
    id("com.google.devtools.ksp") version "2.0.0-1.0.24" apply false
}
```

### 2. Apply KSP Plugin (Module-Level)

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    // Remove: kotlin("kapt")
    // Add:
    id("com.google.devtools.ksp")
}
```

### 3. Swap Dependencies

```kotlin
dependencies {
    // Hilt runtime (unchanged)
    implementation("com.google.dagger:hilt-android:2.51")

    // BEFORE:
    // kapt("com.google.dagger:hilt-android-compiler:2.51")
    // kapt("androidx.hilt:hilt-compiler:1.2.0")

    // AFTER:
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Test dependencies:
    // BEFORE:
    // kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.51")

    // AFTER:
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51")
}
```

### 4. Remove KAPT Plugin

Once all `kapt(...)` calls in the module are replaced with `ksp(...)`:

```kotlin
plugins {
    // Remove this line:
    // kotlin("kapt")
}
```

Also remove any `kapt { ... }` blocks if present (Hilt typically doesn't need one, but check).

---

## Gotchas

### 1. Both Hilt Compilers Must Migrate Together

If you use both `com.google.dagger:hilt-android-compiler` and `androidx.hilt:hilt-compiler`, **both must be migrated to KSP at the same time** in the same module. You cannot have one on KAPT and one on KSP.

```kotlin
// WRONG — mixing KAPT and KSP for Hilt in same module:
kapt("com.google.dagger:hilt-android-compiler:2.51")
ksp("androidx.hilt:hilt-compiler:1.2.0")

// CORRECT — both on KSP:
ksp("com.google.dagger:hilt-android-compiler:2.51")
ksp("androidx.hilt:hilt-compiler:1.2.0")
```

### 2. Other Processors That Generate Types Consumed by Hilt

If you have a custom annotation processor that generates `@Module` or `@EntryPoint` classes consumed by Hilt, that processor must also be migrated to KSP (or at least run before KSP). Otherwise, Hilt's KSP processor won't see the generated types.

### 3. Dagger SPI Plugins

If you use Dagger SPI (Service Provider Interface) plugins for custom validation or code generation, they need to be updated to the KSP-compatible SPI API. The KAPT SPI API is different from the KSP SPI API.

### 4. Incremental Processing

With KAPT, you may have set `kapt.incremental.apt=true` in `gradle.properties`. With KSP, incremental processing is **automatic** — no configuration needed. You can remove that gradle property.

### 5. Multi-Module Projects

Each module that uses Hilt must independently:
- Apply the KSP plugin
- Replace `kapt(...)` with `ksp(...)`
- Remove `kotlin("kapt")` once all KAPT usages in that module are gone

You can migrate modules one at a time. Different modules can temporarily use different processing backends (one on KAPT, another on KSP) as long as each individual module is consistent.

---

## Complete Before/After Example

### Before (KAPT)

```kotlin
// build.gradle.kts (project-level)
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
}
```

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-android-compiler:2.51")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.51")
}
```

### After (KSP)

```kotlin
// build.gradle.kts (project-level)
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.24" apply false
}
```

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51")
}
```

### Source Code — NO Changes

```kotlin
// MyApp.kt — identical before and after
@HiltAndroidApp
class MyApp : Application()

// MainViewModel.kt — identical before and after
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    // ...
}

// AppModule.kt — identical before and after
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository {
        return UserRepositoryImpl()
    }
}
```
