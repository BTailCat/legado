package io.legado.app.ui.filechooser

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.filechooser.adapter.FileAdapter
import io.legado.app.ui.filechooser.adapter.PathAdapter
import io.legado.app.utils.FileUtils
import io.legado.app.utils.applyTint
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.dialog_file_chooser.*


class FileChooserDialog : DialogFragment(),
    Toolbar.OnMenuItemClickListener,
    FileAdapter.CallBack,
    PathAdapter.CallBack {

    companion object {
        const val tag = "FileChooserDialog"
        const val DIRECTORY = 0
        const val FILE = 1

        fun show(
            manager: FragmentManager,
            requestCode: Int,
            mode: Int = FILE,
            title: String? = null,
            initPath: String? = null,
            isShowHomeDir: Boolean = false,
            isShowUpDir: Boolean = true,
            isShowHideDir: Boolean = false
        ) {
            val fragment = (manager.findFragmentByTag(tag) as? FileChooserDialog)
                ?: FileChooserDialog().apply {
                    val bundle = Bundle()
                    bundle.putInt("mode", mode)
                    bundle.putInt("requestCode", requestCode)
                    bundle.putString("title", title)
                    bundle.putBoolean("isShowHomeDir", isShowHomeDir)
                    bundle.putBoolean("isShowUpDir", isShowUpDir)
                    bundle.putBoolean("isShowHideDir", isShowHideDir)
                    bundle.putString("initPath", initPath)
                    arguments = bundle
                }
            fragment.show(manager, tag)
        }
    }

    override var allowExtensions: Array<String?>? = null
    override val isOnlyListDir: Boolean
        get() = mode == DIRECTORY
    override var isShowHomeDir: Boolean = false
    override var isShowUpDir: Boolean = true
    override var isShowHideDir: Boolean = false

    private var requestCode: Int = 0
    var title: String? = null
    private var initPath = FileUtils.getSdCardPath()
    private var mode: Int = FILE
    private lateinit var fileAdapter: FileAdapter
    private lateinit var pathAdapter: PathAdapter

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.8).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_file_chooser, container, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ATH.applyBackgroundTint(view)
        ATH.applyBackgroundTint(rv_path)
        arguments?.let {
            requestCode = it.getInt("requestCode")
            mode = it.getInt("mode", FILE)
            title = it.getString("title")
            isShowHomeDir = it.getBoolean("isShowHomeDir")
            isShowUpDir = it.getBoolean("isShowUpDir")
            isShowHideDir = it.getBoolean("isShowHideDir")
            it.getString("initPath")?.let { path ->
                initPath = path
            }
        }
        tool_bar.title = title ?: let {
            if (isOnlyListDir) {
                getString(R.string.folder_chooser)
            } else {
                getString(R.string.file_chooser)
            }
        }
        initMenu()
        initContentView()
        refreshCurrentDirPath(initPath)
    }

    private fun initMenu() {
        tool_bar.inflateMenu(R.menu.file_chooser)
        if (isOnlyListDir) {
            tool_bar.menu.findItem(R.id.menu_ok).isVisible = true
        }
        tool_bar.menu.applyTint(requireContext(), false)
        tool_bar.setOnMenuItemClickListener(this)
    }

    private fun initContentView() {
        fileAdapter = FileAdapter(requireContext(), this)
        pathAdapter = PathAdapter(requireContext(), this)

        rv_file.addItemDecoration(DividerItemDecoration(activity, LinearLayout.VERTICAL))
        rv_file.layoutManager = LinearLayoutManager(activity)
        rv_file.adapter = fileAdapter

        rv_path.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        rv_path.adapter = pathAdapter

    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_ok -> fileAdapter.currentPath?.let {
                (parentFragment as? CallBack)?.onFilePicked(requestCode, it)
                (activity as? CallBack)?.onFilePicked(requestCode, it)
                dismiss()
            }
        }
        return true
    }

    override fun onFileClick(position: Int) {
        val fileItem = fileAdapter.getItem(position)
        if (fileItem?.isDirectory == true) {
            refreshCurrentDirPath(fileItem.path)
        } else {
            fileItem?.path?.let { path ->
                if (mode != DIRECTORY) {
                    (parentFragment as? CallBack)?.onFilePicked(requestCode, path)
                    (activity as? CallBack)?.onFilePicked(requestCode, path)
                    dismiss()
                }
            }
        }
    }

    override fun onPathClick(position: Int) {
        refreshCurrentDirPath(pathAdapter.getPath(position))
    }

    private fun refreshCurrentDirPath(currentPath: String) {
        if (currentPath == "/") {
            pathAdapter.updatePath("/")
        } else {
            pathAdapter.updatePath(currentPath)
        }
        fileAdapter.loadData(currentPath)
        var adapterCount = fileAdapter.itemCount
        if (isShowHomeDir) {
            adapterCount--
        }
        if (isShowUpDir) {
            adapterCount--
        }
        if (adapterCount < 1) {
            tv_empty.visible()
            tv_empty.setText(R.string.empty)
        } else {
            tv_empty.gone()
        }
    }

    interface CallBack {
        fun onFilePicked(requestCode: Int, currentPath: String) {}
    }
}