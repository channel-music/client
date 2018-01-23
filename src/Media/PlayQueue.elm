module Media.PlayQueue exposing
    ( PlayQueue
    , create
    , empty
    , items
    , append
    , current
    , next
    , previous
    , reset
    , jumpTo)

import Array

arrayIndexOf : a -> Array.Array a -> Maybe Int
arrayIndexOf item arr =
    let
        indexOf arr index =
            case arr of
                [] ->
                    Nothing
                (x::xs) ->
                    if item == x then
                        Just index
                    else
                        indexOf xs (index + 1)
    in
        indexOf (Array.toList arr) 0

type alias PlayQueue a =
    { index : Int
    , items : Array.Array a
    }

create : List a -> PlayQueue a
create items = PlayQueue 0 (Array.fromList items)

empty : PlayQueue a
empty = PlayQueue 0 (Array.empty)

items : PlayQueue a -> List a
items pq = Array.toList pq.items

append : a -> PlayQueue a -> PlayQueue a
append item pq =
    let items = Array.push item pq.items
    in { pq | items = items }

current : PlayQueue a -> Maybe a
current {index, items} =
    Array.get index items

next : PlayQueue a -> PlayQueue a
next pq = {pq | index = pq.index + 1 }

previous : PlayQueue a -> PlayQueue a
previous pq = {pq | index = pq.index - 1 }

reset : PlayQueue a -> PlayQueue a
reset pq = { pq | index = 0 }

jumpTo : a -> PlayQueue a -> PlayQueue a
jumpTo item pq =
    case arrayIndexOf item pq.items of
        Just itemIndex ->
            {pq | index = itemIndex}
        Nothing ->
            pq
