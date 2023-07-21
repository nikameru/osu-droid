package com.reco1l.legacy.ui.multiplayer

import android.animation.Animator
import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.framework.lang.async
import com.reco1l.framework.lang.uiThread
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osuplus.R
import kotlin.math.abs
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

class RoomChat : BaseFragment(), OnEditorActionListener, OnKeyListener
{
    override val layoutID = R.layout.multiplayer_room_chat

    var field: EditText? = null

    var text: TextView? = null

    val log = SpannableStringBuilder()

    private val isExtended: Boolean
        get() = findViewById<View?>(R.id.fullLayout) != null && abs(findViewById<View>(R.id.fullLayout)!!.translationY) < 10


    init
    {
        isDismissOnBackPress = false
    }


    override fun onLoadView()
    {
        reload()

        field = findViewById(R.id.chat_field)!!
        field!!.setOnEditorActionListener(this)
        field!!.setOnKeyListener(this)

        text = findViewById(R.id.chat_text)!!
        text!!.movementMethod = ScrollingMovementMethod()

        // Restoring the chat log in case there is.
        text!!.text = log

        findViewById<View>(R.id.frg_header)!!.animate().cancel()
        findViewById<View>(R.id.frg_header)!!.alpha = 0f
        findViewById<View>(R.id.frg_header)!!.translationY = 100f
        findViewById<View>(R.id.frg_header)!!.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .start()
    }

    fun onRoomChatMessage(username: String, message: String) = uiThread {

        val color = if (username == RoomScene.player!!.name) "#FF4081" else "#FF679B"

        val html = "<font color=$color>$username:</font> <font color=#000000>$message</font>"
        val spanned = HtmlCompat.fromHtml(html, FROM_HTML_MODE_LEGACY)

        if (log.isNotEmpty())
        {
            log.appendLine()
        }
        log.append(spanned)

        text?.text = log
        showPreview(" $message", tag = "$username:", tagColor = color)
    }

    fun onSystemChatMessage(message: String, color: String) = uiThread {

        val htmlError = "<font color=$color>${message}</font>"
        val spanned = HtmlCompat.fromHtml(htmlError, FROM_HTML_MODE_LEGACY)

        if (log.isNotEmpty())
        {
            log.appendLine()
        }
        log.append(spanned)

        text?.text = log
        showPreview(message, contentColor = color)
    }

    private fun showPreview(content: String, contentColor: String? = null, tag: String? = null, tagColor: String? = null)
    {
        RGBColor.hex2Rgb(tagColor ?: "#FFFFFF").apply(RoomScene.chatPreview.tag)
        RGBColor.hex2Rgb(contentColor ?: "#FFFFFF").apply(RoomScene.chatPreview.content)

        RoomScene.chatPreview.setTagText(tag ?: "")
        RoomScene.chatPreview.setContentText(content)
    }

    private fun sendMessage()
    {
        val message = field?.text.takeUnless { it.isNullOrEmpty() } ?: return
        field?.text = null
        field?.clearFocus()

        async {
            try
            {
                RoomAPI.sendMessage(message.toString())
            }
            catch (e: Exception)
            {
                onSystemChatMessage("Error to send message: ${e.message}", "#FF0000")
                e.printStackTrace()
            }
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean
    {
        if (actionId == EditorInfo.IME_ACTION_SEARCH)
        {
            sendMessage()
            return true
        }
        return false
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean
    {
        if (keyCode == KeyEvent.KEYCODE_ENTER && v is TextView)
        {
            onEditorAction(v, EditorInfo.IME_ACTION_SEARCH, event)
            return true
        }
        return false
    }

    private fun reload()
    {
        val showMoreButton = findViewById<View>(R.id.showMoreButton) ?: return
        showMoreButton.setOnTouchListener { v: View, event: MotionEvent ->
            if (event.action == TouchEvent.ACTION_DOWN)
            {
                v.animate().cancel()
                v.animate().scaleY(0.9f).scaleX(0.9f).translationY(v.height * 0.1f).setDuration(100).start()
                toggleVisibility()
                return@setOnTouchListener true
            }
            else if (event.action == TouchEvent.ACTION_UP)
            {
                v.animate().cancel()
                v.animate().scaleY(1f).scaleX(1f).setDuration(100).translationY(0f).start()
                return@setOnTouchListener true
            }
            false
        }
        findViewById<View>(R.id.frg_background)!!.isClickable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun toggleVisibility()
    {
        field?.clearFocus()

        if (isExtended)
        {
            playHidePanelAnim()
            findViewById<View>(R.id.frg_background)!!.setOnTouchListener(null)
            findViewById<View>(R.id.frg_background)!!.isClickable = false
        }
        else
        {
            playShowPanelAnim()
            findViewById<View>(R.id.frg_background)!!.setOnTouchListener { _, event ->
                if (event.action == TouchEvent.ACTION_DOWN)
                {
                    if (isExtended)
                    {
                        toggleVisibility()
                        return@setOnTouchListener true
                    }
                }
                false
            }
            findViewById<View>(R.id.frg_background)!!.isClickable = true
        }
    }

    private fun playShowPanelAnim()
    {
        val fullLayout = findViewById<View>(R.id.fullLayout)
        if (fullLayout != null)
        {
            fullLayout.animate().cancel()
            fullLayout.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .setListener(object : BaseAnimationListener()
                                 {
                                     override fun onAnimationEnd(animation: Animator)
                                     {
                                         super.onAnimationEnd(animation)
                                         findViewById<View>(R.id.frg_background)!!.isClickable = true
                                         findViewById<View>(R.id.frg_background)!!.setOnClickListener { playHidePanelAnim() }
                                     }
                                 })
                    .start()
        }
    }

    private fun playHidePanelAnim()
    {
        val fullLayout = findViewById<View>(R.id.fullLayout)
        if (fullLayout != null)
        {
            fullLayout.animate().cancel()
            fullLayout.animate()
                    .translationY(findViewById<View>(R.id.optionBody)!!.height.toFloat())
                    .setDuration(200)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .setListener(object : BaseAnimationListener()
                                 {
                                     override fun onAnimationEnd(animation: Animator)
                                     {
                                         super.onAnimationEnd(animation)
                                         findViewById<View>(R.id.frg_background)!!.isClickable = false
                                     }
                                 })
                    .start()
        }
    }

    override fun callDismissOnBackPress()
    {
        if (isExtended)
        {
            toggleVisibility()
            return
        }

        if (getGlobal().engine.scene == getGlobal().gameScene.scene)
        {
            getGlobal().gameScene.pause()
            return
        }
        uiThread { RoomScene.leaveDialog.show() }
    }
}
