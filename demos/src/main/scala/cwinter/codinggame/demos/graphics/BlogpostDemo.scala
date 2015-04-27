package cwinter.codinggame.demos.graphics

import cwinter.worldstate.WorldObjectDescriptor

object BlogpostDemo {
  def main(args: Array[String]): Unit = {
    val s = new GraphicsSimulator(
      customObjects = Seq.empty,
      customChangingObjects = generateObjects
    )
    s.run()
  }


  def generateObjects(t: Int): Seq[WorldObjectDescriptor] = {
    Seq()
  }
}
