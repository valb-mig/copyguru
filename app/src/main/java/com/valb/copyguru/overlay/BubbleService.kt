package com.valb.copyguru.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.valb.copyguru.ClipboardHelper
import com.valb.copyguru.MainActivity
import com.valb.copyguru.R
import com.valb.copyguru.data.Copy
import com.valb.copyguru.data.CopyRepository
import com.valb.copyguru.data.Segment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Foreground service that hosts the draggable bubble and the copy panel.
 * Kept on plain Views instead of Compose: an overlay window has no Activity
 * lifecycle owner, and Views avoid that whole class of crashes.
 */
class BubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: CopyRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var bubbleView: View? = null
    private var panelView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams

    private var segments: List<Segment> = emptyList()
    private var selectedSegmentId: Long = FAVORITES_ID
    private var copiesJob: Job? = null

    /**
     * The panel watches for outside touches, and a tap on the bubble counts as one.
     * Without this the bubble tap would close the panel and immediately reopen it,
     * so a dismiss recorded here suppresses the reopen that follows.
     */
    private var outsideDismissAt = 0L

    private lateinit var adapter: CopyAdapter

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = CopyRepository.from(this)
        startForeground(NOTIFICATION_ID, buildNotification())
        if (Settings.canDrawOverlays(this)) {
            showBubble()
            observeSegments()
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        hidePanel()
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        scope.cancel()
        super.onDestroy()
    }

    // region bubble

    private fun showBubble() {
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null)
        // Explicit size: inflating with a null parent drops the layout's own width/height.
        val size = dp(BUBBLE_SIZE_DP)
        bubbleParams = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = screenHeight() / 3
        }
        view.setOnTouchListener(DragTouchListener())
        windowManager.addView(view, bubbleParams)
        bubbleView = view
    }

    /** Distinguishes a tap from a drag, and snaps the bubble to the nearest edge on release. */
    private inner class DragTouchListener : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var moved = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > TOUCH_SLOP || abs(dy) > TOUCH_SLOP) moved = true
                    bubbleParams.x = initialX + dx
                    bubbleParams.y = (initialY + dy).coerceIn(0, screenHeight() - view.height)
                    windowManager.updateViewLayout(view, bubbleParams)
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        snapToEdge(view)
                    } else {
                        togglePanel()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun snapToEdge(view: View) {
        val middle = screenWidth() / 2
        bubbleParams.x = if (bubbleParams.x + view.width / 2 < middle) 0 else screenWidth() - view.width
        windowManager.updateViewLayout(view, bubbleParams)
    }

    // endregion

    // region panel

    private fun togglePanel() {
        if (panelView != null) {
            hidePanel()
            return
        }
        if (SystemClock.uptimeMillis() - outsideDismissAt < OUTSIDE_DISMISS_GRACE_MS) return
        showPanel()
    }

    private fun showPanel() {
        val margin = dp(12)
        val gap = dp(8)
        val bubbleSize = dp(BUBBLE_SIZE_DP)
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null)

        // The panel sits beside the bubble, never on top of it: the bubble keeps the
        // side it was dragged to, the panel takes the remaining width.
        val panelWidth = screenWidth() - bubbleSize - gap - margin
        val panelHeight = (screenHeight() * 0.55f).toInt()
        val bubbleOnLeft = bubbleParams.x + bubbleSize / 2 < screenWidth() / 2

        val params = WindowManager.LayoutParams(
            panelWidth,
            panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (bubbleOnLeft) bubbleParams.x + bubbleSize + gap else margin
            y = bubbleParams.y.coerceIn(margin, (screenHeight() - panelHeight - margin).coerceAtLeast(margin))
        }

        adapter = CopyAdapter(
            onClick = { copy ->
                ClipboardHelper.copy(this, copy.title, copy.content)
                scope.launch { repository.markUsed(copy.id) }
                hidePanel()
            },
            onFavorite = { copy -> scope.launch { repository.toggleFavorite(copy) } }
        )

        view.findViewById<RecyclerView>(R.id.copyList).apply {
            layoutManager = LinearLayoutManager(this@BubbleService)
            adapter = this@BubbleService.adapter
        }
        view.findViewById<TextView>(R.id.closePanel).setOnClickListener { hidePanel() }
        view.findViewById<TextView>(R.id.openApp).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            hidePanel()
        }
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                outsideDismissAt = SystemClock.uptimeMillis()
                hidePanel()
                true
            } else {
                false
            }
        }

        windowManager.addView(view, params)
        panelView = view
        renderChips()
        observeCopies()
    }

    private fun hidePanel() {
        copiesJob?.cancel()
        copiesJob = null
        panelView?.let { runCatching { windowManager.removeView(it) } }
        panelView = null
    }

    private fun renderChips() {
        val container = panelView?.findViewById<LinearLayout>(R.id.segmentChips) ?: return
        container.removeAllViews()
        addChip(container, FAVORITES_ID, "★ " + getString(R.string.favorites), null)
        segments.forEach { addChip(container, it.id, it.name, it.color) }
    }

    private fun addChip(container: LinearLayout, id: Long, label: String, color: Int?) {
        val chip = LayoutInflater.from(this)
            .inflate(R.layout.overlay_chip, container, false) as TextView
        chip.text = label
        val selected = id == selectedSegmentId
        chip.alpha = if (selected) 1f else 0.55f
        if (selected) {
            chip.background.mutate().setTint(color ?: getColor(R.color.brand))
        }
        chip.setOnClickListener {
            selectedSegmentId = id
            renderChips()
            observeCopies()
        }
        container.addView(chip)
    }

    // endregion

    // region data

    private fun observeSegments() {
        scope.launch {
            repository.seedIfEmpty()
            repository.segments.collectLatest { list ->
                segments = list
                if (selectedSegmentId != FAVORITES_ID && list.none { it.id == selectedSegmentId }) {
                    selectedSegmentId = FAVORITES_ID
                    observeCopies()
                }
                if (panelView != null) renderChips()
            }
        }
    }

    private fun observeCopies() {
        copiesJob?.cancel()
        if (panelView == null) return
        val flow = if (selectedSegmentId == FAVORITES_ID) {
            repository.favorites
        } else {
            repository.copiesOf(selectedSegmentId)
        }
        copiesJob = scope.launch {
            flow.collectLatest { list ->
                adapter.submit(list)
                panelView?.findViewById<View>(R.id.emptyLabel)?.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    // endregion

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.bubble_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.bubble_notification_title))
            .setContentText(getString(R.string.bubble_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun screenWidth() = resources.displayMetrics.widthPixels
    private fun screenHeight() = resources.displayMetrics.heightPixels
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val FAVORITES_ID = -1L
        private const val CHANNEL_ID = "copyguru_bubble"
        private const val NOTIFICATION_ID = 42
        private const val TOUCH_SLOP = 12
        private const val BUBBLE_SIZE_DP = 56
        private const val OUTSIDE_DISMISS_GRACE_MS = 250L
        const val ACTION_STOP = "com.valb.copyguru.STOP_BUBBLE"

        fun start(context: Context) {
            val intent = Intent(context, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BubbleService::class.java))
        }

        fun isRunning(): Boolean = running

        @Volatile
        private var running = false
    }
}

private class CopyAdapter(
    private val onClick: (Copy) -> Unit,
    private val onFavorite: (Copy) -> Unit
) : RecyclerView.Adapter<CopyAdapter.Holder>() {

    private var items: List<Copy> = emptyList()

    fun submit(list: List<Copy>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.overlay_copy_item, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.itemTitle)
        private val preview: TextView = view.findViewById(R.id.itemPreview)
        private val favorite: TextView = view.findViewById(R.id.itemFavorite)

        fun bind(copy: Copy) {
            title.text = copy.title
            preview.text = copy.content
            favorite.alpha = if (copy.favorite) 1f else 0.3f
            itemView.setOnClickListener { onClick(copy) }
            favorite.setOnClickListener { onFavorite(copy) }
        }
    }
}
