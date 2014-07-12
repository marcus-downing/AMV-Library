package main

import (
	"./cli"
	"./model"
)

func main() {
	model.Load()
	cli.CLI()
}
