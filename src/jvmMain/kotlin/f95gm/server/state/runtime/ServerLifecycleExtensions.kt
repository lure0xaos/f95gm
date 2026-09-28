package f95gm.server.state.runtime

import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.kernel.OwnedLifecycleResource
import f95gm.server.kernel.LifecycleResource

internal fun <T : LifecycleResource> ServerLifecycle.registerWithDependencies(
    name: LifecycleResourceName,
    resource: T,
    dependsOn: List<LifecycleResourceName>,
    failureBoundary: FailureBoundary = FailureBoundary.LIFECYCLE
): T = register(name, resource, dependsOn.toSet(), failureBoundary)

internal fun <T : OwnedLifecycleResource> ServerLifecycle.registerOwned(
    resource: T,
    dependsOn: List<LifecycleResourceName> = emptyList(),
    failureBoundary: FailureBoundary = FailureBoundary.LIFECYCLE
): T = registerWithDependencies(resource.owner, resource, dependsOn, failureBoundary)
