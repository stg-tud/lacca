import org.scalajs.linker.interface.ModuleSplitStyle

lazy val kanban = project
  .in(file("."))
  .enablePlugins(
    ScalaJSPlugin,
    ScalablyTypedConverterExternalNpmPlugin
  ) // Enable the Scala.js plugin in this project
  .settings(
    name := "kanban",
    scalaVersion := "3.5.0",
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
      "com.lihaoyi" %%% "scalatags" % "0.11.1",
      "com.raquo" %%% "laminar" % "17.0.0",
      "com.raquo" %%% "waypoint" % "8.0.1",
      "io.bullet" %%% "borer-core" % "1.14.1",
      "io.bullet" %%% "borer-derivation" % "1.14.1",
      "de.tu-darmstadt.stg" %%% "rdts" % "0.37.0"
    )
  )
