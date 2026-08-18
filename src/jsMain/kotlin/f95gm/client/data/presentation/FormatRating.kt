package f95gm.client.data.presentation

import f95gm.client.state.config.ClientConstants

internal fun formatRating(value: Double): String = if (value == ClientConstants.NO_RATING) "—" else {
    ((value * ClientConstants.RATING_SCALE).toInt() / ClientConstants.RATING_DECIMAL_SCALE).toString()
}
