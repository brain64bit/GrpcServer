package io.github.brain64bit

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import mu.KotlinLogging
import sip_server.ActionGrpc
import sip_server.SipServer

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
        override fun sendMessage(request: SipServer.Request, responseObserver: StreamObserver<SipServer.Response>) {
            val reason = "Success receive body: ${request.body}"
            val response = SipServer.Response.newBuilder().setStatus("OK").setReason(reason).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
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
        server = NettyServerBuilder.forPort(port)
                .addService(ActionImpl())
                .build()
                .start()

        LOGGER.info("gRPC Server started. Listening on port: "+port)
        Runtime.getRuntime().addShutdownHook(object : Thread(){
            override fun run() {
                LOGGER.warn("*** shutting down gRPC server since JVM is shutting down")
                this@SipServerWrapper.stop()
                LOGGER.warn("*** server shut down")
            }
        })

    }
}
