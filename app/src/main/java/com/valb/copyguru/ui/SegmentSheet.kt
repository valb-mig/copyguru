package com.valb.copyguru.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.valb.copyguru.data.CopyRepository
import com.valb.copyguru.data.Segment
import com.valb.copyguru.data.SegmentWithCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentSheet(
    segments: List<SegmentWithCount>,
    onDismiss: () -> Unit,
    onSave: (Segment) -> Unit,
    onDelete: (Segment) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingId by remember { mutableStateOf(0L) }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(CopyRepository.SEGMENT_COLORS.first()) }
    var pendingDelete by remember { mutableStateOf<SegmentWithCount?>(null) }

    fun reset() {
        editingId = 0L
        name = ""
        color = CopyRepository.SEGMENT_COLORS.first()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Segmentos", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (editingId == 0L) "Novo segmento" else "Renomear segmento") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CopyRepository.SEGMENT_COLORS) { option ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(option))
                            .clickable { color = option },
                        contentAlignment = Alignment.Center
                    ) {
                        if (option == color) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editingId != 0L) {
                    TextButton(onClick = { reset() }) { Text("Cancelar edição") }
                }
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSave(
                            Segment(
                                id = editingId,
                                name = name.trim(),
                                color = color,
                                position = segments.size
                            )
                        )
                        reset()
                    }
                ) { Text(if (editingId == 0L) "Adicionar" else "Salvar") }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(segments, key = { it.id }) { segment ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(segment.color))
                        )
                        Text(
                            text = "${segment.name} · ${segment.copyCount}",
                            modifier = Modifier.weight(1f).padding(start = 10.dp)
                        )
                        IconButton(onClick = {
                            editingId = segment.id
                            name = segment.name
                            color = segment.color
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar segmento")
                        }
                        IconButton(onClick = { pendingDelete = segment }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover segmento")
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remover \"${target.name}\"?") },
            text = {
                Text(
                    if (target.copyCount == 0) {
                        "O segmento será removido."
                    } else {
                        "As ${target.copyCount} copies deste segmento também serão apagadas."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(Segment(target.id, target.name, target.color, target.position))
                    if (editingId == target.id) reset()
                    pendingDelete = null
                }) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
