package mobilecore

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"strings"
)

// FarmOptions are the user's offline configuration preferences. This planner
// does NOT encode game packets or implement any network automation.
type FarmOptions struct {
	Farm      bool `json:"farm"`
	Fishing   bool `json:"fishing"`
	Teacher   bool `json:"teacher"`
	Diamond   bool `json:"diamond"`
	Harvest   bool `json:"harvest"`
	Plant     bool `json:"plant"`
	Water     bool `json:"water"`
	Animals   bool `json:"animals"`
	Merchant  bool `json:"merchant"`
	Cooking   bool `json:"cooking"`
	Starfruit bool `json:"starfruit"`
}

type FarmPlot struct {
	ID    string `json:"id"`
	Ripe  bool   `json:"ripe"`
	Empty bool   `json:"empty"`
	Dry   bool   `json:"dry"`
	Weeds bool   `json:"weeds"`
	Pests bool   `json:"pests"`
}
type FarmAnimal struct {
	ID           string `json:"id"`
	Hungry       bool   `json:"hungry"`
	Ill          bool   `json:"ill"`
	ProductReady bool   `json:"productReady"`
}

// FarmSnapshot is an abstract state model; mapping server game responses to it
// requires authenticated session/proto code that is not in the supplied PC ZIP.
type FarmSnapshot struct {
	Plots          []FarmPlot   `json:"plots"`
	Animals        []FarmAnimal `json:"animals"`
	MerchantReady  bool         `json:"merchantReady"`
	CookingReady   bool         `json:"cookingReady"`
	StarfruitReady bool         `json:"starfruitReady"`
}
type PlannedAction struct {
	Target    string `json:"target"`
	Operation string `json:"operation"`
}

func strictJSON(raw string, dst any) error {
	if len(raw) > 1024*1024 {
		return errors.New("JSON input too large")
	}
	dec := json.NewDecoder(strings.NewReader(raw))
	dec.DisallowUnknownFields()
	if err := dec.Decode(dst); err != nil {
		return err
	}
	var extra any
	if dec.Decode(&extra) != io.EOF {
		return errors.New("unexpected trailing JSON")
	}
	return nil
}

func PlanFarm(snapshot FarmSnapshot, opts FarmOptions) ([]PlannedAction, error) {
	actions := []PlannedAction{}
	if !opts.Farm {
		return actions, nil
	}
	if len(snapshot.Plots) > 500 || len(snapshot.Animals) > 500 {
		return nil, errors.New("too many entities")
	}
	seen := make(map[string]bool, len(snapshot.Plots)+len(snapshot.Animals))
	for _, p := range snapshot.Plots {
		if strings.TrimSpace(p.ID) == "" || len(p.ID) > 64 || seen["plot:"+p.ID] {
			return nil, errors.New("invalid or duplicate plot ID")
		}
		seen["plot:"+p.ID] = true
		target := "plot:" + p.ID
		if p.Ripe && p.Empty {
			return nil, fmt.Errorf("contradictory state for %s", target)
		}
		if p.Ripe && opts.Harvest {
			actions = append(actions, PlannedAction{target, "harvest"})
		}
		if p.Empty && opts.Plant {
			actions = append(actions, PlannedAction{target, "plant"})
		}
		if !p.Ripe && !p.Empty {
			if p.Weeds && opts.Water {
				actions = append(actions, PlannedAction{target, "weed"})
			}
			if p.Pests && opts.Water {
				actions = append(actions, PlannedAction{target, "pest_control"})
			}
			if p.Dry && opts.Water {
				actions = append(actions, PlannedAction{target, "water"})
			}
		}
	}
	for _, a := range snapshot.Animals {
		if strings.TrimSpace(a.ID) == "" || len(a.ID) > 64 || seen["animal:"+a.ID] {
			return nil, errors.New("invalid or duplicate animal ID")
		}
		seen["animal:"+a.ID] = true
		if !opts.Animals {
			continue
		}
		target := "animal:" + a.ID
		if a.Ill {
			actions = append(actions, PlannedAction{target, "heal"})
		}
		if a.Hungry {
			actions = append(actions, PlannedAction{target, "feed"})
		}
		if a.ProductReady {
			actions = append(actions, PlannedAction{target, "collect"})
		}
	}
	if snapshot.MerchantReady && opts.Merchant {
		actions = append(actions, PlannedAction{"merchant", "activate"})
	}
	if snapshot.CookingReady && opts.Cooking {
		actions = append(actions, PlannedAction{"kitchen", "cook"})
	}
	if snapshot.StarfruitReady && opts.Starfruit {
		actions = append(actions, PlannedAction{"starfruit", "collect"})
	}
	return actions, nil
}

// PlanFarmJSON is an offline test/helper API for adapter developers.
// It NEVER talks to the game server, and response should not be labeled bot output.
func (e *Engine) PlanFarmJSON(snapshotJSON string, optionsJSON string) string {
	var snapshot FarmSnapshot
	var options FarmOptions
	result := struct {
		OK      bool            `json:"ok"`
		Actions []PlannedAction `json:"actions"`
		Error   string          `json:"error,omitempty"`
	}{Actions: []PlannedAction{}}
	if err := strictJSON(snapshotJSON, &snapshot); err != nil {
		result.Error = "Invalid farm snapshot"
	} else if err := strictJSON(optionsJSON, &options); err != nil {
		result.Error = "Invalid farm options"
	} else {
		a, err := PlanFarm(snapshot, options)
		if err != nil {
			result.Error = "Farm snapshot rejected"
		} else {
			result.OK = true
			result.Actions = a
		}
	}
	b, _ := json.Marshal(result)
	return string(b)
}
