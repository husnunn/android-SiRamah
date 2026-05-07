package com.gridy.rohmahapp.di.module

import android.content.Context
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.Utils
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val context: Context) {

    @Provides
    @Singleton
    fun provideContext(): Context = context

    @Provides
    @Singleton
    fun providePreferenceClass(): PreferenceClass {
        return PreferenceClass(context)
    }

    @Provides
    @Singleton
    fun provideUtils(): Utils {
        return Utils(context)
    }
}