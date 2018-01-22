import Html exposing (..)
import Html.Events exposing (onClick)
import Bootstrap.Button as Button
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
         | PlayCurrent
         | NextTrack
         | PreviousTrack

update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        TracksLoaded (Ok tracks) ->
            ({model | tracks = tracks, player = Player.create tracks}
             , Cmd.none)

        TracksLoaded (Err _) ->
            (model, Cmd.none)

        StreamError msg ->
            Debug.log msg
            (model, Cmd.none)

        PlayCurrent ->
            let (player, cmd) = Player.play model.player
            in ({model | player = player}, cmd)

        NextTrack ->
            let (player, cmd) = Player.next model.player
            in ({model | player = player}, cmd)

        PreviousTrack ->
            let (player, cmd) = Player.previous model.player
            in ({model | player = player}, cmd)

-- VIEW

view : Model -> Html Msg
view model =
    div []
      [ viewTracks (List.sortBy .track model.tracks)
      , viewPlayer ]

viewTracks : List Track -> Html Msg
viewTracks tracks =
    Table.simpleTable
        (Table.simpleThead
             [ Table.th [] [ text "#" ]
             , Table.th [] [ text "Title" ]
             , Table.th [] [ text "Album" ]
             , Table.th [] [ text "Artist" ]]
        , Table.tbody []
            (List.map viewTrack tracks))

viewTrack : Track -> Table.Row Msg
viewTrack t =
    Table.tr []
      [ Table.td [] [text (toString t.track)]
      , Table.td [] [text t.title]
      , Table.td [] [text t.album]
      , Table.td [] [text t.artist]]

viewPlayer : Html Msg
viewPlayer =
    div []
     [ button [onClick PreviousTrack] [text "Previous"]
     , button [onClick PlayCurrent] [text "Play"]
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
