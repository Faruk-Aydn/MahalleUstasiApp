package com.example.mahalleustasi.di

import com.example.mahalleustasi.data.repository.JobsRepository
import com.example.mahalleustasi.data.repository.MessagesRepository
import com.example.mahalleustasi.data.repository.NotificationsRepository
import com.example.mahalleustasi.data.repository.OffersRepository
import com.example.mahalleustasi.data.repository.OffersRepositoryContract
import com.example.mahalleustasi.data.repository.PaymentsRepository
import com.example.mahalleustasi.data.repository.ReviewsRepository
import com.example.mahalleustasi.data.repository.StorageRepository
import com.example.mahalleustasi.data.repository.UsersRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides @Singleton fun provideJobsRepository(): JobsRepository = JobsRepository()
    @Provides @Singleton fun provideOffersRepository(): OffersRepository = OffersRepository()
    @Provides @Singleton fun provideOffersRepositoryContract(impl: OffersRepository): OffersRepositoryContract = impl
    @Provides @Singleton fun provideUsersRepository(): UsersRepository = UsersRepository()
    @Provides @Singleton fun provideMessagesRepository(): MessagesRepository = MessagesRepository()
    @Provides @Singleton fun provideReviewsRepository(): ReviewsRepository = ReviewsRepository()
    @Provides @Singleton fun provideNotificationsRepository(): NotificationsRepository = NotificationsRepository()
    @Provides @Singleton fun providePaymentsRepository(): PaymentsRepository = PaymentsRepository()

    // Storage repository (Firebase Storage + Auth gerekli)
    @Provides
    @Singleton
    fun provideStorageRepository(
        storage: FirebaseStorage,
        auth: FirebaseAuth
    ): StorageRepository = StorageRepository(storage, auth)
}
