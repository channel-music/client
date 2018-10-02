package com.kalouantonis.channel

import scalafx.scene.Parent

object Elm {
  final class View[Root <: Parent, Msg](private val rootFactory: () => Root) {
    def run(): Root = {
      lazy val root = rootFactory()
      SFXHelpers.run[Root](root)
      root
    }
  }

  trait Update[Msg] {
    def update(event: Msg): Unit
  }

  trait Widget[Root, Model, Msg] {
    def view(model: Model): View[Root, Msg]
  }

  class EventStream
  class Component[Root](eventStream: EventStream, root: Root)

  object SFXHelpers {
    def run[Root <: Parent, W <: Widget[Root, _, _]](widget: W): Unit = {
      val (component, events) = loadWidget[Root, W](widget)
    }

    def loadWidget[Root <: Parent, W <: Widget[Root, _, _]](
        widget: W): (Component[Root], EventStream) = {
      val events = new EventStream()
      val view = widget.view()
      val root = view.run()
      (widget, Component[Root](events, root), events)
    }
  }
}
