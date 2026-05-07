// File: /app/src/main/java/com/gridy/rohmahapp/api/ResponseInterceptor.kt
package com.gridy.rohmahapp.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import android.app.Application
import com.gridy.rohmahapp.RohMahApp
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.Utils
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset
import javax.inject.Inject

@Suppress("UNCHECKED_CAST")
class ResponseInterceptor @Inject constructor(
    moshi: Moshi,
    private val pref: PreferenceClass,
    private val utils: Utils,
    private val application: Application
) : Interceptor {

    private val errorAdapter by lazy {
        moshi.adapter<Map<String, Any>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
            )
        )
    }

    private val defaultErrorMsg by lazy {
        "Terjadi gangguan pada koneksi internet Anda, silahkan ulangi beberapa saat lagi"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val responseCode = response.code
        val responseBody = response.body

        return try {
            val content = responseBody?.source()
                ?.also { it.request(Long.MAX_VALUE) }
                ?.buffer
                ?.clone()
                ?.readString(Charset.forName("UTF-8"))
                ?: defaultErrorMsg

            val contentMap: Map<String, Any> = try {
                errorAdapter.fromJson(content) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            val errorsMap = contentMap["errors"] as? Map<String, *>
            val errorTypes: Array<String> = try {
                errorsMap?.keys?.toTypedArray() ?: emptyArray()
            } catch (e: Exception) {
                emptyArray()
            }

            val validationErrors = parseValidationErrors(errorsMap)

            val errorData: Map<String, Any> = try {
                (contentMap["data"] as? Map<String, Any>) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            if ((responseCode / 100) == 2) {
                val mediaType = responseBody?.contentType()
                responseBody?.close()
                val newBody = content.toResponseBody(mediaType)
                response.newBuilder().body(newBody).build()
            } else {
                val errorBody = (contentMap["message"] as? String) ?: defaultErrorMsg
                responseBody?.close()

                if (responseCode == 401 && !isLoginEndpoint(request.url.encodedPath)) {
                    (application as? RohMahApp)?.let { app ->
                        UnauthorizedSessionCoordinator.notifyUnauthorized(
                            app,
                            pref,
                            errorBody.takeUnless { it == defaultErrorMsg }
                        )
                    }
                }

                throw ApiException(
                    message = errorBody,
                    responseCode = responseCode,
                    errorTypes = errorTypes,
                    data = errorData,
                    validationErrors = validationErrors,
                )
            }
        } catch (e: Exception) {
            responseBody?.close()
            Timber.e(e)
            throw e
        }
    }

    private fun isLoginEndpoint(encodedPath: String): Boolean =
        encodedPath.endsWith("/student/login") || encodedPath.endsWith("/teacher/login")

    private fun parseValidationErrors(errorsAny: Map<String, *>?): Map<String, List<String>> {
        if (errorsAny == null) return emptyMap()
        val out = LinkedHashMap<String, List<String>>()
        for ((k, v) in errorsAny) {
            val key = k as? String ?: continue
            when (v) {
                is List<*> -> out[key] = v.mapNotNull { item -> item?.toString()?.takeIf { it.isNotBlank() } }
                is String -> if (v.isNotBlank()) out[key] = listOf(v)
                else -> Unit
            }
        }
        return out
    }
}

