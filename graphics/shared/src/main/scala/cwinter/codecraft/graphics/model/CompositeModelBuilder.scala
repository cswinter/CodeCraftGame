package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.maths.{Vertex, VertexXYZ}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


private[graphics] trait CompositeModelBuilder[TStatic <: AnyRef, TDynamic]
extends ModelBuilder[TStatic, TDynamic] {
  protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, TDynamic]])

  def subcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, TDynamic]]) =
    if (isCacheable) TheCompositeModelBuilderCache.getOrElseUpdate(signature)(optimized.buildSubcomponents)
    else buildSubcomponents

  def buildModel: Model[TDynamic] = {
    val (staticComponents, dynamicComponents) = subcomponents
    decorate(CompositeModel(
      staticComponents.map(_.getModel),
      dynamicComponents.map(_.getModel)
    ))
  }

  protected def decorate(model: Model[TDynamic]): Model[TDynamic] = model


  override def optimized: CompositeModelBuilder[TStatic, TDynamic] = {
    val (allStatic, allDynamic) = flatten

    val compactedStatic = compact(allStatic)

    val self = this
    new CompositeModelBuilder[TStatic, TDynamic] {
      override def decorate(model: Model[TDynamic]) = self.decorate(model)
      override def signature = self.signature
      override def buildSubcomponents = (compactedStatic, allDynamic)
    }
  }

  def flatten: (mutable.UnrolledBuffer[ModelBuilder[_, Unit]], mutable.UnrolledBuffer[ModelBuilder[_, TDynamic]]) = {
    val allStatic = mutable.UnrolledBuffer.empty[ModelBuilder[_, Unit]]
    val allDynamic = mutable.UnrolledBuffer.empty[ModelBuilder[_, TDynamic]]

    val (staticComponents, dynamicComponents) = buildSubcomponents

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
    val (compactable, other) =
      models
        .partition(isCompactable)
        .asInstanceOf[(Seq[ModelBuilder[_, TDynamic]], Seq[ModelBuilder[_, Unit]])]

    val groupedCompactables =
      compactable.groupBy {
        case p: PrimitiveModelBuilder[_, _, _] => p.material
        case c: VertexCollectionModelBuilder[_] => c.material
        case x => throw new Exception(s"Trying to compact model builder of type ${x.getClass}")
      }

    val compactedPrimitives =
      for ((material, compactables) <- groupedCompactables) yield {
        if (compactables.size == 1) compactables.head
        else {
          val allVertices = ListBuffer.empty[Seq[(VertexXYZ, Vertex)]]
          for (compactable <- compactables) compactable match {
            case p: PrimitiveModelBuilder[_, _, _] =>
              allVertices.append(p.getVertexData)
            case c: VertexCollectionModelBuilder[_] =>
              for (vertices <- c.vertexData)
                allVertices.append(vertices)
          }
          VertexCollectionModelBuilder(allVertices.toList, material.asInstanceOf[Material[VertexXYZ, Vertex, Unit]])
        }
      }

    compactedPrimitives.toSeq.asInstanceOf[Seq[ModelBuilder[_, Unit]]] ++ other
  }

  private def isCompactable(modelBuilder: ModelBuilder[_, Unit]): Boolean =
    modelBuilder.isInstanceOf[PrimitiveModelBuilder[_, _ , Unit]] ||
    modelBuilder.isInstanceOf[VertexCollectionModelBuilder[_]]
}

