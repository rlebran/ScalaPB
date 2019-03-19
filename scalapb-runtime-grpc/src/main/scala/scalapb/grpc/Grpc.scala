package scalapb.grpc

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture, MoreExecutors}
import io.grpc.{Status, StatusException, StatusRuntimeException}
import io.grpc.stub.{ClientCallStreamObserver, ClientResponseObserver, StreamObserver}
import monix.eval.Task
import monix.execution.{Cancelable, CancelablePromise, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer, OverflowStrategy}

import scala.concurrent.{CancellationException, Future, Promise}
import scala.util.Try

object Grpc {
  def guavaFuture2ScalaFuture[A](guavaFuture: ListenableFuture[A]): Future[A] = {
    val p = Promise[A]()
    Futures.addCallback(
      guavaFuture,
      new FutureCallback[A] {
        override def onFailure(t: Throwable): Unit = p.failure(t)
        override def onSuccess(a: A): Unit         = p.success(a)
      },
      MoreExecutors.directExecutor()
    )
    p.future
  }

  def completeObserver[T](observer: StreamObserver[T])(t: Try[T]): Unit =
    t.map(observer.onNext) match {
      case scala.util.Success(_) =>
        observer.onCompleted()
      case scala.util.Failure(s: StatusException) =>
        observer.onError(s)
      case scala.util.Failure(s: StatusRuntimeException) =>
        observer.onError(s)
      case scala.util.Failure(e) =>
        observer.onError(
          Status.INTERNAL.withDescription(e.getMessage).withCause(e).asException()
        )
    }

  def guavaFuture2Task[A](guavaFuture: ListenableFuture[A]): Task[A] = {
    val p = CancelablePromise[A]()
    Futures.addCallback(
      guavaFuture,
      new FutureCallback[A] {
        override def onFailure(t: Throwable): Unit = p.failure(t)
        override def onSuccess(a: A): Unit         = p.success(a)
      },
      MoreExecutors.directExecutor()
    )

    Task.fromCancelablePromise(p)
  }
}

object MonixGrpcAdapters {

  def observe[TReq, TResp](grpcCall: (TReq, StreamObserver[TResp]) => Unit,
                           parameters: TReq)
                          (implicit scheduler: Scheduler): Observable[TResp] = {
    Observable.create[TResp](OverflowStrategy.Fail(2048))((s: Subscriber[TResp]) => {
      val grpcObserver = buildGrpcResponseObserver(s)(scheduler)
      grpcCall(parameters, grpcObserver)
      Cancelable(() => grpcObserver.onError(new CancellationException()))
    })
  }

  private def buildGrpcResponseObserver[ReqT, RespT](observer: Observer[RespT])
                                                    (implicit scheduler: Scheduler) =
    new ClientResponseObserver[ReqT, RespT] {
      override def beforeStart(requestStream: ClientCallStreamObserver[ReqT]): Unit = ()

      override def onError(t: Throwable): Unit = observer.onError(t)

      override def onCompleted(): Unit = observer.onComplete()

      override def onNext(value: RespT): Unit = observer.onNext(value)
    }
}
