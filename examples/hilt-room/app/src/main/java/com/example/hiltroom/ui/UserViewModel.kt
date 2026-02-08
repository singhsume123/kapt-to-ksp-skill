package com.example.hiltroom.ui

import androidx.lifecycle.ViewModel
import com.example.hiltroom.data.User
import com.example.hiltroom.data.UserDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {

    // Uses the nullable Flow from the DAO — this gotcha is still present
    val activeUsers: Flow<List<User>?> = userDao.observeActiveUsers()

    fun getAllUsers(): List<User>? = userDao.getAllUsers()

    fun getUserById(id: Int): User = userDao.getUserById(id)

    fun insertUser(user: User) = userDao.insertUser(user)

    fun deleteUser(user: User) = userDao.deleteUser(user)
}
