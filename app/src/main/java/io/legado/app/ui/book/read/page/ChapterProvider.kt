package io.legado.app.ui.book.read.page

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import com.github.houbb.opencc4j.util.ZhConverterUtil
import io.legado.app.App
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.accentColor


object ChapterProvider {
    var readAloudSpan = ForegroundColorSpan(App.INSTANCE.accentColor)
    private val titleSpan = RelativeSizeSpan(1.2f)

    var textView: ContentTextView? = null

    fun getTextChapter(
        bookChapter: BookChapter,
        content: String,
        chapterSize: Int,
        isHtml: Boolean = false
    ): TextChapter {
        textView?.let {
            val textPages = arrayListOf<TextPage>()
            val pageLines = arrayListOf<Int>()
            val pageLengths = arrayListOf<Int>()
            var surplusText = convertChinese(content)
            var pageIndex = 0
            while (surplusText.isNotEmpty()) {
                val spannableStringBuilder =
                    if (isHtml) {
                        HtmlCompat.fromHtml(
                            surplusText,
                            FROM_HTML_MODE_COMPACT
                        ) as SpannableStringBuilder
                    } else {
                        SpannableStringBuilder(surplusText)
                    }
                if (pageIndex == 0) {
                    val end = surplusText.indexOf("\n")
                    if (end > 0) {
                        spannableStringBuilder.setSpan(
                            titleSpan,
                            0,
                            end,
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                it.text = spannableStringBuilder
                val lastLine = it.getLineNum()
                val lastCharNum = it.getCharNum(lastLine)
                if (lastCharNum == 0) {
                    break
                } else {
                    pageLines.add(lastLine)
                    pageLengths.add(lastCharNum)
                    textPages.add(
                        TextPage(
                            index = pageIndex,
                            text = spannableStringBuilder.delete(
                                lastCharNum,
                                spannableStringBuilder.length
                            ),
                            title = convertChinese(bookChapter.title),
                            chapterSize = chapterSize,
                            chapterIndex = bookChapter.index
                        )
                    )
                    surplusText = surplusText.substring(lastCharNum)
                    pageIndex++
                }
            }
            for (item in textPages) {
                item.pageSize = textPages.size
            }
            return TextChapter(
                bookChapter.index,
                bookChapter.title,
                bookChapter.url,
                textPages,
                pageLines,
                pageLengths,
                chapterSize
            )
        } ?: return TextChapter(
            bookChapter.index,
            bookChapter.title,
            bookChapter.url,
            arrayListOf(),
            arrayListOf(),
            arrayListOf(),
            chapterSize
        )

    }

    fun upReadAloudSpan() {
        readAloudSpan = ForegroundColorSpan(App.INSTANCE.accentColor)
    }

    private fun convertChinese(string: String): String =
        when (AppConfig.isChineseConverterEnable) {
            "1" -> ZhConverterUtil.toTraditional(string)
            "2" -> ZhConverterUtil.toSimple(string)
            else -> string
        }
}