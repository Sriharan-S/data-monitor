package com.sriharan.datamonitor.di

import com.sriharan.datamonitor.data.NetworkUsageRepository
import com.sriharan.datamonitor.data.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindUsageRepository(
        networkUsageRepository: NetworkUsageRepository
    ): UsageRepository
}
