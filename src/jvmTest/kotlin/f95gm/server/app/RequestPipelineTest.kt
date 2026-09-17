package f95gm.server.app

import f95gm.server.http.request.RequestId
import f95gm.server.http.request.RequestMetrics
import f95gm.server.http.request.RequestEndpoint
import f95gm.server.state.runtime.ActiveRequestTracker
import f95gm.server.kernel.ServerClock
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.lang.reflect.Proxy

class RequestPipelineTest {
    private val clock = object : ServerClock {
        override fun epochMillis() = f95gm.server.model.EpochMillis(1L)
        override fun monotonicTimestamp() = f95gm.server.kernel.MonotonicNanos(1_000L)
        override fun monotonicMillisTimestamp() = f95gm.server.kernel.MonotonicMillis(0L)
    }

    @Test
    fun generatedStageOrdersRemainOuterToInnerAndBackOut() = runTest {
        repeat(100) { seed ->
            val order = mutableListOf<String>()
            val stages = (0 until (seed % 8)).map { index ->
                RequestPipelineStage { _, _, next ->
                    order += "before$index"
                    next()
                    order += "after$index"
                }
            }
            val pipeline = RequestPipeline(
                clock, RequestMetrics(), ActiveRequestTracker(),
                RequestPipelineConfiguration(stages)
            )
            val state = pipeline.start(RequestId("request$seed"))
            val call = fakeCall()
            pipeline.execute(call, state) { order += "terminal" }

            val expected = stages.indices.flatMap { listOf("before$it") } +
                listOf("terminal") + stages.indices.reversed().flatMap { listOf("after$it") }
            assertEquals(expected, order)
        }
    }

    @Test
    fun aStageCanShortCircuitWithoutInvokingLaterStages() = runTest {
        val order = mutableListOf<String>()
        val pipeline = RequestPipeline(
            clock, RequestMetrics(), ActiveRequestTracker(),
            RequestPipelineConfiguration(
                listOf(
                    RequestPipelineStage { _, _, _ -> order += "rejected" },
                    RequestPipelineStage { _, _, next -> order += "unexpected"; next() }
                )
            )
        )
        val call = fakeCall()
        pipeline.execute(call, pipeline.start(RequestId("short"))) { order += "terminal" }
        assertEquals(listOf("rejected"), order)
    }

    @Test
    fun finishRecordsTheEndpointFromPipelineState() = runTest {
        val metrics = RequestMetrics()
        val pipeline = RequestPipeline(
            clock, metrics, ActiveRequestTracker(), RequestPipelineConfiguration()
        )
        val state = pipeline.start(RequestId("endpoint"))
        val endpoint = RequestEndpoint("catalog")
        state.endpoint = endpoint

        pipeline.finish(fakeCall(), state)

        assertEquals(1L, metrics.snapshot().endpointRequests[endpoint])
    }

    private fun fakeCall(): ApplicationCall = Proxy.newProxyInstance(
        ApplicationCall::class.java.classLoader,
        arrayOf(ApplicationCall::class.java)
    ) { _, _, _ -> null } as ApplicationCall
}
