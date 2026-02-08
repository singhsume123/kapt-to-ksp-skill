package com.example.hiltonly.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

data class AppConfig(
    val apiUrl: String,
    val debugMode: Boolean
)

interface UserRepository {
    fun getUsers(): List<String>
}

class UserRepositoryImpl : UserRepository {
    override fun getUsers(): List<String> {
        return listOf("Alice", "Bob", "Charlie")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig {
        return AppConfig(
            apiUrl = "https://api.example.com",
            debugMode = true
        )
    }

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository {
        return UserRepositoryImpl()
    }
}
