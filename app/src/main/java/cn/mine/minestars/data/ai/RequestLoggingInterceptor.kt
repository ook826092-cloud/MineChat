package cn.mine.minestars.data.ai

import cn.mine.common.android.LogEntry
import cn.mine.common.android.Logging
import cn.mine.minestars.data.datastore.SettingsStore
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

class RequestLoggingInterceptor(
    private val settingsStore: SettingsStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val requestHeaders = request.headers.toMap()
        val requestBody = request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }

        val response: Response
        var error: String? = null

        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            error = e.message
            Logging.logRequest(
                LogEntry.RequestLog(
                    tag = "HTTP",
                    url = request.url.toString(),
                    method = request.method,
                    requestHeaders = requestHeaders,
                    requestBody = requestBody,
                    error = error
                )
            )
            throw e
        }

        val durationMs = System.currentTimeMillis() - startTime
        val responseHeaders = response.headers.toMap()

        val recordBody = settingsStore.settingsFlow.value.recordHttpResponseBody
        val isStreaming = response.header("Content-Type", "")
            ?.contains("text/event-stream") == true

        val responseBody = if (recordBody && !isStreaming) {
            try {
                response.body.string()
            } catch (_: Exception) {
                null
            }
        } else null

        val loggedResponse = if (responseBody != null) {
            val mediaType = response.body.contentType()
            val newBody = responseBody.toResponseBody(mediaType)
            response.newBuilder().body(newBody).build()
        } else {
            response
        }

        Logging.logRequest(
            LogEntry.RequestLog(
                tag = "HTTP",
                url = request.url.toString(),
                method = request.method,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseCode = response.code,
                responseHeaders = responseHeaders,
                responseBody = responseBody,
                durationMs = durationMs,
                error = error
            )
        )

        return loggedResponse
    }

    private fun okhttp3.Headers.toMap(): Map<String, String> {
        return names().associateWith { get(it) ?: "" }
    }
}
