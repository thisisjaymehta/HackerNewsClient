package com.neuralquark.hackernewsclient.util

import android.text.Html
import android.os.Build
import androidx.core.text.HtmlCompat

object HtmlUtils {
    
    /**
     * Parse HTML content from HN comments/posts
     */
    fun parseHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        
        return HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()
    }
    
    /**
     * Extract domain from URL
     */
    fun extractDomain(url: String?): String? {
        if (url.isNullOrBlank()) return null
        
        return try {
            val uri = java.net.URI(url)
            var host = uri.host ?: return null
            if (host.startsWith("www.")) {
                host = host.substring(4)
            }
            host
        } catch (_: Exception) {
            null
        }
    }
}
