package com.blog.domain.poll.service.mapper

import com.blog.domain.poll.dto.response.OptionCountRow
import com.blog.domain.poll.dto.response.RankingPreviewItem
import com.blog.domain.poll.dto.response.RankingPreviewRaw
import com.blog.domain.poll.dto.response.YesNoCountRaw
import com.blog.domain.poll.dto.response.YesNoPreview
import com.blog.domain.poll.dto.response.RankingPreview
import com.blog.domain.poll.dto.response.RankingResultItem
import com.blog.domain.poll.dto.response.YesNoResults
import com.blog.domain.poll.dto.response.RankingResults

internal fun YesNoCountRaw.toYesNoPreview(): YesNoPreview {
    if (total <= 0) return YesNoPreview(
        yesCount = 0,
        noCount = 0,
        yesPercent = 0,
        noPercent = 0,
    )

    val yesP = ((yesCount * 100.0) / total).toInt()
    // 합 100 보정: no는 100-yes로 고정
    val noP = 100 - yesP

    return YesNoPreview(
        yesCount = yesCount,
        noCount = noCount,
        yesPercent = yesP,
        noPercent = noP,
    )
}

internal fun RankingPreviewRaw.toRankingPreview(topN: Int = 5): RankingPreview {
    if (total <= 0) return RankingPreview(items = emptyList(), etcPercent = null, etcCount = null)

    val percents = top.map { item ->
        val p = ((item.count * 100.0) / total).toInt()
        item to p
    }

    val sumTop = percents.sumOf { it.second }
    var etc = 100 - sumTop

    val items = percents.mapIndexed { idx, (item, p) ->
        val percent = if (idx == percents.lastIndex && etc < 0) p + etc else p

        RankingPreviewItem(
            optionId = item.optionId,
            label = item.label,
            count = item.count,
            percent = percent.coerceIn(0, 100),
            rank = idx + 1,
        )
    }

    if (etc < 0) etc = 0
    val etcPercent = etc.takeIf { it > 0 }
    val etcCount = total - top.sumOf { it.count }

    return RankingPreview(items = items, etcPercent = etcPercent, etcCount = etcCount)
}

internal fun buildYesNoResults(rows: List<OptionCountRow>, total: Long): YesNoResults {
    val yes = rows.firstOrNull { it.sortOrder == 0 }
    val no = rows.firstOrNull { it.sortOrder == 1 }

    val yesCount = yes?.count ?: 0L
    val noCount = no?.count ?: 0L
    val safeTotal = if (total > 0) total else (yesCount + noCount)

    val yesP = if (safeTotal > 0) ((yesCount * 100.0) / safeTotal).toInt() else 0
    val noP = 100 - yesP

    return YesNoResults(
        yesOptionId = yes?.optionId ?: -1L,
        noOptionId = no?.optionId ?: -1L,
        yesCount = yesCount,
        noCount = noCount,
        yesPercent = yesP,
        noPercent = noP,
    )
}

internal fun buildRankingResults(rows: List<OptionCountRow>, total: Long): RankingResults {
    val safeTotal = if (total > 0) total else rows.sumOf { it.count }

    val sorted = rows
        .sortedWith(compareByDescending<OptionCountRow> { it.count }.thenBy { it.sortOrder }.thenBy { it.optionId })

    val items = sorted.mapIndexed { idx, r ->
        val p = if (safeTotal > 0) ((r.count * 100.0) / safeTotal).toInt() else 0
        RankingResultItem(
            optionId = r.optionId,
            label = r.label,
            count = r.count,
            percent = p,
            rank = idx + 1,
        )
    }

    return RankingResults(
        totalVotes = safeTotal,
        items = items,
    )
}