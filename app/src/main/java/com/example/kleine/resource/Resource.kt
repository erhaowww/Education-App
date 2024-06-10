package com.example.kleine.resource

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val status: Status
) {
    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    class Success<T>(data: T?) : Resource<T>(data, null, Status.SUCCESS)
    class Error<T>(message: String?) : Resource<T>(null, message, Status.ERROR)
    class Loading<T> : Resource<T>(null, null, Status.LOADING)
}
