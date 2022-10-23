package com.example.unitvouchsampleapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


/**
 * simple overlay, so that the user is guided to maximize ID image
 */
class IdGuideOverlay(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private var guidePaint: Paint? = null
    private val widthPercent = .70
    private val aspectRatio = 1.60
    private val cornerRadius = 40f
    private fun initPaint() {
        val guide = Paint()
        guide.color = Color.WHITE
        guide.style = Paint.Style.STROKE
        guide.strokeWidth = 8f
        guidePaint = guide
    }

    public override fun onDraw(canvas: Canvas) {
        val centerX = (width / 2).toFloat()
        val centerY = (height / 2).toFloat()
        val halfGuideLength: Float = (width / 2 * widthPercent).toFloat()
        val halfGuideHeight: Float = (halfGuideLength * aspectRatio).toFloat()
        canvas.drawRoundRect(
            centerX - halfGuideLength,
            centerY - halfGuideHeight,
            centerX + halfGuideLength,
            centerY + halfGuideHeight,
            cornerRadius,
            cornerRadius,
            guidePaint!!
        )
    }

    init {
        initPaint()
    }
}