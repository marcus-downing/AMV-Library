package model

import (
	"fmt"
	"path/filepath"
	"strconv"
	// "strings"
	"math"
	"time"
)

//  data types
type LibraryData struct {
	AMVs      []*AMV      `xml:"amvs>amv"`
	Playlists []*Playlist `xml:"playlists>playlist"`
}

type AMV struct {
	Filename string     `xml:"filename"`
	Duration string     `xml:"duration"`
	Level    int        `xml:"level"`
	Scores   []AMVScore `xml:"score"`
}

type AMVScore struct {
	Judge string `xml:"judge"`
	Score int    `xml:"score"`
}

type Playlist struct {
	Date     string   `xml:"date"`
	Viewings []string `xml:"viewing"`
}

//  accessors
func (amv *AMV) Name() string {
	filename := filepath.Base(amv.Filename)
	ext := filepath.Ext(filename)
	return filename[:len(filename)-len(ext)]
}

func (amv *AMV) Folder() string {
	return filepath.Dir(amv.Filename)
}

func (amv *AMV) GetDuration() time.Duration {
	seconds, err := strconv.Atoi(amv.Duration[2 : len(amv.Duration)-1])
	if err != nil {
		return 0
	}
	return time.Duration(seconds) * time.Second
}

func (amv *AMV) GetDurationStr() string {
	dur := amv.GetDuration()
	mins := math.Floor(dur.Minutes())
	seconds := math.Floor(dur.Seconds()) - mins*60
	return fmt.Sprintf("%d:%02d", int(mins), int(seconds))
}

//  the actual library
var Library LibraryData
