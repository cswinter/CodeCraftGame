package cwinter.codinggame.demos.graphics

import cwinter.worldstate._

import scala.util.Random

object Generators {
  def rnd() = Random.nextDouble().toFloat
  def rnd(min: Float, max: Float): Float = {
    assert(min < max, "Cannot have min >= max.")
    rnd() * (max - min) + min
  }

  def rni(n: Int) = if (n <= 0) 0 else Random.nextInt(n)

  def rnd[T](elems: (Int, T)*): T = {
    val totalWeight = elems.map(_._1).sum
    val r = rni(totalWeight)
    var cumulativeWeight = 0
    var i = -1
    do {
      i += 1
      cumulativeWeight += elems(i)._1
    } while (cumulativeWeight < r)
    elems(i)._2
  }

  def randomModule(position: Int) = rnd(
    50 -> StorageModule(Seq(position), rni(9) - 1),
    2 -> Lasers(position, rni(4)),
    2 -> Engines(position),
    2 -> ShieldGenerator(position),
    6 -> ProcessingModule(Seq(position))
  )

  val ModuleCount = Map(3 -> 1, 4 -> 2, 5 -> 4, 6 -> 7, 7 -> 10).withDefaultValue(0)
  def randomModules(n: Int) = {
    Seq.tabulate(ModuleCount(n))(i => randomModule(i))
  }
}
