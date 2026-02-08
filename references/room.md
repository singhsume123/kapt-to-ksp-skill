# Room: KAPT to KSP Migration

## How Room Uses Annotation Processing

### What Room Generates

Room's annotation processor reads your `@Entity`, `@Dao`, and `@Database` annotations and generates:

- **Entity implementations**: Schema definitions, table creation SQL
- **DAO implementations**: SQL query execution, cursor mapping, Flow/LiveData wrappers
- **Database implementation**: `_Impl` class that wires everything together

### Why Room Benefits More from KSP Than Most Libraries

With KAPT, Room reads Java stubs — it sees Java types where Kotlin nullability is expressed through `@Nullable`/`@NotNull` annotations (which can be missing or incorrect). With KSP, Room sees **actual Kotlin types**:

- `String` vs `String?` — Room knows exactly which columns are nullable
- `suspend fun` — Room can generate proper coroutine support
- `List<User>` vs `List<User>?` — Room knows the collection itself is never null
- Default parameter values — Room can see these directly

This means **KSP is stricter than KAPT** — it catches real nullability bugs that KAPT silently ignored.

---

## What Changes

| Area | Changes? | Details |
|------|----------|---------|
| build.gradle.kts | Yes | Plugin, dependency, and argument syntax changes |
| DAO source code | Possibly | Nullable collections, abstract properties, stricter nullability |
| Entity source code | Possibly | Stricter nullability on fields |
| Database source code | No | Usually no changes needed |

---

## Step-by-Step Migration

### Prerequisites

- **Room 2.4.0+** minimum for KSP support
- **Room 2.6.0+** recommended (fixes DefaultImpls issues, better KSP2 support)
- Recommended: **Room 2.6.1** for latest fixes

### 1. Add KSP Plugin

```kotlin
// Project-level build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.24" apply false
}

// Module-level build.gradle.kts
plugins {
    id("com.google.devtools.ksp")
    // Remove: kotlin("kapt")
}
```

### 2. Swap Dependencies

```kotlin
dependencies {
    // Room runtime (unchanged)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // BEFORE:
    // kapt("androidx.room:room-compiler:2.6.1")

    // AFTER:
    ksp("androidx.room:room-compiler:2.6.1")
}
```

### 3. Migrate Argument Syntax

This is the most commonly missed step. KAPT and KSP use completely different syntax:

```kotlin
// BEFORE — KAPT argument syntax:
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.generateKotlin", "true")
    }
}

// AFTER — KSP argument syntax:
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}
```

**Room processor arguments:**

| Argument | Description |
|----------|-------------|
| `room.schemaLocation` | Directory for exported schema JSON files |
| `room.incremental` | Enable incremental annotation processing |
| `room.generateKotlin` | Generate Kotlin code instead of Java (Room 2.6.0+) |
| `room.expandProjection` | Rewrite queries to use only referenced columns |

### 4. Remove KAPT Plugin

```kotlin
plugins {
    // Remove this line:
    // kotlin("kapt")
}
```

Remove any leftover `kapt { ... }` blocks.

---

## Gotchas

This is the critical section. Room with KSP is stricter than Room with KAPT, and you will likely need source code changes.

### Gotcha 1: Nullable Collection Returns Are Disallowed

```kotlin
// KAPT allowed this (compiles fine):
@Query("SELECT * FROM users")
fun getAllUsers(): List<User>?

// KSP rejects this — compile error!
// Error: "Collection return type should be non-null"
```

**Why**: A Room query that returns a collection **never returns null**. If no rows match, Room returns an empty list `[]`. The nullable `List<User>?` is semantically wrong — KAPT just didn't enforce it.

**Fix**:
```kotlin
@Query("SELECT * FROM users")
fun getAllUsers(): List<User>  // Remove the ?
```

This also applies to `Flow`, `LiveData`, and other reactive wrappers around collections:

```kotlin
// BROKEN:
fun observeUsers(): Flow<List<User>?>

// FIXED:
fun observeUsers(): Flow<List<User>>
```

### Gotcha 2: Abstract Properties as DAO Getters Are Disallowed

```kotlin
// KAPT allowed this:
@get:Query("SELECT COUNT(*) FROM users")
abstract val userCount: Int

// KSP rejects this — compile error!
// Error: "DAO functions with query must be abstract functions, not properties"
```

