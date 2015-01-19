package robowars.graphics.application

import java.awt.{TextField, BorderLayout}
import java.awt.event.{KeyEvent, KeyListener, WindowEvent, WindowAdapter}
import javax.media.opengl.awt.GLCanvas
import javax.media.opengl.{GLCapabilities, GLProfile}
import javax.swing.JFrame

import com.jogamp.opengl.util.FPSAnimator
import robowars.graphics.engine.RenderFrame
import robowars.graphics.engine.RenderFrame._

object DrawingCanvas {
  def main(args: Array[String]): Unit = {


    // Setup code
    GLProfile.initSingleton()
    val glp = GLProfile.getDefault
    val caps = new GLCapabilities(glp)
    val canvas = new GLCanvas(caps)

    canvas.addGLEventListener(RenderFrame)



    val jframe = new JFrame("Graphics engine drawing canvas")
    jframe.addWindowListener( new WindowAdapter {
      override def windowClosing(windowEvent: WindowEvent): Unit = {
        jframe.dispose()
        System.exit(0)
      }
    })

    jframe.getContentPane.add(canvas, BorderLayout.CENTER)

    textField = new TextField("FPS: -")
    jframe.getContentPane.add(textField, BorderLayout.NORTH)

    jframe.setSize(1920, 1080)
    jframe.setVisible(true)


    canvas.addKeyListener(new KeyListener {
      val moveSpeed = 100
      val zoomSpeed = 0.2f

      override def keyTyped(keyEvent: KeyEvent): Unit = println(s"keyTyped($keyEvent)")

      override def keyPressed(keyEvent: KeyEvent): Unit = keyEvent.getKeyCode match {
        case 37 /* LEFT */ => camera.x -= moveSpeed * camera.zoomFactor
        case 39 /* RIGHT */ => camera.x += moveSpeed * camera.zoomFactor
        case 38 /* UP */ => camera.y += moveSpeed * camera.zoomFactor
        case 40 /* DOWN */ => camera.y -= moveSpeed * camera.zoomFactor
        case 33 /* PAGE UP */ => camera.zoom -= zoomSpeed
        case 34 /* PAGE DOWN */ => camera.zoom += zoomSpeed
        case _ =>
      } //println(s"keyPressed($keyEvent)")

      override def keyReleased(keyEvent: KeyEvent): Unit = println(s"keyReleased($keyEvent)")
    })

    new FPSAnimator(canvas, 1000).start()
    canvas.transferFocus()
  }
}