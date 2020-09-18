package com.tiga.multitextview.widget

import android.content.Context
import android.graphics.Color
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.tiga.multitextview.R
import com.tiga.multitextview.entity.AtUserModel
import com.tiga.multitextview.entity.Topic
import java.util.*
import java.util.regex.Pattern

class MultiTextView : AppCompatTextView {

    /**
     * true：展开，false：收起
     */
    var mExpanded = false

    /**
     * 状态回调
     */
    private var mCallback: Callback? = null

    /**
     * 源文字内容
     */
    private var mText: String = ""

    /**
     * 最多展示的行数
     */
    private val maxLineCount = 3

    /**
     * 省略文字
     */
    private val ellipsizeText = "..."

    var topicColor = ContextCompat.getColor(context, R.color.cFF1a88ee)
    var atColor = ContextCompat.getColor(context, R.color.cFF1a88ee)

    var mTopicList: List<Topic> = mutableListOf()
    var mAtList: List<AtUserModel> = mutableListOf()
    var onTextClickListener: OnTextClickListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 文字计算辅助工具
        val sl = StaticLayout(
                mText, paint, measuredWidth - paddingLeft - paddingRight
                , Layout.Alignment.ALIGN_CENTER, 1F, 0F, false
        )
        // 总计行数
        var lineCount = sl.lineCount
        if (lineCount > maxLineCount) {
            if (mExpanded) {
                text = handleTopicAndAt(mText)
                mCallback?.onExpand()
            } else {
                lineCount = maxLineCount
                // 省略文字的宽度
                val dotWidth = paint.measureText(ellipsizeText)
                // 找出第 showLineCount 行的文字
                val start = sl.getLineStart(lineCount - 1)
                val end = sl.getLineEnd(lineCount - 1)
                val lineText = if (mText.isNotEmpty()) {
                    mText.substring(start, end)
                } else {
                    ""
                }
                // 将第 showLineCount 行最后的文字替换为 ellipsizeText
                var endIndex = 0
                for (i in lineText.length - 1 downTo 0) {
                    val str = if (lineText.isNotEmpty()) {
                        lineText.substring(i, lineText.length)
                    } else {
                        ""
                    }
                    // 找出文字宽度大于 ellipsizeText 的字符
                    if (paint.measureText(str) >= dotWidth) {
                        endIndex = i
                        break
                    }
                }
                // 新的第 showLineCount 的文字
                if (lineText.isNotEmpty()){
                val lastCodePoint = lineText.codePointAt(endIndex)
                if (isEmojiCharacter(lastCodePoint)) endIndex--
                }
                val newEndLineText = if (lineText.isNotEmpty()) {
                    lineText.substring(0, endIndex) + ellipsizeText
                } else {
                    ""
                }
                // 最终显示的文字
                val totalStr = if (mText.isNotEmpty()){
                    mText.substring(0, start) + newEndLineText
                }else{
                    ""
                }
                text = handleTopicAndAt(totalStr)
                mCallback?.onCollapse()
            }
        } else {
            text = handleTopicAndAt(mText)
            mCallback?.onLoss()
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /**
     * 设置要显示的文字以及状态
     * @param text
     * @param expanded true：展开，false：收起
     * @param callback
     */
    fun setText(textStr: String, topicList: List<Topic>, atList: List<AtUserModel>, expanded: Boolean = false, callback: Callback) {
        mText = textStr
        mExpanded = expanded
        mCallback = callback
        mAtList = atList
        mTopicList = topicList
        // 设置要显示的文字，这一行必须要，否则 onMeasure 宽度测量不正确
        text = textStr
        highlightColor = Color.TRANSPARENT
        movementMethod = LinkMovementMethod.getInstance()
        requestLayout()
    }

    private fun handleTopicAndAt(handleText: String): SpannableStringBuilder {
        val spannableStringBuilder = SpannableStringBuilder(handleText)
        if (mTopicList.isNullOrEmpty() && mAtList.isNullOrEmpty()) return spannableStringBuilder
        rendererTopicSpan(handleText, spannableStringBuilder, mTopicList)
        rendererAtSpan(handleText, spannableStringBuilder, mAtList)
        return spannableStringBuilder
    }

    private fun rendererAtSpan(contentText: String, spannableStringBuilder: SpannableStringBuilder, atList: List<AtUserModel>) {
        if (atList.isEmpty()) {
            return
        }
        //查找@
        atList.forEach {
            var startIndex = 0
            val keyss = escapeExprSpecialWord(it.userName)
//            val pKey = Pattern.compile("@$keyss", Pattern.CASE_INSENSITIVE)
            val pKey = Pattern.compile(keyss, Pattern.CASE_INSENSITIVE)
            val mKey = pKey.matcher(contentText)
            while (startIndex < contentText.length) {
                if (mKey.find(startIndex)) {
                    val start = mKey.start()
                    val end = mKey.end()
                    startIndex = end
                    val clickableSpan: ClickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            onTextClickListener?.onAtClick(it.userName, it.userId)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = atColor
                            ds.isUnderlineText = false
                        }
                    }
                    spannableStringBuilder.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableStringBuilder.setSpan(ForegroundColorSpan(atColor), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                } else {
                    startIndex = spannableStringBuilder.length
                }
            }
        }
    }

