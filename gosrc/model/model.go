package model

import (
	"time"
)

func Load() {
	LoadXML()
	IndexAMVs()
}

//  index
var amvsByName map[string]*AMV
var viewingsByName map[string][]string

func IndexAMVs() {
	amvsByName = make(map[string]*AMV, len(Library.AMVs))
	viewingsByName = make(map[string][]string, len(Library.AMVs))

	for _, amv := range Library.AMVs {
		amvsByName[amv.Filename] = amv
		viewingsByName[amv.Filename] = make([]string, 0, 20)
	}

	for _, playlist := range Library.Playlists {
		for _, name := range playlist.Viewings {
			viewingsByName[name] = append(viewingsByName[name], playlist.Date)
		}
	}
}

func PlaylistAMVs(playlist *Playlist) []*AMV {
	amvs := make([]*AMV, 0, len(playlist.Viewings))
	for _, name := range playlist.Viewings {
		if amv, ok := amvsByName[name]; ok {
			amvs = append(amvs, amv)
		}
	}
	return amvs
}

func AMVViewings(amv *AMV) ([]time.Time, bool) {
	startOfTime, _ := time.Parse("2006-01-02", "2007-01-01")
	viewings := make([]time.Time, 0, len(viewingsByName[amv.Filename]))
	longago := false
	for _, viewing := range viewingsByName[amv.Filename] {
		viewdate, err := time.Parse("2006-01-02", viewing)
		if err == nil {
			if viewdate.After(startOfTime) {
				viewings = append(viewings, viewdate)
			} else {
				longago = true
			}
		}
	}

	// todo: sort viewings

	return viewings, longago
}

func LatestViewing(amv *AMV) *time.Time {
	viewings, _ := AMVViewings(amv)
	if len(viewings) == 0 {
		return nil
	}
	return &viewings[0]
}
