package com.eshwar.rideconnectx.core.di

import com.eshwar.rideconnectx.data.repository.BleRepositoryImpl
import com.eshwar.rideconnectx.domain.repository.BleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BleModule {

    @Binds
    @Singleton
    abstract fun bindBleRepository(
        bleRepositoryImpl: BleRepositoryImpl
        // Switch to BleRepositorySimulatorImpl for testing without a scooter
        // bleRepositorySimulatorImpl: BleRepositorySimulatorImpl
    ): BleRepository
}
