package cwinter.codecraft.core.replay

import java.io.{File, FileWriter}

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.annotation.tailrec

private[core] class FileReplayRecorder(path: String) extends ReplayRecorder {
  final val format = DateTimeFormat.forPattern("YYMMdd-HHmmss")
  val replay = new StringBuilder
  val dir: Boolean = new File(path).mkdirs()
  val replayFile: File = createNewFile(format.print(new DateTime), ".replay")
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

  def filename: File = replayFile.getAbsoluteFile

  override def replayFilepath = Some(filename.toString)
}
