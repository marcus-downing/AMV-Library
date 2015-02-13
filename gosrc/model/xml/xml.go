package xml

import (
	model ".."
	"../../config"
	"encoding/xml"
	"fmt"
	"io/ioutil"
	"os"
	// "strconv"
	"strings"
	"time"
)

//  no-public XML data types
type xmlLibrary struct {
	XMLName   xml.Name
	AMVs      []*amv      `xml:"amvs>amv"`
	Playlists []*playlist `xml:"playlists>playlist"`
}

type amv struct {
	Filename string     `xml:"filename"`
	Duration string     `xml:"duration"`
	Level    int        `xml:"level"`
	Scores   []amvScore `xml:"score"`
}

type amvScore struct {
	Judge string `xml:"judge"`
	Score int    `xml:"score"`
}

type playlist struct {
	Date     string   `xml:"date"`
	Viewings []string `xml:"viewing"`
}

func (amv *amv) toModel() *model.AMV {
	duration := parseDuration(amv.Duration)
	// duration := time.Duration(seconds) * time.Second

	scores := make([]model.AMVScore, 0, len(amv.Scores))
	for _, score := range amv.Scores {
		scores = append(scores, model.AMVScore{score.Judge, score.Score})
	}

	return &model.AMV{amv.Filename, duration, scores}
}

func amvFromModel(modelAMV *model.AMV) *amv {
	duration := modelAMV.Duration.String()
	level := 0
	scores := make([]amvScore, 0, len(modelAMV.Scores))
	for _, score := range modelAMV.Scores {
		scores = append(scores, amvScore{score.Judge, score.Score})
	}
	return &amv{modelAMV.Filename, duration, level, scores}
}

func (playlist *playlist) toModel() *model.Playlist {
	return &model.Playlist{playlist.Date, playlist.Viewings}
}

func playlistFromModel(modelPlaylist *model.Playlist) *playlist {
	return &playlist{modelPlaylist.Date, modelPlaylist.Viewings}
}

func parseDuration(duration string) time.Duration {
	if strings.HasPrefix(duration, "PT") {
		duration = duration[2:]
	}
	duration = strings.ToLower(duration)
	parsed, err := time.ParseDuration(duration)
	if err != nil {
		fmt.Println("Error converting duration:", duration, "-", err)
		return time.Duration(0)
	}
	return parsed
}

type XMLBackingStore struct{}

// load the data from a file
func (store XMLBackingStore) Load() {
	conf := &config.Config

	if conf.Debug {
		fmt.Println("Loading data file: data.xml")
	}
	xmlData, err := ioutil.ReadFile("data.xml")
	if err != nil {
		fmt.Println("Error opening data.xml:", err)
		return
	}
	if len(xmlData) == 0 {
		fmt.Println("No content in data.xml")
		return
	}

	// parse the XML into objects
	var library xmlLibrary
	if err := xml.Unmarshal(xmlData, &library); err != nil {
		fmt.Println("Error decoding XML:", err)
		return
	}
	if conf.Debug {
		fmt.Println("Loaded", len(library.AMVs), "AMVs")
		n := 0
		for _, amv := range library.AMVs {
			n += len(amv.Scores)
		}
		fmt.Println("Loaded", n, "AMV scores")
		fmt.Println("Loaded", len(library.Playlists), "playlists")
	}

	// push to the model
	amvs := make([]*model.AMV, 0, len(library.AMVs))
	for _, amv := range library.AMVs {
		if modelAMV := amv.toModel(); modelAMV != nil {
			amvs = append(amvs, modelAMV)
		}
	}
	model.AMVs = amvs

	playlists := make([]*model.Playlist, 0, len(library.Playlists))
	for _, playlist := range library.Playlists {
		if modelPlaylist := playlist.toModel(); modelPlaylist != nil {
			playlists = append(playlists, modelPlaylist)
		}
	}
	model.Playlists = playlists
}

func (store XMLBackingStore) Save() {
	// pull from the model
	var library xmlLibrary
	library.XMLName = xml.Name{"", "library"}

	library.AMVs = make([]*amv, 0, len(model.AMVs))
	for _, modelAMV := range model.AMVs {
		library.AMVs = append(library.AMVs, amvFromModel(modelAMV))
	}

	library.Playlists = make([]*playlist, 0, len(model.Playlists))
	for _, modelPlaylist := range model.Playlists {
		library.Playlists = append(library.Playlists, playlistFromModel(modelPlaylist))
	}

	// save the xml
	xmlData, err := xml.MarshalIndent(&library, "", "  ")
	if err != nil {
		fmt.Println("Error encoding XML:", err)
		return
	}

	err = ioutil.WriteFile("data.xml", xmlData, os.ModePerm)
	if err != nil {
		fmt.Println("Error saving library:", err)
		return
	}
}
