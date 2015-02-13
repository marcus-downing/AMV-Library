package cli

import (
	// "../config"
	"fmt"
	// "os"
	"../exe"
	"../match"
	"../model"
	"bufio"
	"math/rand"
	"strings"
	"unicode"
	"unicode/utf8"
)

func ScoreCLI(scanner *bufio.Scanner, judge string, terms []string) bool {
	// conf := &config.Config

	fmt.Println(reset + "Score AMVs")

	amv := scorableAMV(judge)
	if amv == nil {
		fmt.Println("Out of AMVs to score")
		return false
	}
	ShowAMV(amv)
	exe.PlayAMV(amv)

	ScorePrompt()
	for scanner.Scan() {
		line := scanner.Text()
		words := strings.Split(line, " ")

		// command
		var command string
		if len(words) > 0 {
			command = strings.ToLower(words[0])
		}

		if command == "quit" || command == "q" {
			return false
		} else if command == "exit" {
			return true
		} else if len(command) == 1 && isDigit(command) {
			// score the AMV

			// next amv!
			amv = scorableAMV(judge)
			if amv == nil {
				fmt.Println("Out of AMVs to score")
				return false
			}
			ShowAMV(amv)
			exe.PlayAMV(amv)
		} else {

		}
		ScorePrompt()
	}
	return false
}

func isDigit(str string) bool {
	rune, _ := utf8.DecodeRuneInString(str)
	return unicode.IsDigit(rune)
}

func scorableAMV(judge string) *model.AMV {
	candidates := match.CandidateAMVs(&match.MatchParameters{
		Unscored: true,
		Judge:    judge,
	})
	if len(candidates) == 0 {
		return nil
	}
	return candidates[rand.Intn(len(candidates))]
}

func ScorePrompt() {
	fmt.Print("\n" + yellow + "score 0-9> " + white)
}
