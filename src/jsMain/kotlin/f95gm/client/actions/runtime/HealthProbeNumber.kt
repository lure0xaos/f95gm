package f95gm.client.actions.runtime

import f95gm.client.state.runtime.clientController

internal var healthProbeNumber
    get() = clientController.healthProbeNumber()
    set(value) {
        clientController.setHealthProbeNumber(value)
    }
