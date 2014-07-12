package cli

import (
	"../config"
	"../match"
	"../model"
	"bufio"
	"fmt"
	"os"
	"strings"
)

func CLI() {
	conf := &config.Config

	if conf.Debug {
		fmt.Println("\033[35mDebugging on\n")
	}
	fmt.Println("\033[0;1m" + conf.Name + "\033[0m")

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
			//playlists.Scan()
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
			ScoreCLI()
		} else {
			//  fallback case: search for matches
			amvs := match.Match(words)
			ShowAMVs(amvs)

			//amvs := match.Find(words)
			//ShowAMVs(amvs)
		}
		Prompt()
	}
}

func Prompt() {
	fmt.Print("\n\033[36m> \033[0m")
}

func ShowAMVs(amvs []*model.AMV) {
	for _, amv := range amvs {
		ShowAMV(amv)
	}
}

/*
	0	 reset
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

func ShowAMV(amv *model.AMV) {
	if config.Config.Debug {
		fmt.Printf("\n   \033[35m%#v", amv)
	}
	fmt.Printf("\n   \033[32m%s   \033[0m(%s)\n", amv.Name(), amv.Folder())
	fmt.Printf("   \033[33mDuration:    %s\n", amv.GetDurationStr())

	viewings, longago := model.AMVViewings(amv)
	if len(viewings) == 0 && !longago {
		fmt.Println("   \033[31mNever shown")
	} else {
		viewed := make([]string, 0, len(viewings)+1)
		for _, viewing := range viewings {
			viewed = append(viewed, viewing.Format("2006 Jan 02"))
		}
		if longago && len(viewings) < 5 {
			if len(viewed) == 0 {
				viewed = append(viewed, "Long ago")
			} else {
				viewed = append(viewed, "long ago")
			}
		}
		fmt.Printf("   \033[36mLast shown:  %s\n", viewed[0])
		if len(viewed) > 1 {
			fmt.Printf("   \033[34mAlso shown:  %s\n", strings.Join(viewed[1:], ", "))
		}
	}
}
