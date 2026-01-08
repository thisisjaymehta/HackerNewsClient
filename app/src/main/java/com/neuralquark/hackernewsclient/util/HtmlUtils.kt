package com.neuralquark.hackernewsclient.util

import android.text.Html
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import java.util.regex.Pattern

object HtmlUtils {
    
    private val URL_PATTERN = Pattern.compile(
        "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
    )
    
    private val HTML_LINK_PATTERN = Pattern.compile(
        "<a\\s+href=[\"']([^\"']+)[\"'][^>]*>([^<]*)</a>",
        Pattern.CASE_INSENSITIVE
    )
    
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
     */
    fun parseHtmlWithLinks(html: String?, linkColor: Color): AnnotatedString {
        if (html.isNullOrBlank()) return AnnotatedString("")
        
        // First convert HTML to plain text but preserve link info
        val plainText = HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()
        
        // Extract links from original HTML
        val links = mutableListOf<Triple<String, Int, Int>>() // url, start, end
        val linkMatcher = HTML_LINK_PATTERN.matcher(html)
        
        return buildAnnotatedString {
            append(plainText)
            
            // Find URLs in the plain text and annotate them
            val urlMatcher = URL_PATTERN.matcher(plainText)
            while (urlMatcher.find()) {
                val start = urlMatcher.start()
                val end = urlMatcher.end()
                val url = urlMatcher.group()
                
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
                    annotation = url,
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
        } catch (_: Exception) {
            null
        }
    }
}
