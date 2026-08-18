package f95gm.server.state.runtime

import f95gm.server.model.auth.UpstreamSession
import java.util.concurrent.ConcurrentHashMap

internal val sessions = ConcurrentHashMap<String, UpstreamSession>()
