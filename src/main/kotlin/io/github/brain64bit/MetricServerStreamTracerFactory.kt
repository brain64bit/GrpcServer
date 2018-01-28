package io.github.brain64bit

import com.codahale.metrics.MetricRegistry
import com.google.common.base.Stopwatch
import com.google.common.base.Supplier
import io.grpc.Metadata
import io.grpc.ServerStreamTracer

class MetricServerStreamTracerFactory: ServerStreamTracer.Factory() {
    var slowCallTimeThreshold: Long? = null
    var metricRegistry: MetricRegistry? = null
    var stopwatchSupplier: Supplier<Stopwatch>? = null

    override fun newServerStreamTracer(fullMethodName: String, headers: Metadata): ServerStreamTracer {
        val stopwatch: Stopwatch? = stopwatchSupplier?.get()
        stopwatch?.start()

        val serverStreamTracer: ServerStreamTracer = MetricServerStreamTracer(
                fullMethodName,
                stopwatch,
                this.slowCallTimeThreshold,
                this.metricRegistry
        )
        return serverStreamTracer;
    }

}