package com.randos.resourcemanager.model

internal data class Resource<T>(
    val name: String,
    val value: T,
    val returnType: String = value!!::class.simpleName.toString()
)