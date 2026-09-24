package f95gm.client.state.session

import f95gm.client.state.runtime.clientController

internal var loaded
    get() = clientController.isSessionLoaded()
    set(value) {
        clientController.setSessionLoaded(value)
    }
