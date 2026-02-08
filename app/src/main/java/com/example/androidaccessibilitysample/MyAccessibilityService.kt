package com.example.androidaccessibilitysample

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import kotlin.math.hypot

class MyAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var controlOverlay: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        var instance: MyAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d("MyAccessibilityService", "Service connected")
        showControlOverlay()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        removeControlOverlay()
        Log.d("MyAccessibilityService", "Service unbound")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}

    private fun showControlOverlay() {
        if (controlOverlay != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        val container = FrameLayout(this)
        val button = Button(this).apply {
            text = "Test Tap"
            setBackgroundColor(Color.parseColor("#80FF0000"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                click(500f, 1000f)
            }
        }
        
        val swipeButton = Button(this).apply {
            text = "Test Swipe"
            setBackgroundColor(Color.parseColor("#800000FF"))
            setTextColor(Color.WHITE)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.topMargin = 150
            this.layoutParams = layoutParams
            setOnClickListener {
                swipe(300f, 1500f, 800f, 500f, 500)
            }
        }

        container.addView(button)
        container.addView(swipeButton)
        controlOverlay = container

        windowManager?.addView(controlOverlay, params)
    }

    private fun removeControlOverlay() {
        controlOverlay?.let {
            windowManager?.removeView(it)
            controlOverlay = null
        }
    }

    private fun showVisualFeedback(x: Float, y: Float, duration: Long = 300, size: Int = 80) {
        val params = WindowManager.LayoutParams(
            size,
            size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = (x - size / 2).toInt()
        params.y = (y - size / 2).toInt()

        val view = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#80FFEB3B")) // Semi-transparent yellow
                setStroke(4, Color.WHITE)
            }
        }

        try {
            windowManager?.addView(view, params)
            mainHandler.postDelayed({
                try {
                    windowManager?.removeView(view)
                } catch (e: Exception) {
                    // View might have been removed already
                }
            }, duration)
        } catch (e: Exception) {
            Log.e("MyAccessibilityService", "Error adding feedback view", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshotAndClick(x: Float, y: Float) {
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                screenshot.hardwareBuffer.close()
                click(x, y)
            }
            override fun onFailure(errorCode: Int) {}
        })
    }

    fun click(x: Float, y: Float) {
        showVisualFeedback(x, y)
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long) {
        // Calculate number of steps based on distance to make it look like a continuous line
        val distance = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
        val stepSize = 20f // pixels between dots
        val steps = (distance / stepSize).toInt().coerceIn(10, 100)
        
        for (i in 0..steps) {
            val progress = i.toFloat() / steps
            val px = x1 + (x2 - x1) * progress
            val py = y1 + (y2 - y1) * progress
            mainHandler.postDelayed({
                // Smaller dots for the path to make it look smoother
                showVisualFeedback(px, py, duration = 400, size = 40)
            }, (duration * progress).toLong())
        }

        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        dispatchGesture(gestureBuilder.build(), null, null)
    }
}
