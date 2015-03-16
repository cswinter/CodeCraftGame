package robowars.graphics.model

import robowars.graphics.matrices._
import robowars.worldstate.WorldObject


class ClosedModel[T](objectState: T, model: Model[T], modelview: Matrix4x4) {
  def draw(material: GenericMaterial): Unit = {
    if (model.hasMaterial(material)) {
      model.update(objectState)
      model.draw(modelview, material)
    }
  }
}


trait Model[T] {
  def update(params: T): Unit

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit

  def hasMaterial(material: GenericMaterial): Boolean


  def scalable: ScalableModel[T] = new ScalableModel(this)
  def identityModelview: IdentityModelviewModel[T] = new IdentityModelviewModel[T](this)
}


class ScalableModel[T](val model: Model[T]) extends Model[(T, Float)] {
  var scale = 1.0f

  def update(params: (T, Float)): Unit = {
    model.update(params._1)
    scale = params._2
  }

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = {
    val scaledModelview = new DilationXYMatrix4x4(scale) * modelview
    model.draw(scaledModelview, material)
  }

  override def hasMaterial(material: GenericMaterial): Boolean = model.hasMaterial(material)
}


class IdentityModelviewModel[T](val model: Model[T]) extends Model[T] {
  def update(params: T): Unit =
    model.update(params)

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    model.draw(IdentityMatrix4x4, material)

  def hasMaterial(material: GenericMaterial): Boolean =
    model.hasMaterial(material)
}


class DynamicModel[T](val modelFactory: T => Model[Unit]) extends Model[T] {
  private[this] var model: Model[Unit] = null

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    model.draw(modelview, material)

  def hasMaterial(material: GenericMaterial): Boolean =
    model == null || model.hasMaterial(material)

  def update(params: T) = {
    model = modelFactory(params)
  }
}


trait ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic

  def getModel: Model[TDynamic] = {
    if (isCacheable) TheModelCache.getOrElseUpdate(signature)(buildModel)
    else buildModel
  }

  protected def buildModel: Model[TDynamic]

  def isCacheable: Boolean = true
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

class StaticCompositeModel(val models: Seq[Model[Unit]]) extends CompositeModel[Unit] {
  def update(params: Unit): Unit = { }
}
