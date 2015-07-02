package cwinter.codecraft.graphics.application

import java.awt.{TextField, BorderLayout}
import java.awt.event.{KeyEvent, KeyListener, WindowEvent, WindowAdapter}
import javax.media.opengl.awt.GLCanvas
import javax.media.opengl.{GLCapabilities, GLProfile}
import javax.swing.JFrame

import com.jogamp.opengl.util.FPSAnimator
import cwinter.codecraft.graphics.engine._
import RenderFrame._
import cwinter.codecraft.graphics.worldstate.Simulator

object DrawingCanvas {
  def run(gameWorld: Simulator, fps: Int = 60): Unit = {
    println("Initialising...")

    RenderFrame.gameWorld = gameWorld

    // Setup code
    GLProfile.initSingleton()
    val glp = GLProfile.getDefault
    val caps = new GLCapabilities(glp)
    val canvas = new GLCanvas(caps)

    canvas.addGLEventListener(RenderFrame)



    val jframe = new JFrame("Graphics engine drawing canvas")
    jframe.addWindowListener( new WindowAdapter {
      override def windowClosing(windowEvent: WindowEvent): Unit = {
        println("exiting...")
        jframe.dispose()
        System.exit(0)
      }
    })

    jframe.getContentPane.add(canvas, BorderLayout.CENTER)

    textField = new TextField("FPS: -")
    jframe.getContentPane.add(textField, BorderLayout.NORTH)

    jframe.setSize(1920, 1080)
    jframe.setResizable(true)
    jframe.setVisible(true)


    val keyEventHandler = new KeyEventHandler(gameWorld, camera)
    canvas.addKeyListener(new KeyListener {
      override def keyPressed(keyEvent: KeyEvent): Unit = {
        val key = keyEvent.getKeyCode match {
          case 37 => LeftArrow
          case 39 => RightArrow
          case 38 => UpArrow
          case 40 => DownArrow
          case 33 => PageUp
          case 34 => PageDown
          case _ => Letter(keyEvent.getKeyChar)
        }
        keyEventHandler.keypress(key)
      }
      override def keyTyped(keyEvent: KeyEvent): Unit = ()
      override def keyReleased(keyEvent: KeyEvent): Unit = ()
    })

    new FPSAnimator(canvas, fps).start()
    canvas.transferFocus()
  }
}