package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.maths.{VertexXYZ, Vertex}

import scala.collection.mutable


private[graphics] trait CompositeModelBuilder[TStatic, TDynamic]
extends ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic

  protected def build: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, TDynamic]])

  def buildModel: Model[TDynamic] = {
    val (staticComponents, dynamicComponents) = build
    decorate(CompositeModel(
      staticComponents.map(_.getModel),
      dynamicComponents.map(_.getModel)
    ))
  }

  protected def decorate(model: Model[TDynamic]): Model[TDynamic] = model


  override def optimized: ModelBuilder[TStatic, TDynamic] = {
    val (allStatic, allDynamic) = flatten

    val compactedStatic = compact(allStatic)

    val self = this
    new CompositeModelBuilder[TStatic, TDynamic] {
      override def decorate(model: Model[TDynamic]) = self.decorate(model)
      override def signature = self.signature
      override def build = (compactedStatic, allDynamic)
    }
  }

  def flatten: (mutable.UnrolledBuffer[ModelBuilder[_, Unit]], mutable.UnrolledBuffer[ModelBuilder[_, TDynamic]]) = {
    val allStatic = mutable.UnrolledBuffer.empty[ModelBuilder[_, Unit]]
    val allDynamic = mutable.UnrolledBuffer.empty[ModelBuilder[_, TDynamic]]

    val (staticComponents, dynamicComponents) = build

    for (static <- staticComponents) static match {
      case compositeChild: CompositeModelBuilder[_, Unit] =>
        val (static1, static2) = compositeChild.flatten
        allStatic concat static1
        allStatic concat static2
      case other => allStatic append other.optimized
    }

    for (dynamic <- dynamicComponents) dynamic match {
      case compositeChild: CompositeModelBuilder[_, TDynamic] =>
        val (static1, dynamic1) = compositeChild.flatten
        allStatic concat static1
        allDynamic  concat dynamic1
      case other => allDynamic append other.optimized
    }

    (allStatic, allDynamic)
  }

  def compact(models: Seq[ModelBuilder[_, Unit]]): Seq[ModelBuilder[_, Unit]] = {
    val (primitives, other) =
      models
        .partition(model => model.isInstanceOf[PrimitiveModelBuilder[_, _ <: Vertex, Unit]])
        .asInstanceOf[(Seq[PrimitiveModelBuilder[_, _ <: Vertex, Unit]], Seq[ModelBuilder[_, Unit]])]
    val groupedPrimitives =
      primitives.groupBy(_.material)

    val compactedPrimitives =
      for ((material, primitives) <- groupedPrimitives) yield {
        if (primitives.size == 1) primitives.head
        else {
          val allVertices =
            for (p <- primitives) yield p.getVertexData
          VertexCollectionModelBuilder(allVertices, material.asInstanceOf[Material[VertexXYZ, Vertex, Unit]])
        }
      }

    compactedPrimitives.toSeq ++ other
  }
}

