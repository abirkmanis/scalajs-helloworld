package tutorial.webapp

import org.scalajs.dom.raw
import org.scalajs.dom.raw.WebGLRenderingContext._
import org.scalajs.dom.raw.{HTMLImageElement, WebGLBuffer, WebGLRenderingContext, WebGLTexture}

import scala.scalajs.js.typedarray.{Float32Array, Int8Array}

object NormalAxis {
  val Z = 0
  val Y = 1
  val X = 2
}

class ProgramAll(implicit val gl: raw.WebGLRenderingContext) {

  // todo: VAO (is not in WebGL 1.0, is in OES_vertex_array_object - is it worth it?)
  // Encapsulates data needed for a single call to a glDraw* - glDrawElements in this case.
  // All attributes are interleaved in a single VBO.
  case class Drawable(mode: Int, vbo: WebGLBuffer, ibo: WebGLBuffer, texture: WebGLTexture, count: Int)

  class DrawableBuilder(val mode: Int, val image: HTMLImageElement)(implicit gl: WebGLRenderingContext) {
    val positions = scala.collection.mutable.Buffer[Vector3]()
    val uvs = scala.collection.mutable.Buffer[Vector2]()
    val colors = scala.collection.mutable.Buffer[Color]()

    val indices = scala.collection.mutable.Buffer[Int]()

    val white = Color(1, 1, 1, 1)

    def addVertex(position: Vector3, uv: Vector2, color: Color = white): DrawableBuilder = {
      positions += position
      uvs += uv
      colors += color
      this
    }

    def addTriangle(i: Int, j: Int, k: Int): DrawableBuilder = {
      indices += i
      indices += j
      indices += k
      this
    }

    def compile: Drawable = {
      val values = new Float32Array(strideInFloats * positions.size)

      positions.zipWithIndex.foreach { case (e, i) => e.toArray(values, strideInFloats * i) }
      uvs.zipWithIndex.foreach { case (e, i) => e.toArray(values, strideInFloats * i + 3) }
      colors.zipWithIndex.foreach { case (e, i) => e.toArray(values, strideInFloats * i + 3 + 2) }

      val vbo = gl.createBuffer()
      gl.bindBuffer(ARRAY_BUFFER, vbo)
      gl.bufferData(ARRAY_BUFFER, values, STATIC_DRAW)

      // todo: make sure 8 bits are enough
      val indices = new Int8Array(this.indices.size)
      this.indices.zipWithIndex.foreach { case (e, i) => indices(i) = e.toByte }

      val ibo = gl.createBuffer()
      gl.bindBuffer(ELEMENT_ARRAY_BUFFER, ibo)
      gl.bufferData(ELEMENT_ARRAY_BUFFER, indices, STATIC_DRAW)

      val drawable = Drawable(mode, vbo, ibo, Texture.toTexture(image), this.indices.size)

      drawables += drawable

      drawable
    }

    val d = List(List(0, 1, 1, 0), List(0, 0, 1, 1), List(0, 0, 0, 0))

    // A utility method, adds nothing that cannot be done without it.
    def buildSquare(x0: Int, y0: Int, z0: Int, normalAxis: Int = NormalAxis.Z) = {
      val dx = d((0 + normalAxis) % 3)
      val dy = d((1 + normalAxis) % 3)
      val dz = d((2 + normalAxis) % 3)
      val start = positions.size
      addVertex(Vector3(x0 + dx(0), y0 + dy(0), z0 + dz(0)), Vector2(d(0)(0), d(1)(0)))
        .addVertex(Vector3(x0 + dx(1), y0 + dy(1), z0 + dz(1)), Vector2(d(0)(1), d(1)(1)))
        .addVertex(Vector3(x0 + dx(2), y0 + dy(2), z0 + dz(2)), Vector2(d(0)(2), d(1)(2)))
        .addVertex(Vector3(x0 + dx(3), y0 + dy(3), z0 + dz(3)), Vector2(d(0)(3), d(1)(3)))
        .addTriangle(start + 0, start + 1, start + 2)
        .addTriangle(start + 0, start + 2, start + 3)
    }
  }

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

  def newDrawable(image: HTMLImageElement, mode: Int = TRIANGLES) = new DrawableBuilder(mode, image)

  def use = {
    gl.useProgram(program)
    gl.enableVertexAttribArray(this.position)
    gl.enableVertexAttribArray(this.uv)
    gl.enableVertexAttribArray(this.color)
  }

  val floatSize = 4
  val positionOffset = 0
  val positionSize = 3 * floatSize
  val uvOffset = positionOffset + positionSize
  val uvSize = 2 * floatSize
  val colorOffset = uvOffset + uvSize
  val colorSize = 4 * floatSize
  val strideInBytes = positionSize + uvSize + colorSize
  val strideInFloats = strideInBytes / floatSize

  def render(projectionMatrix: Matrix4, viewMatrix: Matrix4) = {
    use

    // use unit 0
    gl.uniform1i(texture, 0)
    gl.uniformMatrix4fv(this.projectionMatrix, false, projectionMatrix.asArray)
    gl.uniformMatrix4fv(this.viewMatrix, false, viewMatrix.asArray)

    drawables.foreach { d =>
      gl.bindBuffer(ARRAY_BUFFER, d.vbo)
      gl.bindBuffer(ELEMENT_ARRAY_BUFFER, d.ibo)

      // set up unit 0
      gl.activeTexture(TEXTURE0)
      gl.bindTexture(TEXTURE_2D, d.texture)

      gl.vertexAttribPointer(this.position, 3, FLOAT, false, strideInBytes, positionOffset)
      gl.vertexAttribPointer(this.uv, 2, FLOAT, false, strideInBytes, uvOffset)
      gl.vertexAttribPointer(this.color, 4, FLOAT, false, strideInBytes, colorOffset)

      gl.drawElements(d.mode, d.count, UNSIGNED_BYTE, 0)
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
