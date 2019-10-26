package io.legado.app.ui.book.source.manage

import android.app.Application
import android.text.TextUtils
import com.jayway.jsonpath.JsonPath
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.IHttpGetApi
import io.legado.app.data.entities.BookSource
import io.legado.app.help.http.HttpHelper
import io.legado.app.help.storage.OldRule
import io.legado.app.help.storage.Restore.jsonPath
import io.legado.app.utils.*
import java.io.File

class BookSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSource) {
        execute {
            bookSource.customOrder = App.db.bookSourceDao().minOrder - 1
            App.db.bookSourceDao().insert(bookSource)
        }
    }

    fun del(bookSource: BookSource) {
        execute { App.db.bookSourceDao().delete(bookSource) }
    }

    fun update(vararg bookSource: BookSource) {
        execute { App.db.bookSourceDao().update(*bookSource) }
    }

    fun upOrder() {
        execute {
            val sources = App.db.bookSourceDao().all
            for ((index: Int, source: BookSource) in sources.withIndex()) {
                source.customOrder = index + 1
            }
            App.db.bookSourceDao().update(*sources.toTypedArray())
        }
    }

    fun enableSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.bookSourceDao().enableSection(*ids.toTypedArray())
        }
    }

    fun disableSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.bookSourceDao().disableSection(*ids.toTypedArray())
        }
    }

    fun enableSelectExplore(ids: LinkedHashSet<String>) {
        execute {
            App.db.bookSourceDao().enableSectionExplore(*ids.toTypedArray())
        }
    }

    fun disableSelectExplore(ids: LinkedHashSet<String>) {
        execute {
            App.db.bookSourceDao().disableSectionExplore(*ids.toTypedArray())
        }
    }

    fun delSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.bookSourceDao().delSection(*ids.toTypedArray())
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = App.db.bookSourceDao().noGroup
            sources.map { source ->
                source.bookSourceGroup = group
            }
            App.db.bookSourceDao().update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = App.db.bookSourceDao().getByGroup(oldGroup)
            sources.map { source ->
                source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.bookSourceGroup = TextUtils.join(",", it)
                }
            }
            App.db.bookSourceDao().update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = App.db.bookSourceDao().getByGroup(group)
                sources.map { source ->
                    source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.bookSourceGroup = TextUtils.join(",", it)
                    }
                }
                App.db.bookSourceDao().update(*sources.toTypedArray())
            }
        }
    }

    fun importSourceFromFilePath(path: String) {
        execute {
            val file = File(path)
            if (file.exists()) {
                importSource(file.readText())
            }
        }
    }

    fun importSource(text: String) {
        execute {
            val text1 = text.trim()
            if (text1.isJsonObject()) {
                val json = JsonPath.parse(text1)
                val urls = json.read<List<String>>("$.sourceUrls")
                if (!urls.isNullOrEmpty()) {
                    urls.forEach { importSourceUrl(it) }
                } else {
                    OldRule.jsonToBookSource(text1)?.let {
                        App.db.bookSourceDao().insert(it)
                    }
                    toast("成功导入1条")
                }
            } else if (text1.isJsonArray()) {
                val bookSources = mutableListOf<BookSource>()
                val items: List<Map<String, Any>> = jsonPath.parse(text1).read("$")
                for (item in items) {
                    val jsonItem = jsonPath.parse(item)
                    OldRule.jsonToBookSource(jsonItem.jsonString())?.let {
                        bookSources.add(it)
                    }
                }
                App.db.bookSourceDao().insert(*bookSources.toTypedArray())
                toast("成功导入${bookSources.size}条")
            } else if (text1.isAbsUrl()) {
                importSourceUrl(text1)
            } else {
                toast("格式不对")
            }
        }.onError {
            toast(it.localizedMessage)
        }
    }

    private fun importSourceUrl(url: String) {
        execute {
            NetworkUtils.getBaseUrl(url)?.let {
                val response = HttpHelper.getApiService<IHttpGetApi>(it).get(url, mapOf()).execute()
                response.body()?.let { body ->
                    importSource(body)
                }
            }
        }.onError {
            toast(it.localizedMessage)
        }
    }
}