package cwinter.codecraft.util

import scala.collection.mutable.ArrayBuffer


class Stopwatch {
  private type Millis = Double
  private var measurements = Map.empty[Symbol, ArrayBuffer[Long]]
  private var startTime = Map.empty[Symbol, Long]


  def measure[T](name: Symbol)(code: => T): T = {
    val start = System.nanoTime()
    try {
      code
    } finally {
      collectMeasurement(name, System.nanoTime() - start)
    }
  }

  def beginMeasurement(name: Symbol): Unit = synchronized {
    require(!startTime.contains(name),
      s"Trying to measure $name, but measurement of $name has already begun.")
    startTime += name -> System.nanoTime()
  }

  def endMeasurement(name: Symbol): Unit = synchronized {
    require(startTime.contains(name),
      s"Trying to measure runtime for $name, but no such measurement is currently running.")
    collectMeasurement(name, System.nanoTime() - startTime(name))
    startTime -= name
  }

  private def collectMeasurement(name: Symbol, elapsedNanos: Long) = synchronized {
    measurements.get(name) match {
      case Some(buffer) => buffer += elapsedNanos
      case None => measurements += name -> ArrayBuffer(elapsedNanos)
    }
  }

  def compileReport: String = {
    val header = List("Section", "Count", "Mean", "Min", "Max")
    val rows =
      for {
        (name, data) <- measurements
        statistics = computeStatistics(data)
      } yield name.toString.tail :: toRow(statistics)
    asciiTable(header :: rows.toList)
  }

  private def computeStatistics(measurements: Seq[Long]) = Statistics(
    n = measurements.size,
    mean = toMillis(measurements.sum) / measurements.size,
    min = toMillis(measurements.min),
    max = toMillis(measurements.max)
  )

  private def toRow(data: Statistics) = List(
    f"${data.n}", f"${data.mean}%.3f", f"${data.min}%.3f", f"${data.max}%.3f"
  )

  private def toMillis(nanos: Long): Millis = nanos * 0.000001

  private case class Statistics(n: Int, mean: Millis, min: Millis, max: Millis)

  private def asciiTable(rows: List[Seq[String]]): String = {
    require(rows.map(_.length).toSet.size == 1, "All rows must have the same length.")
    val n = rows.head.size
    val lengths = rows.map(_.map(_.length))
    val colWidths = lengths.fold(Seq.fill(n)(0))((acc, row) => seqMax(acc, row))
    val rowStrings =
      for {
        row <- rows
        paddedRow = for ((entry, colWidth) <- row zip colWidths) yield rightPad(entry, colWidth)
      } yield paddedRow.mkString(" | ")
    val dividingLine = colWidths.map("-" * _).mkString("-+-")
    (rowStrings.head :: dividingLine :: rowStrings.tail).mkString("\n")
  }

  private def rightPad(string: String, width: Int): String = {
    val padding = " " * (width - string.length)
    s"$string$padding"
  }


  private def seqMax(seqA: Seq[Int], seqB: Seq[Int]): Seq[Int] =
    for ((a, b) <- seqA zip seqB)
      yield math.max(a, b)
}

