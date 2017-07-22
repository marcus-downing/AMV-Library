package playlists

import (
	config "../config"
	// model "../model"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"time"
)

func BackupPlaylist(date string) {
	dir := config.Config.PlaylistDirectory
	path := dir + date + ".zpl"
	if _, err := os.Stat(path); err != nil {
		fmt.Println("\nCouldn't read playlist", date+":", err)
		return
	}

	items := ScanPlaylistZpl(path)
	if len(items) < config.Config.CompletePlaylistThreshold {
		fmt.Println("\nSkipping incomplete playlist:", date)
		return
	}

	//  copy the files
	fmt.Println("\nBacking up playlist:", date)

	for _, backupDir := range config.BackupDestinations() {
		if _, err := os.Stat(backupDir.Destination); err != nil {
			fmt.Println("Destination "+backupDir.Destination+" unreachable:", err)
			continue
		}
		// playlistName := backupDir.PlaylistName(date)
		if config.Debug {
			fmt.Println(" - " + backupDir.Destination)
		}

		newitems := make([]ScannedItem, 0, len(items))
		for _, item := range items {
			newpath, err := backupFile(item.Name, date, backupDir)
			if err == nil && newpath != nil {
				newitems = append(newitems, ScannedItem{*newpath, item.Duration})
			}
		}
		if len(newitems) > 0 {
			//  write the new playlist file
			//  write the transformed playlist
			// writePlaylist(newitems)
		}
	}

}

func backupFile(src, date string, backupDir config.BackupDestination) (*string, error) {
	var itemname string

	if strings.HasPrefix(src, config.Config.LibraryDirectory) {
		itemname = "AMVs" + string(os.PathSeparator) + strings.Trim(src[len(config.Config.LibraryDirectory):], string(os.PathSeparator))
	} else if strings.HasPrefix(src, config.Config.AnimeDirectory) {
		itemname = "Anime" + string(os.PathSeparator) + strings.Trim(src[len(config.Config.LibraryDirectory):], string(os.PathSeparator))
	} else {
		fmt.Println("Where is this?", src)
		return nil, nil
	}

	dst := backupDir.LocatePath(itemname, date)

	// ext := filepath.Ext(itemname)

	if config.Debug {
		fmt.Println("   - " + itemname)
		fmt.Println("      < " + src)
		fmt.Println("      > " + dst)
	} else {
		fmt.Print("   " + itemname)
	}

	var srcSize int64
	if stat, err := os.Stat(src); err != nil {
		fmt.Println("    ... stat error:", err)
		return nil, err
	} else {
		srcSize = stat.Size()
	}

	srcFile, err := os.Open(src)
	if err != nil {
		fmt.Println("    ... open error:", err)
		return nil, err
	}

	var dstFile *os.File
	if stat, err := os.Stat(dst); err == nil {
		dstSize := stat.Size()
		if dstSize < srcSize {
			fmt.Println("    ... resuming copy")
			dstFile, err = os.OpenFile(dst, os.O_APPEND, 0777)
			if err != nil {
				fmt.Println("    ... append error:", err)
				return nil, err
			}
			srcFile.Seek(dstSize, 0)
		} else if dstSize > srcSize {
			fmt.Println("   ... huh? Destination file is bigger!")
			return nil, nil
		} else {
			fmt.Println("    ... exists")
			return nil, nil
		}
	} else {
		dstDir := filepath.Dir(dst)
		if _, err := os.Stat(dstDir); err != nil {
			fmt.Println("      - Creating dir:", dstDir)
			if err := os.MkdirAll(dstDir, 0777); err != nil {
				fmt.Println("    ... mkdirs error:", err)
				return nil, err
			}
		}

		dstFile, err = os.Create(dst)
		if err != nil {
			fmt.Println("    ... create error:", err)
			return nil, err
		}
	}

	// fmt.Println("io.Copy", dstFile, srcFile)
	if _, err := io.Copy(dstFile, srcFile); err != nil {
		fmt.Println("    ... copy error:", err)
		dstFile.Close()
		return nil, err
	}
	if err = dstFile.Close(); err != nil {
		fmt.Println("    ... error closing file:", err)
		return nil, err
	}

	return &dst, nil
}

const dateFormat = "2006 01 02"

func BackupAll() {
	dir := config.Config.PlaylistDirectory
	files, err := ioutil.ReadDir(dir)
	if err != nil {
		fmt.Println(err)
		return
	}

	today := time.Now()

	for _, file := range files {
		filename := file.Name()
		ext := filepath.Ext(filename)
		if ext == ".zpl" {
			date := filename[:len(filename)-len(ext)]
			if timestamp, err := time.Parse(dateFormat, date); err == nil {
				if timestamp.After(today) {
					BackupPlaylist(date)
				}
			}
		}
	}
}
