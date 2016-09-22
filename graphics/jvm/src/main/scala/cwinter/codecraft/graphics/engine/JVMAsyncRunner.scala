package cwinter.codecraft.graphics.engine

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

private[codecraft] trait JVMAsyncRunner { self: Simulator =>
  val id = JVMAsyncRunner.count.addAndGet(1)

  import JVMAsyncRunner._

  private[codecraft] def runAsync(): Unit = {
    if (stopped || gameStatus != Running) return
    performAsyncUpdate()(ec).onComplete {
      case Success(_) => runAsync()
      case Failure(x) => x.printStackTrace()
    }
  }
}

private[codecraft] object JVMAsyncRunner {
  val count = new AtomicInteger(0)
  private val threadPool = Executors.newFixedThreadPool(32)
  implicit val ec = new ExecutionContext {
    override def reportFailure(cause: Throwable): Unit = { cause.printStackTrace() }
    override def execute(runnable: Runnable): Unit = threadPool.submit(runnable)
  }
}

