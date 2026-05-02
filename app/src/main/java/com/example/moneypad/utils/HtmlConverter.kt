package com.example.moneypad.utils

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat

object HtmlConverter {

    fun String.parseHtmlToAnnotatedString(): AnnotatedString {
        val imageRegex = Regex("""\[Image:\s*(.*?)\]""")
        val uris = mutableListOf<String>()

        // Pre-process tags that HtmlCompat doesn't handle natively the way we want
        val preProcessed = this.replace("<mark>", "<span style=\"background-color:#FFFF00;\">")
            .replace("</mark>", "</span>")
            .replace("<h1>", "<span style=\"font-size:1.5em;\"><b>")
            .replace("</h1>", "</b></span>")
            .replace(imageRegex) { match ->
                uris.add(match.groupValues[1])
                "\uFFFC"
            }
            
        val spanned = HtmlCompat.fromHtml(preProcessed, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val builder = AnnotatedString.Builder(spanned.toString())
        
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            when (span) {
                is StyleSpan -> {
                    if (span.style == Typeface.BOLD || span.style == Typeface.BOLD_ITALIC) {
                        builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    }
                    if (span.style == Typeface.ITALIC || span.style == Typeface.BOLD_ITALIC) {
                        builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    }
                }
                is UnderlineSpan -> {
                    builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                }
                is BackgroundColorSpan -> {
                    builder.addStyle(SpanStyle(background = androidx.compose.ui.graphics.Color(span.backgroundColor)), start, end)
                }
                is RelativeSizeSpan -> {
                    builder.addStyle(SpanStyle(fontSize = (16 * span.sizeChange).sp), start, end)
                }
                is TypefaceSpan -> {
                    val family = when (span.family) {
                        "serif" -> FontFamily.Serif
                        "monospace" -> FontFamily.Monospace
                        "cursive" -> FontFamily.Cursive
                        "sans-serif" -> FontFamily.SansSerif
                        else -> FontFamily.Default
                    }
                    builder.addStyle(SpanStyle(fontFamily = family), start, end)
                }
            }
        }
        
        val rawText = spanned.toString()
        var uriIndex = 0
        for (i in rawText.indices) {
            if (rawText[i] == '\uFFFC') {
                if (uriIndex < uris.size) {
                    val uri = uris[uriIndex++]
                    builder.addStyle(SpanStyle(fontSize = 200.sp, color = androidx.compose.ui.graphics.Color.Transparent), i, i + 1)
                    builder.addStringAnnotation("IMAGE", uri, i, i + 1)
                }
            }
        }
        
        return builder.toAnnotatedString()
    }

    fun AnnotatedString.toHtmlString(): String {
        val spannable = SpannableStringBuilder(text)
        spanStyles.forEach { span ->
            val style = span.item
            val start = span.start
            val end = span.end
            
            if (style.fontWeight == FontWeight.Bold) {
                spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (style.fontStyle == FontStyle.Italic) {
                spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (style.textDecoration == TextDecoration.Underline) {
                spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (style.background != androidx.compose.ui.graphics.Color.Unspecified) {
                spannable.setSpan(BackgroundColorSpan(Color.YELLOW), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (style.fontFamily != null) {
                val familyName = when (style.fontFamily) {
                    FontFamily.Serif -> "serif"
                    FontFamily.Monospace -> "monospace"
                    FontFamily.Cursive -> "cursive"
                    FontFamily.SansSerif -> "sans-serif"
                    else -> "sans-serif"
                }
                spannable.setSpan(TypefaceSpan(familyName), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (style.fontSize.isSp && style.fontSize.value > 18f && style.fontSize.value < 100f) {
                spannable.setSpan(RelativeSizeSpan(1.5f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return HtmlCompat.toHtml(spannable, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE).trim()
    }
}