package f95gm.client.data.api

import f95gm.messages.UiMessages
import f95gm.client.model.runtime.HttpStatusCodeValue

internal fun interface ApiFallback {
    fun message(status: HttpStatusCodeValue?): String
}

internal object ApiFallbacks {
    val Generic = ApiFallback { _ -> apiErrorFallback }
    val SignIn = ApiFallback { _ -> UiMessages.app_signInFailed() }
    val ItemDetails = ApiFallback { _ -> UiMessages.app_itemDetailsLoadFailed() }
    val FilterOptions = ApiFallback { _ -> UiMessages.app_filterOptionsLoadFailed() }
    val SavedFilters = ApiFallback { _ -> UiMessages.app_savedFiltersLoadFailed() }
    val MarkedGames = ApiFallback { _ -> UiMessages.app_markedGamesCheckFailed() }
    val FilterSave = ApiFallback { _ -> UiMessages.app_filterSaveFailed() }
    val FilterRemove = ApiFallback { _ -> UiMessages.app_filterRemoveFailed() }
    val QuickLinksExport = ApiFallback { _ -> UiMessages.app_quickLinksExportFailed() }
    val QuickLinksImport = ApiFallback { _ -> UiMessages.app_quickLinksImportFailed() }
    val TrackingSave = ApiFallback { _ -> UiMessages.app_trackingStateSaveFailed() }
    val TrackingRemove = ApiFallback { _ -> UiMessages.app_trackingStateRemoveFailed() }
}
