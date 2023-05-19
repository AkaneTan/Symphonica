package org.akanework.symphonica

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors

class SymphonicaApplication : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}