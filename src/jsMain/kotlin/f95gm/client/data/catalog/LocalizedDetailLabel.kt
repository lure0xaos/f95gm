package f95gm.client.data.catalog

import f95gm.messages.UiMessages

internal fun localizedDetailLabel(value: String): String = when (value.trim().lowercase()) {
    "thread updated" -> UiMessages.app_threadUpdated()
    "release date" -> UiMessages.app_releaseDate()
    "developer" -> UiMessages.app_developer()
    "censored" -> UiMessages.app_censored()
    "version" -> UiMessages.app_version()
    "os" -> UiMessages.app_os()
    "language" -> UiMessages.app_language()
    "length" -> UiMessages.app_length()
    "store" -> UiMessages.app_store()
    "genre" -> UiMessages.app_genre()
    "installation" -> UiMessages.app_installation()
    "changelog" -> UiMessages.app_changelog()
    "developer notes" -> UiMessages.app_developerNotes()
    "download" -> UiMessages.app_download()
    else -> value
}
