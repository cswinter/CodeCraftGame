package cwinter.codecraft.core.api

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExport


@JSExport
class JSDroneController extends DroneControllerBase {
  private[this] var _nativeController: js.Dynamic = null

  private[this] def nativeFun(name: String): Option[js.Dynamic] = {
    if (_nativeController == null) None
    else {
      val field = _nativeController.selectDynamic(name)
      if (js.typeOf(field) == "function") Some(field)
      else None
    }
  }


  def nativeController = _nativeController
  def nativeController_=(value: js.Dynamic): Unit = {
    _nativeController = value
    _nativeController.drone = this.asInstanceOf[js.Any]
  }

  override def onSpawn(): Unit = {
    nativeFun("onSpawn").foreach(onSpawn => onSpawn())
  }

  override def onDeath(): Unit = {
    nativeFun("onDeath").foreach(onDeath => onDeath())
  }

  override def onTick(): Unit = {
    nativeFun("onTick").foreach(onTick => onTick())
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = {
    nativeFun("onMineralEntersVision").foreach(onEntersVision => onEntersVision(mineralCrystal.asInstanceOf[js.Any]))
  }

  override def onDroneEntersVision(drone: Drone): Unit = {
    nativeFun("onDroneEntersVision").foreach(onEntersVision => onEntersVision(drone.asInstanceOf[js.Any]))
  }

  override def onArrivesAtPosition(): Unit = {
    nativeFun("onArrivesAtPosition").foreach(f => f())
  }

  override def onArrivesAtMineral(mineralCrystal: MineralCrystal): Unit = {
    nativeFun("onArrivesAtMineral").foreach(f => f(mineralCrystal.asInstanceOf[js.Any]))
  }

  override def onArrivesAtDrone(drone: Drone): Unit = {
    nativeFun("onArrivesAtDrone").foreach(f => f(drone.asInstanceOf[js.Any]))
  }

  /**
   * Gets all mineral crystals stored by this drone.
   */
  @JSExport
  def storedMinerals: js.Array[MineralCrystal] = super.storedMineralsScala.toJSArray

  /**
   * Gets all drones currently within the sight radius of this drone.
   */
  @JSExport
  def dronesInSight: js.Array[Drone] = super.dronesInSightScala.toJSArray
}
