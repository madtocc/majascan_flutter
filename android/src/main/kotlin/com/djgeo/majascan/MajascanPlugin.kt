package com.djgeo.majascan

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import com.djgeo.majascan.g_scanner.QrCodeScannerActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry


class MajascanPlugin :FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener {


    companion object {
        private const val channelName = "majascan"
        const val SCANRESULT = "scan"
        const val Request_Scan = 1
    }

    private var mResult: Result? = null
    private var mResultPeriod = 0L
    private var activity: Activity? = null
    private var channel: MethodChannel? = null

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            SCANRESULT -> {
                val args: Map<String, String>? = call.arguments()
                activity.let {
                    val intent = Intent(it, QrCodeScannerActivity::class.java)
                    args?.keys?.map { key -> intent.putExtra(key, args[key]) }
                    it?.startActivityForResult(intent, Request_Scan)
                    mResult = result
                }
            }
            else -> result.notImplemented()
        }
    }
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, channelName)
        channel?.setMethodCallHandler(this)

    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
    }

    //issue tracking https://github.com/flutter/flutter/issues/29092
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val currentTime = System.currentTimeMillis()
        if (requestCode == Request_Scan && resultCode == RESULT_OK && data != null) {
            if (currentTime - mResultPeriod >= 1000) {
                mResultPeriod = currentTime
                val resultString = data.getStringExtra(QrCodeScannerActivity.BUNDLE_SCAN_CALLBACK)
                resultString?.let {
                    mResult?.success(it)
                }
                return true
            }
        }
        return false
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this);
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
