package cwinter.codinggame.core.replay

import java.io.FileWriter

class FileReplayRecorder(filename: String) extends ReplayRecorder {
  val replay = new StringBuilder
  val writer = new FileWriter(filename)


  protected override def writeLine(string: String): Unit = {
    writer.write(string + "\n")
    writer.flush()
  }
}
