package com.bookmyturf.app

import android.app.Application
import com.bookmyturf.app.data.DemoBookMyTurfRepository

class BookMyTurfApplication : Application() {
    val repository by lazy { DemoBookMyTurfRepository() }
}
