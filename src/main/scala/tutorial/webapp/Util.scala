package tutorial.webapp

import org.scalajs.dom.raw
import org.scalajs.dom.raw.HTMLImageElement
import org.scalajs.dom.raw.WebGLRenderingContext._

import scala.scalajs.js
import scala.scalajs.js.typedarray.Float32Array

object Texture {
  def toTexture(image: HTMLImageElement)(implicit gl: raw.WebGLRenderingContext) = {
    val textureId = gl.createTexture()
    gl.bindTexture(TEXTURE_2D, textureId)
    gl.texImage2D(TEXTURE_2D, 0, RGBA, RGBA, UNSIGNED_BYTE, image)
    gl.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, LINEAR)
    gl.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, LINEAR_MIPMAP_NEAREST)
    gl.generateMipmap(TEXTURE_2D)
    gl.bindTexture(TEXTURE_2D, null)
    textureId
  }
}

case class Color(r: Float, g: Float, b: Float, a: Float) {
  def toArray(destination: Float32Array, i: Int): Unit = {
    destination(i) = r
    destination(i + 1) = g
    destination(i + 2) = b
    destination(i + 3) = a
  }

  def asArray = new Float32Array(js.Array(r, g, b, a))
}

case class Vector2(x: Float, y: Float) {
  def toArray(destination: Float32Array, i: Int): Unit = {
    destination(i) = x
    destination(i + 1) = y
  }

  def asArray = new Float32Array(js.Array(x, y))
}

case class Vector3(x: Float, y: Float, z: Float) {
  def toArray(destination: Float32Array, i: Int): Unit = {
    destination(i) = x
    destination(i + 1) = y
    destination(i + 2) = z
  }

  def asArray = new Float32Array(js.Array(x, y, z))
}

class Matrix4() {
  val array = new Float32Array(16)
  array(0) = 1
  array(5) = 1
  array(10) = 1
  array(15) = 1

  def this(fov: Float, aspect: Float, near: Float, far: Float) = {
    this
    val top = near * Math.tan(Math.PI / 180 * fov / 2).toFloat
    val bottom = -top
    val right = top * aspect
    val left = -right
    array(0) = 2 * near / (right - left)
    array(8) = (right + left) / (right - left)
    array(5) = 2 * near / (top - bottom)
    array(9) = (top + bottom) / (top - bottom)
    array(10) = (far + near) / (far - near)
    array(14) = -2 * (far * near) / (far - near)
    array(11) = 1
    array(15) = 0
  }

  def this(sx: Float, sy: Float) = {
    this
    array(0) = sx
    array(5) = sy
  }

  def this(dx: Float, dy: Float, dz: Float) = {
    this
    array(12) = dx
    array(13) = dy
    array(14) = dz
  }

  def toArray(destination: Float32Array, i: Int): Unit = {
    destination.set(array)
  }

  def asArray = array
}

