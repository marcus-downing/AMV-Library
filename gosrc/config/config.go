package config

import (
	"fmt"
	toml "github.com/BurntSushi/toml"
	"io/ioutil"
)

type Configuration struct {
	Name              string
	Debug             bool
	Fail              bool
	LibraryDirectory  string `toml:"libraryDir"`
	PlaylistDirectory string `toml:"playlistDir"`
	BackupDirectory   string `toml:"backupDir"`
	Categories        map[string]Category
	Levels            Levels
	Exe               ExePaths //map[string]string
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

func init() {
	Config = Configuration{
		Name:       "AMV Library",
		Debug:      false,
		Fail:       false,
		Categories: make(map[string]Category, 20),
	}

	configData, err := ioutil.ReadFile("config.toml")
	if err != nil {
		fmt.Println("Error opening config.toml:", err)
		Config.Fail = true
		return
	}
	if _, err := toml.Decode(string(configData), &Config); err != nil {
		// handle error
		fmt.Println("Error decoding config.toml:", err)
		Config.Fail = true
	}

	if Config.Debug {
		DebugConfig()
	}
}

func DebugConfig() {
	fmt.Printf("Config: %#v\n", Config)
}
