package com.example.scamdetectorapp.ui.overlay

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.example.scamdetectorapp.R

class OverlayController(private val context : Context) {

    // 創建windowManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // 創建懸浮窗視圖
    private var overlayView : View? = null

    // 創建倒計時器
    private var countDownTimer : CountDownTimer? = null

    // 顯示懸浮窗
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun show(onContinueClicked : () -> Unit, onEndCallClicked : () -> Unit){
        if (overlayView != null) return

        // 權限檢查
        if(!checkOverlayPermission()){
            requestOverlayPermission()
            return
        }

        // 開啟懸浮窗 xml元件
        val contextWithTheme = ContextThemeWrapper(context, R.style.Theme_ScamDetectorApp)
        overlayView = View.inflate(contextWithTheme, R.layout.overlay_warning, null)

        val param = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(
            overlayView,
            param
        )

        // 加上按鈕監聽，點擊關閉視窗
        overlayView?.findViewById<Button>(R.id.endCall)?.setOnClickListener{
            onEndCallClicked()
            close()
        }

        // 30秒倒數冷卻按鈕
        val btnContinue = overlayView?.findViewById<Button>(R.id.countdown)
        btnContinue?.isEnabled = false
        btnContinue?.backgroundTintList = ColorStateList.valueOf(Color.argb(100, 128, 128, 128))
        btnContinue?.setTextColor(Color.GRAY)

        countDownTimer = object : CountDownTimer(30000, 1000){
            override fun onTick(millisUntilNotFinished: Long) {
                val second = millisUntilNotFinished / 1000
                btnContinue?.text = "請等待 ${second} 秒"
            }

            override fun onFinish() {
                btnContinue?.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                btnContinue?.setTextColor(context.getColor(R.color.overlay_text_secondary))
                btnContinue?.isEnabled = true
                btnContinue?.text = "仍要繼續"
                btnContinue?.setOnClickListener {
                    onContinueClicked()
                    close()
                }
            }
        }.start()
    }

    // 關閉懸浮窗
    fun close(){
        overlayView?.let{ view ->
            countDownTimer?.cancel()
            windowManager.removeView(view)
            overlayView = null
        }
    }

    private fun checkOverlayPermission() : Boolean{
        return Settings.canDrawOverlays(context)
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(context)
            .setCancelable(true)
            .setTitle("需要開啟懸浮窗權限")
            .setMessage("從設定中開啟「顯示於其他應用程式上方」")
            .setPositiveButton("開啟設定"){_, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}