package com.blog.domain.poll.service.mapper

import com.blog.domain.poll.dto.response.*

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
    val showEtc = optionCount > topN

    // total이 0이어도 top에는 0들이 들어올 수 있음(이미 left join + groupBy라서)
    // 퍼센트 계산은 total=0이면 전부 0으로
    val percents = top.map { item ->
        val p = if (total > 0) ((item.count * 100.0) / total).toInt() else 0
        item to p
    }

    val sumTop = percents.sumOf { it.second }

    val items = percents.mapIndexed { idx, (item, p) ->
        RankingPreviewItem(
            optionId = item.optionId,
            label = item.label,
            count = item.count,
            percent = p.coerceIn(0, 100),
            rank = idx + 1,
        )
    }

    val etcCountValue = (total - top.sumOf { it.count }).coerceAtLeast(0)

    val etc = when {
        total <= 0 -> 0
        etcCountValue == 0L -> 0
        else -> (100 - sumTop).coerceAtLeast(0)
    }

    val etcPercent = if (showEtc) etc else etc.takeIf { it > 0 }
    val etcCount = if (showEtc) etcCountValue else etcCountValue.takeIf { it > 0 }

    val hasEtc = showEtc
    return RankingPreview(items = items, etcPercent = etcPercent, etcCount = etcCount, hasEtc = hasEtc)
}

internal fun buildYesNoResults(rows: List<OptionCountRow>, total: Long): YesNoResults {
    val yes = rows.firstOrNull { it.sortOrder == 0 }
    val no = rows.firstOrNull { it.sortOrder == 1 }

    val yesCount = yes?.count ?: 0L
    val noCount = no?.count ?: 0L
    val safeTotal = if (total > 0) total else (yesCount + noCount)

    if (safeTotal <= 0L) {
        return YesNoResults(
            yesOptionId = yes?.optionId ?: -1L,
            noOptionId = no?.optionId ?: -1L,
            yesCount = 0L,
            noCount = 0L,
            yesPercent = 0,
            noPercent = 0,
        )
    }

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