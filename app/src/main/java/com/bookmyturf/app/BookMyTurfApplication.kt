package com.bookmyturf.app

import android.app.Application
import com.bookmyturf.app.data.FirebaseBookMyTurfRepository

class BookMyTurfApplication : Application() {
    val repository by lazy { FirebaseBookMyTurfRepository() }
}
