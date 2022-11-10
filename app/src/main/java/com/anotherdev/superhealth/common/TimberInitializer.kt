package com.anotherdev.superhealth.common

import android.content.Context
import androidx.startup.Initializer
import timber.log.Timber

class TimberInitializer : Initializer<Timber.Tree> {

    override fun create(context: Context): Timber.Tree {
        val logcatTree = Timber.DebugTree()
        Timber.plant(logcatTree)
        return logcatTree
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}