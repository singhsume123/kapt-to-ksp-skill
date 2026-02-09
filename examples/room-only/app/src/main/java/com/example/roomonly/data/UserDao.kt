package com.example.roomonly.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // GOTCHA 1: Nullable collection return — KSP will reject this
    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>?

    // GOTCHA 2: Abstract property as DAO getter — KSP will reject this
    @get:Query("SELECT COUNT(*) FROM users")
    val userCount: Int

    // GOTCHA 3: Non-null return for optional single row — KSP will flag this
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): User

    // GOTCHA 4: Nullable Flow of collection — KSP will reject this
    @Query("SELECT * FROM users WHERE isActive = 1")
    fun observeActiveUsers(): Flow<List<User>?>

    // These are fine — no changes needed for KSP
    @Insert
    fun insertUser(user: User)

    @Delete
    fun deleteUser(user: User)

    @Query("DELETE FROM users")
    fun deleteAll()
}
