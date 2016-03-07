package tutorial.webapp.unsupported

import org.scalajs.dom.raw
import org.scalajs.dom.raw.WebGLRenderingContext._
import tutorial.webapp.{Attribute, Color, Vector2}

import scala.scalajs.js.typedarray.Float32Array

class ProgramVPosition2Color(implicit val gl: raw.WebGLRenderingContext) {
  val vShader = gl.createShader(VERTEX_SHADER)
  val vertText = "attribute vec2 position;" +
    "attribute vec4 color;" +
    "varying vec4 vColor;" +
    "void main(){" +
    "gl_Position = vec4(position,-.99,1);" +
    "vColor=color;" +
    "}"
  gl.shaderSource(vShader, vertText)
  gl.compileShader(vShader)

  val fShader = gl.createShader(FRAGMENT_SHADER)
  val fragText = "precision highp float;" +
    "varying vec4 vColor;" +
    "void main(){" +
    "gl_FragColor = vColor;" +
    "}"
  gl.shaderSource(fShader, fragText)
  gl.compileShader(fShader)

  val program = gl.createProgram()
  gl.attachShader(program, vShader)
  gl.attachShader(program, fShader)
  gl.linkProgram(program)

  val position = gl.getAttribLocation(program, "position")
  val color = gl.getAttribLocation(program, "color")

  val drawables = scala.collection.mutable.Buffer[Drawable]()

  case class Drawable(mode: Int, count: Int, position: Attribute, color: Attribute)

  class DrawableBuilder(val mode: Int) {
    val positions = scala.collection.mutable.Buffer[Vector2]()
    val colors = scala.collection.mutable.Buffer[Color]()

    def addVertex(position: Vector2, color: Color): DrawableBuilder = {
      positions += position
      colors += color
      this
    }

    def compile: Unit = {
      val count = positions.size
      val position = new Float32Array(2 * count)
      positions.zipWithIndex.foreach { case (e, i) => e.toArray(position, 2 * i) }
      val color = new Float32Array(4 * count)
      colors.zipWithIndex.foreach { case (e, i) => e.toArray(color, 4 * i) }
      drawables += Drawable(mode, count, new Attribute(new Float32Array(position)), new Attribute(new Float32Array(color)))
    }
  }

  def newDrawable(mode: Int) = new DrawableBuilder(mode)

  def use = {
    gl.useProgram(program)
    gl.enableVertexAttribArray(this.position)
    gl.enableVertexAttribArray(this.color)
  }

  def render() = {
    use

    drawables.foreach { d =>
      gl.bindBuffer(ARRAY_BUFFER, d.position.buffer)
      gl.vertexAttribPointer(this.position, 2, FLOAT, false, d.position.offset, d.position.stride)

      gl.bindBuffer(ARRAY_BUFFER, d.color.buffer)
      gl.vertexAttribPointer(this.color, 4, FLOAT, false, d.color.offset, d.color.stride)

      gl.drawArrays(d.mode, 0, d.count)
    }

    unuse
  }

  def unuse = {
    gl.disableVertexAttribArray(this.position)
    gl.disableVertexAttribArray(this.color)
    gl.useProgram(null)
  }
}