    private fun rendererTopicSpan(contentText: String, spannableStringBuilder: SpannableStringBuilder, topicList: List<Topic>) {
        if (topicList.isEmpty()) {
            return
        }
        //查找##
        //+ 匹配前面的子表达式一次或多次(大于等于1次）。例如，“zo+”能匹配“zo”以及“zoo”，但不能匹配“z”。+等价于{1,}。
        //[^xyz] 负值字符集合。匹配未包含的任意字符。例如，“[^abc]”可以匹配“plain”中的“plin”任一字符。
//        val pattern = Pattern.compile("#[^#]+#") // #[^#]+#  #[^\s]+?#
//        val matcherSub = pattern.matcher(contentText)
        val topics = HashMap<Long, String>()
        for (topic in topicList) {
            topics[topic.id] = "#" + topic.title + "#"
        }
        for ((key, value) in topics) {
            var startIndex = 0
            val pKey = Pattern.compile(escapeExprSpecialWord(value), Pattern.CASE_INSENSITIVE)
            val mKey = pKey.matcher(contentText)
            while (startIndex < contentText.length) {
                if (mKey.find(startIndex)) {
                    val start = mKey.start()
                    val end = mKey.end()
                    startIndex = end
                    val clickableSpan: ClickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            topicList.find { "#" + it.title + "#" == value }?.run {
                                onTextClickListener?.onTopicClick(this)
                            }
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = topicColor
                            ds.isUnderlineText = false
                        }
                    }
                    spannableStringBuilder.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableStringBuilder.setSpan(ForegroundColorSpan(topicColor), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                } else {
                    startIndex = spannableStringBuilder.length
                }
            }
        }
    }

    private fun isEmojiCharacter(codePoint: Int): Boolean {
        return (codePoint in 0x2600..0x27BF // 杂项符号与符号字体
                || codePoint == 0x303D || codePoint == 0x2049 || codePoint == 0x203C || codePoint in 0x2000..0x200F //
                || codePoint in 0x2028..0x202F //
                || codePoint == 0x205F //
                || codePoint in 0x2065..0x206F //
                /* 标点符号占用区域 */
                || codePoint in 0x2100..0x214F // 字母符号
                || codePoint in 0x2300..0x23FF // 各种技术符号
                || codePoint in 0x2B00..0x2BFF // 箭头A
                || codePoint in 0x2900..0x297F // 箭头B
                || codePoint in 0x3200..0x32FF // 中文符号
                || codePoint in 0xD800..0xDFFF // 高低位替代符保留区域
                || codePoint in 0xE000..0xF8FF // 私有保留区域
                || codePoint in 0xFE00..0xFE0F // 变异选择器
                || codePoint >= 0x10000) // Plane在第二平面以上的，char都不可以存，全部都转
    }

    /**
     * 转义正则特殊字符 （$()*+.[]?\^{},|）
     *
     * @param keyword
     * @return
     */
    private fun escapeExprSpecialWord(keyword: String): String {
        var word = keyword
        if (!TextUtils.isEmpty(keyword)) {
            val fbsArr = arrayOf("\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|")
            for (key in fbsArr) {
                if (keyword.contains(key)) {
                    word = word.replace(key, "\\" + key)
                }
            }
        }
        return word
    }

    override fun scrollTo(x: Int, y: Int) {

    }

    /**
     * 展开收起状态变化
     * @param expanded
     */
    fun setChanged(expanded: Boolean) {
        mExpanded = expanded
        requestLayout()
    }

    interface Callback {
        /**
         * 展开状态
         */
        fun onExpand()

        /**
         * 收起状态
         */
        fun onCollapse()

        /**
         * 行数小于最小行数，不满足展开或者收起条件
         */
        fun onLoss()
    }

    interface OnTextClickListener {
        fun onTopicClick(topic: Topic)
        fun onAtClick(name: String, id: Long)
    }
}