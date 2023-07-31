package org.akanework.symphonica.logic.util

import android.content.res.Resources
import android.util.TypedValue
import android.view.View

/**
 * Converts dp to px
 *
 * @param res Resources
 * @param dp  the value in dp
 * @return int
 */
fun toPixels(res: Resources, dp: Float): Int {
    return (dp * res.displayMetrics.density).toInt()
}

/**
 * Converts sp to px
 *
 * @param res Resources
 * @param sp  the value in sp
 * @return int
 */
fun toScreenPixels(res: Resources, sp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, res.displayMetrics).toInt()
}

fun isRtl(res: Resources): Boolean {
    return res.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
}