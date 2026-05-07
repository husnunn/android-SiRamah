package com.gridy.rohmahapp.di

import com.gridy.rohmahapp.di.module.AppModule
import com.gridy.rohmahapp.di.module.NetworkModule
import com.gridy.rohmahapp.pages.auth.LoginActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class
    ]
)
interface AppComponent {
    fun inject(activity: LoginActivity)
}