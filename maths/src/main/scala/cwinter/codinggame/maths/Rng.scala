package cwinter.codinggame.maths

object Rng {
  private[this] val random = scala.util.Random
  val seed = 83//scala.util.Random.nextInt(100)
  println(s"Rng seed: $seed")
  scala.util.Random.setSeed(seed)


  def int(min: Int, max: Int): Int = {
    assert(min <= max)
    random.nextInt(max - min + 1) + min
  }
  
  def bernoulli(p: Double): Boolean = {
    assert(p >= 0)
    assert(p <= 1)
    random.nextDouble() <= p
  }
  
  def vector2(size: Float = 1): Vector2 = {
    val direction = 2 * math.Pi * random.nextFloat()
    size * Vector2(direction)
  }

  def vector2(xMin: Float, xMax: Float, yMin: Float, yMax: Float): Vector2 = {
    Vector2(float(xMin, xMax), float(yMin, yMax))
  }

  def float(min: Float, max: Float): Float = double(min, max).toFloat

  def double(min: Double, max: Double): Double = {
    assert(min <= max)
    random.nextDouble() * (max - min) + min
  }
}
