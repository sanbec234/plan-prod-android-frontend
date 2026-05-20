package com.hanghub.app.ui.maps

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.hanghub.app.data.HHPlace
import com.hanghub.app.data.HHUser

internal object ScoutPinViews {

    fun place(context: Context, place: HHPlace, onClick: () -> Unit): View {
        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 10))
            background = roundedBg(0xFFFFFFFF.toInt(), 999f)
            elevation = dp(context, 3).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val emoji = TextView(context).apply {
            text = place.emoji
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }

        val name = TextView(context).apply {
            text = place.name
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(context, 8))
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }

        pill.addView(emoji)
        pill.addView(name)

        // Mapbox ViewAnnotationManager expects non-null LayoutParams.
        if (pill.layoutParams == null) {
            pill.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return pill
    }

    fun user(context: Context, user: HHUser, onClick: () -> Unit): View {
        val bubble = TextView(context).apply {
            text = user.avatar
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            width = dp(context, 36)
            height = dp(context, 36)
            background = roundedBg(0xFFFFFFFF.toInt(), 999f).apply {
                setStroke(dp(context, 2), 0x33000000)
            }
            elevation = dp(context, 3).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        // Mapbox ViewAnnotationManager expects non-null LayoutParams.
        if (bubble.layoutParams == null) {
            bubble.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return bubble
    }

    private fun roundedBg(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp
            setColor(color)
        }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
