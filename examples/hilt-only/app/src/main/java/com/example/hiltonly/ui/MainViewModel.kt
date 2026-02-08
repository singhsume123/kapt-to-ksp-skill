package com.example.hiltonly.ui

import androidx.lifecycle.ViewModel
import com.example.hiltonly.di.AppConfig
import com.example.hiltonly.di.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appConfig: AppConfig
) : ViewModel() {

    fun getUsers(): List<String> = userRepository.getUsers()

    fun getApiUrl(): String = appConfig.apiUrl
}
