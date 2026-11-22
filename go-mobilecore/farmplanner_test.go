package mobilecore

import (
	"encoding/json"
	"testing"
)

func TestOfflineFarmPlanning(t *testing.T) {
	snapshot := FarmSnapshot{
		Plots:         []FarmPlot{{ID: "1", Ripe: true}, {ID: "2", Empty: true}, {ID: "3", Dry: true, Weeds: true, Pests: true}},
		Animals:       []FarmAnimal{{ID: "chicken", Hungry: true, Ill: true, ProductReady: true}},
		MerchantReady: true, CookingReady: true, StarfruitReady: true,
	}
	opts := FarmOptions{Farm: true, Harvest: true, Plant: true, Water: true, Animals: true, Merchant: true, Cooking: true, Starfruit: true}
	actions, err := PlanFarm(snapshot, opts)
	if err != nil {
		t.Fatal(err)
	}
	expected := []string{"harvest", "plant", "weed", "pest_control", "water", "heal", "feed", "collect", "activate", "cook", "collect"}
	if len(actions) != len(expected) {
		t.Fatalf("got %#v", actions)
	}
	for i, a := range actions {
		if a.Operation != expected[i] {
			t.Fatalf("action %d: %#v", i, a)
		}
	}
	opts.Farm = false
	actions, err = PlanFarm(snapshot, opts)
	if err != nil || len(actions) != 0 {
		t.Fatal("disabled farm should not emit actions")
	}
}
func TestFarmValidationAndPublicAPI(t *testing.T) {
	e := NewEngine()
	raw := e.PlanFarmJSON(`{"plots":[{"id":"p01","ripe":true}],"animals":[]}`, `{"farm":true,"harvest":true}`)
	var result struct {
		OK      bool            `json:"ok"`
		Actions []PlannedAction `json:"actions"`
	}
	if err := json.Unmarshal([]byte(raw), &result); err != nil {
		t.Fatal(err)
	}
	if !result.OK || len(result.Actions) != 1 || result.Actions[0].Operation != "harvest" {
		t.Fatal(raw)
	}
	for _, snap := range []string{`{"plots":[{"id":"p","ripe":true,"empty":true}]}`, `{"plots":[{"id":"p"},{"id":"p"}]}`, `{"plots":[],"password":"never"}`} {
		raw = e.PlanFarmJSON(snap, `{"farm":true,"harvest":true}`)
		var bad struct {
			OK bool `json:"ok"`
		}
		_ = json.Unmarshal([]byte(raw), &bad)
		if bad.OK {
			t.Fatal("accepted invalid", snap)
		}
	}
}
