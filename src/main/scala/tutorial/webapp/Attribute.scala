package tutorial.webapp

import org.scalajs.dom.raw.{WebGLBuffer, WebGLRenderingContext}
import org.scalajs.dom.raw.WebGLRenderingContext._

import scala.scalajs.js.typedarray.Float32Array

class Attribute(val buffer:WebGLBuffer, val stride: Int, val offset: Int)(implicit gl: WebGLRenderingContext) {
}