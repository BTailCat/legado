package io.legado.app.help.storage

import android.content.Context
import io.legado.app.App
import io.legado.app.constant.PreferKey
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.webdav.WebDav
import io.legado.app.lib.webdav.http.HttpAuth
import io.legado.app.utils.FileUtils
import io.legado.app.utils.ZipUtils
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jetbrains.anko.selector
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

object WebDavHelp {
    private val zipFilePath = FileUtils.getCachePath() + "/backup" + ".zip"
    private val unzipFilesPath by lazy {
        FileUtils.getCachePath()
    }

    private fun getWebDavUrl(): String? {
        var url = App.INSTANCE.getPrefString(PreferKey.webDavUrl)
        if (url.isNullOrBlank()) return null
        if (!url.endsWith("/")) url += "/"
        return url
    }

    private fun initWebDav(): Boolean {
        val account = App.INSTANCE.getPrefString(PreferKey.webDavAccount)
        val password = App.INSTANCE.getPrefString(PreferKey.webDavPassword)
        if (!account.isNullOrBlank() && !password.isNullOrBlank()) {
            HttpAuth.auth = HttpAuth.Auth(account, password)
            return true
        }
        return false
    }

    private fun getWebDavFileNames(): ArrayList<String> {
        val url = getWebDavUrl()
        val names = arrayListOf<String>()
        if (!url.isNullOrBlank() && initWebDav()) {
            var files = WebDav(url + "legado/").listFiles()
            files = files.reversed()
            for (index: Int in 0 until min(10, files.size)) {
                files[index].displayName?.let {
                    names.add(it)
                }
            }
        }
        return names
    }

    suspend fun showRestoreDialog(context: Context, restoreSuccess: () -> Unit): Boolean {
        val names = withContext(IO) { getWebDavFileNames() }
        return if (names.isNotEmpty()) {
            context.selector(title = "选择恢复文件", items = names) { _, index ->
                if (index in 0 until names.size) {
                    restoreWebDav(names[index], restoreSuccess)
                }
            }
            true
        } else {
            false
        }
    }

    private fun restoreWebDav(name: String, success: () -> Unit) {
        Coroutine.async {
            getWebDavUrl()?.let {
                val file = WebDav(it + "legado/" + name)
                file.downloadTo(zipFilePath, true)
                @Suppress("BlockingMethodInNonBlockingContext")
                ZipUtils.unzipFile(zipFilePath, unzipFilesPath)
                Restore.restore(unzipFilesPath)
            }
        }.onSuccess {
            success.invoke()
        }
    }

    fun backUpWebDav(path: String) {
        if (initWebDav()) {
            val paths = arrayListOf(*Backup.backupFileNames)
            for (i in 0 until paths.size) {
                paths[i] = path + File.separator + paths[i]
            }
            FileUtils.deleteFile(zipFilePath)
            if (ZipUtils.zipFiles(paths, zipFilePath)) {
                WebDav(getWebDavUrl() + "legado").makeAsDir()
                val putUrl = getWebDavUrl() + "legado/backup" +
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date(System.currentTimeMillis())) + ".zip"
                WebDav(putUrl).upload(zipFilePath)
            }
        }
    }
}