package cwinter.codecraft.util.maths

object Rng {
  val seed = scala.util.Random.nextInt(100000)
  private[this] val random = new scala.util.Random(seed)


  def int(max: Int): Int = {
    random.nextInt(max)
  }

  def int(min: Int, max: Int): Int = {
    assert(min <= max)
    random.nextInt(max - min + 1) + min
  }
  
  def bernoulli(p: Double): Boolean = {
    assert(p >= 0)
    assert(p <= 1)
    random.nextDouble() <= p
  }
  
  def vector2(size: Double = 1): Vector2 = {
    val direction = 2 * math.Pi * random.nextFloat()
    size * Vector2(direction)
  }


  def vector2(xMin: Double, xMax: Double, yMin: Double, yMax: Double): Vector2 = {
    Vector2(double(xMin, xMax), double(yMin, yMax))
  }

  def vector2(bounds: Rectangle): Vector2 = {
    vector2(bounds.xMin, bounds.xMax, bounds.yMin, bounds.yMax)
  }

  def float(min: Float, max: Float): Float = double(min, max).toFloat

  def double(): Double = {
    random.nextDouble()
  }

  def gaussian2D(): Vector2 = Vector2(random.nextGaussian(), random.nextGaussian())

  def double(min: Double, max: Double): Double = {
    assert(min <= max)
    random.nextDouble() * (max - min) + min
  }
}
