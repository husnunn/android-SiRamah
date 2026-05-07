package com.gridy.rohmahapp.pages

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.gridy.rohmahapp.R

open class BaseFragment : Fragment() {

    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    /** Sisa langkah async sebelum `stopRefreshing()` (dipanggil dari `onRefreshData`). */
    private var swipeRefreshRemainingSteps: Int = 0

    open fun onRefreshData() {}

    // Default: false. Override jadi true di fragment yang butuh refresh.
    open fun isSwipeRefreshEnabled(): Boolean = false


    protected fun setupSwipeRefresh(swipeLayout: SwipeRefreshLayout) {
        if (!isSwipeRefreshEnabled()) return

        swipeRefreshLayout = swipeLayout
        swipeRefreshLayout?.apply {
            isEnabled = true
            setColorSchemeResources(
                R.color.on_primary,
                R.color.on_secondary_container
            )
            setOnRefreshListener {
                onRefreshData()
            }
        }
    }

    /** Set jumlah callback `swipeRefreshStepDone()` yang harus selesai sebelum indikator hilang. */
    protected fun beginTrackedSwipeRefresh(steps: Int) {
        swipeRefreshRemainingSteps = steps.coerceAtLeast(0)
    }

    /** Panggil dari observer ketika satu request reload selesai (sukses atau gagal). */
    protected fun swipeRefreshStepDone() {
        if (swipeRefreshRemainingSteps <= 0) return
        swipeRefreshRemainingSteps--
        if (swipeRefreshRemainingSteps == 0) {
            stopRefreshing()
        }
    }

    fun stopRefreshing() {
        swipeRefreshLayout?.isRefreshing = false
    }
    fun showLoading(isLoading: Boolean, message: String = "Please wait...") {
        val layoutLoading = view?.findViewById<View>(R.id.layout_loading)
//        val progressText = view?.findViewById<TextView>(R.id.progressText)

        layoutLoading?.visibility = if (isLoading) View.VISIBLE else View.GONE
//        progressText?.text = message
    }

    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        context?.let {
            Toast.makeText(it, message, duration).show()
        }
    }

}
