package mobilecore

// This file is the explicit missing integration point.
// Replace with a real adapter calling the original session/proto/farmer code.
// NEVER return a dummy worker that reports success without performing game tasks.
func newGameWorker() worker { return nil }
