package com.neuralquark.hackernewsclient.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import android.util.Patterns

object HtmlUtils {
    
    /**
     * Parse HTML content from HN comments/posts - returns plain string
     */
    fun parseHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        
        return HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()
    }
    
    /**
     * Parse HTML content and return AnnotatedString with clickable links
     * Uses Android's Patterns.WEB_URL for robust URL detection
     */
    fun parseHtmlWithLinks(html: String?, linkColor: Color): AnnotatedString {
        if (html.isNullOrBlank()) return AnnotatedString("")
        
        // First convert HTML to plain text
        val plainText = HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()
        
        return buildAnnotatedString {
            append(plainText)
            
            // Find URLs in the plain text using Android's robust URL pattern
            val urlMatcher = Patterns.WEB_URL.matcher(plainText)
            while (urlMatcher.find()) {
                val start = urlMatcher.start()
                val end = urlMatcher.end()
                val url = urlMatcher.group() ?: continue
                
                // Ensure URL has a scheme for proper handling
                val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else {
                    url
                }
                
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = start,
                    end = end
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = fullUrl,
                    start = start,
                    end = end
                )
            }
        }
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
        } catch (e: Exception) {
            android.util.Log.d("HtmlUtils", "Failed to extract domain from URL: $url", e)
            null
        }
    }
}
