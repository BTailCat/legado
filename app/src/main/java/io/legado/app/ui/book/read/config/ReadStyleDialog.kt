package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.get
import androidx.fragment.app.DialogFragment
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.ImageLoader
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.read.Help
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.widget.font.FontSelectDialog
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_book_read.*
import kotlinx.android.synthetic.main.dialog_read_book_style.*
import org.jetbrains.anko.sdk27.listeners.onCheckedChange
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class ReadStyleDialog : DialogFragment(), FontSelectDialog.CallBack {

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.let {
            Help.upSystemUiVisibility(it)
            it.windowManager?.defaultDisplay?.getMetrics(dm)
        }
        dialog?.window?.let {
            it.setBackgroundDrawableResource(R.color.background)
            it.decorView.setPadding(0, 5, 0, 0)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            it.attributes = attr
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_read_book_style, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initOnClick()
    }

    override fun onDestroy() {
        super.onDestroy()
        ReadBookConfig.save()
    }

    private fun initData() {
        requireContext().getPrefInt(PreferKey.pageAnim).let {
            if (it >= 0 && it < rg_page_anim.childCount) {
                rg_page_anim.check(rg_page_anim[it].id)
            }
        }
        upStyle()
        setBg()
        upBg()
    }

    private fun initOnClick() {
        tv_text_bold.onClick {
            with(ReadBookConfig.getConfig()) {
                textBold = !textBold
                tv_text_bold.isSelected = textBold
            }
            postEvent(EventBus.UP_CONFIG, false)
        }
        tv_text_font.onClick {
            FontSelectDialog().show(childFragmentManager, "fontSelectDialog")
        }
        tv_text_indent.onClick {
            selector(
                title = getString(R.string.text_indent),
                items = resources.getStringArray(R.array.indent).toList()
            ) { _, index ->
                putPrefInt("textIndent", index)
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        tv_padding.onClick {
            val activity = activity
            dismiss()
            if (activity is ReadBookActivity) {
                activity.showPaddingConfig()
            }
        }
        seek_text_size.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ReadBookConfig.getConfig().textSize = progress + 5
                tv_text_size.text = ReadBookConfig.getConfig().textSize.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                postEvent(EventBus.UP_CONFIG, true)
            }
        })
        iv_text_size_add.onClick {
            seek_text_size.progressAdd(1)
            postEvent(EventBus.UP_CONFIG, true)
        }
        iv_text_size_remove.onClick {
            seek_text_size.progressAdd(-1)
            postEvent(EventBus.UP_CONFIG, true)
        }
        seek_text_letter_spacing.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                with(ReadBookConfig.getConfig()) {
                    letterSpacing = (seek_text_letter_spacing.progress - 5) / 10f
                    tv_text_letter_spacing.text = letterSpacing.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                postEvent(EventBus.UP_CONFIG, true)
            }
        })
        iv_text_letter_spacing_add.onClick {
            seek_text_letter_spacing.progressAdd(1)
            postEvent(EventBus.UP_CONFIG, true)
        }
        iv_text_letter_spacing_remove.onClick {
            seek_text_letter_spacing.progressAdd(-1)
            postEvent(EventBus.UP_CONFIG, true)
        }
        seek_line_size.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                with(ReadBookConfig.getConfig()) {
                    lineSpacingExtra = seek_line_size.progress
                    tv_line_size.text = lineSpacingExtra.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                postEvent(EventBus.UP_CONFIG, true)
            }
        })
        iv_line_size_add.onClick {
            seek_line_size.progressAdd(1)
            postEvent(EventBus.UP_CONFIG, true)
        }
        iv_line_size_remove.onClick {
            seek_line_size.progressAdd(-1)
            postEvent(EventBus.UP_CONFIG, true)
        }
        rg_page_anim.onCheckedChange { _, checkedId ->
            for (i in 0 until rg_page_anim.childCount) {
                if (checkedId == rg_page_anim[i].id) {
                    requireContext().putPrefInt(PreferKey.pageAnim, i)
                    val activity = activity
                    if (activity is ReadBookActivity) {
                        activity.page_view.upPageAnim(i)
                    }
                    break
                }
            }
        }
        tv_bg0.onClick { changeBg(0) }
        tv_bg0.onLongClick { showBgTextConfig(0) }
        tv_bg1.onClick { changeBg(1) }
        tv_bg1.onLongClick { showBgTextConfig(1) }
        tv_bg2.onClick { changeBg(2) }
        tv_bg2.onLongClick { showBgTextConfig(2) }
        tv_bg3.onClick { changeBg(3) }
        tv_bg3.onLongClick { showBgTextConfig(3) }
        tv_bg4.onClick { changeBg(4) }
        tv_bg4.onLongClick { showBgTextConfig(4) }
    }

    private fun changeBg(index: Int) {
        if (ReadBookConfig.styleSelect != index) {
            ReadBookConfig.styleSelect = index
            ReadBookConfig.upBg()
            upStyle()
            upBg()
            postEvent(EventBus.UP_CONFIG, true)
        }
    }

    private fun showBgTextConfig(index: Int): Boolean {
        dismiss()
        changeBg(index)
        val activity = activity
        if (activity is ReadBookActivity) {
            activity.showBgTextConfig()
        }
        return true
    }

    private fun upStyle() {
        ReadBookConfig.getConfig().let {
            tv_text_bold.isSelected = it.textBold
            seek_text_size.progress = it.textSize - 5
            tv_text_size.text = it.textSize.toString()
            seek_text_letter_spacing.progress = (it.letterSpacing * 10).toInt() + 5
            tv_text_letter_spacing.text = it.letterSpacing.toString()
            seek_line_size.progress = it.lineSpacingExtra
            tv_line_size.text = it.lineSpacingExtra.toString()
        }
    }

    private fun setBg() {
        tv_bg0.setTextColor(ReadBookConfig.getConfig(0).textColor())
        tv_bg1.setTextColor(ReadBookConfig.getConfig(1).textColor())
        tv_bg2.setTextColor(ReadBookConfig.getConfig(2).textColor())
        tv_bg3.setTextColor(ReadBookConfig.getConfig(3).textColor())
        tv_bg4.setTextColor(ReadBookConfig.getConfig(4).textColor())
        for (i in 0..4) {
            val iv = when (i) {
                1 -> bg1
                2 -> bg2
                3 -> bg3
                4 -> bg4
                else -> bg0
            }
            ReadBookConfig.getConfig(i).apply {
                when (bgType()) {
                    2 -> ImageLoader.load(requireContext(), bgStr()).centerCrop().into(iv)
                    else -> iv.setImageDrawable(bgDrawable(100, 150))
                }
            }
        }
    }

    private fun upBg() {
        bg0.borderColor = requireContext().primaryColor
        bg1.borderColor = requireContext().primaryColor
        bg2.borderColor = requireContext().primaryColor
        bg3.borderColor = requireContext().primaryColor
        bg4.borderColor = requireContext().primaryColor
        when (ReadBookConfig.styleSelect) {
            1 -> bg1.borderColor = requireContext().accentColor
            2 -> bg2.borderColor = requireContext().accentColor
            3 -> bg3.borderColor = requireContext().accentColor
            4 -> bg4.borderColor = requireContext().accentColor
            else -> bg0.borderColor = requireContext().accentColor
        }
    }

    override val curFontPath: String
        get() = requireContext().getPrefString(PreferKey.readBookFont) ?: ""

    override fun selectFile(path: String) {
        requireContext().putPrefString(PreferKey.readBookFont, path)
        postEvent(EventBus.UP_CONFIG, true)
    }
}