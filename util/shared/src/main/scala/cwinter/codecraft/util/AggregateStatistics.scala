package cwinter.codecraft.util


private[codecraft] class AggregateStatistics {
  private val emaDecay = 0.9
  private[this] var _count = 0
  private[this] var _total = 0.0
  private[this] var _ema = 0.0
  private[this] var _last = 0.0

  def addMeasurement(measurement: Double): Unit = {
    _count += 1
    _ema = emaDecay * _ema + (1 - emaDecay) * measurement
    _total += measurement
    _last = measurement
  }

  def average = _total / _count
  def count = _count
  def ema = _ema
  def last = _last
  def total = _total

  def display: String = f"Last: $last%.4g  Average: $average%.4g  EMA: $ema%.4g  Count: $count"
}

