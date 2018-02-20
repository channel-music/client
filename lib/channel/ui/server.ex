defmodule Channel.UI.Server do
  @behaviour :wx_object

  require Logger

  ############################################################
  # Client
  ############################################################
  def start_link do
    :wx_object.start_link(__MODULE__, [], [])
  end

  def stop(wx_server) do
    :wx_object.stop(wx_server)
  end

  ############################################################
  # Server
  ############################################################
  def init(_args) do
    frame = :wxFrame.new(:wx.null(), -1, "Channel", size: {800, 600})
    menu_bar = create_menu_bar()

    panel = :wxPanel.new(frame)

    sizer = :wxBoxSizer.new(:wx_const.horizontal)

    song_list = create_song_list(panel)
    play_list = create_song_list(panel)

    # TODO: find a better way to keep state in sync
    :timer.send_interval(2_000, :update_song_list)

    :wxSizer.add(sizer, song_list, flag: :wx_const.expand)
    :wxSizer.add(sizer, play_list, flag: :wx_const.expand)
    :wxPanel.setSizer(panel, sizer)

    :wxFrame.setMenuBar(frame, menu_bar)
    :wxFrame.show(frame)

    {frame, %{
        menu_bar: menu_bar,
        song_list: song_list,
        play_list: play_list,
    }}
  end

  def handle_event(ev, state) do
    Logger.debug "Received unhandled event: #{inspect(ev)}"
    {:noreply, state}
  end

  def handle_info(:update_song_list, %{song_list: list} = state) do
    update_list(list, Channel.TrackStorage.get_tracks())
    {:noreply, state}
  end

  def handle_info(info, state) do
    Logger.warn "Unhandled info: #{inspect(info)}"
    {:noreply, state}
  end

  #
  # Helpers
  #
  defp create_menu_bar do
    menu_bar = :wxMenuBar.new()

    file_menu = :wxMenu.new()
    quit_item = :wxMenuItem.new(id: 400, text: "&Quit")
    :wxMenu.append(file_menu, quit_item)

    help_menu = :wxMenu.new()
    about_item = :wxMenuItem.new(id: 500, text: "About")
    :wxMenu.append(help_menu, about_item)

    :wxMenuBar.append(menu_bar, file_menu, "&File")
    :wxMenuBar.append(menu_bar, help_menu, "&Help")

    menu_bar
  end

  @list_id 300

  defp create_song_list(panel) do
    list = :wxListCtrl.new(panel,
      winid: @list_id, style: :wx_const.lc_report
    )

    list_item = :wxListItem.new()
    add_list_entry = fn ({name, align, defsize}, col) ->
      :wxListItem.setText(list_item, name)
      :wxListItem.setAlign(list_item, align)
      :wxListCtrl.insertColumn(list, col, list_item)
      :wxListCtrl.setColumnWidth(list, col, defsize)
      col + 1
    end

    list_columns = [
      {"#",      :wx_const.list_format_left, 150},
      {"Title",  :wx_const.list_format_left, 150},
      {"Album",  :wx_const.list_format_left, 150},
      {"Artist", :wx_const.list_format_left, 150}
    ]
    Enum.reduce(list_columns, 0, add_list_entry)
    :wxListItem.destroy(list_item)

    list
  end

  defp update_list(list, tracks) do
    :wxListCtrl.deleteAllItems(list)

    update = fn (
      %{"track" => track,
        "title" => title,
        "album" => album,
        "artist" => artist},
      row
    ) ->
      # First insert an empty row
      _item = :wxListCtrl.insertItem(list, row, "")
      # Add items to the row by column
      [track, title, album, artist]
      |> Enum.with_index()
      |> Enum.each(fn {val, col} ->
        :wxListCtrl.setItem(list, row, col, to_string(val))
      end)
      row + 1
    end

    List.foldl(tracks, 0, update)
    list
  end
end
