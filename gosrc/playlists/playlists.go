package playlists

import (
	"../config"
	model "../model"
	"fmt"
	// "io"
	// "bufio"
	// "code.google.com/p/go-charset/charset"
	"io/ioutil"
	// "os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

type item struct {
	name     string
	duration string
}

func Scan() {
	conf := config.Config
	dir := conf.PlaylistDirectory

	if config.Config.Debug {
		fmt.Println("Reading dir", dir)
	}
	files, err := ioutil.ReadDir(dir)
	if err != nil {
		fmt.Println(err)
		return
	}

	n := 0
	for _, file := range files {
		filename := file.Name()
		path := dir + filename
		ext := filepath.Ext(filename)
		date := filename[:len(filename)-len(ext)]
		if ext == ".zpl" {
			items := ScanPlaylistZpl(filename, path)
			insertScannedItems(date, items)
			n++
		} else if ext == ".m3u" || ext == ".m3u8" {
			items := ScanPlaylistM3U(filename, path)
			insertScannedItems(date, items)
			n++
		}
	}

	model.IndexAMVs()
	model.Save()

	fmt.Println("Scanned", n, "playlists")
}

func insertScannedItems(date string, items []item) {
	conf := config.Config

	for _, item := range items {
		if conf.Debug {
			fmt.Println("Insert item", date, item)
		}
		if strings.HasPrefix(item.name, conf.LibraryDirectory) {
			path := item.name[len(conf.LibraryDirectory):]

			amv := model.AMVbyName(path)
			if amv == nil {
				amv = &model.AMV{path, 0, nil}
				if conf.Debug {
					fmt.Println("Adding new AMV: ")
				}
				model.AMVs = append(model.AMVs, amv)
			}

			if seconds, err := strconv.Atoi(item.duration); err == nil {
				duration := time.Duration(seconds) * time.Second
				if duration != 0 {
					amv.Duration = duration
				}
			}
		}

	}
}

func ScanPlaylistZpl(filename, path string) []item {
	if config.Config.Debug {
		fmt.Println("Reading ZoomPlayer playlist", filename)
	}

	data, err := ioutil.ReadFile(path)
	if err != nil {
		fmt.Println("Error reading", filename+":", err)
		return nil
	}

	decoded, err := utf16toString(data)
	if err != nil {
		fmt.Println("Error decoding", filename+":", err)
		return nil
	}

	lines := strings.Split(decoded, "\n")
	lines = append(lines, "br!")
	found := make([]item, 0, len(lines)/2)

	var nm, dr string
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "nm=") {
			nm = line[3:]
		} else if strings.HasPrefix(line, "dr=") {
			dr = line[3:]
		} else if line == "br!" {
			if nm != "" {
				found = append(found, item{nm, dr})
			}
			nm = ""
			dr = ""
		}
	}

	return found
}

func ScanPlaylistM3U(filename, path string) []item {
	if config.Config.Debug {
		fmt.Println("Reading M3U playlist", filename)
	}

	data, err := ioutil.ReadFile(path)
	if err != nil {
		fmt.Println("Error reading", filename+":", err)
		return nil
	}

	decoded := string(data)

	lines := strings.Split(decoded, "\n")
	if len(lines) == 0 {
		return nil
	}
	if lines[0] != "#EXTM3U" {
		fmt.Println("Warning: missing #EXTM3U header")
	}
	found := make([]item, 0, len(lines)/2)

	var dur string = ""
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "#EXTINF") {
			parts := strings.Split(line[1:], ",")
			if len(parts) > 0 {
				dur = parts[0]
			}
		} else if line != "" && !strings.HasPrefix(line, "#") {
			found = append(found, item{line, dur})
			dur = ""
		}
	}

	return found
}
