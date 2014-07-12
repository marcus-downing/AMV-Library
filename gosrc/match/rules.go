package match

import (
	"../model"
	// "fmt"
	"math"
	"regexp"
	"strings"
	"time"
)

// a function that nudges the score up or down
type ScoreFunc func(amv *model.AMV, params *MatchParameters) float64

func searchScore(amv *model.AMV, params *MatchParameters) float64 {
	if len(params.Terms) == 0 {
		return 0
	}

	score := 0.0
	name := strings.ToLower(amv.Name())
	for _, term := range params.Terms {
		if ok, _ := regexp.MatchString("\\b"+term+"\\b", name); ok {
			score++
		} else if ok, _ := regexp.MatchString(term, name); ok {
			score = score + 0.5
		}
	}
	if score == 0 {
		return -5000
	}
	return float64(score) / float64(len(params.Terms))
}

func durationScore(amv *model.AMV, params *MatchParameters) float64 {
	if params.Duration == 0 {
		return 0
	}

	diffSecs := math.Abs(amv.GetDuration().Seconds() - params.Duration.Seconds())
	return -diffSecs / 10
}

func recentScore(amv *model.AMV, params *MatchParameters) float64 {
	latest := model.LatestViewing(amv)
	if latest == nil {
		return 0
	}

	today := time.Now()
	diff := today.Sub(*latest)
	if diff < 0 {
		diff = 0
	}
	diffDays := int(diff.Hours() / 24)
	prox := math.Max(0, (720.0-float64(diffDays))/600.0)
	adjustment := -(prox * prox)
	// fmt.Println("Difference:", diffDays, "  Proximity:", prox, "  Adjustment:", adjustment)
	return adjustment
}

func styleScore(amv *model.AMV, params *MatchParameters) float64 {
	return 0
}

func folderScore(amv *model.AMV, params *MatchParameters) float64 {
	folder := amv.Folder()
	if strings.HasPrefix(folder, "Gems") {
		return 0.3
	}
	if strings.HasPrefix(folder, "Amusing") {
		return -0.05
	}
	return 0
}

func voteScore(amv *model.AMV, params *MatchParameters) float64 {
	if len(amv.Scores) == 0 {
		return 0
	}

	num := 2.0
	total := 0.0
	for _, amvScore := range amv.Scores {
		adj := (float64(amvScore.Score) - 5) / 5
		total += adj
		num++
	}
	return total / num
}

//  The actual list
var scoreFunctions []ScoreFunc = []ScoreFunc{
	searchScore,
	durationScore,
	recentScore,
	styleScore,
	folderScore,
	voteScore,
}
