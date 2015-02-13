package model

import (
	"fmt"
	"path/filepath"
	// "strconv"
	// "strings"
	"math"
	"time"
)

//  AMVs
type AMV struct {
	Filename string
	Duration time.Duration
	Scores   []AMVScore
}

type AMVScore struct {
	Judge string
	Score int
}

func (amv *AMV) Name() string {
	filename := filepath.Base(amv.Filename)
	ext := filepath.Ext(filename)
	return filename[:len(filename)-len(ext)]
}

func (amv *AMV) Folder() string {
	return filepath.Dir(amv.Filename)
}

func (amv *AMV) HasDuration() bool {
	return amv.Duration != 0
}

func (amv *AMV) GetDurationStr() string {
	mins := math.Floor(amv.Duration.Minutes())
	seconds := math.Floor(amv.Duration.Seconds()) - mins*60
	return fmt.Sprintf("%d:%02d", int(mins), int(seconds))
}

func (amv *AMV) ScoreBy(judge string) *AMVScore {
	for i, score := range amv.Scores {
		if score.Judge == judge {
			return &amv.Scores[i]
		}
	}
	return nil
}

//  Playlists
type Playlist struct {
	Date     string
	Viewings []string
}

func (playlist *Playlist) AMVs() []*AMV {
	amvs := make([]*AMV, 0, len(playlist.Viewings))
	for _, name := range playlist.Viewings {
		if amv, ok := amvsByName[name]; ok {
			amvs = append(amvs, amv)
		}
	}
	return amvs
}

//  the actual library

var AMVs []*AMV
var Playlists []*Playlist
