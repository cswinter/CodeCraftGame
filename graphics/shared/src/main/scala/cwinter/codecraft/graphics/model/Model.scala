package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.{DilationXYMatrix4x4, IdentityMatrix4x4, Matrix4x4}

import scala.annotation.tailrec


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
  def setVertexCount(n: Int): Unit

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit

  def hasMaterial(material: GenericMaterial): Boolean

  def vertexCount: Int

  def scalable(transpose: Boolean = false): ScalableModel[T] = new ScalableModel(this, transpose)
  def identityModelview: IdentityModelviewModel[T] = new IdentityModelviewModel[T](this)
}


class EmptyModel[T] extends Model[T] {
  def update(params: T) = ()
  def setVertexCount(n: Int) = ()
  def draw(modelview: Matrix4x4, material: GenericMaterial) = ()
  def hasMaterial(material: GenericMaterial) = false
  def vertexCount = 0
}

object EmptyModel extends Model[Unit] {
  def update(params: Unit) = ()
  def setVertexCount(n: Int) = ()
  def draw(modelview: Matrix4x4, material: GenericMaterial) = ()
  def hasMaterial(material: GenericMaterial) = false
  def vertexCount = 0
}


class ScalableModel[T](val model: Model[T], transpose: Boolean = false) extends Model[(T, Float)] {
  private[this] var scale = 1.0f

  def update(params: (T, Float)): Unit = {
    model.update(params._1)
    scale = params._2
  }

  def setVertexCount(n: Int): Unit = model.setVertexCount(n)

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = {
    val scaledModelview =
      if (transpose) modelview * new DilationXYMatrix4x4(scale)
      else new DilationXYMatrix4x4(scale) * modelview
    model.draw(scaledModelview, material)
  }

  override def hasMaterial(material: GenericMaterial): Boolean = model.hasMaterial(material)

  def vertexCount = model.vertexCount
}

class ImmediateModeModel extends Model[Seq[Model[Unit]]] {
  private[this] var models = new StaticCompositeModel(Seq())

  override def update(params: Seq[Model[Unit]]): Unit =
    models = new StaticCompositeModel(params)

  override def setVertexCount(n: Int): Unit =
    models.setVertexCount(n)

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    models.draw(modelview, material)

  override def vertexCount: Int = models.vertexCount

  override def hasMaterial(material: GenericMaterial): Boolean = models.hasMaterial(material)
}

case class IsHidden(value: Boolean) extends AnyVal

class HideableModel[T](val model: Model[T]) extends Model[(IsHidden, T)] {
  private[this] var show = true

  def update(params: (IsHidden, T)): Unit = {
    val (isHidden, baseParams) = params
    show = !isHidden.value
    if (show)
      model.update(baseParams)
  }

  def setVertexCount(n: Int): Unit =
    if (show) model.setVertexCount(n)

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    if (show)
      model.draw(modelview, material)

  def hasMaterial(material: GenericMaterial): Boolean =
    model.hasMaterial(material)

  def vertexCount = model.vertexCount
}


class IdentityModelviewModel[T](val model: Model[T]) extends Model[T] {
  def update(params: T): Unit =
    model.update(params)

  def setVertexCount(n: Int): Unit = model.setVertexCount(n)

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    model.draw(IdentityMatrix4x4, material)

  def hasMaterial(material: GenericMaterial): Boolean =
    model.hasMaterial(material)

  def vertexCount = model.vertexCount
}


class DynamicModel[T](val modelFactory: T => Model[Unit]) extends Model[T] {
  private[this] var model: Model[Unit] = null

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    model.draw(modelview, material)

  def setVertexCount(n: Int): Unit =
    model.setVertexCount(n)

  def hasMaterial(material: GenericMaterial): Boolean =
    model == null || model.hasMaterial(material)

  def update(params: T) = {
    model = modelFactory(params)
  }

  def vertexCount = model.vertexCount
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

  def setVertexCount(n: Int): Unit = {
    assert(n >= 0)

    @tailrec
    def _setVertexCount(remaining: Int, models: Seq[Model[_]]): Unit = {
      models.headOption match {
        case Some(model) =>
          val count = model.vertexCount
          val allocated = math.min(count, remaining)
          model.setVertexCount(allocated)
          _setVertexCount(remaining - allocated, models.tail)
        case None =>
      }
    }

    _setVertexCount(n, models)
  }

  // TODO: make more efficient (keep set of all materials?)
  def hasMaterial(material: GenericMaterial): Boolean =
    models.exists(_.hasMaterial(material))

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    for {
      model <- models
      if model.hasMaterial(material)
    } model.draw(modelview, material)

  def vertexCount = models.map(_.vertexCount).sum
}

class StaticCompositeModel(val models: Seq[Model[Unit]]) extends CompositeModel[Unit] {
  def update(params: Unit): Unit = { }
}
