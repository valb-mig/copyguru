package com.valb.copyguru.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.valb.copyguru.data.Copy
import com.valb.copyguru.data.SegmentWithCount

/**
 * Create/edit sheet. [copy] with id 0 means "new copy".
 * Requires at least one segment to exist, since every copy belongs to one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyEditorSheet(
    copy: Copy,
    segments: List<SegmentWithCount>,
    onDismiss: () -> Unit,
    onSave: (Copy) -> Unit,
    onDelete: (Copy) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    var title by remember { mutableStateOf(copy.title) }
    var content by remember { mutableStateOf(copy.content) }
    var favorite by remember { mutableStateOf(copy.favorite) }
    var segmentId by remember {
        mutableStateOf(
            if (copy.segmentId != 0L) copy.segmentId else segments.firstOrNull()?.id ?: 0L
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            // Scrollable body: gives up height to keep the action row below always visible.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (copy.id == 0L) "Nova copy" else "Editar copy",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { favorite = !favorite }) {
                        Icon(
                            imageVector = if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favoritar",
                            tint = if (favorite) Color(0xFFFDCB6E) else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da copy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Texto") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 260.dp)
                )

                Text("Segmento", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(segments, key = { it.id }) { segment ->
                        FilterChip(
                            selected = segment.id == segmentId,
                            onClick = { segmentId = segment.id },
                            label = { Text(segment.name) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (copy.id != 0L) {
                    TextButton(onClick = { onDelete(copy) }) { Text("Remover") }
                }
                Row(modifier = Modifier.weight(1f)) {}
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(
                    enabled = title.isNotBlank() && content.isNotBlank() && segmentId != 0L,
                    onClick = {
                        onSave(
                            copy.copy(
                                title = title.trim(),
                                content = content.trim(),
                                favorite = favorite,
                                segmentId = segmentId
                            )
                        )
                    }
                ) { Text("Salvar") }
            }
        }
    }
}
