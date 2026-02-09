package com.example.roomonly.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // FIX 1: Non-nullable collection return (was List<User>?)
    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>

    // FIX 2: Function instead of abstract property (was @get:Query val userCount)
    @Query("SELECT COUNT(*) FROM users")
    fun getUserCount(): Int

    // FIX 3: Nullable return for optional single row (was User)
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): User?

    // FIX 4: Non-nullable Flow of collection (was Flow<List<User>?>)
    @Query("SELECT * FROM users WHERE isActive = 1")
    fun observeActiveUsers(): Flow<List<User>>

    @Insert
    fun insertUser(user: User)

    @Delete
    fun deleteUser(user: User)

    @Query("DELETE FROM users")
    fun deleteAll()
}
