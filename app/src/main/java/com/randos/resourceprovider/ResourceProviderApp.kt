package com.randos.resourceprovider

import android.app.Application

class ResourceProviderApp: Application(){

    override fun onCreate() {
        super.onCreate()

        ResourceManager.initialize(this)
    }
}