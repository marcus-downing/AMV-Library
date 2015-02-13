package match

import (
	"../config"
	"../model"
	"fmt"
	"math/rand"
	"strconv"
	"strings"
	"time"
)

func init() {
	rand.Seed(time.Now().UTC().UnixNano())
}

func Any() *model.AMV {
	return model.AMVs[rand.Intn(len(model.AMVs))]
}

func Match(terms []string) []*model.AMV {
	params := parseTerms(terms)
	candidates := CandidateAMVs(params)
	if params.All {
		return candidates
	}
	scored := scoreAMVs(candidates, params)
	viable, threshold := thresholdAMVs(scored)
	params.Threshold = threshold
	selected := selectAMVs(viable, params)
	return actualAMVs(selected)
}

type MatchParameters struct {
	Terms       []string
	Categories  []config.Category
	Number      int
	All         bool
	AnyCategory bool
	NeverShown  bool
	Unscored    bool
	Judge       string
	Threshold   float64
	Duration    time.Duration
}

type MatchAMV struct {
	AMV   *model.AMV
	Score float64
}

func parseTerms(terms []string) *MatchParameters {
	conf := &config.Config
	params := MatchParameters{
		Terms:  make([]string, 0, len(terms)),
		Number: 1,
	}

	quotedMode := false
	durationMode := false
	var quotedTerms []string
terms:
	for _, term := range terms {
		term = strings.ToLower(term)
		if term == "" {
			continue
		}

		// multi-word modes...
		if durationMode {
			var mins, seconds int
			if _, err := fmt.Sscanf(term, "%d:%d", &mins, &seconds); err != nil {
				fmt.Println("Duration formation should be 0:00")
				fmt.Println(err)
			} else {
				params.Duration = (time.Duration(mins)*60 + time.Duration(seconds)) * time.Second
			}
			durationMode = false
		}

		if quotedMode {
			quotedTerms = append(quotedTerms, term)
			if strings.HasSuffix(term, "\"") {
				quotedMode = false
				quotedText := conflateQuotedTerms(quotedTerms)
				params.Terms = append(params.Terms, quotedText)
			}
			continue
		}

		// start a quoted section
		if strings.HasPrefix(term, "\"") {
			quotedMode = true
			quotedTerms = make([]string, 0, 5)
			quotedTerms = append(quotedTerms, term)
			continue
		}

		// keywords
		if term == "never" {
			params.NeverShown = true
			continue
		}

		if term == "any" {
			params.AnyCategory = true
			continue
		}

		if term == "all" {
			params.All = true
			continue
		}
		if term == "duration" {
			durationMode = true
		}

		// categories
		for slug, cat := range conf.Categories {
			slug = strings.TrimSpace(strings.TrimPrefix(slug, "categories."))
			name := strings.TrimSpace(strings.ToLower(cat.Name))
			if term == slug || term == name {
				params.Categories = append(params.Categories, cat)
				continue terms
			}
		}

		// number of results
		if n, err := strconv.Atoi(term); err == nil && n > 0 {
			params.Number = n
			continue
		}

		params.Terms = append(params.Terms, term)
	}

	if len(quotedTerms) > 0 {
		quotedText := conflateQuotedTerms(quotedTerms)
		params.Terms = append(params.Terms, quotedText)
	}

	// if params.AnyCategory {
	// 	params.Categories = conf.Categories
	// }

	if params.Number < 1 {
		params.Number = 1
	}

	if conf.Debug {
		fmt.Printf("   Search params: %#v\n", params)
	}
	return &params
}

func conflateQuotedTerms(terms []string) string {
	terms[0] = strings.TrimPrefix(terms[0], "\"")
	terms[len(terms)-1] = strings.TrimSuffix(terms[len(terms)-1], "\"")
	return strings.Join(terms, " ")
}

