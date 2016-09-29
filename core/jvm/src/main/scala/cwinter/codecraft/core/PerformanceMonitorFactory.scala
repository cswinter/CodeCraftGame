package cwinter.codecraft.core

import etm.core.configuration.{BasicEtmConfigurator, EtmManager}
import etm.core.monitor.EtmPoint
import etm.core.renderer.SimpleTextRenderer


private[codecraft] object PerformanceMonitorFactory {
  @volatile var assigned = false

  def performanceMonitor: PerformanceMonitor = new SimplePerformanceMonitor
}


private[codecraft] class JETMPerformanceMonitor extends PerformanceMonitor {
  private val debug = false
  private var activePoints = Map.empty[Symbol, EtmPoint]

  BasicEtmConfigurator.configure(true)
  private val monitor = EtmManager.getEtmMonitor
  monitor.start()


  override def measure[T](name: Symbol)(code: => T): T = {
    if (debug) println(s">$name")
    val point = synchronized { monitor.createPoint(name.toString) }
    val result = try {
      code
    } finally {
      synchronized { point.collect() }
    }
    if (debug) println(s"<$name")
    result
  }

  override def beginMeasurement(name: Symbol): Unit = synchronized {
    if (debug) println(s">$name")
    val point = monitor.createPoint(name.toString)
    activePoints += name -> point
  }

  override def endMeasurement(name: Symbol): Unit = synchronized {
    activePoints.get(name) match {
      case Some(point) =>
        point.collect()
        activePoints -= name
        if (debug) println(s"<$name")
      case None =>
        throw new Exception(s"Tried to end measurement of $name, but there is no active point for $name.")
    }
  }


  override def compileReport: String = synchronized {
    monitor.render(new SimpleTextRenderer)
    ""
  }
}

