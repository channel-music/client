defmodule Channel.Media.PlayQueue do
  alias Channel.Media.PlayQueue

  @enforce_keys [:items, :index]
  defstruct [items: [], index: 0]

  @doc """
  Creates a new play queue with the given items. Defaults
  to an empty list.
  """
  def new(items \\ []) do
    %PlayQueue{items: items, index: 0}
  end

  @doc """
  Returns an updated play queue moved to the next item. Will move
  to the next item even if it doesn't exist.
  """
  def next(%PlayQueue{index: index} = pq) do
    %PlayQueue{pq | index: index + 1}
  end

  @doc """
  Returns an updated play queue moved to the previous item. Will
  move to the previous item even if it doesn't exist.
  """
  def previous(%PlayQueue{index: index} = pq) do
    %PlayQueue{pq | index: index - 1}
  end

  @doc """
  Returns the current item in the play queue or `nil` if there is none.
  """
  def current(%PlayQueue{items: items, index: index}) do
    Enum.at(items, index)
  end

  @doc """
  Returns the current item in the play queue or throws an exception if
  there is none.
  """
  def current!(%PlayQueue{items: items, index: index}) do
    Enum.fetch!(items, index)
  end

  @doc """
  Returns true if the given play queue is empty.
  """
  def empty?(%PlayQueue{items: []}), do: true
  def empty?(%PlayQueue{}), do: false

  @doc """
  Returns a new play queue with the given item appended to the end.
  """
  def append(%PlayQueue{items: items} = pq, item) do
    %PlayQueue{pq | items: items ++ [item]}
  end

  @doc """
  Returns a new play queue reset back to the first item.
  """
  def reset(%PlayQueue{} = pq) do
    %PlayQueue{pq | index: 0}
  end
end
