package com.justself.klique.customRecyclerView

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.justself.klique.Logger

class ScaledCarousel(
    context: Context,
    @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
    private val minScale: Float = 0.8f,
    private val maxScale: Float = 1f
) : LinearLayoutManager(context, orientation, false) {

    // Called when initially laying out children
    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)
        applyScaleToChildren()
    }

    // Called on every scroll; we re-apply scaling after the default scroll logic
    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        val scrolled = super.scrollVerticallyBy(dy, recycler, state)
        applyScaleToChildren()
        return scrolled
    }

    /** Loop through visible children and scale them based on their top offset. */
    private fun  applyScaleToChildren() {
        val maxDistance = getChildAt(0)?.height?.times(1.5f) ?: return

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val distance = child.top.toFloat().coerceAtLeast(0f)
            val fraction = 1f - (distance / maxDistance).coerceIn(0f, 1f)
            val scale = minScale + (maxScale - minScale) * fraction
            if (scale.isNaN()){
                Logger.d("IsNan", "continue, child: $maxDistance, distance: $distance")
                continue
            }

            // ✅ Align scale to left
            child.pivotX = 0f
            child.pivotY = 0f

            child.scaleX = scale
            child.scaleY = scale

            // 🚫 NO translationX – it causes drift
            child.translationX = 0f
        }
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
        }
        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }
}
