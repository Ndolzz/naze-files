package com.naze.files

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder

/**
 * Naze Files application entry point.
 *
 * Also supplies the app-wide Coil [ImageLoader] with GIF and video-frame
 * decoding registered, so every AsyncImage (image viewer, file browser
 * thumbnails) can show animated GIFs and real video thumbnails without each
 * call site wiring its own decoder.
 */
class NazeFilesApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}
