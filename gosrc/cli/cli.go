package cli

import (
	// "../backup"
	"../config"
	"../match"
	"../model"
	"../playlists"
	"bufio"
	"fmt"
	"os"
	"runtime"
	"strings"
	"unicode/utf8"
)

/*
	0  reset
	30 black
	31 red
	32 green
	33 yellow
	34 blue
	35 purple
	36 cyan
	37 white
	;1 bold
*/

var (
	reset  = "\033[0m"
	black  = "\033[30m"
	red    = "\033[31m"
	green  = "\033[32m"
	yellow = "\033[33m"
	blue   = "\033[34m"
	purple = "\033[35m"
	cyan   = "\033[36m"
	white  = "\033[37m"
)

func init() {
	if runtime.GOOS == "windows" {
		reset = ""
		black = ""
		red = ""
		green = ""
		yellow = ""
		blue = ""
		purple = ""
		cyan = ""
		white = ""
	}
}

func CLI() {
	conf := &config.Config

	if conf.Debug {
		fmt.Println(purple + "Debugging on\n")
	}
	fmt.Println(reset + conf.Name)

	scanner := bufio.NewScanner(os.Stdin)
	Prompt()
	for scanner.Scan() {
		line := scanner.Text()
		words := strings.Split(line, " ")

		// command
		var command, cmd2 string
		if len(words) > 0 {
			command = strings.ToLower(words[0])
		}
		if len(words) > 1 {
			cmd2 = strings.ToLower(words[1])
		}

		if command == "quit" || command == "exit" {
			return
		} else if command == "scan" {
			//  scan playlist files and update the database
			playlists.Scan()
		} else if command == "debug" {
			if cmd2 == "on" || cmd2 == "true" {
				conf.Debug = true
				fmt.Println("Debugging on")
			} else if cmd2 == "off" || cmd2 == "false" {
				conf.Debug = false
				fmt.Println("Debugging off")
			} else if cmd2 == "conf" || cmd2 == "config" {
				config.DebugConfig()
			}
		} else if command == "score" {
			//  separate command line to score AMVs
			scoreWords := words[1:]
			scorer := ""
			if strings.HasPrefix(cmd2, "@") {
				scorer = strings.TrimPrefix(cmd2, "@")
				scoreWords = scoreWords[1:]
			}
			if exit := ScoreCLI(scanner, scorer, scoreWords); exit {
				return
			}
		} else if command == "backup" {
			//  backup the playlist and its attached files to another computer
			if len(words) >= 4 {
				playlist := strings.Join(words[1:4], " ")
				playlists.BackupPlaylist(playlist)
			} else {
				playlists.BackupAll()
			}
		} else {
			//  fallback case: search for matches
			amvs := match.Match(words)
			if len(amvs) == 0 {
				fmt.Println("\n  ?")
			}

			ShowAMVs(amvs)

			//amvs := match.Find(words)
			//ShowAMVs(amvs)
		}
		Prompt()
	}
}

func Prompt() {
	fmt.Print("\n" + green + "bas> " + white)
}

func ShowAMVs(amvs []*model.AMV) {
	for _, amv := range amvs {
		ShowAMV(amv)
	}
}

func padString(str string, len int) string {
	strlen := utf8.RuneCountInString(str)
	if len > strlen {
		padding := strings.Repeat(" ", len-strlen)
		return str + padding
	}
	return str
}

func ShowAMV(amv *model.AMV) {
	if config.Debug {
		fmt.Printf("\n  "+purple+"%#v\n", amv)
	}
	fmt.Printf("\n  Name:        "+green+"%s"+white+"        (%s)\n", amv.Name(), amv.Folder())
	// fmt.Printf(reset+"\n   %s "+green+"%s\n", padString(amv.Folder(), 12), amv.Name())
	if amv.HasDuration() {
		fmt.Printf("  "+yellow+"Duration:    %s\n", amv.GetDurationStr())
		// } else {
		// 	fmt.Printf("   " + yellow + "Duration unknown\n")
	}

	viewings, longago := model.AMVviewings(amv)
	if len(viewings) == 0 && !longago {
		fmt.Println("  " + red + "Never shown")
	} else {
		viewed := make([]string, 0, len(viewings)+1)
		for _, viewing := range viewings {
			viewed = append(viewed, viewing.Format("2 Jan 2006"))
		}
		if len(viewed) > 6 {
			viewed = viewed[0:6]
			longago = true
		}
		if longago {
			if len(viewed) == 0 {
				viewed = append(viewed, "Long ago")
			} else {
				viewed = append(viewed, "...")
			}
		}
		fmt.Printf("  "+cyan+"Last shown:  %s\n", viewed[0])
		if len(viewed) > 1 {
			fmt.Printf("  "+purple+"Also shown:  %s\n", strings.Join(viewed[1:], ", "))
		}
	}
}
