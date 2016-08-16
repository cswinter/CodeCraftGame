package cwinter.codecraft.core

object PerformanceMonitorFactory {
  def performanceMonitor: PerformanceMonitor = new MockPerformanceMonitor()
}


