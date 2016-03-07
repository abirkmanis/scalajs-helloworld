enablePlugins(ScalaJSPlugin)

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.0"

name := "ScalaJS"

version := "1.0"

scalaVersion := "2.11.7"


enablePlugins(JettyPlugin)

webappPostProcess := {
  webappDir: File =>
    val baseDir = baseDirectory.value
    IO.copyFile(baseDir / "hello.html", webappDir / "hello.html")
    IO.copyFile(baseDir / "target" / "scala-2.11" / "scalajs-fastopt.js",
      webappDir / "target" / "scala-2.11" / "scalajs-fastopt.js")

    baseDir.listFiles(GlobFilter("*.jpg")).foreach {
      f =>
        IO.copyFile(f, webappDir / f.getName)
    }
}

webappPostProcess <<= (webappPostProcess).dependsOn(fastOptJS in Compile)