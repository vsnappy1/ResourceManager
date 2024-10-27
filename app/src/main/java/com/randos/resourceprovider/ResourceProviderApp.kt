package com.randos.resourceprovider

import android.app.Application
import com.randos.resourcemanager.InstallResourceManager

@InstallResourceManager
class ResourceProviderApp: Application() {

    override fun onCreate() {
        super.onCreate()

        ResourceManager.initialize(this)
    }
}