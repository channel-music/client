module Media.PlayQueue exposing (..)

import Array

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
