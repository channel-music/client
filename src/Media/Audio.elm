port module Media.Audio exposing (..)

port open : String -> Cmd a

port play : () -> Cmd a
port pause : () ->  Cmd a

port onStreamFinished : (String -> msg) -> Sub msg
port onStreamError : (String -> msg) -> Sub msg
