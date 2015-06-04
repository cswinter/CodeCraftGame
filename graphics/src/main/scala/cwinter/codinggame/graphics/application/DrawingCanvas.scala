package cwinter.codinggame.graphics.application

import java.awt.{TextField, BorderLayout}
import java.awt.event.{KeyEvent, KeyListener, WindowEvent, WindowAdapter}
import javax.media.opengl.awt.GLCanvas
import javax.media.opengl.{GLCapabilities, GLProfile}
import javax.swing.JFrame

import com.jogamp.opengl.util.FPSAnimator
import cwinter.codinggame.graphics.engine.RenderFrame
import RenderFrame._
import cwinter.codinggame.worldstate.Simulator

object DrawingCanvas {
  def run(gameWorld: Simulator, fps: Int = 60): Unit = {
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
    jframe.setResizable(true)
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
        case 82 /* R */ =>
          if (gameWorld.framerateTarget >= 30) gameWorld.framerateTarget -= 10
          else if (gameWorld.framerateTarget >= 15) gameWorld.framerateTarget -= 5
          else if (gameWorld.framerateTarget > 1) gameWorld.framerateTarget -= 1
        case 70 /* F */ =>
          if (gameWorld.framerateTarget < 10) gameWorld.framerateTarget += 1
          else if (gameWorld.framerateTarget < 30) gameWorld.framerateTarget += 5
          else gameWorld.framerateTarget += 10
        case 32 /* SPACEBAR */ => gameWorld.togglePause()
        case 77 => jframe.setBounds(0, 0, 3840, 2160)
        case 80 /* P */ =>
          if (gameWorld.framerateTarget == 5) gameWorld.framerateTarget = 30
          else gameWorld.framerateTarget = 5
        case _ => gameWorld.handleKeypress(keyEvent)
      }

      override def keyReleased(keyEvent: KeyEvent): Unit = ()
    })

    new FPSAnimator(canvas, fps).start()
    canvas.transferFocus()
  }
}