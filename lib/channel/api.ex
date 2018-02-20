defmodule Channel.API do
  use HTTPoison.Base

  def process_url(url) do
    # TODO: load from settings
    "http://localhost:3000" <> url
  end

  def process_request_body(body) do
    with {:ok, json_str} <- Poison.encode(body) do
      json_str
    end
  end

  def process_response_body(body) do
    with {:ok, json} <- Poison.decode(body) do
      json
    end
  end
end
