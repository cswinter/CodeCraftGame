package cwinter.codecraft.util.modules

import cwinter.codecraft.util.maths.{NullVectorXY, VertexXY, Geometry}


private[codecraft] object ModulePosition {

  def apply(droneSize: Int, moduleIndex: Int): VertexXY = {
    ModulePosition(droneSize)(moduleIndex)
  }

  def apply(droneSize: Int, moduleIndex: Seq[Int]): Seq[VertexXY] = {
    moduleIndex.map(this(droneSize, _))
  }

  def center(droneSize: Int, modulePositions: Seq[Int]): VertexXY = {
    modulePositions.map(i => this(droneSize, i)).reduce(_ + _) / modulePositions.size
  }


  private val energyPositions = Seq(VertexXY(0, 0)) ++ Geometry.polygonVertices2(6, radius = 4.5f)
  def energyPosition(i: Int): VertexXY = {
    energyPositions(i)
  }

  //noinspection ZeroIndexToHead
  val ModulePosition = Map[Int, IndexedSeq[VertexXY]](
    3 -> IndexedSeq(VertexXY(0, 0)),

    4 -> IndexedSeq(VertexXY(9, 9), VertexXY(-9, -9)),

    5 -> Geometry.polygonVertices2(4, radius = 17),

    6 -> permutation(
      Geometry.polygonVertices2(6, radius = 25) :+ NullVectorXY,
      IndexedSeq(1, 0, 5, 6, 4, 3, 2)
    ),

    7 -> permutation(
      Geometry.polygonVertices2(7, radius = 33) ++
        Geometry.polygonVertices(3, orientation = math.Pi.toFloat, radius = 13),
      IndexedSeq(0, 1, 2, 8, 3, 9, 4, 5, 7, 6)
    )
  )

  def moduleCount(size: Int): Int = ModulePosition(size).length

  def size(moduleCount: Int): Int = ModulePosition.filter(_._2.length >= moduleCount).minBy(_._2.length)._1

  final val MaxModules = ModulePosition.map(_._2.length).max

  private def permutation[T](set: IndexedSeq[T], indices: IndexedSeq[Int]): IndexedSeq[T] = {
    IndexedSeq.tabulate(set.size)(i => set(indices(i)))
  }
}

