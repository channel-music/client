defmodule ChannelTest do
  use ExUnit.Case
  doctest Channel

  test "greets the world" do
    assert Channel.hello() == :world
  end
end
