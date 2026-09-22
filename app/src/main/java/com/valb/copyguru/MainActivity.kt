package com.valb.copyguru

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valb.copyguru.data.Copy
import com.valb.copyguru.overlay.BubbleService
import com.valb.copyguru.ui.CopyEditorSheet
import com.valb.copyguru.ui.CopyGuruTheme
import com.valb.copyguru.ui.CopyGuruViewModel
import com.valb.copyguru.ui.HomeScreen
import com.valb.copyguru.ui.SegmentSheet

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                BubbleService.start(this)
            } else {
                Toast.makeText(this, "Permissão de sobreposição negada.", Toast.LENGTH_LONG).show()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            CopyGuruTheme {
                val viewModel: CopyGuruViewModel = viewModel()
                val segments by viewModel.segments.collectAsStateWithLifecycle()
                val copies by viewModel.copies.collectAsStateWithLifecycle()
                val selectedSegment by viewModel.selectedSegment.collectAsStateWithLifecycle()
                val query by viewModel.query.collectAsStateWithLifecycle()

                var editing by remember { mutableStateOf<Copy?>(null) }
                var showSegments by remember { mutableStateOf(false) }
                var bubbleActive by remember { mutableStateOf(BubbleService.isRunning()) }

                HomeScreen(
                    segments = segments,
                    copies = copies,
                    selectedSegment = selectedSegment,
                    query = query,
                    bubbleActive = bubbleActive,
                    onQueryChange = viewModel::setQuery,
                    onSelectSegment = viewModel::selectSegment,
                    onToggleBubble = {
                        bubbleActive = toggleBubble(bubbleActive)
                    },
                    onManageSegments = { showSegments = true },
                    onNewCopy = {
                        if (segments.isEmpty()) {
                            showSegments = true
                        } else {
                            editing = Copy(segmentId = segments.first().id, title = "", content = "")
                        }
                    },
                    onEditCopy = { editing = it },
                    onCopyToClipboard = { copy ->
                        ClipboardHelper.copy(this, copy.title, copy.content)
                        viewModel.markUsed(copy)
                    },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDeleteCopy = viewModel::deleteCopy
                )

                editing?.let { current ->
                    CopyEditorSheet(
                        copy = current,
                        segments = segments,
                        onDismiss = { editing = null },
                        onSave = {
                            viewModel.saveCopy(it)
                            editing = null
                        },
                        onDelete = {
                            viewModel.deleteCopy(it)
                            editing = null
                        }
                    )
                }

                if (showSegments) {
                    SegmentSheet(
                        segments = segments,
                        onDismiss = { showSegments = false },
                        onSave = viewModel::saveSegment,
                        onDelete = viewModel::deleteSegment
                    )
                }
            }
        }
    }

    /** Returns the bubble state after the toggle; permission flow may finish it asynchronously. */
    private fun toggleBubble(active: Boolean): Boolean {
        if (active) {
            BubbleService.stop(this)
            return false
        }
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return false
        }
        BubbleService.start(this)
        return true
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
