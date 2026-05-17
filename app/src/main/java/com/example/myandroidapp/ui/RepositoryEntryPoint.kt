package com.example.myandroidapp.ui

import com.example.myandroidapp.data.ArticlesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    val repository: ArticlesRepository
}
