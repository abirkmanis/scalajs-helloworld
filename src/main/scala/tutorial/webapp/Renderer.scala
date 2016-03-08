package tutorial.webapp

import org.scalajs.dom.raw.WebGLRenderingContext._
import org.scalajs.dom.raw.{HTMLImageElement, UIEvent}
import org.scalajs.dom.{html, raw, _}
import tutorial.webapp.NormalAxis._

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
    val handledCommands = new mutable.HashSet[String]()

    {
      val codesToCommands = Map(87 -> "up", 65 -> "left", 83 -> "down", 68 -> "right")
      document.onkeydown = { e: KeyboardEvent =>
        val command = codesToCommands.getOrElse(e.keyCode, "")
        activeCommands += command
      }
      document.onkeyup = { e: KeyboardEvent =>
        val command = codesToCommands.getOrElse(e.keyCode, "")
        activeCommands -= command
        handledCommands -= command
      }
    }

    gl.clearColor(0.0, 0.0, 0.0, 1.0)
    gl.enable(CULL_FACE)
    gl.enable(DEPTH_TEST)

    // todo: macro generate ProgramX from its uniforms/attributes and shader sources
    {
      val program = new ProgramAll()
      var dx = 2f
      var dy = 0f
      var dxa = dx
      var dya = dy
      drawables += { () =>
        val hotCommands = activeCommands -- handledCommands
        if (hotCommands.contains("left"))
          dx += 1
        if (hotCommands.contains("right"))
          dx -= 1
        if (hotCommands.contains("down"))
          dy += 1
        if (hotCommands.contains("up"))
          dy -= 1
        handledCommands ++= activeCommands

        dxa -= (dxa - dx) * 0.1f
        dya -= (dya - dy) * 0.1f

        val projectionMatrix = new Matrix4(45, canvas.width.toFloat / canvas.height, .1f, 10)
        console.log(dx + ", " + dy)
        val viewMatrix = new Matrix4(dxa, dya, 4)
        program.render(projectionMatrix, viewMatrix)
      }

      program.newDrawable(images("stucco.jpg"))
        .buildSquare(0, 0, 0, X)
        .buildSquare(0, 0, 0, Y)
        .buildSquare(0, 0, 0, Z)
        .buildSquare(1, 0, 0, _X)
        .buildSquare(0, 1, 0, _Y)
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

      program.newDrawable(images("metal.jpg"))
        .buildSquare(-2, 1, 2)
        .buildSquare(-2, 0, 2)
        .buildSquare(-2, -1, 2)
        .compile

      program.newDrawable(images("scratched.jpg"))
        .buildSquare(-1, 1, 1, X)
        .buildSquare(-1, 0, 1, X)
        .buildSquare(-1, -1, 1, X)
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
