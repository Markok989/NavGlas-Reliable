package com.example.naviglas.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.naviglas.R

class OverlayVoiceWidget(private val context: Context, private val onWidgetClick: () -> Unit) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    /**
     * Prikazuje lebdeći mehur (Overlay Widget) preko Google Maps-a.
     */
    fun showOverlay(statusText: String = "NaviGlas", alpha: Float = 1.0f) {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(context)) return

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 150
        }

        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_widget_layout, null).apply {
            findViewById<TextView>(R.id.tvOverlayStatus)?.text = statusText
            this.alpha = alpha.coerceIn(0.3f, 1.0f)
            setOnClickListener {
                onWidgetClick()
            }
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateStatus(statusText: String) {
        overlayView?.findViewById<TextView>(R.id.tvOverlayStatus)?.text = statusText
    }

    fun updateAlpha(alpha: Float) {
        overlayView?.alpha = alpha.coerceIn(0.3f, 1.0f)
    }

    fun hideOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }
}
