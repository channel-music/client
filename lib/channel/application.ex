defmodule Channel.Application do
  use Application

  def start(_type, _args) do
    # TODO: fix supervision strategy. Track storage is critical,
    # TODO: but if UI is killed the whole app should die
    children = [
      {Channel.TrackStorage, {}},
      {Channel.UI, {}},
      {Channel.Media.Player, {}},
    ]

    Supervisor.start_link(children, strategy: :rest_for_one)
  end
end
