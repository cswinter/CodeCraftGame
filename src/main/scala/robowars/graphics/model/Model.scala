package robowars.graphics.model

import robowars.graphics.matrices.Matrix4x4
import robowars.worldstate.MineralObject


trait Model[T] {
  def update(params: T): Unit
  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit
  def hasMaterial(material: GenericMaterial): Boolean
}


trait ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic

  def getModel: Model[TDynamic] = {
    TheModelCache.getOrElseUpdate(signature)(buildModel)
  }

  protected def buildModel: Model[TDynamic]
}


trait CompositeModel[T] <: Model[T] {
  def models: Seq[Model[_]]
  def update(params: T): Unit

  // TODO: make more efficient (keep set of all materials?)
  def hasMaterial(material: GenericMaterial): Boolean =
    models.exists(_.hasMaterial(material))

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    for {
      model <- models
      if model.hasMaterial(material)
    } model.draw(modelview, material)
}

/*
case class MineralSize(size: Int)

class MineralModelBuilder(mineral: MineralObject) extends ModelBuilder[MineralSize, MineralObject] {
  val signature = MineralSize(mineral.size)

  override protected def buildModel: Model[MineralObject] = {
    val model1 = ???

    new MineralObjectModel(model1)
  }
}

class MineralObjectModel(val model1: Model[Int]) extends CompositeModel[MineralObject] {
  val models = Seq(model1)

  override def update(params: MineralObject): Unit = {
    model1.update(params.time)
  }
}*/
