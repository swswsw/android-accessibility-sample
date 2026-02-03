package com.example.androidaccessibilitysample

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

class MyAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MyAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("MyAccessibilityService", "Service connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        Log.d("MyAccessibilityService", "Service unbound")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshotAndClick(x: Float, y: Float) {
        Log.d("MyAccessibilityService", "Attempting to take screenshot...")
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                Log.d("MyAccessibilityService", "Screenshot taken successfully")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    screenshot.hardwareBuffer.close()
                }
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
