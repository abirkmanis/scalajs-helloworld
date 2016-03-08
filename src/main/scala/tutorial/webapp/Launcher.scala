package tutorial.webapp

import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLImageElement

import scala.collection.mutable
import scala.scalajs.js.JSApp

object Launcher extends JSApp {
  val toLoad = new mutable.HashSet[String]
  val loaded = new mutable.HashMap[String, HTMLImageElement]
  var isReady = false

  def loadImage(name: String): Unit = {
    val image = document.createElement("img").asInstanceOf[HTMLImageElement]
    image.src = name
    toLoad += image.src
    console.log("Loading " + image.src)
    image.onload = { e: Event => onImage(image) }
  }

  def onImage(image: HTMLImageElement): Unit = {
    console.log("Loaded " + image.src)
    toLoad -= image.src
    loaded += ((image.src, image))
    checkReady()
  }

  def checkReady(): Unit = {
    if (toLoad.isEmpty && isReady)
      new Renderer(loaded.map { case (n: String, i: HTMLImageElement) => (n.substring(n.lastIndexOf('/') + 1), i) }.toMap).start()
  }

  def ready(): Unit = {
    isReady = true
    checkReady()
  }

  def main(): Unit = {
    loadImage("stucco.jpg")
    loadImage("stone.jpg")
    loadImage("metal.jpg")
    loadImage("scratched.jpg")
    ready()
  }
}