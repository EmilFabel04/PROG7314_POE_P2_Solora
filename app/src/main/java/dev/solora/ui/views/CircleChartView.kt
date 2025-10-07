package dev.solora.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import dev.solora.R
import kotlin.math.cos
import kotlin.math.sin

data class ChartData(
    val label: String,
    val value: Float,
    val color: Int,
    val percentage: Float
)

class CircleChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var chartData: List<ChartData> = emptyList()
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radius: Float = 0f
    private val strokeWidth = 50f
    
    // Color scheme: Black, Very Light Orange, Orange, Dark Orange
    private val defaultColors = listOf(
        Color.BLACK, // Black for first segment
        ContextCompat.getColor(context, R.color.solora_orange_very_light), // Very light orange
        ContextCompat.getColor(context, R.color.solora_orange), // Main orange
        ContextCompat.getColor(context, R.color.solora_orange_dark) // Dark orange
    )
    
    init {
        setupPaints()
    }
    
    private fun setupPaints() {
        textPaint.apply {
            color = Color.BLACK
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        
        labelPaint.apply {
            color = Color.BLACK
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        
        paint.apply {
            style = Paint.Style.STROKE
            strokeWidth = this@CircleChartView.strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
    }
    
    fun setChartData(data: List<ChartData>) {
        chartData = data
        invalidate()
    }
    
    fun setChartDataSimple(labels: List<String>, values: List<Float>) {
        val total = values.sum()
        if (total == 0f) return
        
        val chartDataList = labels.zip(values).mapIndexed { index, (label, value) ->
            val percentage = (value / total) * 100f
            val color = defaultColors[index % defaultColors.size]
            ChartData(label, value, color, percentage)
        }
        
        setChartData(chartDataList)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        centerX = w / 2f
        centerY = h / 2f
        radius = minOf(w, h) / 2f - strokeWidth - 50f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (chartData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        
        val total = chartData.sumOf { it.value.toDouble() }.toFloat()
        if (total == 0f) {
            drawEmptyState(canvas)
            return
        }
        
        var startAngle = -90f // Start from top
        
        // Draw arc segments
        chartData.forEach { data ->
            val sweepAngle = (data.value / total) * 360f
            
            paint.color = data.color
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle,
                false,
                paint
            )
            
            startAngle += sweepAngle
        }
        
        // Draw center text
        drawCenterText(canvas, total)
    }
    
    private fun drawCenterText(canvas: Canvas, total: Float) {
        val centerText = total.toInt().toString()
        val labelText = "Total"
        
        // Draw total value
        val textBounds = Rect()
        textPaint.getTextBounds(centerText, 0, centerText.length, textBounds)
        canvas.drawText(
            centerText,
            centerX,
            centerY - textBounds.height() / 2f,
            textPaint
        )
        
        // Draw label
        val labelBounds = Rect()
        labelPaint.getTextBounds(labelText, 0, labelText.length, labelBounds)
        canvas.drawText(
            labelText,
            centerX,
            centerY + labelBounds.height(),
            labelPaint
        )
    }
    
    private fun drawEmptyState(canvas: Canvas) {
        paint.color = Color.LTGRAY
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        textPaint.color = Color.GRAY
        canvas.drawText(
            "No Data",
            centerX,
            centerY,
            textPaint
        )
    }
}
