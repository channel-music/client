-module(wx_const).
-compile(export_all).

-include_lib("wx/include/wx.hrl").

vertical()   -> ?wxVERTICAL.
horizontal() -> ?wxHORIZONTAL.

expand() -> ?wxEXPAND.

list_format_left()  -> ?wxLIST_FORMAT_LEFT.
list_format_right() -> ?wxLIST_FORMAT_RIGHT.

lc_report() -> ?wxLC_REPORT.
