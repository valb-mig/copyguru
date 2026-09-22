package com.valb.copyguru.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class CopyRepository(private val dao: CopyGuruDao) {

    val segments: Flow<List<Segment>> = dao.observeSegments()
    val segmentsWithCount: Flow<List<SegmentWithCount>> = dao.observeSegmentsWithCount()
    val allCopies: Flow<List<Copy>> = dao.observeAllCopies()
    val favorites: Flow<List<Copy>> = dao.observeFavorites()

    fun copiesOf(segmentId: Long): Flow<List<Copy>> = dao.observeCopiesBySegment(segmentId)

    suspend fun segmentList(): List<Segment> = dao.getSegments()
    suspend fun copyList(segmentId: Long): List<Copy> = dao.getCopiesBySegment(segmentId)
    suspend fun favoriteList(): List<Copy> = dao.getFavorites()

    suspend fun saveSegment(segment: Segment) {
        if (segment.id == 0L) dao.insertSegment(segment) else dao.updateSegment(segment)
    }

    suspend fun deleteSegment(segment: Segment) = dao.deleteSegment(segment)

    suspend fun saveCopy(copy: Copy) {
        val stamped = copy.copy(updatedAt = System.currentTimeMillis())
        if (copy.id == 0L) dao.insertCopy(stamped) else dao.updateCopy(stamped)
    }

    suspend fun deleteCopy(copy: Copy) = dao.deleteCopy(copy)

    suspend fun toggleFavorite(copy: Copy) = dao.setFavorite(copy.id, !copy.favorite)

    suspend fun markUsed(id: Long) = dao.bumpUsage(id)

    suspend fun seedIfEmpty() {
        if (dao.segmentCount() > 0) return
        val geral = dao.insertSegment(Segment(name = "Geral", color = SEGMENT_COLORS[0], position = 0))
        dao.insertCopy(
            Copy(
                segmentId = geral,
                title = "Boas-vindas",
                content = "Olá! Tudo bem? Como posso te ajudar hoje?",
                favorite = true
            )
        )
        dao.insertCopy(
            Copy(
                segmentId = geral,
                title = "PIX",
                content = "Segue a chave PIX para pagamento:"
            )
        )
    }

    companion object {
        val SEGMENT_COLORS = listOf(
            0xFF6C5CE7.toInt(),
            0xFF00B894.toInt(),
            0xFFE17055.toInt(),
            0xFF0984E3.toInt(),
            0xFFD63031.toInt(),
            0xFFFDCB6E.toInt(),
            0xFFE84393.toInt(),
            0xFF636E72.toInt()
        )

        fun from(context: Context): CopyRepository =
            CopyRepository(CopyGuruDatabase.get(context).dao())
    }
}
