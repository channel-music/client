'use strict'

const Elm = require('./dist/elm.js')

const container = document.getElementById('container')
const channel = Elm.Main.embed(container)

//
// Audio interface
//
const audioStream = new window.Audio()

channel.ports.open.subscribe(url => {
  audioStream.src = url

  audioStream.addEventListener('ended', () => (
    channel.ports.onStreamFinished.send(url)
  ))

  audioStream.addEventListener('error', err => (
    channel.ports.onStreamError.send(err)
  ))
})

channel.ports.play.subscribe(id => audioStream.play())
channel.ports.pause.subscribe(id => audioStream.pause())
