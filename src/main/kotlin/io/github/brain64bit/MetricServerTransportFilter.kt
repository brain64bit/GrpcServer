package io.github.brain64bit

import com.codahale.metrics.Counter
import com.codahale.metrics.MetricRegistry
import io.grpc.Attributes
import io.grpc.ServerTransportFilter
import mu.KotlinLogging

class MetricServerTransportFilter: ServerTransportFilter {
    private val metricRegistry: MetricRegistry

    companion object {
        private val LOGGER = KotlinLogging.logger(SipServerWrapper::class.java.name)
    }

    constructor(metricRegistry: MetricRegistry){
        this.metricRegistry = metricRegistry
    }

    override fun transportReady(transportAttrs: Attributes): Attributes {
        val counter: Counter = this.metricRegistry.counter("connection")
        counter.inc()
        LOGGER.info("transportReady")
        return transportAttrs
    }

    override fun transportTerminated(transportAttrs: Attributes) {
        val counter: Counter = this.metricRegistry.counter("connection")
        counter.dec()
        LOGGER.info("transportTerminated")
    }
}