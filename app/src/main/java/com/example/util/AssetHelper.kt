package com.example.util

import android.content.Context
import android.util.Log
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility for loading and caching externalized template and specification assets.
 * Eliminates large in-line string literals and prevents XSS/injection bugs.
 */
object AssetHelper {
    private const val TAG = "AssetHelper"
    private val memoryCache = ConcurrentHashMap<String, String>()

    fun loadAsset(context: Context, path: String, useCache: Boolean = true): String {
        if (useCache && memoryCache.containsKey(path)) {
            return memoryCache[path] ?: ""
        }

        return try {
            context.assets.open(path).use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    val content = reader.readText()
                    if (useCache) {
                        memoryCache[path] = content
                    }
                    content
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load asset at $path", e)
            ""
        }
    }
}
