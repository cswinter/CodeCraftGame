package cwinter.graphics.application

import java.awt.{TextField, BorderLayout}
import java.awt.event.{KeyEvent, KeyListener, WindowEvent, WindowAdapter}
import javax.media.opengl.awt.GLCanvas
import javax.media.opengl.{GLCapabilities, GLProfile}
import javax.swing.JFrame

import com.jogamp.opengl.util.FPSAnimator
import cwinter.graphics.engine.RenderFrame
import cwinter.graphics.engine.RenderFrame._
import cwinter.worldstate.GameWorld

object DrawingCanvas {
  def run(gameWorld: GameWorld): Unit = {
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

      override def keyTyped(keyEvent: KeyEvent): Unit = ()

      override def keyPressed(keyEvent: KeyEvent): Unit = keyEvent.getKeyCode match {
        case 37 | 65 /* LEFT, A */ => camera.x -= moveSpeed * camera.zoomFactor
        case 39 | 68 /* RIGHT, D */ => camera.x += moveSpeed * camera.zoomFactor
        case 38 | 87 /* UP, W */ => camera.y += moveSpeed * camera.zoomFactor
        case 40 | 83 /* DOWN, S */ => camera.y -= moveSpeed * camera.zoomFactor
        case 33 | 81 /* PAGE UP, Q */ => camera.zoom -= zoomSpeed
        case 34 | 69 /* PAGE DOWN, E */ => camera.zoom += zoomSpeed
        case 32 /* SPACEBAR */ => RenderFrame.togglePause()
        case _ =>
      } //println(s"keyPressed($keyEvent)")

      override def keyReleased(keyEvent: KeyEvent): Unit = ()
    })

    new FPSAnimator(canvas, 60).start()
    canvas.transferFocus()
  }
}