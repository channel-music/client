package com.kalouantonis.channel

object Elm {
  class View[Root, Msg](private val rootFactory: () => Root) {
    lazy val root = rootFactory()
  }

  trait Update[Msg] {
    def update(event: Msg): Unit
  }

  trait Widget[Root, Model, Msg] {
    def view(model: Model): View[Root, Msg]
  }
}
