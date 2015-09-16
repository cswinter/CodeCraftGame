package cwinter.codecraft.core.api

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExport


@JSExport
class JSDroneController extends DroneControllerBase {
  private[this] var _nativeController: js.Dynamic = null

  private[this] def callNativeFun(name: String, args: js.Any*): Unit = {
    if (_nativeController != null) {
      val field = _nativeController.selectDynamic(name)
      if (js.typeOf(field) == "function") {
        field.asInstanceOf[js.Function].call(_nativeController, args:_*)
      }
    }
  }


  def nativeController = _nativeController
  def nativeController_=(value: js.Dynamic): Unit = {
    _nativeController = value
    _nativeController.drone = this.asInstanceOf[js.Any]
  }

  @JSExport
  def buildDrone(controller: JSDroneController, spec: js.Dynamic): Unit = {
    def getOrElse0(fieldName: String): Int = {
      val value = spec.selectDynamic(fieldName)
      //noinspection ComparingUnrelatedTypes,TypeCheckCanBeMatch
      if (value.isInstanceOf[Int]) value.asInstanceOf[Int]
      else 0
    }

    buildDrone(
      controller,
      storageModules = getOrElse0("storageModules"),
      missileBatteries = getOrElse0("missileBatteries"),
      refineries = getOrElse0("refineries"),
      constructors = getOrElse0("constructors"),
      engines = getOrElse0("engines"),
      shieldGenerators = getOrElse0("shieldGenerators")
    )
  }

  override def onSpawn(): Unit = callNativeFun("onSpawn")

  override def onDeath(): Unit = callNativeFun("onDeath")

  override def onTick(): Unit = callNativeFun("onTick")

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit =
    callNativeFun("onMineralEntersVision", mineralCrystal.asInstanceOf[js.Any])

  override def onDroneEntersVision(drone: Drone): Unit =
    callNativeFun("onDroneEntersVision", drone.asInstanceOf[js.Any])

  override def onArrivesAtPosition(): Unit =
    callNativeFun("onArrivesAtPosition")

  override def onArrivesAtMineral(mineralCrystal: MineralCrystal): Unit =
    callNativeFun("onArrivesAtMineral", mineralCrystal.asInstanceOf[js.Any])

  override def onArrivesAtDrone(drone: Drone): Unit =
    callNativeFun("onArrivesAtDrone", drone.asInstanceOf[js.Any])

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
