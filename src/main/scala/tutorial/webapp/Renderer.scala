package tutorial.webapp

import org.scalajs.dom.raw.WebGLRenderingContext._
import org.scalajs.dom.raw.{HTMLImageElement, UIEvent}
import org.scalajs.dom.{html, raw, _}

import scala.collection.mutable
import scala.util.Random

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

    val r = new Random()
    val map = (-6 to 6).flatMap { x => (-4 to 4).map { y => ((x, y), r.nextInt(4)) } }.toMap
    console.log(map.toString)

    {
      val program = new ProgramAll()
      var dx = -.5f
      var dy = -.5f
      var dxa = dx
      var dya = dy
      var dza = 0f
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
        handledCommands ++= hotCommands

        var dz = 5f - map.getOrElse((-dx.ceil.toInt, -dy.ceil.toInt), 4)
        dxa -= (dxa - dx) * 0.1f
        dya -= (dya - dy) * 0.1f
        dza -= (dza - dz) * 0.1f

        val projectionMatrix = new Matrix4(30, canvas.width.toFloat / canvas.height, .1f, 10)
        val viewMatrix = new Matrix4(dxa, dya, dza)
        program.render(projectionMatrix, viewMatrix)
      }

      program.buildGrid(map, Seq(images("stucco.jpg"), images("stone.jpg"), images("metal.jpg"), images("scratched.jpg")))
    }

    resize()

    def requestAnimation(): Unit = {
      render()
      document.defaultView.requestAnimationFrame { d: Double => requestAnimation() }
    }
    requestAnimation()
  }
}
