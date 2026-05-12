package com.workplat.englishpulish.di

import android.content.Context
import androidx.room.Room
import com.workplat.englishpulish.data.db.AppDatabase
import com.workplat.englishpulish.data.db.ReviewLogDao
import com.workplat.englishpulish.data.db.ReviewStateDao
import com.workplat.englishpulish.data.db.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "english_pulish.db")
            .build()

    @Provides
    fun provideWordDao(db: AppDatabase): WordDao = db.wordDao()

    @Provides
    fun provideReviewStateDao(db: AppDatabase): ReviewStateDao = db.reviewStateDao()

    @Provides
    fun provideReviewLogDao(db: AppDatabase): ReviewLogDao = db.reviewLogDao()
}
