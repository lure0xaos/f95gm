package f95gm.server.state.runtime

internal interface RouteDependencies :
    SessionRouteDependencies,
    UpstreamRouteDependencies,
    MediaRouteDependencies,
    ApplicationRouteDependencies
