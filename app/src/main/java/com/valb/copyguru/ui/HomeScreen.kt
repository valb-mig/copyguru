package com.valb.copyguru.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valb.copyguru.data.Copy
import com.valb.copyguru.data.SegmentWithCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    segments: List<SegmentWithCount>,
    copies: List<Copy>,
    selectedSegment: Long,
    query: String,
    bubbleActive: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectSegment: (Long) -> Unit,
    onToggleBubble: () -> Unit,
    onManageSegments: () -> Unit,
    onNewCopy: () -> Unit,
    onEditCopy: (Copy) -> Unit,
    onCopyToClipboard: (Copy) -> Unit,
    onToggleFavorite: (Copy) -> Unit,
    onDeleteCopy: (Copy) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CopyGuru") },
                actions = {
                    IconButton(onClick = onToggleBubble) {
                        Icon(
                            Icons.Default.BubbleChart,
                            contentDescription = if (bubbleActive) "Desligar bolinha" else "Ligar bolinha",
                            tint = if (bubbleActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = onManageSegments) {
                        Icon(Icons.Default.Tune, contentDescription = "Segmentos")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewCopy) {
                Icon(Icons.Default.Add, contentDescription = "Nova copy")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Buscar copy") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SegmentChip("Todas", copies.size.takeIf { selectedSegment == Filters.ALL }, null,
                        selectedSegment == Filters.ALL) { onSelectSegment(Filters.ALL) }
                }
                item {
                    SegmentChip("★ Favoritas", null, null, selectedSegment == Filters.FAVORITES) {
                        onSelectSegment(Filters.FAVORITES)
                    }
                }
                items(segments, key = { it.id }) { segment ->
                    SegmentChip(
                        label = segment.name,
                        count = segment.copyCount,
                        color = Color(segment.color),
                        selected = selectedSegment == segment.id
                    ) { onSelectSegment(segment.id) }
                }
            }

            if (copies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nenhuma copy aqui ainda.\nToque em + para criar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(copies, key = { it.id }) { copy ->
                        CopyCard(
                            copy = copy,
                            segmentColor = segments.firstOrNull { it.id == copy.segmentId }?.color,
                            onClick = { onCopyToClipboard(copy) },
                            onEdit = { onEditCopy(copy) },
                            onFavorite = { onToggleFavorite(copy) },
                            onDelete = { onDeleteCopy(copy) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentChip(
    label: String,
    count: Int?,
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(if (count != null) "$label · $count" else label) },
        leadingIcon = color?.let {
            {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(it))
            }
        },
        colors = FilterChipDefaults.filterChipColors()
    )
}

@Composable
private fun CopyCard(
    copy: Copy,
    segmentColor: Int?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (segmentColor != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(segmentColor))
                    )
                }
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = if (segmentColor != null) 8.dp else 0.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (copy.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favoritar",
                        tint = if (copy.favorite) Color(0xFFFDCB6E) else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(
                text = copy.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover")
                }
            }
        }
    }
}
