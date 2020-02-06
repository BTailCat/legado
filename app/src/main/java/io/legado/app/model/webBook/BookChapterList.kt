package io.legado.app.model.webBook

import android.text.TextUtils
import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

object BookChapterList {

    suspend fun analyzeChapterList(
        coroutineScope: CoroutineScope,
        book: Book,
        body: String?,
        bookSource: BookSource,
        baseUrl: String
    ): List<BookChapter> {
        var chapterList = arrayListOf<BookChapter>()
        body ?: throw Exception(
            App.INSTANCE.getString(R.string.error_get_web_content, baseUrl)
        )
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        val tocRule = bookSource.getTocRule()
        val nextUrlList = arrayListOf(baseUrl)
        var reverse = false
        var listRule = tocRule.chapterList ?: ""
        if (listRule.startsWith("-")) {
            reverse = true
            listRule = listRule.substring(1)
        }
        if (listRule.startsWith("+")) {
            listRule = listRule.substring(1)
        }
        var chapterData =
            analyzeChapterList(body, baseUrl, tocRule, listRule, book, bookSource, log = true)
        chapterData.chapterList?.let {
            chapterList.addAll(it)
        }
        if (chapterData.nextUrl.size == 1) {
            var nextUrl = chapterData.nextUrl[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                nextUrlList.add(nextUrl)
                AnalyzeUrl(
                    ruleUrl = nextUrl, book = book, headerMapF = bookSource.getHeaderMap()
                ).getResponseAwait()
                    .body?.let { nextBody ->
                    chapterData = analyzeChapterList(
                        nextBody, nextUrl, tocRule, listRule,
                        book, bookSource, log = false
                    )
                    nextUrl = if (chapterData.nextUrl.isNotEmpty())
                        chapterData.nextUrl[0]
                    else ""
                    chapterData.chapterList?.let {
                        chapterList.addAll(it)
                    }
                }
            }
            Debug.log(bookSource.bookSourceUrl, "◇目录总页数:${nextUrlList.size}")
        } else if (chapterData.nextUrl.size > 1) {
            val chapterDataList = arrayListOf<ChapterData<String>>()
            for (item in chapterData.nextUrl) {
                val data = ChapterData(nextUrl = item)
                chapterDataList.add(data)
            }
            for (item in chapterDataList) {
                withContext(coroutineScope.coroutineContext) {
                    val nextBody = AnalyzeUrl(
                        ruleUrl = item.nextUrl,
                        book = book,
                        headerMapF = bookSource.getHeaderMap()
                    ).getResponseAwait().body
                    val nextChapterData = analyzeChapterList(
                        nextBody, item.nextUrl, tocRule, listRule, book, bookSource
                    )
                    item.chapterList = nextChapterData.chapterList
                }
            }
            for (item in chapterDataList) {
                item.chapterList?.let {
                    chapterList.addAll(it)
                }
            }
        }
        //去重
        if (!reverse) {
            chapterList.reverse()
        }
        val lh = LinkedHashSet(chapterList)
        chapterList = ArrayList(lh)
        chapterList.reverse()
        for ((index, item) in chapterList.withIndex()) {
            item.index = index
        }
        book.latestChapterTitle = chapterList.last().title
        book.durChapterTitle =
            chapterList.getOrNull(book.durChapterIndex)?.title ?: book.latestChapterTitle
        if (book.totalChapterNum < chapterList.size) {
            book.lastCheckCount = chapterList.size - book.totalChapterNum
        }
        book.totalChapterNum = chapterList.size
        return chapterList
    }


    private fun analyzeChapterList(
        body: String?,
        baseUrl: String,
        tocRule: TocRule,
        listRule: String,
        book: Book,
        bookSource: BookSource,
        getNextUrl: Boolean = true,
        log: Boolean = false
    ): ChapterData<List<String>> {
        val chapterList = arrayListOf<BookChapter>()
        val nextUrlList = arrayListOf<String>()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body, baseUrl)
        val nextTocRule = tocRule.nextTocUrl
        if (getNextUrl && !nextTocRule.isNullOrEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "┌获取目录下一页列表", log)
            analyzeRule.getStringList(nextTocRule, true)?.let {
                for (item in it) {
                    if (item != baseUrl) {
                        nextUrlList.add(item)
                    }
                }
            }
            Debug.log(
                bookSource.bookSourceUrl,
                "└" + TextUtils.join("，\n", nextUrlList),
                log
            )
        }
        Debug.log(bookSource.bookSourceUrl, "┌获取目录列表", log)
        val elements = analyzeRule.getElements(listRule)
        Debug.log(bookSource.bookSourceUrl, "└列表大小:${elements.size}", log)
        if (elements.isNotEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "┌获取首章名称", log)
            val nameRule = analyzeRule.splitSourceRule(tocRule.chapterName)
            val urlRule = analyzeRule.splitSourceRule(tocRule.chapterUrl)
            val vipRule = analyzeRule.splitSourceRule(tocRule.isVip)
            val update = analyzeRule.splitSourceRule(tocRule.updateTime)
            var isVip: String?
            for (item in elements) {
                analyzeRule.setContent(item)
                val bookChapter = BookChapter(bookUrl = book.bookUrl)
                analyzeRule.chapter = bookChapter
                bookChapter.title = analyzeRule.getString(nameRule)
                bookChapter.url = analyzeRule.getString(urlRule, true)
                bookChapter.tag = analyzeRule.getString(update)
                isVip = analyzeRule.getString(vipRule)
                if (bookChapter.url.isEmpty()) bookChapter.url = baseUrl
                if (bookChapter.title.isNotEmpty()) {
                    if (isVip.isNotEmpty() && isVip != "null" && isVip != "false" && isVip != "0") {
                        bookChapter.title = "\uD83D\uDD12" + bookChapter.title
                    }
                    chapterList.add(bookChapter)
                }
            }
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].title}", log)
            Debug.log(bookSource.bookSourceUrl, "┌获取首章链接", log)
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].url}", log)
            Debug.log(bookSource.bookSourceUrl, "┌获取首章信息", log)
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].tag}", log)
        }
        return ChapterData(chapterList, nextUrlList)
    }

}