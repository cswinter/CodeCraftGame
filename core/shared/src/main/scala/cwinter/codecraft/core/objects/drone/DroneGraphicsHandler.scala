package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.graphics.{CollisionMarkerModel, DroneModuleDescriptor, DroneModelParameters, DroneModel}
import cwinter.codecraft.graphics.engine.{PositionDescriptor, ModelDescriptor}
import cwinter.codecraft.util.maths.{Vector2, Float0To1}


private[codecraft] trait DroneGraphicsHandler { self: DroneImpl =>
  import DroneGraphicsHandler._
  private[this] val oldPositions = collection.mutable.Queue.empty[(Float, Float, Float)]
  private[this] var cachedDescriptor: Option[DroneModel] = None
  private[this] var _collisionMarkers = List.empty[(CollisionMarkerModel, Float)]


  override def descriptor: Seq[ModelDescriptor[_]] = {
    val positionDescr =
      PositionDescriptor(
        position.x,
        position.y,
        dynamics.orientation
      )
    val harvestBeams =
      for {
        s <- storage
        d <- s.beamDescriptor
      } yield ModelDescriptor(positionDescr, d)
    val constructionBeams =
      for {
        m <- manipulator
        d <- m.beamDescriptor
      } yield ModelDescriptor(positionDescr, d)

    Seq(
      ModelDescriptor(
        positionDescr,
        cachedDescriptor.getOrElse(recreateDescriptor()),
        modelParameters
      )
    ) ++ storage.toSeq.flatMap(_.energyGlobeAnimations) ++ harvestBeams.toSeq ++ constructionBeams.toSeq ++
      _collisionMarkers.map(cm => ModelDescriptor(positionDescr, cm._1, cm._2 / CollisionMarkerLifetime))
  }

  private def recreateDescriptor(): DroneModel = {
    val newDescriptor =
      DroneModel(
        spec.sides,
        moduleDescriptors,
        shieldGenerators.nonEmpty,
        hullState,
        constructionProgress.nonEmpty,
        if (spec.engines > 0 && context.settings.allowModuleAnimation && constructionProgress.isEmpty)
          context.simulator.timestep % 100
        else 0,
        player.color
      )
    cachedDescriptor = Some(newDescriptor)
    newDescriptor
  }

  private def modelParameters = DroneModelParameters(
    shieldGenerators.map(_.hitpointPercentage),
    constructionProgress.map(p => Float0To1(p / spec.buildTime.toFloat))
  )

  private def moduleDescriptors: Seq[DroneModuleDescriptor] = {
    for {
      Some(m) <- droneModules
      descr <- m.descriptors
    } yield descr
  }

  private[drone] def invalidateModelCache(): Unit = cachedDescriptor = None

  protected def recordPosition(): Unit = {
    oldPositions.enqueue((position.x, position.y, dynamics.orientation))
    if (oldPositions.length > NJetPositions) oldPositions.dequeue()
  }

  protected def addCollisionMarker(collisionPosition: Vector2): Unit = {
    if (context.settings.allowCollisionAnimation) {
      val collisionAngle = (collisionPosition - position).orientation - dynamics.orientation
      _collisionMarkers ::= ((
        CollisionMarkerModel(radius, collisionAngle.toFloat),
        CollisionMarkerLifetime))
      invalidateModelCache()
    }
  }

  protected def updateCollisionMarkers(): Unit = {
    _collisionMarkers = for (
      (model, lifetime) <- _collisionMarkers
      if lifetime > 0
    ) yield (model, lifetime - 1)
  }
}

private[core] object DroneGraphicsHandler {
  val CollisionMarkerLifetime = 50f
  val NJetPositions = 6
}

