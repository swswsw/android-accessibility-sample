package com.example.androidaccessibilitysample

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

class MyAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        var instance: MyAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d("MyAccessibilityService", "Service connected")
        showOverlay()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        removeOverlay()
        Log.d("MyAccessibilityService", "Service unbound")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    private fun showOverlay() {
        if (overlayView != null) return

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
        params.y = 100

        val container = FrameLayout(this)
        val button = Button(this).apply {
            text = "Click Me"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            setOnClickListener {
                Log.d("MyAccessibilityService", "Overlay button clicked")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    takeScreenshotAndClick(500f, 500f)
                }
            }
        }
        container.addView(button)
        overlayView = container

        windowManager?.addView(overlayView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshotAndClick(x: Float, y: Float) {
        Log.d("MyAccessibilityService", "Attempting to take screenshot...")
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                Log.d("MyAccessibilityService", "Screenshot taken successfully")
                screenshot.hardwareBuffer.close()
                click(x, y)
            }

            override fun onFailure(errorCode: Int) {
                Log.e("MyAccessibilityService", "Failed to take screenshot: $errorCode")
            }
        })
    }

    private fun click(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d("MyAccessibilityService", "Click completed at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.d("MyAccessibilityService", "Click cancelled")
            }
        }, null)
    }
}
