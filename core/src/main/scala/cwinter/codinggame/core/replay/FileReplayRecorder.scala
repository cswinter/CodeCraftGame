package cwinter.codinggame.core.replay

import java.io.FileWriter
import java.io.File
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.annotation.tailrec

class FileReplayRecorder(path: String) extends ReplayRecorder {
  final val format = DateTimeFormat.forPattern("YYMMdd-HHmmss")
  val replay = new StringBuilder
  val dir = new File(path).mkdirs()
  val replayFile = createNewFile(format.print(new DateTime), ".replay")
  val writer = new FileWriter(replayFile)


  @tailrec final def createNewFile(filenamePrefix: String, filenameSuffix: String, attempt: Int = 0): File = {
    val infix =
      if (attempt == 0) ""
      else s" ($attempt)"
    val f = new File(path + "/" + filenamePrefix + infix + filenameSuffix)
    if (f.createNewFile()) f
    else createNewFile(filenamePrefix, filenameSuffix, attempt + 1)
  }

  protected override def writeLine(string: String): Unit = {
    writer.write(string + "\n")
    writer.flush()
  }

  def filename = replayFile.getAbsoluteFile
}