package main

import (
	"./cli"
	"./model"
	"./model/xml"
)

func main() {
	model.SetBackingStore(xml.XMLBackingStore{})
	model.Load()
	model.IndexAMVs()
	cli.CLI()
}
