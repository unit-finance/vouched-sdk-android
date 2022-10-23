package com.example.unitvouchsampleapp

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.example.unitvouchsampleapp.databinding.ActivityMainBinding
import com.example.unitvouchsampleapp.databinding.IncludeIdConfirmationBinding
import com.example.unitvouchsampleapp.databinding.IncludeIdManualCaptureBinding
import com.example.unitvouchsampleapp.databinding.IncludeIdTimeoutBinding
import com.google.gson.Gson
import id.vouched.android.VouchedCameraHelper

/**
 * Handle UI changes and holds a reference to a `UNVouchedService` that is using the Vouched SDK.
 *
 * This controller demonstrates the following flow:
- Capture an id
- Capture a selfie
- Print the result of the captures that was returned from Vouched
 */
class MainActivity : AppCompatActivity(), UNVouchedService.Listener {

    private lateinit var service: UNVouchedService

    // UI elements.
    private lateinit var binding: ActivityMainBinding
    private lateinit var previewView: PreviewView
    private lateinit var instructionTextView: TextView
    private lateinit var idGuideOverlay: IdGuideOverlay
    private lateinit var idConfirmationView: IncludeIdConfirmationBinding
    private lateinit var timeOutView: IncludeIdTimeoutBinding
    private lateinit var manualCaptureView: IncludeIdManualCaptureBinding
    private lateinit var continueButton: Button

    // A mode to capture, starts with id capturing.
    private var mode = VouchedCameraHelper.Mode.ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        continueButton = binding.continueButton
        manualCaptureView = binding.manualCaptureView
        timeOutView = binding.timeoutView
        idConfirmationView = binding.idConfirmationView
        previewView = binding.previewView
        instructionTextView = binding.instructionsTextView
        idGuideOverlay = binding.guideOverlay

        //Shows a rectangle on screen to guide the user.
        if (mode != VouchedCameraHelper.Mode.ID) idGuideOverlay.visibility = View.INVISIBLE

        service = UNVouchedService(this)
        service.start(mode, previewView, this)

        continueButton.setOnClickListener {
            instructionTextView.visibility = View.VISIBLE
            timeOutView.root.visibility = View.INVISIBLE
            continueButton.visibility = View.INVISIBLE
            if (this.mode == VouchedCameraHelper.Mode.ID) {
                //Finished capturing an ID.
                this.mode = VouchedCameraHelper.Mode.FACE
                idGuideOverlay.visibility = View.INVISIBLE
                this.service.start(this.mode, previewView, this)
                this.onResume()
            } else if (this.mode == VouchedCameraHelper.Mode.FACE) {
                // Finished capturing ID and Selfie.
                service.postConfirm { jobResult ->
                    val gson = Gson()
                    when (jobResult) {
                        is UNVouchedService.OnResult.Success -> println(
                            "Job Results: " + gson.toJson(
                                jobResult.job.result
                            )
                        )
                        is UNVouchedService.OnResult.Failure -> println("Error @postConfirm failure: " + jobResult.error.message)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        service.onResume()
    }

    override fun onPause() {
        service.onPause()
        super.onPause()
    }

    private fun hideConfirmOverlay() {
        idConfirmationView.root.visibility = View.INVISIBLE
        instructionTextView.visibility = View.VISIBLE
    }

    private fun showManualCaptureView() {
        manualCaptureView.root.visibility = View.VISIBLE
        manualCaptureView.captureButton.setOnClickListener {
            manualCaptureView.root.visibility = View.INVISIBLE
            service.capturePhoto()
        }
    }

    private fun showContinueButton() {
        continueButton.visibility = View.VISIBLE
        instructionTextView.text = "Success"
    }

    // Callbacks for the UNVouchedService Listener.
    /**
     * Shows on screen different messages that come from the service.
     */
    override fun onProgressUpdate(str: String) {
        instructionTextView.text = str
    }

    /**
     * Gets access to a Job result when it is returned from Vouched.
     */
    override fun onCaptureComplete(result: UNVouchedService.OnResult) {
        return when (result) {
            is UNVouchedService.OnResult.Success -> {
                println("Success with JobID " + result.job.id)
                when (mode) {
                    VouchedCameraHelper.Mode.ID -> {
                        showContinueButton()
                        continueButton.setText("Continue to Selfie capture")
                    }
                    VouchedCameraHelper.Mode.FACE -> {
                        showContinueButton()
                        continueButton.setText("Continue to results")
                    }
                    else -> {}
                }
            }
            is UNVouchedService.OnResult.Failure -> print("onCaptureComplete failure: " + result.error.message)
        }
    }

    /**
     * A confirmation view for a captured image.
     */
    override fun showConfirmOverlay(capturedImage: Bitmap?, didConfirm: (Boolean) -> Unit) {
        idConfirmationView.root.visibility = View.VISIBLE
        idConfirmationView.imageView.setImageBitmap(capturedImage)
        instructionTextView.visibility = View.INVISIBLE

        idConfirmationView.confirmButton.setOnClickListener {
            hideConfirmOverlay()
            didConfirm.invoke(true)
        }

        idConfirmationView.retryButton.setOnClickListener {
            hideConfirmOverlay()
            didConfirm.invoke(false)
        }
    }

    /**
     * A view that is visible after a certain amount of time without a successful image capture.
     */
    override fun showTimeoutView(shouldRetryCapture: (Boolean) -> Unit) {
        timeOutView.root.visibility = View.VISIBLE
        timeOutView.manuallyCaptureOptionButton.setOnClickListener {
            instructionTextView.visibility = View.INVISIBLE
            timeOutView.root.visibility = View.INVISIBLE
            showManualCaptureView()
            shouldRetryCapture.invoke(false)
        }

        timeOutView.retryButton.setOnClickListener {
            timeOutView.root.visibility = View.INVISIBLE
            shouldRetryCapture.invoke(true)
        }
    }
}



