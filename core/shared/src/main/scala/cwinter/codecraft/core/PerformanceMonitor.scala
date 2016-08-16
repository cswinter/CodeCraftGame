package cwinter.codecraft.core

import cwinter.codecraft.util.Stopwatch


trait PerformanceMonitor {
  def measure[T](name: Symbol)(code: => T): T
  def beginMeasurement(name: Symbol)
  def endMeasurement(name: Symbol)
  def compileReport: String
}

class MockPerformanceMonitor extends PerformanceMonitor {
  override def measure[T](name: Symbol)(code: => T): T = code
  override def compileReport: String = "MockPerformanceMonitor does not perform any measurements."
  override def beginMeasurement(name: Symbol) = ()
  override def endMeasurement(name: Symbol) = ()
}

class SimplePerformanceMonitor extends Stopwatch with PerformanceMonitor

