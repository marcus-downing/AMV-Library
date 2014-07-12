package model

import (
	"fmt"
	//"os"
	"../config"
	"encoding/xml"
	"io/ioutil"
)

func LoadXML() {
	/*
		xmlFile, err := os.Open("data.xml")
		if err != nil {
			fmt.Println("Error opening data.xml:", err)
			return
		}
		defer xmlFile.Close()
	*/

	conf := &config.Config

	if conf.Debug {
		fmt.Println("Loading data file: data.xml")
	}
	xmlData, err := ioutil.ReadFile("data.xml")
	if err != nil {
		fmt.Println("Error opening data.xml:", err)
		return
	}
	if len(xmlData) == 0 {
		fmt.Println("No content in data.xml")
		return
	}
	if err := xml.Unmarshal(xmlData, &Library); err != nil {
		fmt.Println("Error decoding XML:", err)
		return
	}
	if conf.Debug {
		fmt.Println("Loaded", len(Library.AMVs), "AMVs")
		n := 0
		for _, amv := range Library.AMVs {
			n += len(amv.Scores)
		}
		fmt.Println("Loaded", n, "AMV scores")
		fmt.Println("Loaded", len(Library.Playlists), "playlists")
	}
}

func SaveXML() {

}
