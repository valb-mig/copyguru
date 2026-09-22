package com.valb.copyguru

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast

object ClipboardHelper {

    /** Copies [text] to the clipboard and shows a toast on API levels that don't show their own. */
    fun copy(context: Context, label: String, text: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(label, text))
        // Android 13+ shows its own clipboard confirmation popup.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, context.getString(R.string.copied, label), Toast.LENGTH_SHORT).show()
        }
    }
}
