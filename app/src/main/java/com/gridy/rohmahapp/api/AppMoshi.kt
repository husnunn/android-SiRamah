package com.gridy.rohmahapp.api

import com.gridy.rohmahapp.api.moshi.FlexibleStringJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object AppMoshi {
    fun build(): Moshi =
        Moshi.Builder()
            .add(FlexibleStringJsonAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
}
