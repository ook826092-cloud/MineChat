//package cn.mine.minestars.data.ai.mcp.transport
//
//import android.util.Log
//import io.ktor.client.HttpClient
//import io.ktor.client.plugins.sse.ClientSSESession
//import io.ktor.client.plugins.sse.SSEClient
//import io.ktor.client.plugins.sse.sse
//import io.ktor.client.plugins.sse.sseSession
//import io.ktor.client.request.HttpRequestBuilder
//import io.ktor.client.request.post
//import io.ktor.client.request.setBody
//import io.ktor.client.statement.bodyAsText
//import io.ktor.http.ContentType
//import io.ktor.http.HttpHeaders
//import io.ktor.http.isSuccess
//import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
//import io.modelcontextprotocol.kotlin.sdk.shared.TransportSendOptions
//import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
//import io.modelcontextprotocol.kotlin.sdk.types.McpJson
//import kotlinx.coroutines.CancellationException
//import kotlinx.coroutines.CompletableDeferred
//import kotlinx.coroutines.CoroutineName
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.ensureActive
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withTimeout
//import kotlinx.serialization.SerializationException
//import kotlin.concurrent.atomics.AtomicBoolean
//import kotlin.concurrent.atomics.ExperimentalAtomicApi
//import kotlin.time.Duration
//
//private const val TAG = "StreamableHttpClientTransport"
//private const val STREAMABLE_HTTP_SESSION_ENDPOINT = "session"
//
//@OptIn(ExperimentalAtomicApi::class)
//class StreamableHttpClientTransport(
//    private val client: HttpClient,
//    private val urlString: String?,
//    private val sseEndpoint: String = STREAMABLE_HTTP_SESSION_ENDPOINT,
//    private val reconnectionTime: Duration? = null,
//    private val requestBuilder: HttpRequestBuilder.() -> Unit = {},
//) : AbstractTransport() {
//
//    private val initialized: AtomicBoolean = AtomicBoolean(false)
//    private val endpoint = CompletableDeferred<String>()
//
//    private lateinit var session: ClientSSESession
//    private lateinit var scope: CoroutineScope
//    private var job: Job? = null
//
//    override suspend fun start() {
//        check(initialized.compareAndSet(expectedValue = false, newValue = true)) {
//            "StreamableHttpClientTransport already started! If using Client class, note that connect() calls start() automatically."
//        }
//
//        try {
//            session = client.sseSession(
//                urlString = "$urlString/$sseEndpoint",
//                reconnectionTime = reconnectionTime,
//                block = requestBuilder,
//            )
//
//            scope = CoroutineScope(session.coroutineContext + SupervisorJob())
//            job = scope.launch(CoroutineName("StreamableHttpClientTransport#${hashCode()}")) {
//                collectMessages()
//            }
//
//            val sessionId = withTimeout(5000) { endpoint.await() }
//            Log.d(TAG, "Session established: $sessionId")
//
//            val postUrl = "$urlString/$sseEndpoint/$sessionId"
//            val response = client.post(postUrl) {
//                requestBuilder()
//                headers.append(HttpHeaders.ContentType, ContentType.Application.Json)
//            }
//            Log.d(TAG, "POST $postUrl -> HTTP ${response.status}")
//        } catch (e: Exception) {
//            closeResources()
//            initialized.store(false)
//            throw e
//        }
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    override suspend fun send(message: JSONRPCMessage, options: TransportSendOptions?) {
//        if (!initialized.load()) error("StreamableHttpClientTransport is not initialized!")
//        if (job?.isActive != true) error("StreamableHttpClientTransport is closed!")
//        if (!endpoint.isCompleted) error("Not connected! Missing session ID!")
//
//        try {
//            val postUrl = "$urlString/$sseEndpoint/${endpoint.getCompleted()}"
//            val response = client.post(postUrl) {
//                requestBuilder()
//                headers.append(HttpHeaders.ContentType, ContentType.Application.Json)
//                setBody(McpJson.encodeToString(message))
//            }
//
//            val bodyText = response.bodyAsText()
//            if (!response.status.isSuccess()) {
//                error("Error POSTing to endpoint (HTTP ${response.status}): $bodyText")
//            }
//
//            Log.d(TAG, "Client successfully sent message via Streamable HTTP $postUrl")
//        } catch (e: Throwable) {
//            _onError(e)
//            throw e
//        }
//    }
//
//    override suspend fun close() {
//        if (!initialized.load()) error("StreamableHttpClientTransport is not initialized!")
//        closeResources()
//    }
//
//    private suspend fun CoroutineScope.collectMessages() {
//        try {
//            session.incoming.collect { event ->
//                ensureActive()
//
//                when (event.event) {
//                    "error" -> {
//                        val error = IllegalStateException("SSE error: ${event.data}")
//                        _onError(error)
//                        throw error
//                    }
//
//                    "message" -> handleMessage(event.data.orEmpty())
//
//                    else -> {
//                        if (endpoint.isCompleted) {
//                            handleMessage(event.data.orEmpty())
//                        } else {
//                            Log.d(TAG, "Received session endpoint: ${event.data}")
//                            endpoint.complete(event.data.orEmpty())
//                        }
//                    }
//                }
//            }
//        } catch (e: CancellationException) {
//            throw e
//        } catch (e: Exception) {
//            _onError(e)
//            throw e
//        }
//    }
//
//    private fun closeResources() {
//        if (::scope.isInitialized) {
//            scope.cancel()
//        }
//        initialized.store(false)
//    }
//}
