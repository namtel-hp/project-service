package com.ampnet.projectservice.exception

class InvalidRequestException(
    val errorCode: ErrorCode,
    exceptionMessage: String,
    throwable: Throwable? = null,
    val errors: Map<String, String> = emptyMap()
) : Exception(exceptionMessage, throwable)

class ResourceAlreadyExistsException(
    val errorCode: ErrorCode,
    exceptionMessage: String,
    val errors: Map<String, String> = emptyMap()
) : Exception(exceptionMessage)

class ResourceNotFoundException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class InternalException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

class GrpcException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)
