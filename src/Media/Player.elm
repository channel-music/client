module Media.Player exposing
    ( Track
    , Player
    , empty
    , create
    , play
    , pause
    , next
    , previous
    , isPlaying)

import Media.Audio as Audio
import Media.PlayQueue as PQ

type alias Track =
    { id : Int
    , track : Int
    , title : String
    , album : String
    , artist : String
    , url : String
    }

type StreamState = Playing | Paused | Stopped

type alias PlayQueue = PQ.PlayQueue Track

type alias Player =
    { playQueue : PQ.PlayQueue Track
    , streamState : StreamState }

empty : Player
empty = create []

create : List Track -> Player
create tracks = Player (PQ.create tracks) Stopped

play : Player -> (Player, Cmd msg)
play player =
    case PQ.current player.playQueue of
        Just track ->
            ( {player | streamState = Playing},
                 Cmd.batch [ Audio.open track.url
                           , Audio.play ()])
        Nothing ->
            (player, Cmd.none)

pause : Player -> (Player, Cmd msg)
pause player =
    ( {player | streamState = Paused}, Audio.pause ())

reset : Player -> (Player, Cmd msg)
reset player =
    ( {player | playQueue = PQ.reset player.playQueue
              , streamState = Stopped},
          Cmd.none)

moveTo : (PlayQueue -> PlayQueue) -> Player -> (Player, Cmd msg)
moveTo mover player =
    let
        playQueue = mover player.playQueue
    in
        case PQ.current playQueue of
            Just _ ->
                play { player | playQueue = playQueue }
            Nothing ->
                reset player

next : Player -> (Player, Cmd msg)
next = moveTo PQ.next

previous : Player -> (Player, Cmd msg)
previous = moveTo PQ.previous

isPlaying : Player -> Bool
isPlaying { streamState } = streamState == Playing
