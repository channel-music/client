defmodule Channel.UI do
  use GenServer, restart: :temporary

  require Logger

  #
  # Client
  #
  def start_link(args) do
    GenServer.start_link(__MODULE__, args)
  end

  #
  # Server
  #
  def init(_args) do
    Process.flag(:trap_exit, true)
    Logger.info("Initializing WX...")
    # TODO: use this
    :wx.new()
    {:ok, Channel.UI.Server.start_link()}
  end

  def terminate(_reason, wx_server) do
    Channel.UI.Server.stop(wx_server)
    :wx.destroy()
    :ok
  end
end
