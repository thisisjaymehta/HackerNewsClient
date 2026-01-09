package com.neuralquark.hackernewsclient.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

object ChromeTabsUtils {
    
    /**
     * Open a URL in Chrome Custom Tabs
     */
    fun openUrl(context: Context, url: String, toolbarColor: Int) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColor)
                    .build()
            )
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()
        
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}
