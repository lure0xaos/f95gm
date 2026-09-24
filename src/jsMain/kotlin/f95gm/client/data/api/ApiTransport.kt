package f95gm.client.data.api

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

internal class ApiTransport(private val client: HttpClient) {
    suspend fun request(path: String, configure: HttpRequestBuilder.() -> Unit): HttpResponse = try {
        client.request(path, configure)
    } catch (error: CancellationException) {
        throw error
    } catch (error: IOException) {
        throw ApiTransportException(path, error)
    } catch (error: HttpRequestTimeoutException) {
        throw ApiTransportException(path, error)
    }
}
