package catt.custom.view

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log.i
import android.util.TypedValue
import android.view.*
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import java.lang.IllegalArgumentException

class SideFadeLayout
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val _TAG: String = SideFadeLayout::class.java.simpleName

    private val _flingDrag: Int = 500
    private val widthPixels: Int
        get() = DisplayMetrics().run DisplayMetrics@{
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).run {
                defaultDisplay.getMetrics(this@DisplayMetrics)
            }
            widthPixels
        }

    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                return when {
                    !whetherShowMenu && velocityX > 0 && Math.abs(velocityX) > _flingDrag -> {
                        openMenu()
                        return true
                    }
                    whetherShowMenu && velocityX < 0 && Math.abs(velocityX) > _flingDrag -> {
                        closeMenu()
                        return true
                    }
                    else -> false
                }
            }
        })
    }


    private val _menuWidth: Int
        get() = widthPixels - contentMinMarginEnd
    private var menuMinAlpha: Float /*菜单最小渐隐值*/ = 0F

    private var contentMinMarginEnd: Int /*内容最小边距*/ = 0
    private var contentMinMarginEndPercentage: Float = 0F

    private var whetherShowMenu: Boolean = false
    private val _linearLayout: ViewGroup?
        get() = getChildAt(0)?.run { this as ViewGroup }

    private val _menuLayout: ViewGroup?
        get() = _linearLayout?.run { getChildAt(0) as ViewGroup }

    private val _contentLayout: ViewGroup?
        get() = _linearLayout?.run { getChildAt(1) as ViewGroup }

    private var whetherIntercept: Boolean = false

    init {
        context.obtainStyledAttributes(attrs, R.styleable.SideFadeLayout)
                .apply {
                    val menuLayoutId = this.getResourceId(R.styleable.SideFadeLayout_catt_MenuLayout, 0)
                    val contentLayoutId = this.getResourceId(R.styleable.SideFadeLayout_catt_ContentLayout, 0)
                    if (menuLayoutId == 0 || contentLayoutId == 0) {
                        throw IllegalArgumentException("Menu attribute and content attribute cannot be empty.")
                    }
                    i(_TAG, "Perform attributes loading.")
                    this@SideFadeLayout.addView(
                            (LayoutInflater.from(context).inflate(R.layout.linearlayout, this@SideFadeLayout, false) as ViewGroup)
                                    .run {
                                        removeAllViews()
                                        addView(LayoutInflater.from(context).inflate(menuLayoutId, this, false),
                                                LayoutParams(0, LayoutParams.MATCH_PARENT))
                                        addView(LayoutInflater.from(context).inflate(contentLayoutId, this, false),
                                                LayoutParams(0, LayoutParams.MATCH_PARENT))
                                        this
                                    }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

                    menuMinAlpha = getFloat(R.styleable.SideFadeLayout_catt_MenuContraction_AlphaPercentage, 0.5F)

                    contentMinMarginEndPercentage = getFloat(R.styleable.SideFadeLayout_catt_ContentContraction_MarginEndPercentage, 0.1F)
                    contentMinMarginEnd = (widthPixels * contentMinMarginEndPercentage).toInt()
                }.recycle()
        overScrollMode = OVER_SCROLL_NEVER
    }

    /**这个方法在布局(XML)解析完毕才会触发*/
    override fun onFinishInflate() {
        super.onFinishInflate()
        _menuLayout?.apply {
            layoutParams.width = _menuWidth
        }
        _contentLayout?.apply {
            layoutParams.width = widthPixels
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        //初始化进入是关闭模式
        whetherShowMenu = false
        scrollTo(_menuWidth, 0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        //处理点击事件
        //当菜单打开时候，手指触摸右边内容部分需要关闭菜单，还需要拦截事件（打开Menu的情况下点击内容页不会响应点击事件）
        return ev.run {
            whetherIntercept = when {
                whetherShowMenu && x > _menuWidth -> {
                    closeMenu()
                    true
                }
                else -> super.onInterceptTouchEvent(ev)
            }
            whetherIntercept
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        //手指抬起后判断打开还是关闭
        return ev.run {
            when {
                whetherIntercept -> true
                gestureDetector.onTouchEvent(ev) -> true
                ev.action == MotionEvent.ACTION_UP -> /*根据当前滚动的距离进行判断*/ {
                    if ((_menuWidth) / 2 < scrollX) {
                        closeMenu()
                    } else {
                        openMenu()
                    }
                    true
                }
                else -> super.onTouchEvent(ev)
            }
        }
    }

    //处理右边的缩放，处理左边的缩放以及透明，需要不断的获取当前滚动的位置

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        _menuLayout?.apply {
            val menuScale = 1 - l / _menuWidth.toFloat()
            val leftScale = 1F - contentMinMarginEndPercentage + contentMinMarginEndPercentage * menuScale
            alpha = when (menuScale > menuMinAlpha) {
                true -> menuScale
                false -> menuMinAlpha
            }
            pivotX = _menuWidth.toFloat()
            scaleY = leftScale
            scaleX = leftScale
        }
        _contentLayout?.apply {
            val contentScale = l / _menuWidth.toFloat()
            val rightScale = 1F - contentMinMarginEndPercentage + contentMinMarginEndPercentage * contentScale
            pivotX = 0F
            scaleY = rightScale
            scaleX = rightScale
        }
    }

    fun closeMenu() {
        whetherShowMenu = false
        smoothScrollTo(_menuWidth, 0)
    }

    fun openMenu() {
        whetherShowMenu = true
        smoothScrollTo(0, 0)
    }

    private fun convertPx(unit: Int, value: Int) = TypedValue.applyDimension(unit, value.toFloat(), resources.displayMetrics)
}