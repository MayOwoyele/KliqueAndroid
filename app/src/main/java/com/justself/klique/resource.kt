package com.justself.klique



sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val code: Int? = null  // Make code optional
) {
    class Loading<T> : Resource<T>()
    class Success<T>(data: T, code: Int? = null) : Resource<T>(data = data, code = code)  // Code is now optional
    class Error<T>(message: String, code: Int, data: T? = null) : Resource<T>(data, message, code)
}
