package com.pact.app

import android.app.Application
import com.pact.app.core.PactState

class PactApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Keep home-screen widgets in sync with every state change.
        PactState.get(this).onChanged = { PactWidget.updateAll(this) }
    }
}
