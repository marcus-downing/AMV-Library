package config

import (
	"fmt"
	toml "github.com/BurntSushi/toml"
	"io/ioutil"
	"os"
)

type Configuration struct {
	Name                      string
	Debug                     bool
	Fail                      bool
	CompletePlaylistThreshold int
	LibraryDirectory          string     `toml:"libraryDir"`
	PlaylistDirectory         string     `toml:"playlistDir"`
	AnimeDirectory            string     `toml:"animeDir"`
	BackupDirectories         [][]string `toml:"backupTo"`
	Categories                map[string]Category
	Levels                    Levels
	Exe                       ExePaths //map[string]string

	// BackupDirectory           string     `toml:"backupDir"`
}

type Category struct {
	Name    string
	Folders []string
}

type Levels struct {
	one   []string
	two   []string
	three []string
	four  []string
	five  []string
}

type ExePaths struct {
	ZoomPlayer         string
	MediaPlayerClassic string
	VLC                string
}

func (conf *Configuration) GetCategories() []Category {
	out := make([]Category, 0, len(conf.Categories))
	for _, cat := range conf.Categories {
		out = append(out, cat)
	}
	return out
}

var Config Configuration
var Debug bool

func init() {
	LoadConfig(true)
}

func LoadConfig(initial bool) {
	config := Configuration{
		Name:       "AMV Library",
		Debug:      false,
		Fail:       false,
		Categories: make(map[string]Category, 20),
	}
	if initial {
		Config = config
	}

	configData, err := ioutil.ReadFile("config.toml")
	if err != nil {
		fmt.Println("Error opening config.toml:", err)
		Config.Fail = true
		return
	}
	if _, err := toml.Decode(string(configData), &config); err != nil {
		// handle error
		fmt.Println("Error decoding config.toml:", err)
		Config.Fail = true
		return
	}

	// if that worked, swap the config for the new one
	Config = config
	Debug = config.Debug

	if Debug {
		DebugConfig()
	}
}

func DebugConfig() {
	fmt.Printf("Config: %#v\n", Config)
}

// accessors
type BackupDestination struct {
	Destination string
	Format      string
	SplitByWeek bool
}

func BackupDestinations() []BackupDestination {
	destinations := make([]BackupDestination, 0, len(Config.BackupDirectories))
	for _, dst := range Config.BackupDirectories {
		dst = append(dst, "", "", "", "")
		path := dst[0]
		format := dst[1]
		splitByWeek := dst[2] == "true"
		destinations = append(destinations, BackupDestination{path, format, splitByWeek})
	}
	return destinations
}

func (dst *BackupDestination) PlaylistName(playlist string) string {
	return dst.LocatePath(playlist+dst.Format, playlist)
}

func (dst *BackupDestination) LocatePath(localpath, playlist string) string {
	sep := string(os.PathSeparator)
	if dst.SplitByWeek {
		return dst.Destination + sep + playlist + sep + localpath
	}
	return dst.Destination + sep + localpath
}