func CandidateAMVs(params *MatchParameters) []*model.AMV {
	all := model.AMVs
	candidates := make([]*model.AMV, 0, len(all))
	conf := &config.Config
	if len(params.Categories) == 0 {
		forbiddenFolders := categoryFolders(conf.GetCategories())
		for _, amv := range all {
			if !isAMVinFolders(amv, forbiddenFolders) {
				candidates = append(candidates, amv)
			}
		}
	} else {
		permittedFolders := categoryFolders(params.Categories)
		for _, amv := range all {
			if isAMVinFolders(amv, permittedFolders) {
				candidates = append(candidates, amv)
			}
		}
	}
	if conf.Debug {
		fmt.Println("Found", len(candidates), "candidates")
	}

	if params.NeverShown {
		neverShown := make([]*model.AMV, 0, len(candidates))
		for _, amv := range candidates {
			viewings, _ := model.AMVviewings(amv)
			if len(viewings) == 0 {
				neverShown = append(neverShown, amv)
			}
		}
		candidates = neverShown
		if conf.Debug {
			fmt.Println("Never shown:", len(candidates), "candidates")
		}
	}

	if params.Unscored {
		unscored := make([]*model.AMV, 0, len(candidates))
		for _, amv := range candidates {
			score := amv.ScoreBy("sadie")
			if score == nil {
				unscored = append(unscored, amv)
			}
		}
		candidates = unscored
		if conf.Debug {
			fmt.Println("Unscored by", params.Judge, ":", len(candidates), "candidates")
		}
	}
	return candidates
}

func categoryFolders(categories []config.Category) []string {
	conf := &config.Config
	folders := make([]string, 0, len(conf.Categories)*3)
	for _, category := range categories {
		for _, folder := range category.Folders {
			folders = append(folders, folder)
		}
	}
	return folders
}

func isAMVinFolders(amv *model.AMV, folders []string) bool {
	for _, folder := range folders {
		if strings.HasPrefix(amv.Folder(), folder) {
			return true
		}
	}
	return false
}

func scoreAMVs(amvs []*model.AMV, params *MatchParameters) []*MatchAMV {
	scored := make([]MatchAMV, 0, len(amvs))

	for _, amv := range amvs {
		if amv != nil {
			score := 0.0
			for _, function := range scoreFunctions {
				score += function(amv, params)
			}

			scored = append(scored, MatchAMV{
				AMV:   amv,
				Score: score,
			})
		}
	}

	// make a slice of pointers
	out := make([]*MatchAMV, 0, len(scored))
	for i, _ := range scored {
		out = append(out, &scored[i])
	}
	return out
}

func thresholdAMVs(amvs []*MatchAMV) ([]*MatchAMV, float64) {
	peak := 0.0
	for _, amv := range amvs {
		if amv.Score > peak {
			peak = amv.Score
		}
	}

	threshold := peak - 1.0
	out := make([]*MatchAMV, 0, len(amvs))
	for _, amv := range amvs {
		if amv.Score >= threshold {
			out = append(out, amv)
		}
	}
	return out, peak
}

func selectAMVs(amvs []*MatchAMV, params *MatchParameters) []*MatchAMV {
	pool := make([]*MatchAMV, 0, len(amvs))
	for _, amv := range amvs {
		pool = append(pool, amv)
	}

	out := make([]*MatchAMV, 0, params.Number)
	rem := len(pool)
	adjust := rem / 3
	if adjust < 10 {
		adjust = 10
	}

	for len(out) < params.Number && rem > 0 {
		i := rand.Intn(len(pool))
		if pool[i] == nil {
			continue
		}

		amv := pool[i]
		check := rand.Float64() + amv.Score
		if check > params.Threshold {
			out = append(out, amv)
			pool[i] = nil // prevent duplicate results

			// if we're exhausting the pool, squish it down to save from wasted lookups
			rem--
			if rem < adjust {
				pool2 := make([]*MatchAMV, 0, rem)
				for _, amv := range pool {
					if amv != nil {
						pool2 = append(pool2, amv)
					}
				}
				pool = pool2
				rem = len(pool)
				adjust = rem / 3
				if adjust < 10 {
					adjust = 10
				}
			}
		}
	}

	return out
}

func actualAMVs(amvs []*MatchAMV) []*model.AMV {
	out := make([]*model.AMV, 0, len(amvs))
	for _, amv := range amvs {
		if amv != nil {
			out = append(out, amv.AMV)
		}
	}
	return out
}
