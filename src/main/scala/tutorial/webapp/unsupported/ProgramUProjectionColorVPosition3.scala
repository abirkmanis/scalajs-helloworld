package tutorial.webapp.unsupported

import org.scalajs.dom.raw
import org.scalajs.dom.raw.WebGLRenderingContext._
import tutorial.webapp.{Attribute, Color, Matrix4, Vector3}

import scala.scalajs.js.typedarray.Float32Array

class ProgramUProjectionColorVPosition3(implicit val gl: raw.WebGLRenderingContext) {
  val vShader = gl.createShader(VERTEX_SHADER)
  val vertText = "uniform mat4 projectionMatrix;" +
    "uniform mat4 viewMatrix;" +
    "attribute vec3 position;" +
    "void main(){" +
    "gl_Position = projectionMatrix * viewMatrix * vec4(position, 1);" +
    "}"
  gl.shaderSource(vShader, vertText)
  gl.compileShader(vShader)

  val fShader = gl.createShader(FRAGMENT_SHADER)
  val fragText = "precision highp float;uniform vec4 color;void main(){gl_FragColor = color;}"
  gl.shaderSource(fShader, fragText)
  gl.compileShader(fShader)

  val program = gl.createProgram()
  gl.attachShader(program, vShader)
  gl.attachShader(program, fShader)
  gl.linkProgram(program)

  val projectionMatrix = gl.getUniformLocation(program, "projectionMatrix")
  val viewMatrix = gl.getUniformLocation(program, "viewMatrix")
  val color = gl.getUniformLocation(program, "color")
  val position = gl.getAttribLocation(program, "position")

  val drawables = scala.collection.mutable.Buffer[Drawable]()

  case class Drawable(mode: Int, count: Int, position: Attribute, var color: Color)

  class DrawableBuilder(val mode: Int, color: Color) {
    val positions = scala.collection.mutable.Buffer[Vector3]()

    def addVertex(position: Vector3): DrawableBuilder = {
      positions += position
      this
    }

    def compile: Drawable = {
      val count = positions.size
      val position = new Float32Array(3 * count)
      positions.zipWithIndex.foreach { case (e, i) => e.toArray(position, 3 * i) }
      val drawable = Drawable(mode, count, new Attribute(new Float32Array(position)), color)
      drawables += drawable
      drawable
    }
  }

  def newDrawable(mode: Int, color: Color) = new DrawableBuilder(mode, color)

  def use = {
    gl.useProgram(program)
    gl.enableVertexAttribArray(this.position)
  }

  def render(projectionMatrix: Matrix4, viewMatrix: Matrix4) = {
    use

    gl.uniformMatrix4fv(this.projectionMatrix, false, projectionMatrix.asArray)
    gl.uniformMatrix4fv(this.viewMatrix, false, viewMatrix.asArray)

    drawables.foreach { d =>
      gl.uniform4fv(color, d.color.asArray)

      gl.bindBuffer(ARRAY_BUFFER, d.position.buffer)
      gl.vertexAttribPointer(this.position, 3, FLOAT, false, d.position.offset, d.position.stride)

      gl.drawArrays(d.mode, 0, d.count)
    }

    unuse
  }

  def unuse = {
    gl.disableVertexAttribArray(this.position)
    gl.useProgram(null)
  }
}
