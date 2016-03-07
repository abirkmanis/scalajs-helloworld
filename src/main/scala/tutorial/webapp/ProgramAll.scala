package tutorial.webapp

import org.scalajs.dom.raw
import org.scalajs.dom.raw.WebGLRenderingContext._
import org.scalajs.dom.raw.{HTMLImageElement, WebGLRenderingContext, WebGLTexture}

import scala.scalajs.js.typedarray.Float32Array

case class Drawable(mode: Int, texture: WebGLTexture, count: Int, position: Attribute, uv: Attribute, color: Attribute)

class DrawableBuilder(val callback: Drawable => {}, val mode: Int, val image: HTMLImageElement)(implicit gl: WebGLRenderingContext) {
  val positions = scala.collection.mutable.Buffer[Vector3]()
  val uvs = scala.collection.mutable.Buffer[Vector2]()
  val colors = scala.collection.mutable.Buffer[Color]()

  def addVertex(position: Vector3, uv: Vector2, color: Color): DrawableBuilder = {
    positions += position
    uvs += uv
    colors += color
    this
  }

  def addVertex(position: Vector3, color: Color): DrawableBuilder = {
    positions += position
    uvs += Vector2(position.x, position.y)
    colors += color
    this
  }

  val white = Color(1, 1, 1, 1)

  def addVertex(position: Vector3): DrawableBuilder = {
    positions += position
    uvs += Vector2(position.x, position.y)
    colors += white
    this
  }

  def compile: Drawable = {
    val count = positions.size
    val position = new Float32Array(3 * count)
    positions.zipWithIndex.foreach { case (e, i) => e.toArray(position, 3 * i) }
    val uv = new Float32Array(2 * count)
    uvs.zipWithIndex.foreach { case (e, i) => e.toArray(uv, 2 * i) }
    val color = new Float32Array(4 * count)
    colors.zipWithIndex.foreach { case (e, i) => e.toArray(color, 4 * i) }
    val drawable = Drawable(mode, Texture.toTexture(image), count, new Attribute(new Float32Array(position)), new Attribute(new Float32Array(uv)), new Attribute(new Float32Array(color)))
    callback(drawable)
    drawable
  }

  def buildSquare(x0: Int, y0: Int, z: Int) = {
    addVertex(Vector3(x0, y0, z))
    .addVertex(Vector3(x0 + 1, y0, z))
    .addVertex(Vector3(x0 + 1, y0 + 1, z))
    .addVertex(Vector3(x0, y0, z))
    .addVertex(Vector3(x0 + 1, y0 + 1, z))
    .addVertex(Vector3(x0, y0 + 1, z))
  }
}

class ProgramAll(implicit val gl: raw.WebGLRenderingContext) {
  val vShader = gl.createShader(VERTEX_SHADER)
  val vertText =
    "uniform mat4 projectionMatrix;" +
      "uniform mat4 viewMatrix;" +
      "attribute vec3 position;" +
      "attribute vec2 texCoord;" +
      "varying vec2 vTexCoord;" +
      "attribute vec4 color;" +
      "varying vec4 vColor;" +
      "void main(){" +
      "gl_Position = projectionMatrix * viewMatrix * vec4(position, 1);" +
      "vTexCoord = texCoord;" +
      "vColor=color;" +
      "}"
  gl.shaderSource(vShader, vertText)
  gl.compileShader(vShader)

  val fShader = gl.createShader(FRAGMENT_SHADER)
  val fragText = "precision highp float;" +
    "uniform sampler2D texture;" +
    "varying vec2 vTexCoord;" +
    "varying vec4 vColor;" +
    "void main(){" +
    "gl_FragColor = texture2D(texture,vTexCoord) * vColor;" +
    "}"
  gl.shaderSource(fShader, fragText)
  gl.compileShader(fShader)

  val program = gl.createProgram()
  gl.attachShader(program, vShader)
  gl.attachShader(program, fShader)
  gl.linkProgram(program)

  val projectionMatrix = gl.getUniformLocation(program, "projectionMatrix")
  val viewMatrix = gl.getUniformLocation(program, "viewMatrix")
  val texture = gl.getUniformLocation(program, "texture")
  val position = gl.getAttribLocation(program, "position")
  val uv = gl.getAttribLocation(program, "texCoord")
  val color = gl.getAttribLocation(program, "color")

  val drawables = scala.collection.mutable.Buffer[Drawable]()

  def newDrawable(mode: Int, image: HTMLImageElement) = new DrawableBuilder((d: Drawable) => drawables += d, mode, image)

  def use = {
    gl.useProgram(program)
    gl.enableVertexAttribArray(this.position)
    gl.enableVertexAttribArray(this.uv)
    gl.enableVertexAttribArray(this.color)
  }

  def render(projectionMatrix: Matrix4, viewMatrix: Matrix4) = {
    use

    gl.uniformMatrix4fv(this.projectionMatrix, false, projectionMatrix.asArray)
    gl.uniformMatrix4fv(this.viewMatrix, false, viewMatrix.asArray)

    drawables.foreach { d =>
      // set up unit 0
      gl.activeTexture(TEXTURE0)
      gl.bindTexture(TEXTURE_2D, d.texture)
      // use unit 0
      gl.uniform1i(texture, 0)

      gl.bindBuffer(ARRAY_BUFFER, d.position.buffer)
      gl.vertexAttribPointer(this.position, 3, FLOAT, false, d.position.offset, d.position.stride)

      gl.bindBuffer(ARRAY_BUFFER, d.uv.buffer)
      gl.vertexAttribPointer(this.uv, 2, FLOAT, false, d.uv.offset, d.uv.stride)

      gl.bindBuffer(ARRAY_BUFFER, d.color.buffer)
      gl.vertexAttribPointer(this.color, 4, FLOAT, false, d.color.offset, d.color.stride)

      gl.drawArrays(d.mode, 0, d.count)
    }

    unuse
  }

  def unuse = {
    gl.disableVertexAttribArray(this.position)
    gl.disableVertexAttribArray(this.uv)
    gl.disableVertexAttribArray(this.color)
    gl.useProgram(null)
  }
}
