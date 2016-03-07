package tutorial.webapp.unsupported

import org.scalajs.dom.raw
import org.scalajs.dom.raw.WebGLRenderingContext._
import org.scalajs.dom.raw.{HTMLImageElement, WebGLTexture}
import tutorial.webapp.{Attribute, Texture, Vector2}

import scala.scalajs.js.typedarray.Float32Array

class ProgramVPosition2Texture(implicit val gl: raw.WebGLRenderingContext) {
  val vShader = gl.createShader(VERTEX_SHADER)
  val vertText =
    "attribute vec2 position;" +
      "attribute vec2 texCoord;" +
      "varying vec2 vTexCoord;" +
      "void main(){" +
      "gl_Position = vec4(position,.99,1);" +
      "vTexCoord = texCoord;" +
      "}"
  gl.shaderSource(vShader, vertText)
  gl.compileShader(vShader)

  val fShader = gl.createShader(FRAGMENT_SHADER)
  val fragText = "precision highp float;" +
    "varying vec2 vTexCoord;" +
    "uniform sampler2D texture;" +
    "void main(){" +
    "gl_FragColor = texture2D(texture,vTexCoord);" +
    "}"
  gl.shaderSource(fShader, fragText)
  gl.compileShader(fShader)

  val program = gl.createProgram()
  gl.attachShader(program, vShader)
  gl.attachShader(program, fShader)
  gl.linkProgram(program)

  val texture = gl.getUniformLocation(program, "texture")
  val position = gl.getAttribLocation(program, "position")
  val uv = gl.getAttribLocation(program, "texCoord")

  val drawables = scala.collection.mutable.Buffer[Drawable]()

  case class Drawable(mode: Int, texture: WebGLTexture, count: Int, position: Attribute, uv: Attribute)

  class DrawableBuilder(val mode: Int, val image: HTMLImageElement) {
    val positions = scala.collection.mutable.Buffer[Vector2]()
    val uvs = scala.collection.mutable.Buffer[Vector2]()

    def addVertex(position: Vector2, uv: Vector2): DrawableBuilder = {
      positions += position
      uvs += uv
      this
    }

    def addVertex(position: Vector2): DrawableBuilder = addVertex(position, position)

    def compile: Unit = {
      val count = positions.size

      val position = new Float32Array(2 * count)
      positions.zipWithIndex.foreach { case (e, i) => e.toArray(position, 2 * i) }

      val uv = new Float32Array(2 * count)
      uvs.zipWithIndex.foreach { case (e, i) => e.toArray(uv, 2 * i) }

      drawables += Drawable(mode, Texture.toTexture(image), count, new Attribute(new Float32Array(position)), new Attribute(new Float32Array(uv)))
    }
  }

  def newDrawable(mode: Int, image: HTMLImageElement) = new DrawableBuilder(mode, image)

  def use = {
    gl.useProgram(program)
    gl.enableVertexAttribArray(this.position)
    gl.enableVertexAttribArray(this.uv)
  }

  def render() = {
    use

    drawables.foreach { d =>
      // set up unit 0
      gl.activeTexture(TEXTURE0)
      gl.bindTexture(TEXTURE_2D, d.texture)
      // use unit 0
      gl.uniform1i(texture, 0)

      gl.bindBuffer(ARRAY_BUFFER, d.position.buffer)
      gl.vertexAttribPointer(this.position, 2, FLOAT, false, d.position.offset, d.position.stride)

      gl.bindBuffer(ARRAY_BUFFER, d.uv.buffer)
      gl.vertexAttribPointer(this.uv, 2, FLOAT, false, d.uv.offset, d.uv.stride)

      gl.drawArrays(d.mode, 0, d.count)
    }

    unuse
  }

  def unuse = {
    gl.disableVertexAttribArray(this.position)
    gl.disableVertexAttribArray(this.uv)
    gl.useProgram(null)
  }
}
