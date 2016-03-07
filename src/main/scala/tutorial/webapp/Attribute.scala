package tutorial.webapp

import org.scalajs.dom.raw.WebGLRenderingContext
import org.scalajs.dom.raw.WebGLRenderingContext._

import scala.scalajs.js.typedarray.Float32Array

// no sense in providing these if a new buffer is created per attribute
//, val stride: Int = 0, val offset: Int = 0
class Attribute(values: Float32Array)(implicit gl: WebGLRenderingContext) {
  val stride: Int = 0
  val offset: Int = 0
  val buffer = gl.createBuffer()
  gl.bindBuffer(ARRAY_BUFFER, buffer)
  gl.bufferData(ARRAY_BUFFER, values, STATIC_DRAW)
}