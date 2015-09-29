package cwinter.codecraft.core.api

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExport


@JSExport
class JSDroneController(
  private[this] var _errorHandler: Option[(Throwable, String, JSDroneController) => Unit] = None,
  private[this] var _nativeControllerName: String = ""
) extends DroneControllerBase {
  private[this] var _nativeController: js.Dynamic = null
  def errorHandler_=(value: (Throwable, String, JSDroneController) => Unit) = _errorHandler = Some(value)

  def nativeController = _nativeController
  def updateController(controller: js.Dynamic, controllerName: String): Unit = {
    _nativeController = controller
    _nativeController.drone = this.asInstanceOf[js.Any]
    _nativeControllerName = controllerName
  }

  def controllerName = _nativeControllerName

  private[this] def callNativeFun(name: String, args: js.Any*): Unit = {
    if (_nativeController != null) {
      val field = _nativeController.selectDynamic(name)
      if (js.typeOf(field) == "function") {
        try {
          field.asInstanceOf[js.Function].call(_nativeController, args: _*)
        } catch {
          case e: Throwable =>
            drone.error(s"Exception thrown in $name, see console for details.")
            _errorHandler.foreach(_(e, name, this))
        }
      }
    }
  }

  @JSExport
  @deprecated("Use buildDrone(controllerName: String, spec: js.Dynamic).", "0.11.1.0")
  def buildDrone(controller: JSDroneController, spec: js.Dynamic): Unit = {
    if (!JSDroneController.buildDroneDeprWarnShown) {
      val errmsg = "buildDrone(controller: JSDroneController, spec: js.Dynamic) has been deprecated. " +
        "Use the more convenient buildDrone(controllerName: String, spec: js.Dynamic) instead, " +
        "which allows you to eliminate the call to Game.getController."
      Console.err.println(errmsg)
      drone.warn(errmsg)
      JSDroneController.buildDroneDeprWarnShown = true
    }

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

  @JSExport
  def buildDrone(controllerName: String, spec: js.Dynamic): Unit = {
    def getOrElse0(fieldName: String): Int = {
      val value = spec.selectDynamic(fieldName)
      //noinspection ComparingUnrelatedTypes,TypeCheckCanBeMatch
      if (value.isInstanceOf[Int]) value.asInstanceOf[Int]
      else 0
    }

    val controller = JSDroneController.droneControllerProvider(controllerName)
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


@JSExport
object JSDroneController {
  private var buildDroneDeprWarnShown = false

  var droneControllerProvider: String => DroneControllerBase = null
}