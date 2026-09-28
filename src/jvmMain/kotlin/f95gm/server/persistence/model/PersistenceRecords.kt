package f95gm.server.persistence.model

import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.model.EpochMillis
import f95gm.server.model.filters.SavedFilter
import f95gm.domain.api.FilterOptionRequest
import f95gm.server.model.marks.GameMark

/** Stable persistence-shaped values; database code does not need to construct route models. */
internal data class MarkRecord(
    val threadId: String,
    val trackingState: GameTrackingState,
    val storedVersion: String,
    val lastSeenUpdateVersion: String,
    val updatedAt: EpochMillis
)

internal data class SavedFilterOptionRecord(val id: String, val name: String)

internal data class SavedFilterRecord(
    val id: String,
    val name: String,
    val category: F95Category,
    val sort: F95Sort,
    val search: String,
    val creatorSearch: String,
    val searchMode: F95SearchMode,
    val selectedTags: List<SavedFilterOptionRecord>,
    val excludedTags: List<SavedFilterOptionRecord>,
    val selectedPrefixes: List<SavedFilterOptionRecord>,
    val excludedPrefixes: List<SavedFilterOptionRecord>,
    val tagType: F95TagType,
    val dateLimit: Int,
    val updatedAt: EpochMillis
)

internal fun GameMark.toRecord() = MarkRecord(
    threadId.value, trackingState, storedVersion.value, lastSeenUpdateVersion.value, updatedAt
)

internal fun MarkRecord.toDomain() = GameMark(
    threadId = F95ThreadId(threadId), trackingState = trackingState,
    storedVersion = StoredVersion(storedVersion), lastSeenUpdateVersion = StoredVersion(lastSeenUpdateVersion),
    updatedAt = updatedAt
)

private fun List<FilterOptionRequest>.toRecords() = map { SavedFilterOptionRecord(it.id.value, it.name.value) }
private fun List<SavedFilterOptionRecord>.toDomain() = map { FilterOptionRequest(F95OptionId(it.id), F95OptionName(it.name)) }

internal fun SavedFilter.toRecord() = SavedFilterRecord(
    id.value, name.value, category, sort, search, creatorSearch, searchMode,
    selectedTags.toRecords(), excludedTags.toRecords(), selectedPrefixes.toRecords(), excludedPrefixes.toRecords(),
    tagType, dateLimit, updatedAt
)

internal fun SavedFilterRecord.toDomain() = SavedFilter(
    id = F95FilterId(id), name = F95OptionName(name), category = category, sort = sort,
    search = search, creatorSearch = creatorSearch, searchMode = searchMode,
    selectedTags = selectedTags.toDomain(), excludedTags = excludedTags.toDomain(),
    selectedPrefixes = selectedPrefixes.toDomain(), excludedPrefixes = excludedPrefixes.toDomain(),
    tagType = tagType, dateLimit = dateLimit, updatedAt = updatedAt
)

