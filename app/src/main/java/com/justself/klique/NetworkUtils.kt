package com.justself.klique
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
    var baseUrl = "https://192.168.0.204/everythinglucii/api/"

    // Flag to control certificate trusting (for easy removal later)
    private var trustAllCertificates = true

    init {
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
        method: String = "POST",
        params: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
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
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(params.map { (key, value) ->
                        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
                    }.joinToString("&"))
                }
            }
        }

        val response = try {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } catch (e: IOException) {
            BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() } // Handle error stream on exceptions
        } finally {
            connection.disconnect() // Ensuring connection is closed after execution
        }

        // Log the HTTP response code and the method used
        Log.d("NetworkUtils", "HTTP ${method} Response Code: ${connection.responseCode}")

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP error code: ${connection.responseCode} - $response")
        }

        response
    }
    suspend fun makeRequestWithStatusCode(
        endpoint: String,
        method: String = "POST",
        params: Map<String, String> = emptyMap()
    ): Pair<String, Int> = withContext(Dispatchers.IO) {
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
    suspend fun resourceRequest(
        endpoint: String,
        method: String = "GET", // Default to GET for plain resource fetching
        params: Map<String, String> = emptyMap()
    ): ByteArray = withContext(Dispatchers.IO) {
        // Construct query string for GET requests (same logic as before)
        val query = if (method == "GET" && params.isNotEmpty()) {
            "?" + params.map {
                "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
            }.joinToString("&")
        } else ""

        val url = URL(endpoint + query)

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
