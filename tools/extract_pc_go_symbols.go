//go:build ignore
// Reproduces the static symbol inventory from a stripped Go executable.
// Usage: go run tools/extract_pc_go_symbols.go /path/to/avatar-bot-linux-amd64
package main

import (
    "debug/elf"
    "debug/gosym"
    "fmt"
    "os"
    "strings"
)
func main(){
    if len(os.Args)<2 { fmt.Fprintln(os.Stderr,"usage: go run extract_pc_go_symbols.go <linux-ELF>");os.Exit(2) }
    f,err:=elf.Open(os.Args[1]);if err!=nil{panic(err)};defer f.Close()
    pcln,err:=f.Section(".gopclntab").Data();if err!=nil{panic(err)}
    tbl,err:=gosym.NewTable(nil,gosym.NewLineTable(pcln,f.Section(".text").Addr));if err!=nil{panic(err)}
    for _,fn:=range tbl.Funcs {
        if strings.Contains(fn.Name,"avatar-bot/internal/") {fmt.Printf("0x%x\t%s\n",fn.Entry,fn.Name)}
    }
}
