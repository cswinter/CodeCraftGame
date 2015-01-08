package robowars.graphics.application

import java.awt.BorderLayout
import javax.media.opengl.awt.GLCanvas
import javax.media.opengl.{GLCapabilities, GLProfile}

import com.jogamp.opengl.util.FPSAnimator

import scala.swing.{MainFrame, SwingApplication}


object HelloWorld extends SwingApplication {

  def startup(args: Array[String]): Unit = {

    GLProfile.initSingleton()
    val glp = GLProfile.getDefault
    val caps = new GLCapabilities(glp)
    val canvas = new GLCanvas(caps)
    canvas.addGLEventListener(MyGLEventListener)

    val frame = new MainFrame()
    frame.resizable = true
    frame.peer.setSize(1920, 1080)
    frame.peer.add(canvas, BorderLayout.CENTER)
    frame.peer.setVisible(true)

    val animator = new FPSAnimator(canvas, 60)
    animator.start()
  }
}
