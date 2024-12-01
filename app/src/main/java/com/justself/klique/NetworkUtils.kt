package com.justself.klique
import android.content.Context
import android.util.Log
import com.justself.klique.MyKliqueApp.Companion.appContext
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
                }

                override fun checkServerTrusted(
                    certs: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

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
        jsonBody: String? = null,
        binaryBody: ByteArray? = null,
        useJWT: Boolean = false
    ): Triple<Boolean, String, Int> {
        Log.d("GistDescription", endpoint)
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
                if (useJWT) {
                    val accessToken = JWTNetworkCaller.fetchAccessToken()
                        ?: throw IllegalStateException("Access token is null. Ensure you're logged in.")
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
                if (method == KliqueHttpMethod.POST) {
                    if (binaryBody != null) {
                        setRequestProperty("Content-Type", "application/octet-stream")
                        doOutput = true
                        outputStream.use { it.write(binaryBody) }
                    }
                    else {
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
            }
            val response = try {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } catch (e: IOException) {
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() } // Handle error stream on exceptions
            } finally {
                connection.disconnect()
            }
            Log.d("NetworkUtils", "HTTP $method Response Code: ${connection.responseCode}")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP error code: ${connection.responseCode} - $response")
            }
            val isSuccessful = connection.responseCode == HttpURLConnection.HTTP_OK
            Log.d("NetworkUtils", "response is $response, $isSuccessful")
            Triple(isSuccessful, response, connection.responseCode)
        }
    }
    suspend fun makeMultipartRequest(
        endpoint: String,
        fields: List<MultipartField>
    ): Triple<Boolean, String, Int> {
        val baseUrl = baseUrl
            ?: throw IllegalStateException("NetworkUtils is not initialized. Call initialize() first.")

        return withContext(Dispatchers.IO) {
            val url = URL(baseUrl + endpoint)
            val boundary = "Boundary-" + System.currentTimeMillis()
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                doOutput = true
            }

            connection.outputStream.use { outputStream ->
                val writer = outputStream.bufferedWriter()
                for (field in fields) {
                    writer.append("--$boundary\r\n")
                    if (field.value is ByteArray && field.mimeType != null) {
                        writer.append("Content-Disposition: form-data; name=\"${field.name}\"; filename=\"${field.fileName ?: "file"}\"\r\n")
                        writer.append("Content-Type: ${field.mimeType.type}\r\n\r\n")
                        writer.flush()
                        outputStream.write(field.value)
                        outputStream.flush()
                    } else if (field.value is String) {
                        writer.append("Content-Disposition: form-data; name=\"${field.name}\"\r\n\r\n")
                        writer.append("${field.value}\r\n")
                    }
                    writer.flush()
                }

                // End boundary
                writer.append("\r\n--$boundary--\r\n")
                writer.flush()
            }

            val response = try {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } catch (e: IOException) {
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            } finally {
                connection.disconnect()
            }

            val isSuccessful = connection.responseCode == HttpURLConnection.HTTP_OK
            Triple(isSuccessful, response, connection.responseCode)
        }
    }

    fun fixLocalHostUrl(url: String): String {
        return if (url.contains("127.0.0.1")) {
            baseUrl?.let { base ->
                val ip = base.removePrefix("http://")
                    .removeSuffix("/")
                    .split(":")
                    .firstOrNull() ?: return url
                url.replace("127.0.0.1", ip)
            } ?: url
        } else {
            url
        }
    }
}
suspend fun downloadFromUrl(url: String): ByteArray = withContext(Dispatchers.IO) {
    val newUrl = NetworkUtils.fixLocalHostUrl(url = url)
    val connection = URL(newUrl).openConnection() as HttpURLConnection
    connection.apply {
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 10000
        doInput = true
        connect()
    }
    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IOException("Failed to download file: HTTP ${connection.responseCode}")
    }
    connection.inputStream.use { it.readBytes() }
}
enum class KliqueHttpMethod(val method: String) {
    GET("GET"),
    POST("POST")
}
enum class MimeType(val type: String) {
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    TEXT_PLAIN("text/plain"),
    APPLICATION_JSON("application/json"),
    APPLICATION_OCTET_STREAM("application/octet-stream"),
    VIDEO_MP4("video/mp4"),
    AUDIO_MPEG4("audio/m4a");
}
data class MultipartField(
    val name: String,
    val value: Any,
    val mimeType: MimeType? = null,
    val fileName: String? = null
)