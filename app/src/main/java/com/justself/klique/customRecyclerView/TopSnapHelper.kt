package com.justself.klique.customRecyclerView

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class TopSnapHelper : LinearSnapHelper() {
    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        if (layoutManager !is LinearLayoutManager) return null

        var closestView: View? = null
        var minDistance = Int.MAX_VALUE

        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val distance = kotlin.math.abs(child.top)
            if (distance < minDistance) {
                minDistance = distance
                closestView = child
            }
        }

        return closestView
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray {
        val yDistance = targetView.top
        return intArrayOf(0, yDistance)
    }
}