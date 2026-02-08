package com.example.hiltroom.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>

    @Query("SELECT COUNT(*) FROM users")
    fun getUserCount(): Int

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): User?

    @Query("SELECT * FROM users WHERE isActive = 1")
    fun observeActiveUsers(): Flow<List<User>>

    @Insert
    fun insertUser(user: User)

    @Delete
    fun deleteUser(user: User)

    @Query("DELETE FROM users")
    fun deleteAll()
}
