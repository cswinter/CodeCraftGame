package cwinter.codecraft.core

import cwinter.codecraft.util.Stopwatch


private[codecraft] trait PerformanceMonitor {
  def measure[T](name: Symbol)(code: => T): T
  def beginMeasurement(name: Symbol)
  def endMeasurement(name: Symbol)
  def compileReport: String
}

private[codecraft] class MockPerformanceMonitor extends PerformanceMonitor {
  override def measure[T](name: Symbol)(code: => T): T = code
  override def compileReport: String = "MockPerformanceMonitor does not perform any measurements."
  override def beginMeasurement(name: Symbol) = ()
  override def endMeasurement(name: Symbol) = ()
}

private[codecraft] class SimplePerformanceMonitor extends Stopwatch with PerformanceMonitor

