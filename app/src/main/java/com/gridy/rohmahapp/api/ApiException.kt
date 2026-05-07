package com.gridy.rohmahapp.api

import java.io.IOException

class ApiException(
    override val message: String?,
    val responseCode: Int?,
    val errorTypes: Array<String> = emptyArray(),
    val data: Map<String, Any> = emptyMap(),
    val validationErrors: Map<String, List<String>> = emptyMap(),
) : IOException(message)

/** 401 (non-login): ditangani [UnauthorizedSessionCoordinator]. */
fun Throwable.isUnauthorizedSessionHandled(): Boolean =
    this is ApiException && responseCode == 401