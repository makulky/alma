package com.alma.app

import android.app.Application

class AlmaApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
