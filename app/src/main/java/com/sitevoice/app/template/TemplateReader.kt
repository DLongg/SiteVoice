package com.sitevoice.app.template

import android.content.Context
import java.io.InputStream

class TemplateReader(private val context: Context) {
    fun readTemplateAsset(templateId: String, fileName: String): String {
        return context.assets.open("templates/$templateId/$fileName").bufferedReader().use {
            it.readText()
        }
    }

    fun openTemplateStream(templateId: String, fileName: String): InputStream {
        return context.assets.open("templates/$templateId/$fileName")
    }
}
