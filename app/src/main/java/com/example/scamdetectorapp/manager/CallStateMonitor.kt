package com.example.scamdetectorapp.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@RequiresApi(Build.VERSION_CODES.S)
class CallStateMonitor (private val context : Context){
    companion object {
        private const val TAG = "CallState"
    }

    private val _callState = MutableStateFlow(TelephonyManager.CALL_STATE_IDLE)
    val callState : StateFlow<Int> = _callState

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener{
        override fun onCallStateChanged(state: Int) {
            when(state){
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d(TAG, "Call Ended")
                    _callState.value = TelephonyManager.CALL_STATE_IDLE
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d(TAG, "In Call")
                    _callState.value = TelephonyManager.CALL_STATE_OFFHOOK
                }

                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.d(TAG, "Ringing")
                    _callState.value = TelephonyManager.CALL_STATE_RINGING
                }
            }
        }
    }

    fun startListening(){
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
        } else {
            Log.e(TAG, "READ_PHONE_STATE permission not granted")
        }
    }

    fun stopListening(){
        telephonyManager.unregisterTelephonyCallback(callback)
    }
}