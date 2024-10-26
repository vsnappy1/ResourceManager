package com.randos.resourcemanager.model

data class ValueVariant<T>(
    val identifier: String,
    val value: T
)