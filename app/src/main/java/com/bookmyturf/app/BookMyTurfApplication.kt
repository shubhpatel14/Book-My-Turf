package com.bookmyturf.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.bookmyturf.app.data.FirebaseBookMyTurfRepository
import com.bookmyturf.app.ui.components.DataUriFetcher
import okhttp3.OkHttpClient

class BookMyTurfApplication : Application(), ImageLoaderFactory {
    val repository by lazy { FirebaseBookMyTurfRepository() }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(request)
            }
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                add(DataUriFetcher.Factory())
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
