package dev.sito8943.focushabit.core.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** The request never got an answer: no network, timeout, DNS. Retried later on its own. */
class NetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** The server answered with an error. [message] is PostgREST's own when it sent one. */
class ApiException(val status: Int, message: String) : Exception(message)

/** The build has no Supabase URL/key (see `local.properties`). */
class NotConfiguredException : Exception("Supabase is not configured")

/**
 * Minimal PostgREST client: the anon key as `apikey` and Bearer, JSON in and
 * out. No supabase-kt: four verbs on two tables do not justify Ktor.
 */
class SupabaseRest(
    baseUrl: String,
    private val anonKey: String,
    private val json: Json,
) {

    private val restUrl = baseUrl.trimEnd('/') + "/rest/v1/"

    val isConfigured: Boolean = baseUrl.isNotBlank() && anonKey.isNotBlank()

    private val client = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    /** @param path table plus PostgREST query, e.g. `habits?code=eq.ABCD`. */
    suspend fun get(path: String): String = execute(path, "GET", null)

    /** Inserts and returns the created rows (`Prefer: return=representation`). */
    suspend fun post(path: String, body: String): String =
        execute(path, "POST", body, prefer = "return=representation")

    suspend fun patch(path: String, body: String) {
        execute(path, "PATCH", body, prefer = "return=minimal")
    }

    private suspend fun execute(
        path: String,
        method: String,
        body: String?,
        prefer: String? = null,
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured) throw NotConfiguredException()
        val table = path.substringBefore('?')
        val request = Request.Builder()
            .url(restUrl + path)
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .header("Accept", "application/json")
            .apply { if (prefer != null) header("Prefer", prefer) }
            .method(method, body?.toRequestBody(JSON))
            .build()

        val response = try {
            client.newCall(request).await()
        } catch (error: IOException) {
            // No URL in the message: it carries the recovery code.
            throw NetworkException("$method $table: ${error.javaClass.simpleName}", error)
        }
        response.use {
            val text = it.body.string()
            if (!it.isSuccessful) throw ApiException(it.code, errorMessage(it.code, text))
            text
        }
    }

    private fun errorMessage(status: Int, body: String): String {
        val message = runCatching {
            json.parseToJsonElement(body) as JsonObject
        }.getOrNull()?.get("message")?.jsonPrimitive?.contentOrNull
        return message ?: "HTTP $status"
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = continuation.resume(response)

        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
    })
}
