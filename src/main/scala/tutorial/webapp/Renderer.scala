package tutorial.webapp

import org.scalajs.dom.raw.WebGLRenderingContext._
import org.scalajs.dom.raw.{HTMLImageElement, UIEvent}
import org.scalajs.dom.{html, raw, _}

import scala.collection.mutable

class Renderer(val images: Map[String, HTMLImageElement]) {
  val canvas = document.createElement("canvas").asInstanceOf[html.Canvas]
  document.body.appendChild(canvas)
  document.defaultView.onresize = { e: UIEvent => resize }
  implicit val gl: raw.WebGLRenderingContext = canvas.getContext("webgl").asInstanceOf[raw.WebGLRenderingContext]
  gl.getContextAttributes()

  val drawables = mutable.Buffer[() => Unit]()

  def render(): Unit = {
    gl.clear(COLOR_BUFFER_BIT | DEPTH_BUFFER_BIT)
    drawables.foreach(d => d())
  }

  def resize(): Unit = {
    canvas.width = document.body.clientWidth
    canvas.height = document.body.clientHeight
    gl.viewport(0, 0, canvas.width, canvas.height)
  }

  def start(): Unit = {
    val activeCommands = new mutable.HashSet[String]()

    {
      val codesToCommands = Map(87 -> "up", 65 -> "left", 83 -> "down", 68 -> "right")
      document.onkeydown = { e: KeyboardEvent =>
        console.log(e.keyCode)
        val command = codesToCommands.getOrElse(e.keyCode, "")
        console.log(command)
        activeCommands += command
        console.log(activeCommands.toString())
      }
      document.onkeyup = { e: KeyboardEvent =>
        console.log(e.keyCode)
        val command = codesToCommands.getOrElse(e.keyCode, "")
        console.log(command)
        activeCommands -= command
        console.log(activeCommands.toString())
      }
    }

    gl.clearColor(0.0, 0.0, 0.0, 1.0)
    gl.enable(CULL_FACE)
    gl.enable(DEPTH_TEST)

    // todo: macro generate ProgramX from its uniforms/attributes and shader sources
    {
      val program = new ProgramAll()
      var dx = 0f
      var dy = 0f
      drawables += { () =>
        if (activeCommands.contains("left"))
          dx -= 0.01f
        if (activeCommands.contains("right"))
          dx += 0.01f
        if (activeCommands.contains("down"))
          dy -= 0.01f
        if (activeCommands.contains("up"))
          dy += 0.01f
        val projectionMatrix = new Matrix4(45, canvas.width.toFloat / canvas.height, .1f, 10)
        val viewMatrix = new Matrix4(dx, dy, 4)
        program.render(projectionMatrix, viewMatrix)
      }

      program.newDrawable(images("stucco.jpg"))
        .buildSquare(0, 0, 0)
        .compile

      program.newDrawable(images("stone.jpg"))
        .buildSquare(0, 1, 1)
        .buildSquare(1, 0, 1)
        .buildSquare(1, 1, 1)
        .buildSquare(-1, 1, 1)
        .buildSquare(-1, 0, 1)
        .buildSquare(-1, -1, 1)
        .buildSquare(0, -1, 1)
        .buildSquare(1, -1, 1)
        .compile
    }

    resize()

    def requestAnimation(): Unit = {
      render()
      document.defaultView.requestAnimationFrame { d: Double => requestAnimation() }
    }
    requestAnimation()
  }
}
