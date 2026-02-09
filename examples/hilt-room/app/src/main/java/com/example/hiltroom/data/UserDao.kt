package com.example.hiltroom.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // GOTCHA: Nullable collection return — KSP will reject this
    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>?

    // GOTCHA: Abstract property as DAO getter — KSP will reject this
    @get:Query("SELECT COUNT(*) FROM users")
    val userCount: Int

    // GOTCHA: Non-null return for optional single row
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): User

    // GOTCHA: Nullable Flow of collection — KSP will reject this
    @Query("SELECT * FROM users WHERE isActive = 1")
    fun observeActiveUsers(): Flow<List<User>?>

    @Insert
    fun insertUser(user: User)

    @Delete
    fun deleteUser(user: User)

    @Query("DELETE FROM users")
    fun deleteAll()
}
