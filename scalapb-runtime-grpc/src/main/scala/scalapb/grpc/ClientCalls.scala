package scalapb.grpc
import io.grpc.stub.StreamObserver
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import monix.eval.Task
import monix.reactive.Observable

import scala.collection.JavaConverters._
import scala.concurrent.Future

object ClientCalls {
  def blockingUnaryCall[ReqT, RespT](
      channel: Channel,
      method: MethodDescriptor[ReqT, RespT],
      options: CallOptions,
      request: ReqT
  ): RespT = {
    io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(method, options), request)
  }

  def asyncUnaryCall[ReqT, RespT](
      channel: Channel,
      method: MethodDescriptor[ReqT, RespT],
      options: CallOptions,
      request: ReqT
  ): Future[RespT] = {
    Grpc.guavaFuture2ScalaFuture(
      io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(method, options), request)
    )
  }

  def taskAsyncUnaryCall[ReqT, RespT](
      channel: Channel,
      method: MethodDescriptor[ReqT, RespT],
      options: CallOptions,
      request: ReqT
  ): Task[RespT] = {
    Grpc.guavaFuture2Task(
      io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(method, options), request)
    )
  }

  def blockingServerStreamingCall[ReqT, RespT](
      channel: Channel,
      method: MethodDescriptor[ReqT, RespT],
      options: CallOptions,
      request: ReqT
  ): Iterator[RespT] = {
    io.grpc.stub.ClientCalls
      .blockingServerStreamingCall(channel.newCall(method, options), request)
      .asScala
  }

  def asyncServerStreamingCall[ReqT, RespT](
      channel: Channel,
      method: MethodDescriptor[ReqT, RespT],
      options: CallOptions,
      request: ReqT,
      responseObserver: StreamObserver[RespT]
  ): Unit = {
    io.grpc.stub.ClientCalls
      .asyncServerStreamingCall(channel.newCall(method, options), request, responseObserver)
  }

  def monixAsyncServerStreamingCall[ReqT, RespT](
      channel: Channel,
      method: MethodDescriptor[ReqT, RespT],
      options: CallOptions,
      request: ReqT,
      responseObserver: StreamObserver[RespT]
  ): Observable[RespT] = {
    MonixGrpcAdapters.observe(
      io.grpc.stub.ClientCalls
        .asyncServerStreamingCall(channel.newCall(method, options), request, responseObserver),
      request
    )(monix.execution.Scheduler.global)
  }

  def asyncClientStreamingCall[ReqT, RespT](
      channel: Channel,
      method: MethodDescriptor[ReqT, RespT],
      options: CallOptions,
      responseObserver: StreamObserver[RespT]
  ): StreamObserver[ReqT] = {
    io.grpc.stub.ClientCalls
      .asyncClientStreamingCall(channel.newCall(method, options), responseObserver)
  }

  def asyncBidiStreamingCall[ReqT, RespT](
      channel: Channel,
      method: MethodDescriptor[ReqT, RespT],
      options: CallOptions,
      responseObserver: StreamObserver[RespT]
  ): StreamObserver[ReqT] = {
    io.grpc.stub.ClientCalls
      .asyncBidiStreamingCall(channel.newCall(method, options), responseObserver)
  }
}
