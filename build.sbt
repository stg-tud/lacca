import org.scalajs.linker.interface.ModuleSplitStyle

val Http4sVersion = "0.23.32"
val CirceVersion = "0.14.15"
val LogbackVersion = "1.4.11"
val CatsParseVersion = "0.3.10"

val ScalaVersion = "3.6.4"

lazy val kanban = project
  .in(file("."))
  .enablePlugins(
    ScalaJSPlugin,
    ScalablyTypedConverterExternalNpmPlugin
  ) // Enable the Scala.js plugin in this project
  .settings(
    name := "kanban",
    scalaVersion := ScalaVersion,
    // Tell Scala.js that this is an application with a main method
    scalaJSUseMainModuleInitializer := true,
    externalNpm := baseDirectory.value,

    // Ignore several Trystero dependencies in ScalablyTyped to avoid `stImport` errors
    stIgnore := List("libp2p", "firebase", "@supabase/supabase-js"),

    /* Configure Scala.js to emit modules in the optimal way to
     * connect to Vite's incremental reload.
     * - emit ECMAScript modules
     * - emit as many small modules as possible for classes in the "livechart" package
     * - emit as few (large) modules as possible for all other classes
     *   (in particular, for the standard library)
     */
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("kanban"))
        )
    },
    /* Depend on the scalajs-dom library.
     * It provides static types for the browser DOM APIs.
     */
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "com.raquo" %%% "laminar" % "17.0.0",
      "com.raquo" %%% "waypoint" % "8.0.1",
      "de.tu-darmstadt.stg" %%% "rdts" % "0.37.0",
      "org.getshaka" %%% "native-converter" % "0.9.0"
    ),

    // depend on UCAN scala version
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies += "com.github.stg-tud" % "ucan-scala" % "0.1.6"
  )

lazy val server = project
  .in(file("server"))
//  .enablePlugins(
//    ScalaJSPlugin,
//  ) // Enable the Scala.js plugin in this project
  .settings(
    name := "server",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := ScalaVersion,
//    scalaJSUseMainModuleInitializer := true,
//    scalaJSLinkerConfig ~= {
//      _.withModuleKind(ModuleKind.ESModule)
//    },
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %%% "http4s-circe" % Http4sVersion,
      "io.circe" %%% "circe-generic" % CirceVersion,
      "org.http4s" %%% "http4s-dsl" % Http4sVersion,
/*
      "org.typelevel" %% "cats-parse" % CatsParseVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion
*/
    )
  )
