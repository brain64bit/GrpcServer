package io.github.brain64bit

import com.codahale.metrics.JmxReporter
import com.codahale.metrics.MetricRegistry
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.internal.GrpcUtil
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import mu.KotlinLogging
import sip_server.ActionGrpc
import sip_server.SipServer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class SipServerWrapper {
    private var server: Server? = null

    companion object {
        private val LOGGER = KotlinLogging.logger(SipServerWrapper::class.java.name)

        @JvmStatic
        fun main(args: Array<String>){
            val server = SipServerWrapper()
            server.start()
            server.blockUntilShutdown()
        }
    }

    internal class ActionImpl : ActionGrpc.ActionImplBase() {
        private var callCount: Int = 0
        override fun sendMessage(request: SipServer.Request, responseObserver: StreamObserver<SipServer.Response>) {
            val reason = "Success receive body: ${request.body}"
            val response = SipServer.Response.newBuilder().setStatus("OK").setReason(reason).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
        fun callCount(): Int{
            return callCount
        }
    }

    private fun stop(){
        if(server != null){
            server!!.shutdown()
        }
    }

    @Throws(InterruptedException::class)
    private fun blockUntilShutdown(){
        if(server != null){
            server!!.awaitTermination()
        }
    }

    @Throws(InterruptedException::class)
    private fun start() {
        val port:Int = 50051;
        val action = ActionImpl()
        val metricRegistry = MetricRegistry()

        val threadPoolExecutor = ThreadPoolExecutor(
                64,
                64,
                (1000 * 60 * 10).toLong(),
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(65536))
        val bossEventLoopGroup = NioEventLoopGroup(2)
        val workerEventLoopGroup = NioEventLoopGroup(40)

        val metricsServerTransportFilter = MetricServerTransportFilter(metricRegistry)
        val metricsServerStreamTracerFactory = MetricServerStreamTracerFactory()

        metricsServerStreamTracerFactory.stopwatchSupplier = GrpcUtil.STOPWATCH_SUPPLIER
        metricsServerStreamTracerFactory.slowCallTimeThreshold = 20L
        metricsServerStreamTracerFactory.metricRegistry = metricRegistry

        server = NettyServerBuilder.forPort(port)
                .bossEventLoopGroup(bossEventLoopGroup)
                .workerEventLoopGroup(workerEventLoopGroup)
                .channelType(NioServerSocketChannel::class.java)
                .executor(threadPoolExecutor)
                .flowControlWindow(1024 * 1024 * 1)
                .maxConcurrentCallsPerConnection(Integer.MAX_VALUE)
                .maxHeaderListSize(1024 * 8)
                .keepAliveTime(1000 * 60 * 10, TimeUnit.MILLISECONDS)
                .keepAliveTimeout(1000 * 30, TimeUnit.MILLISECONDS)
                .addService(action)
                .addTransportFilter(metricsServerTransportFilter)
                .addStreamTracerFactory(metricsServerStreamTracerFactory)
                .build()
                .start()

        val reporter = JmxReporter.forRegistry(metricRegistry).build()
        reporter.start()

        LOGGER.warn("gRPC Server started. Listening on port: "+port)
        Runtime.getRuntime().addShutdownHook(object : Thread(){
            override fun run() {
                LOGGER.warn("*** shutting down gRPC server since JVM is shutting down")
                this@SipServerWrapper.stop()
                LOGGER.warn("*** server shut down")
            }
        })

    }
}
