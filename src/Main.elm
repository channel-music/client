module Main exposing (main)

import Html exposing (..)
import Html.Events exposing (onClick)
import Bootstrap.Table as Table
import Http
import Json.Decode exposing (..)

import Media.Audio
import Media.Player as Player
import Media.Player exposing (Track, Player)

main : Program Never Model Msg
main = Html.program
       { init = init
       , view = view
       , update = update
       , subscriptions = subscriptions}

-- MODEL

type alias Model =
    { tracks : List Track
    , player : Player
    }

init : (Model, Cmd Msg)
init = (Model [] Player.empty, loadTracks)

-- UPDATE

type Msg = TracksLoaded (Result Http.Error (List Track))
         | StreamError String
         | TogglePlaying
         | NextTrack
         | PreviousTrack
         | JumpToTrack Track

update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        TracksLoaded (Ok tracks) ->
            ({model | tracks = tracks, player = Player.create tracks}
             , Cmd.none)

        TracksLoaded (Err _) ->
            (model, Cmd.none)

        StreamError err ->
            Debug.log err
            (model, Cmd.none)

        TogglePlaying ->
            if Player.isPlaying model.player then
                runAudio model Player.pause
            else
                runAudio model Player.play

        NextTrack ->
            runAudio model Player.next

        PreviousTrack ->
            runAudio model Player.previous

        JumpToTrack track ->
            runAudio model (Player.jumpTo track)

-- VIEW

view : Model -> Html Msg
view model =
    div []
      [ viewTracks (List.sortBy .track model.tracks)
      , viewPlayer model.player]

viewTracks : List Track -> Html Msg
viewTracks tracks =
    Table.simpleTable
        (Table.simpleThead
             [ Table.th [] [ text "#" ]
             , Table.th [] [ text "Title" ]
             , Table.th [] [ text "Album" ]
             , Table.th [] [ text "Artist" ]
             , Table.th [] [ ]]
        , Table.tbody []
            (List.map viewTrack tracks))

viewTrack : Track -> Table.Row Msg
viewTrack t =
    Table.tr []
      [ Table.td [] [text (toString t.track)]
      , Table.td [] [text t.title]
      , Table.td [] [text t.album]
      , Table.td [] [text t.artist]
      , Table.td [] [viewTrackActions t] ]

viewTrackActions : Track -> Html Msg
viewTrackActions t =
    button [onClick (JumpToTrack t)] [text "|>"]

viewPlayer : Player ->  Html Msg
viewPlayer player =
    div []
     [ button [onClick PreviousTrack] [text "Previous"]
     , button [onClick TogglePlaying]
         [
          if Player.isPlaying player then
              text "Pause"
          else
              text "Play"
         ]
     , button [onClick NextTrack] [text "Next"]]

-- SUBSCRIPTIONS

subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch [ Media.Audio.onStreamFinished (\s -> NextTrack)
              , Media.Audio.onStreamError StreamError]

-- COMMANDS

loadTracks : Cmd Msg
loadTracks =
    let
        request = Http.get "http://localhost:3000/songs" decodeTracks
    in
        Http.send TracksLoaded request

-- TODO: monad
runAudio : Model -> (Player -> (Player, Cmd msg)) -> (Model, Cmd msg)
runAudio model audioFn =
    let (player, cmd) = audioFn model.player
    in ({model | player = player}, cmd)

-- DECODERS

decodeTrack : Decoder Track
decodeTrack = map6 Track
                (field "id" int)
                (field "track" int)
                (field "title" string)
                (field "album" string)
                (field "artist" string)
                (field "file" string)

decodeTracks : Decoder (List Track)
decodeTracks = list decodeTrack
