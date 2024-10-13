package com.justself.klique
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkUtils {
    private var baseUrl: String? = null

    // Flag to control certificate trusting (for easy removal later)
    private var trustAllCertificates = true

    fun initialize(context: Context) {
        baseUrl = context.getString(R.string.base_url)
        Log.d("NetworkUtils", "Base URL: $baseUrl")

        if (trustAllCertificates) {
            setupTrustAllCertificates()
        }
    }

    private fun setupTrustAllCertificates() {
        // Create a TrustManager that blindly trusts all certificates
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(
                    certs: Array<out X509Certificate>?,
                    authType: String?
                ) {
                } // Empty implementation - trust everything

                override fun checkServerTrusted(
                    certs: Array<out X509Certificate>?,
                    authType: String?
                ) {
                } // Empty implementation - trust everything

                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
            }
        )

        // Install the TrustManager
        try {
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, java.security.SecureRandom())

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

            // Disable hostname verification - HIGHLY INSECURE!
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun makeRequest(
        endpoint: String,
        method: KliqueHttpMethod = KliqueHttpMethod.POST,
        params: Map<String, String>,
        jsonBody: String? = null
    ): Pair<Boolean, String> {
        val baseUrl = baseUrl
            ?: throw IllegalStateException("NetworkUtils is not initialized. Call initialize() first.")

        return withContext(Dispatchers.IO) {
            val query = if (method == KliqueHttpMethod.GET && params.isNotEmpty()) {
                "?" + params.map {
                    "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
                }.joinToString("&")
            } else ""

            val url = URL(baseUrl + endpoint + query)
            Log.d("NetworkUtils", "The url is $url")

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method.method
                if (method == KliqueHttpMethod.POST) {
                    setRequestProperty(
                        "Content-Type",
                        jsonBody?.let { "application/json" }
                            ?: "application/x-www-form-urlencoded")
                    doOutput = true
                    if (jsonBody != null) {
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(jsonBody)
                        }
                    } else {
                        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                            writer.write(params.map { (key, value) ->
                                "${URLEncoder.encode(key, "UTF-8")}=${
                                    URLEncoder.encode(
                                        value, "UTF-8"
                                    )
                                }"
                            }.joinToString("&"))
                        }
                    }
                }
            }

            val response = try {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } catch (e: IOException) {
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() } // Handle error stream on exceptions
            } finally {
                connection.disconnect()
            }

            // Log the HTTP response code and the method used
            Log.d("NetworkUtils", "HTTP ${method} Response Code: ${connection.responseCode}")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP error code: ${connection.responseCode} - $response")
            }
            val isSuccessful = connection.responseCode == HttpURLConnection.HTTP_OK
            Log.d("NetworkUtils", "response is $response, $isSuccessful")
            Pair(isSuccessful, response)
        }
    }

    suspend fun makeRequestWithStatusCode(
        endpoint: String,
        method: String = "POST",
        params: Map<String, String> = emptyMap()
    ): Pair<String, Int> {
        val baseUrl = baseUrl ?: throw IllegalStateException("NetworkUtils is not initialized. Call initialize() first.")

        return withContext(Dispatchers.IO) {
            val query = if (method == "GET" && params.isNotEmpty()) {
                "?" + params.map {
                    "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
                }.joinToString("&")
            } else ""

            val url = URL(baseUrl + endpoint + query)

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                if (method == "POST") {
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    doOutput = true
                    BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                        writer.write(params.map { (key, value) ->
                            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
                        }.joinToString("&"))
                    }
                }
                connect()
            }

            val responseCode = connection.responseCode
            val responseBody = try {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } catch (e: IOException) {
                // If there's an IOException, the response body might come from the error stream
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            } finally {
                connection.disconnect()
            }

            Log.d("NetworkUtils", "HTTP ${method} Response Code: $responseCode")
            Pair(responseBody, responseCode)
        }
    }

    suspend fun resourceRequest(
        endpoint: String,
        method: String = "GET", // Default to GET for plain resource fetching
        params: Map<String, String> = emptyMap()
    ): ByteArray {
        val baseUrl = baseUrl ?: throw IllegalStateException("NetworkUtils is not initialized. Call initialize() first.")

        return withContext(Dispatchers.IO) {
            // Construct query string for GET requests (same logic as before)
            val query = if (method == "GET" && params.isNotEmpty()) {
                "?" + params.map {
                    "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
                }.joinToString("&")
            } else ""

            val url = URL(baseUrl + endpoint + query)

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method

                // No content-type specific settings unless absolutely necessary
            }

            // Get the raw input stream
            val response = try {
                connection.inputStream.use { it.readBytes() }
            } catch (e: IOException) {
                // Handle errors appropriately
                throw e // You might want to customize error handling
            } finally {
                connection.disconnect()
            }

            Log.d("NetworkUtils", "HTTP ${method} Response Code: ${connection.responseCode}")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP error code: ${connection.responseCode}")
            }

            response
        }
    }
}
enum class KliqueHttpMethod(val method: String) {
    GET("GET"),
    POST("POST")
}