**Why**: A Kotlin `val` property implies a stored/cached value. But a `@Query` hits the database every time it's accessed. Using a function makes the "this does work every call" semantics clear.

**Fix**:
```kotlin
@Query("SELECT COUNT(*) FROM users")
abstract fun getUserCount(): Int
```

### Gotcha 3: Stricter Nullability on Single-Row Returns

```kotlin
// With KAPT, this compiles but crashes at runtime if no user found:
@Query("SELECT * FROM users WHERE id = :id")
fun getUserById(id: Int): User

// KSP forces you to think about this:
// If the query can return no rows, the return type MUST be nullable
```

**Why**: KSP sees that the query might return zero rows. If the return type is non-null `User`, Room would have to throw an exception when no row is found. KSP makes you explicitly choose.

**Fix** — make it nullable if the row might not exist:
```kotlin
@Query("SELECT * FROM users WHERE id = :id")
fun getUserById(id: Int): User?  // Add ? if row might not exist
```

Or keep it non-null if you guarantee the row always exists (Room will throw if it doesn't).

### Gotcha 4: DefaultImpls Errors with Default Interface Methods

```kotlin
// If your DAO is an interface with default methods:
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): List<User>

    // Default method:
    fun getFirst(): User? = getAll().firstOrNull()
}
```

With Room < 2.6.0 and KSP, you may get `DefaultImpls` compilation errors.

**Fix**: Update to **Room 2.6.0+**. This is the recommended minimum version for KSP.

### Gotcha 5: TypeConverter Nullability Must Match

```kotlin
// KAPT was lenient about TypeConverter nullability:
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Date {  // non-null Long
        return Date(value)
    }
}

// But if your entity has:
@Entity
data class User(
    val createdAt: Date?  // nullable Date backed by nullable Long column
)
```

KSP enforces that the TypeConverter parameter nullability matches the column nullability.

**Fix**:
```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {  // Match the column nullability
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {  // Match the column nullability
        return date?.time
    }
}
```

### Gotcha 6: @Serializable + Room Entity Conflict with KSP2

If an entity class uses both `@Entity` (Room) and `@Serializable` (kotlinx.serialization), and both Room and kotlinx-serialization are using KSP, you may get conflicts in KSP2 where both processors try to generate code for the same class.

**Fix**: If you encounter this, ensure you're on the latest versions of both libraries. As a workaround, separate the serialization model from the Room entity using a mapper.

---

## Before/After Examples

### Build File Diff

```kotlin
// BEFORE — app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
}

android { /* ... */ }

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
}
```

```kotlin
// AFTER — app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android { /* ... */ }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

### DAO Source Code Diff (Showing All Main Fixes)

```kotlin
// BEFORE — UserDao.kt (compiles with KAPT)
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>?              // Gotcha 1: nullable collection

    @get:Query("SELECT COUNT(*) FROM users")
    val userCount: Int                          // Gotcha 2: abstract property

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): User             // Gotcha 3: non-null optional row

    @Query("SELECT * FROM users")
    fun observeAllUsers(): Flow<List<User>?>   // Gotcha 1 variant: nullable Flow

    @Insert
    fun insertUser(user: User)

    @Delete
    fun deleteUser(user: User)

    @Query("DELETE FROM users")
    fun deleteAll()
}
```

```kotlin
// AFTER — UserDao.kt (compiles with KSP)
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>               // Fixed: non-null collection

    @Query("SELECT COUNT(*) FROM users")
    fun getUserCount(): Int                     // Fixed: function instead of property

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): User?            // Fixed: nullable for optional row

    @Query("SELECT * FROM users")
    fun observeAllUsers(): Flow<List<User>>    // Fixed: non-null Flow

    @Insert
    fun insertUser(user: User)                 // No change needed

    @Delete
    fun deleteUser(user: User)                 // No change needed

    @Query("DELETE FROM users")
    fun deleteAll()                            // No change needed
}
```

### Combined Hilt + Room Module Example

When a module uses both Hilt and Room with KAPT, migrate both at the same time:

```kotlin
// BEFORE:
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-android-compiler:2.51")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.51")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
}
```

```kotlin
// AFTER:
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

**Key point**: Migrate Hilt and Room together in the same module to avoid mixed KAPT/KSP issues.
