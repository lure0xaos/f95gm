package f95gm.client.state.runtime

import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

/** Feature-specific operation entry points keep job keys out of UI/action composition code. */
internal fun ClientCatalogActionsPort.launchCatalogOperation(block: suspend () -> Unit): Job =
    launchReplaceableOperation(ClientJobKey.CATALOG_LOAD, block)

internal fun ClientDetailActionsPort.launchDetailOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.DETAIL_LOAD, block)

internal fun ClientAuthActionsPort.launchAuthenticationOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.AUTHENTICATION, block)

internal fun ClientConnectionActionsPort.launchJvmConnectionOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.JVM_CONNECTION, block)

internal fun ClientMarksActionsPort.launchVersionCheckOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.VERSION_CHECK, block)

internal fun ClientMarksActionsPort.launchMarkedGamesCheckOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.MARKED_GAMES_CHECK, block)

internal fun ClientFiltersActionsPort.launchSavedFiltersLoadOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.SAVED_FILTERS_LOAD, block)

internal fun ClientCatalogActionsPort.launchFilterOptionsOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.FILTER_OPTIONS_LOAD, block)

internal fun ClientFiltersActionsPort.launchFilterSaveOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.FILTER_SAVE, block)

internal fun ClientFiltersActionsPort.launchFilterImportOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.FILTER_IMPORT, block)

internal fun ClientFiltersActionsPort.launchFilterExportOperation(block: suspend () -> Unit): Job =
    launchOperation(ClientJobKey.FILTER_EXPORT, block)

internal fun ClientMarksActionsPort.launchMarkMutationOperation(
    threadId: f95gm.domain.values.F95ThreadId,
    block: suspend () -> Unit
): Job = launchOperation(ClientJobKey.markMutation(threadId), block)

internal fun ClientOperationPort.launchReplaceableOperation(
    key: ClientJobKey,
    block: suspend () -> Unit,
    onCancelled: () -> Unit = {}
): Job = launchOperation(key) {
    try {
        block()
    } catch (cancelled: CancellationException) {
        onCancelled()
        throw cancelled
    }
}
