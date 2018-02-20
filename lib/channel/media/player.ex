defmodule Channel.Media.Player do
  use GenServer

  alias Channel.Media.PlayQueue

  ############################################################
  # Client
  ############################################################
  def start_link(_) do
    GenServer.start_link(__MODULE__, nil, name: :media_player)
  end

  def next_track do
    GenServer.cast(:media_player, :next)
  end

  def previous_track do
    GenServer.cast(:media_player, :previous)
  end

  def current_track do
    GenServer.call(:media_player, :current)
  end

  def queue(track) do
    GenServer.cast(:media_player, {:queue, track})
  end

  ############################################################
  # Server
  ############################################################
  def init(_) do
    send(self(), :init_audio)
    {:ok, %{play_queue: PlayQueue.new()}}
  end

  def handle_info(:init_audio, state) do
    {:noreply, state}
  end

  def handle_call(:current, _, %{play_queue: pq} = state) do
    {:reply, PlayQueue.current(pq), state}
  end

  def handle_cast(:next, %{play_queue: pq}) do
    {:noreply, %{play_queue: PlayQueue.next(pq)}}
  end

  def handle_cast(:previous, %{play_queue: pq}) do
    new_pq = pq
    |> PlayQueue.previous()
    |> PlayQueue.current()
    |> case do
         nil -> PlayQueue.reset(pq)
         _   -> pq
       end

    {:noreply, %{play_queue: new_pq}}
  end

  def handle_cast({:queue, track}, %{play_queue: pq}) do
    {:noreply, %{play_queue: PlayQueue.append(pq, track)}}
  end
end
