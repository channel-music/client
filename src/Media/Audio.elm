port module Media.Audio exposing (..)

-- Open a new audio stream. Will close any other
-- running audio streams.
port open : String -> Cmd a

-- Start the currently declared stream.
port play : () -> Cmd a
-- Pause the currently declared stream.
port pause : () ->  Cmd a

-- Called when the stream has finished running.
port onStreamFinished : (() -> msg) -> Sub msg
-- Called when the stream gives an error.
port onStreamError : (String -> msg) -> Sub msg
