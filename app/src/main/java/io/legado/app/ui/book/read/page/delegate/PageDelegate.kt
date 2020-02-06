package io.legado.app.ui.book.read.page.delegate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Scroller
import androidx.annotation.CallSuper
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.google.android.material.snackbar.Snackbar
import io.legado.app.help.AppConfig
import io.legado.app.ui.book.read.page.ContentView
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.utils.screenshot
import io.legado.app.utils.snackbar
import kotlin.math.abs

abstract class PageDelegate(protected val pageView: PageView) {
    val centerRectF = RectF(
        pageView.width * 0.33f, pageView.height * 0.33f,
        pageView.width * 0.66f, pageView.height * 0.66f
    )
    //起始点
    protected var startX: Float = 0.toFloat()
    protected var startY: Float = 0.toFloat()
    //触碰点
    protected var touchX: Float = 0.toFloat()
    protected var touchY: Float = 0.toFloat()

    protected val nextPage: ContentView?
        get() = pageView.nextPage

    protected val curPage: ContentView?
        get() = pageView.curPage

    protected val prevPage: ContentView?
        get() = pageView.prevPage

    protected var bitmap: Bitmap? = null

    protected var viewWidth: Int = pageView.width
    protected var viewHeight: Int = pageView.height
    //textView在顶端或低端
    protected var atTop: Boolean = false
    protected var atBottom: Boolean = false

    private var snackbar: Snackbar? = null

    private val scroller: Scroller by lazy {
        Scroller(
            pageView.context,
            FastOutLinearInInterpolator()
        )
    }

    private val detector: GestureDetector by lazy {
        GestureDetector(
            pageView.context,
            GestureListener()
        )
    }

    var isMoved = false
    var noNext = true

    //移动方向
    var direction = Direction.NONE
    var isCancel = false
    var isRunning = false
    var isStarted = false

    open fun setStartPoint(x: Float, y: Float, invalidate: Boolean = true) {
        startX = x
        startY = y

        if (invalidate) {
            invalidate()
        }
    }

    open fun setTouchPoint(x: Float, y: Float, invalidate: Boolean = true) {
        touchX = x
        touchY = y

        if (invalidate) {
            invalidate()
        }

        onScroll()
    }

    protected fun invalidate() {
        pageView.invalidate()
    }

    protected fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        scroller.startScroll(
            startX,
            startY,
            dx,
            dy,
            if (dx != 0) (abs(dx) * 0.3).toInt() else (abs(dy) * 0.3).toInt()
        )
        isRunning = true
        isStarted = true
        invalidate()
    }

    protected fun stopScroll() {
        isRunning = false
        isStarted = false
        invalidate()
        if (pageView.isScrollDelegate) {
            pageView.postDelayed({
                bitmap?.recycle()
                bitmap = null
            }, 100)
        } else {
            bitmap?.recycle()
            bitmap = null
        }
    }

    fun setViewSize(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        invalidate()
        centerRectF.set(
            width * 0.33f, height * 0.33f,
            width * 0.66f, height * 0.66f
        )
    }

    fun scroll() {
        if (scroller.computeScrollOffset()) {
            setTouchPoint(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if (isStarted) {
            setTouchPoint(scroller.finalX.toFloat(), scroller.finalY.toFloat(), false)
            onScrollStop()
            stopScroll()
        }
    }

    fun abort() {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
    }

    fun start(direction: Direction) {
        if (isStarted) return
        if (direction === Direction.NEXT) {
            val x = viewWidth.toFloat()
            val y = viewHeight.toFloat()
            //初始化动画
            setStartPoint(x, y, false)
            //设置点击点
            setTouchPoint(x, y, false)
            //设置方向
            if (!hasNext()) {
                return
            }
        } else {
            val x = 0.toFloat()
            val y = viewHeight.toFloat()
            //初始化动画
            setStartPoint(x, y, false)
            //设置点击点
            setTouchPoint(x, y, false)
            //设置方向方向
            if (!hasPrev()) {
                return
            }
        }
        onScrollStart()
    }

    /**
     * 触摸事件处理
     */
    @CallSuper
    open fun onTouch(event: MotionEvent): Boolean {
        if (isStarted) return false
        if (curPage?.isTextSelected() == true) {
            curPage?.dispatchTouchEvent(event)
            return true
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            curPage?.let {
                it.contentTextView()?.let { contentTextView ->
                    atTop = contentTextView.atTop()
                    atBottom = contentTextView.atBottom()
                }
                it.dispatchTouchEvent(event)
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            curPage?.dispatchTouchEvent(event)
            if (isMoved) {
                // 开启翻页效果
                if (!noNext) onScrollStart()
                return true
            }
        }
        return detector.onTouchEvent(event)
    }

    abstract fun onScrollStart()//scroller start

    abstract fun onDraw(canvas: Canvas)//绘制

    abstract fun onScrollStop()//scroller finish

    open fun onScroll() {//移动contentView， slidePage
    }

    abstract fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean

    enum class Direction {
        NONE, PREV, NEXT
    }

    /**
     * 触摸事件处理
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
//            abort()
            //是否移动
            isMoved = false
            //是否存在下一章
            noNext = false
            //是否正在执行动画
            isRunning = false
            //取消
            isCancel = false
            //是下一章还是前一章
            direction = Direction.NONE
            //设置起始位置的触摸点
            setStartPoint(e.x, e.y)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val x = e.x
            val y = e.y
            if (centerRectF.contains(x, y)) {
                pageView.callBack?.clickCenter()
                setTouchPoint(x, y)
            } else {
                bitmap = if (x > viewWidth / 2 ||
                    AppConfig.clickAllNext
                ) {
                    //设置动画方向
                    if (!hasNext()) {
                        return true
                    }
                    //下一页截图
                    nextPage?.screenshot()
                } else {
                    if (!hasPrev()) {
                        return true
                    }
                    //上一页截图
                    prevPage?.screenshot()
                }
                setTouchPoint(x, y)
                onScrollStart()
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return this@PageDelegate.onScroll(e1, e2, distanceX, distanceY)
        }
    }

    fun hasPrev(): Boolean {
        //上一页的参数配置
        direction = Direction.PREV
        val hasPrev = pageView.pageFactory?.hasPrev() == true
        if (!hasPrev) {
            snackbar ?: let {
                snackbar = pageView.snackbar("没有上一页")
            }
            snackbar?.let {
                if (!it.isShown) {
                    it.setText("没有上一页")
                    it.show()
                }
            }
        }
        return hasPrev
    }

    fun hasNext(): Boolean {
        //进行下一页的配置
        direction = Direction.NEXT
        val hasNext = pageView.pageFactory?.hasNext() == true
        if (!hasNext) {
            snackbar ?: let {
                snackbar = pageView.snackbar("没有下一页")
            }
            snackbar?.let {
                if (!it.isShown) {
                    it.setText("没有下一页")
                    it.show()
                }
            }
        }
        return hasNext
    }
}