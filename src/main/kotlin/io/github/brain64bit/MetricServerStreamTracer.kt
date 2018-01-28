package io.github.brain64bit

import com.codahale.metrics.Histogram
import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import com.google.common.base.Stopwatch
import io.grpc.ServerStreamTracer
import io.grpc.Status
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MetricServerStreamTracer: ServerStreamTracer {
    private val fullMethodName: String?
    private val streamClosed: AtomicBoolean = AtomicBoolean(false)
    private val stopwatch: Stopwatch?
    private val slowCallTimeThreshold: Long?
    private val metricRegistry: MetricRegistry?

    constructor(fullMethodName: String?, stopwatch: Stopwatch?, slowCallTimeThreshold: Long?, metricRegistry: MetricRegistry?) : super() {
        this.fullMethodName = fullMethodName
        this.stopwatch = stopwatch
        this.slowCallTimeThreshold = slowCallTimeThreshold
        this.metricRegistry = metricRegistry
    }

    companion object {
        private val LOGGER = KotlinLogging.logger(SipServerWrapper::class.java.name)
    }

    override fun inboundMessage(seqNo: Int) {
        try {
            val meter: Meter? = this.metricRegistry?.meter("grpc.server.request-per-second")
            meter?.mark()
        }catch (exception: Exception){
            LOGGER.error("grpc.server.request-per-second", exception)
        }
    }

    override fun outboundMessage(seqNo: Int) {
        try {
            val meter: Meter? = this.metricRegistry?.meter("grpc.server.response-per-second")
            meter?.mark()
        }catch (exception: Exception){
            LOGGER.error("grpc.server.response-per-second", exception)
        }
    }

    override fun streamClosed(status: Status) {
        if (! this.streamClosed.compareAndSet(false, true)) {
            return;
        }

        try {
            this.stopwatch?.stop();
        } catch (exception: Exception) {
            LOGGER.error("could not stop stopwatch.", exception);
        }

        try {
            val meter: Meter? = this.metricRegistry?.meter("request-per-second");
            meter?.mark();
        } catch (exception: Exception) {
            LOGGER.error("request-per-second", exception);
        }

        if (status.isOk){
            try {
                val meter: Meter? = this.metricRegistry?.meter("succeeded-request-per-second");
                meter?.mark();
            } catch (exception: Exception) {
                LOGGER.error("succeeded-request-per-second", exception);
            }

            val responseTime: Long = this.stopwatch?.elapsed(TimeUnit.MILLISECONDS) ?: 0

            try {
                val histogram: Histogram? = this.metricRegistry?.histogram("response-time")
                histogram?.update(responseTime)
            }catch (exception: Exception){
                LOGGER.error("response-time", exception);
            }

            if(this.slowCallTimeThreshold?.compareTo(responseTime) == -1){
                try {
                    val meter: Meter? = this.metricRegistry?.meter("slow-request-per-second")
                    meter?.mark()
                }catch (exception: Exception){
                    LOGGER.error("slow-request-per-second", exception);
                }
            }
        }else{

        }
    }
}