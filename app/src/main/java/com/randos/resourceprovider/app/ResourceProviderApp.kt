package com.randos.resourceprovider.app

import android.app.Application
import com.randos.resourcemanager.InstallResourceManager

@InstallResourceManager(namespace = "com.randos.resourceprovider")
class ResourceProviderApp: Application() {

    override fun onCreate() {
        super.onCreate()

        ResourceManager.initialize(this)
    }
}