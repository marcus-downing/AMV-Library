package model

import (
	"../config"
	"fmt"
	"time"
)

//  index
var amvsByName map[string]*AMV
var viewingsByName map[string][]string

func IndexAMVs() {
	conf := &config.Config

	amvsByName = make(map[string]*AMV, len(AMVs))
	viewingsByName = make(map[string][]string, len(AMVs))

	if conf.Debug {
		fmt.Println("Indexing AMVs")
	}
	for _, amv := range AMVs {
		amvsByName[amv.Filename] = amv
		viewingsByName[amv.Filename] = make([]string, 0, 5)
	}

	if conf.Debug {
		fmt.Println("Indexing playlists")
	}

	for _, playlist := range Playlists {
		for _, name := range playlist.Viewings {
			viewingsByName[name] = append(viewingsByName[name], playlist.Date)
		}
	}

	if conf.Debug {
		fmt.Println("Indexed", len(amvsByName), "AMVs and", len(Playlists), "playlists")
	}
}

func AMVbyName(name string) *AMV {
	return amvsByName[name]
}

func AMVviewings(amv *AMV) ([]time.Time, bool) {
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
	viewings, _ := AMVviewings(amv)
	if len(viewings) == 0 {
		return nil
	}
	return &viewings[0]
}
