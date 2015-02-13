package model

type ModelBackingStore interface {
	Load()
	Save()
}

var theBackingStore ModelBackingStore

func SetBackingStore(store ModelBackingStore) {
	theBackingStore = store
}

func Load() {
	if theBackingStore != nil {
		theBackingStore.Load()
	}
}

func Save() {
	if theBackingStore != nil {
		theBackingStore.Save()
	}
}
