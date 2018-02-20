defmodule Channel.TrackStorage do
  use GenServer

  require Logger

  @request_interval 5_000

  ############################################################
  # Client
  ############################################################
  def start_link(_) do
    GenServer.start_link(__MODULE__, nil,
      name: :track_storage
    )
  end

  def get_tracks do
    GenServer.call(:track_storage, :get_tracks)
  end

  ############################################################
  # Server
  ############################################################
  def init(_) do
    # always fetch on start
    send(self(), :fetch_tracks)
    # setup timer to re-fetch
    :timer.send_interval(@request_interval, :fetch_tracks)
    {:ok, []}
  end

  def handle_info(:fetch_tracks, current_tracks) do
    case Channel.API.get("/songs") do
      {:ok, %HTTPoison.Response{status_code: 200, body: tracks}} ->
        {:noreply, tracks}
      {:ok, %HTTPoison.Response{} = resp} ->
        Logger.error("Invalid response from server: #{inspect(resp)}")
        {:noreply, current_tracks}
      {:error, reason} ->
        Logger.error("Failed to fetch tracks: #{inspect(reason)}")
        # Maintain original state
        {:noreply, current_tracks}
    end
  end

  def handle_call(:get_tracks, _, current_tracks) do
    {:reply, current_tracks, current_tracks}
  end
end
