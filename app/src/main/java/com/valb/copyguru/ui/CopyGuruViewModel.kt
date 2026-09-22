package com.valb.copyguru.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.valb.copyguru.data.Copy
import com.valb.copyguru.data.CopyRepository
import com.valb.copyguru.data.Segment
import com.valb.copyguru.data.SegmentWithCount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Pseudo-segment ids used by the filter row. */
object Filters {
    const val ALL = -2L
    const val FAVORITES = -1L
}

class CopyGuruViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CopyRepository.from(app)

    private val _selectedSegment = MutableStateFlow(Filters.ALL)
    val selectedSegment: StateFlow<Long> = _selectedSegment.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val segments: StateFlow<List<SegmentWithCount>> = repository.segmentsWithCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val copies: StateFlow<List<Copy>> = combine(_selectedSegment, _query) { segment, query -> segment to query }
        .flatMapLatest { (segment, query) ->
            val source = when (segment) {
                Filters.ALL -> repository.allCopies
                Filters.FAVORITES -> repository.favorites
                else -> repository.copiesOf(segment)
            }
            source.map { list ->
                if (query.isBlank()) {
                    list
                } else {
                    list.filter {
                        it.title.contains(query, ignoreCase = true) ||
                            it.content.contains(query, ignoreCase = true)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun selectSegment(id: Long) {
        _selectedSegment.value = id
    }

    fun setQuery(text: String) {
        _query.value = text
    }

    fun saveCopy(copy: Copy) = viewModelScope.launch { repository.saveCopy(copy) }

    fun deleteCopy(copy: Copy) = viewModelScope.launch { repository.deleteCopy(copy) }

    fun toggleFavorite(copy: Copy) = viewModelScope.launch { repository.toggleFavorite(copy) }

    fun markUsed(copy: Copy) = viewModelScope.launch { repository.markUsed(copy.id) }

    fun saveSegment(segment: Segment) = viewModelScope.launch { repository.saveSegment(segment) }

    fun deleteSegment(segment: Segment) = viewModelScope.launch {
        repository.deleteSegment(segment)
        if (_selectedSegment.value == segment.id) _selectedSegment.value = Filters.ALL
    }
}
