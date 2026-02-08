package com.example.androidaccessibilitysample

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
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

    @SuppressLint("ClickableViewAccessibility")
    fun showControlOverlay() {
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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 4)
            val drawable = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(Color.parseColor("#CC000000")) // Semi-transparent black background
            }
            background = drawable
        }

        val buttonWidth = 140 // Fixed width for a thinner look

        // Tap Button
        val tapButton = Button(this).apply {
            text = "TAP"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#80FF0000"))
            textSize = 10f
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                click(500f, 1000f)
            }
        }

        // Swipe Button
        val swipeButton = Button(this).apply {
            text = "SWIPE"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#800000FF"))
            textSize = 10f
            setPadding(0, 0, 0, 0)
            val lp = LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 4
            layoutParams = lp
            setOnClickListener {
                swipe(300f, 1500f, 800f, 500f, 500)
            }
        }

        // Screenshot Button
        val screenshotButton = Button(this).apply {
            text = "SHOT"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#8000FF00"))
            textSize = 10f
            setPadding(0, 0, 0, 0)
            val lp = LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 4
            layoutParams = lp
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            Log.d("MyAccessibilityService", "Screenshot captured successfully")
                            Toast.makeText(this@MyAccessibilityService, "Screenshot captured", Toast.LENGTH_SHORT).show()
                            screenshot.hardwareBuffer.close()
                        }
                        override fun onFailure(errorCode: Int) {
                            Log.e("MyAccessibilityService", "Screenshot failed: $errorCode")
                            Toast.makeText(this@MyAccessibilityService, "Screenshot failed", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(this@MyAccessibilityService, "Requires Android 11+", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Close Button
        val closeButton = Button(this).apply {
            text = "✕"
            setTextColor(Color.GRAY)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 18f
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                removeControlOverlay()
            }
        }

        // Move Button
        val moveButton = Button(this).apply {
            text = "✥"
            setTextColor(Color.GRAY)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 18f
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(layout, params)
                        true
                    }
                    else -> false
                }
            }
        }

        layout.addView(tapButton)
        layout.addView(swipeButton)
        layout.addView(screenshotButton)
        layout.addView(closeButton)
        layout.addView(moveButton)
        
        controlOverlay = layout
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
                setColor(Color.parseColor("#80FFEB3B"))
                setStroke(4, Color.WHITE)
            }
        }

        try {
            windowManager?.addView(view, params)
            mainHandler.postDelayed({
                try {
                    windowManager?.removeView(view)
                } catch (e: Exception) {}
            }, duration)
        } catch (e: Exception) {}
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
        val distance = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
        val stepSize = 20f
        val steps = (distance / stepSize).toInt().coerceIn(10, 100)
        
        for (i in 0..steps) {
            val progress = i.toFloat() / steps
            val px = x1 + (x2 - x1) * progress
            val py = y1 + (y2 - y1) * progress
            mainHandler.postDelayed({
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
