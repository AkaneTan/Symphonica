package org.akanework.symphonica.ui.component

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * This view class is responsible for creating a squiggly line animation.
 * The squiggly line is created using a quadratic bezier curve and is animated
 * using a ValueAnimator.
 *
 * @param context: Context instance.
 * @param attrs: Attribute set passed to the view.
 * @param defStyleAttr: Default style attributes passed to the view.
 */
class SquigglyView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Represents the stroke width in pixels.
     */
    private val strokeWidthPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        4f,
        context.resources.displayMetrics
    )

    private val strokeColor = MaterialColors.getColor(
        this,
        com.google.android.material.R.attr.colorPrimary
    )

    /**
     * Paint instance used to draw squiggly line.
     */
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = strokeColor
        strokeWidth = strokeWidthPx
    }

    /**
     * Path instance used to create squiggly line.
     */
    private val path = Path()

    /**
     * Animator instance responsible for animating squiggly line.
     */
    private val animator = ValueAnimator.ofInt(0, 100).apply {
        duration = 2000
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { animation ->
            xOffset = (animation.currentPlayTime / 10).toFloat() // Here we control the speed of the squiggly line
            invalidate()
        }
    }

    /**
     * Offset value used to animate squiggly line.
     */
    private var xOffset: Float = 0f

    /**
     * Method responsible for drawing squiggly line.
     *
     * @param canvas: Canvas instance where squiggly line is drawn.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        path.reset()
        path.moveTo(-xOffset, height / 2f)
        var i = -xOffset
        while (i < width) {
            path.rQuadTo(25f, -30f, 50f, 0f)
            path.rQuadTo(25f, 30f, 50f, 0f)
            i += 100
        }

        canvas.drawPath(path, paint)
    }

    /**
     * Method called when view is attached to window.
     * Here we start the squiggly line animation.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    /**
     * Method called when view is detached from window.
     * Here we stop the squiggly line animation.
     */
    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}

