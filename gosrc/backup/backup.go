package backup

import (
	model "../model"
	playlists "../playlists"
)

func Playlist(playlist model.Playlist) {

}

func All() {
	playlists.Scan()

}
