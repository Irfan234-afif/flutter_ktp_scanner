package id.mncinnovation.mnc_identifier_ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import id.mncinnovation.ocr.ExtractDataOCRListener
import id.mncinnovation.ocr.MNCIdentifierOCR
import id.mncinnovation.ocr.model.OCRResultModel

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File

/** MncIdentifierOcrPlugin */
class MncIdentifierOcrPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private lateinit var activity: Activity
  private var result: Result? = null
  private var activityPluginBinding: ActivityPluginBinding? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "mnc_identifier_ocr")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  companion object {
    const val CAPTURE_EKTP_REQUEST_CODE = 102
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    this.result = result

    if (call.method == "startCaptureKtp") {
      //get argument from flutter
      val withFlash: Boolean? = call.argument("withFlash")
      val cameraOnly: Boolean? = call.argument("cameraOnly")

      print("cameraOnly")
      print(cameraOnly)

      MNCIdentifierOCR.config(withFlash, cameraOnly)
      MNCIdentifierOCR.startCapture(activity, CAPTURE_EKTP_REQUEST_CODE)
    }else if(call.method == "processImage"){
        val path: String? = call.argument("path")
        val listener = object : ExtractDataOCRListener {
            override fun onStart() {

            }

            override fun onFinish(value: OCRResultModel) {
                result?.success(value.toJson())
                clearMethodCallAndResult()
            }
        }
        MNCIdentifierOCR.extractDataFromUri(Uri.fromFile(File(path)), context, listener)
    }
  }

 fun clearMethodCallAndResult() {
    this.result = null
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if(requestCode == CAPTURE_EKTP_REQUEST_CODE){
      if (resultCode == Activity.RESULT_OK) {
        val captureKtpResult = MNCIdentifierOCR.getOCRResult(data)
        result?.success(captureKtpResult?.toJson())
        clearMethodCallAndResult()
        return true
      } else if(resultCode == Activity.RESULT_CANCELED){
        result?.error("Canceled by user", "Mnc-identifier-ocr: activity canceled by user", "")
        clearMethodCallAndResult()
        return  false
      }
    }
    result?.error("Invalid request code", "Mnc-identifier-ocr: Received request code: $requestCode", "Expected request code: $CAPTURE_EKTP_REQUEST_CODE")
    clearMethodCallAndResult()
    return false
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.getActivity()
    this.activityPluginBinding = binding
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivity() {
      activityPluginBinding?.removeActivityResultListener(this)
      activityPluginBinding = null
  }
}
