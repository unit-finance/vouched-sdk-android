package com.example.unitvouchsampleapp

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import id.vouched.android.*
import id.vouched.android.BarcodeDetect.OnBarcodeResultListener
import id.vouched.android.exception.VouchedAssetsMissingException
import id.vouched.android.exception.VouchedCameraHelperException
import id.vouched.android.liveness.LivenessMode
import id.vouched.android.model.Insight
import id.vouched.android.model.Job
import id.vouched.android.model.JobResponse
import id.vouched.android.model.Params
import id.vouched.android.model.error.VouchedError
import java.util.*

/**
 * A service that uses Vouched SDK and was made for Unit customers to implement the SDK in an easier way.
 */
class UNVouchedService(
    private val listener: Listener
) : CardDetect.OnDetectResultListener,
    OnBarcodeResultListener,
    FaceDetect.OnDetectResultListener,
    VouchedSession.OnJobResponseListener {

    /**
     *  Updates the MainActivity.
     */
    interface Listener {
        fun onProgressUpdate(str: String)
        fun onCaptureComplete(result: OnResult)
        fun showConfirmOverlay(capturedImage: Bitmap?, didConfirm: (Boolean) -> (Unit))
        fun showTimeoutView(shouldRetryCapture: (Boolean) -> (Unit))
    }

    /**
     * Used to pass the result from Vouched to the MainActivity that holds the service.
     */
    sealed class OnResult {
        class Success(val job: Job) : OnResult()
        class Failure(val error: VouchedError) : OnResult()
    }
    
    // Enter a public key
    private var session: VouchedSession? = VouchedSession(
        ""
    )
    val PERMISSION_REQUESTS = 1

    private lateinit var cameraHelper: VouchedCameraHelper
    private lateinit var mode: VouchedCameraHelper.Mode
    private lateinit var previewView: PreviewView
    lateinit var context: Context
    private var isPostingId = false

    /**
     * Flags
     */
    private var inputFirstName: String? = "John"
    private var inputLastName: String? = "Smith"
    private val idConfirmationEnabled = true

    /**
     * Configures the VouchedCameraHelper.
     *
     * Usually the first function to be called after the service is initialized.
     */
    fun start(mode: VouchedCameraHelper.Mode, previewView: PreviewView, context: Context) {
        this.mode = mode
        this.previewView = previewView
        this.context = context
        configureHelper(mode)
    }

    /**
     * This helper is introduced to integrate VouchedSDK and provide the optimal photography.
     *
     * The mode will affect the type of detector that will be used.
     */
    private fun configureHelper(mode: VouchedCameraHelper.Mode) {
        try {
            var detectOptions: VouchedCameraHelperOptions? = null
            when (mode) {
                VouchedCameraHelper.Mode.ID -> {
                    detectOptions = detectionOptionsForId()
                }
                VouchedCameraHelper.Mode.FACE -> {
                    detectOptions = detectionOptionsForFace()
                }
                else -> {}
            }
            cameraHelper = VouchedCameraHelper(
                this.context,
                this.context as LifecycleOwner,
                ContextCompat.getMainExecutor(this.context),
                previewView,
                mode,
                detectOptions
            )
        } catch (e: VouchedAssetsMissingException) {
            e.printStackTrace()
        }
    }

    /**
     * Configures the relevant flags for the helper when id is being captured.
     */
    private fun detectionOptionsForId(): VouchedCameraHelperOptions {
        return VouchedCameraHelperOptions.Builder()
            .withCardDetectOptions(
                CardDetectOptions.Builder()
                    .withEnableDistanceCheck(false)
                    .withEnhanceInfoExtraction(false)
                    .withEnableOrientationCheck(false)
                    .build()
            )
            .withCardDetectResultListener(this)
            .withBarcodeDetectResultListener(this)
            .withCameraFlashDisabled(true)
            .withTimeOut(20000, this::handleTimeOut)
            .build()
    }

    /**
     * Configures the relevant flags for the helper when face is being captured.
     */
    private fun detectionOptionsForFace(): VouchedCameraHelperOptions {
        return VouchedCameraHelperOptions.Builder()
            .withFaceDetectOptions(
                FaceDetectOptions.Builder()
                    .withLivenessMode(LivenessMode.NONE)
                    .build()
            )
            .withFaceDetectResultListener(this)
            .build()
    }

    fun onResume() {
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        } else {
            try {
                cameraHelper.onResume()
            } catch (e: VouchedCameraHelperException) {
                e.printStackTrace()
            }
        }
    }

    fun onPause() {
        cameraHelper.onPause()
    }

    /**
     * Making sure the card result is ready to process.
     */
    override fun onCardDetectResult(cardDetectResult: CardDetectResult?) {
        when (cardDetectResult?.step) {
            Step.POSTABLE -> {
                isPostingId = true
                if (idConfirmationEnabled) {
                    runOnUiThread(listener.showConfirmOverlay(cardDetectResult.imageBitmap) { didConfirm ->
                        if (didConfirm) {
                            this.onProgressUpdate("Processing")
                            postId(cardDetectResult)
                        } else {
                            onResume()
                        }
                    }
                    )
                } else {
                    this.onProgressUpdate("Please wait. Processing image.")
                    onPause()
                    postId(cardDetectResult)
                }
            }
            else -> {
                this.onProgressUpdate(cardDetectResult?.instruction)
            }
        }
    }

    /**
     * Posting the id captured by Vouched Camera Helper to create a job.
     */
    private fun postId(cardDetectResult: CardDetectResult) {
        this.onProgressUpdate("Please wait. Processing image.")
        val currentMode = cameraHelper.currentMode
        if (currentMode == VouchedCameraHelper.Mode.ID) {
            session?.postFrontId(
                this.context,
                cardDetectResult,
                getParamsBuilderWithInputData(),
                this
            )
        } else if (currentMode == VouchedCameraHelper.Mode.ID_BACK) {
            session?.postBackId(this.context, cardDetectResult, Params.Builder(), this)
        }
    }

    /**
     * Posting the id captured by the user manually to create a job.
     */
    private fun postId(manualCaptureImage: Bitmap) {
        this.onProgressUpdate("Please wait. Processing image.")
        val currentMode = cameraHelper.currentMode
        if (currentMode == VouchedCameraHelper.Mode.ID) {
            session?.postFrontId(
                this.context,
                manualCaptureImage,
                getParamsBuilderWithInputData(),
                this
            )
        } else if (currentMode == VouchedCameraHelper.Mode.ID_BACK) {
            session?.postBackId(this.context, manualCaptureImage, Params.Builder(), this)
        }
    }

    /**
     * Making sure the barcode result is ready to process and posting the result to create a job;
     */
    override fun onBarcodeResult(barcodeResult: BarcodeResult?) {
        if (barcodeResult != null) {
            this.onProgressUpdate("Please wait. Processing image.")
            onPause()
            session?.postBackId(this.context, barcodeResult, Params.Builder(), this)
        } else {
            this.onProgressUpdate("Focus camera on back of ID")
        }
    }

    /**
     * Making sure the face result is ready to process and posting the result to create a job;
     */
    override fun onFaceDetectResult(faceDetectResult: FaceDetectResult?) {
        when (faceDetectResult?.step) {
            Step.POSTABLE -> {
                this.onProgressUpdate("Please wait. Processing image.")
                onPause()
                session?.postFace(this.context, faceDetectResult, null, this)
            }
            else -> {
                this.onProgressUpdate(faceDetectResult?.instruction)
            }
        }
    }

    private fun getParamsBuilderWithInputData(): Params.Builder? =
        Params.Builder().withFirstName(inputFirstName).withLastName(inputLastName)

    private fun hasJobErrors(response: JobResponse): Boolean {
        if (response.error != null) {
            onProgressUpdate("Error @onJobResponse")
            updateCaptureResult(OnResult.Failure(response.error))
            return true
        }
        return false
    }

    private fun onJobInsights(insight: Insight) {
        val resumeCamera = Runnable { onResume() }
        onProgressUpdate(insight)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread(null, resumeCamera)
            }
        }, 5000)
    }

    private fun onJobIdResponse(job: Job) {
        isPostingId = false
        // determine if the job response requires other id processing
        // NOTE: This only processes the response if .withEnhanceInfoExtraction is set to true,
        // otherwise, it will always return a next state of COMPLETED
        cameraHelper.updateDetectionModes(job.result)
        // advance the mode to the next ID detection state.
        val next = cameraHelper.nextMode
        if (next != VouchedCameraHelper.Mode.COMPLETED) {
            try {
                cameraHelper.switchMode(next)
            } catch (e: VouchedAssetsMissingException) {
                e.printStackTrace()
            }

            if (next == VouchedCameraHelper.Mode.ID_BACK || next == VouchedCameraHelper.Mode.BARCODE) {
                updateForModeChange()
            }
        } else {
            onPause()
        }
    }

    /**
     * The response from Vouched to posting;
     *
     * When no errors are found, a job is created.
     */
    override fun onJobResponse(response: JobResponse) {
        if (hasJobErrors(response)) return
        isPostingId = false
        val job = response.job
        VouchedLogger.getInstance().info(job.toJson())
        val insights = VouchedUtils.extractInsights(response.job)
        if (insights.size != 0) {
            onJobInsights(insights[0])
        } else {
            if (this.mode == VouchedCameraHelper.Mode.ID) {
                onJobIdResponse(job)
            }
            onProgressUpdate("Success")
            updateCaptureResult(OnResult.Success(job))
        }
    }

    private fun updateForModeChange() {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                onProgressUpdate("Turn ID card over to backside and lay on surface")
            }
        }, 2000)
    }

    /**
     * Updates the listener with instructions,insights and custom messages.
     */
    private fun onProgressUpdate(str: String) {
        runOnUiThread(listener.onProgressUpdate(str))
    }

    /**
     * Runs actions that affect UI.
     */
    private fun runOnUiThread(listenerFun: Unit? = null, runnable: Runnable? = null) {
        if (this.context is Activity) {
            listenerFun?.let { (this.context as? Activity)?.runOnUiThread { listenerFun } }
            runnable?.let { (this.context as? Activity)?.runOnUiThread { runnable } }
        }
    }

    /**
     * Updates the listener with an instruction that can be found in the `UNVouchedServicePermissionExt.kt` file.
     */
    private fun onProgressUpdate(instruction: Instruction?) {
        instruction?.let {
            val message = instruction.getDescriptiveText(this.mode)
            message?.let {
                this.onProgressUpdate(message)
            }
        }
    }

    /**
     * Updates the listener with an insight that can be found in the `UNVouchedServicePermissionExt.kt` file.
     */
    private fun onProgressUpdate(insight: Insight?) {
        if (insight != null) {
            this.onProgressUpdate(insight.getDescriptiveText())
        }
    }

    /**
     * Triggered for id capture when the 'withTimeOut' flag is configured in the detection options.
     */
    private fun handleTimeOut() {
        if (!isPostingId) {
            runOnUiThread(listener.showTimeoutView { shouldRetryCapture ->
                if (shouldRetryCapture) cameraHelper.clearAndRestartTimeout()
            })
        }
    }

    /**
     * Manual capture of an id.
     */
    fun capturePhoto() {
        cameraHelper.capturePhoto { image: Bitmap? ->
            runOnUiThread(listener.showConfirmOverlay(image) { didConfirm ->
                if (didConfirm) {
                    image?.let { postId(image) }
                } else {
                    cameraHelper.clearAndRestartTimeout()
                }
            }
            )
        }
    }

    /**
     * Updates the listener with the result status that was sent from Vouched to the capture picture.
     *
     * Attached to the result is the job or error that was obtained.
     */
    private fun updateCaptureResult(result: OnResult) {
        runOnUiThread(listener.onCaptureComplete(result))
    }

    /**
     * Creates a job response with the results of the session.
     *
     * This response will show the results for the id and face captures and the relevant confidences obtained by Vouched.
     */
    fun postConfirm(onResult: (OnResult) -> Unit) {
        this.onProgressUpdate("Processing")
        session?.confirm(
            this.context, null
        ) { jobResponse ->
            val job = jobResponse.job
            if (job != null) {
                this.onProgressUpdate("Job results were printed")
                return@confirm onResult(OnResult.Success(job))
            } else {
                updateCaptureResult(OnResult.Failure(jobResponse.error))
                return@confirm
            }
        }
    }
}

