package io.legado.app.ui.explore

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_bookshelf_list.view.iv_cover
import kotlinx.android.synthetic.main.item_bookshelf_list.view.tv_name
import kotlinx.android.synthetic.main.item_search.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ExploreShowAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_search) {

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) =
        with(holder.itemView) {
            tv_name.text = item.name
            tv_author.text = context.getString(R.string.author_show, item.author)
            if (item.latestChapterTitle.isNullOrEmpty()) {
                tv_lasted.gone()
            } else {
                tv_lasted.text = context.getString(R.string.lasted_show, item.latestChapterTitle)
                tv_lasted.visible()
            }
            tv_introduce.text = context.getString(R.string.intro_show, item.intro)
            val kinds = item.getKindList()
            if (kinds.isEmpty()) {
                ll_kind.gone()
            } else {
                ll_kind.visible()
                for (index in 0..2) {
                    if (kinds.size > index) {
                        when (index) {
                            0 -> {
                                tv_kind.text = kinds[index]
                                tv_kind.visible()
                            }
                            1 -> {
                                tv_kind_1.text = kinds[index]
                                tv_kind_1.visible()
                            }
                            2 -> {
                                tv_kind_2.text = kinds[index]
                                tv_kind_2.visible()
                            }
                        }
                    } else {
                        when (index) {
                            0 -> tv_kind.gone()
                            1 -> tv_kind_1.gone()
                            2 -> tv_kind_2.gone()
                        }
                    }
                }
            }
            iv_cover.load(item.coverUrl, item.name, item.author)
            onClick {
                callBack.showBookInfo(item.toBook())
            }
        }

    interface CallBack {
        fun showBookInfo(book: Book)
    }
}