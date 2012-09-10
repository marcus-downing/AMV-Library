package bas.library

import java.util.Properties
import java.io.{FileInputStream, File}

object Settings {
    val delim = """\"""
    val basePath = """C:\Users\Marcus Downing\Documents\AMVs\"""
    val altPath = List("""G:\AMVs\""", """Z:\AMVs\""", """C:\Documents\AMVs\""")
    val playlistFolder = """C:\Users\Marcus Downing\Documents\Anime\Playlists\"""
    val backupPath = """C:\Users\Marcus Downing\Documents\Anime Society\Backup\"""

    val levelPaths = Map(
        1 -> List("Serious"),
        2 -> List("Semi-serious", "Mood", "Dance", "Action"),
        3 -> List("Long", "Classical", "Meta", "Trailers"),
        4 -> List("Musicals", "Amusing", "Parody", "Anthology", "Long/Anthology", "Gems", "Spoken", "Short"),
        5 -> List("Baffling", "Dubious", "Hyper")
    )

    val restrictPaths = List("Buffer", "Long", "Christmas", "Halloween", "Meta", "Serious")
    val excludePaths = List("April Fool", "Incoming", "BAS", "Dyslexic Studeos")
    val includePaths = List()
    val excludePatterns = List("""Thumbs\.db""", """.*\.ass""", """.*\.txt""")
    val allCategories = (levelPaths(1) ::: levelPaths(2) ::: levelPaths(3) ::: levelPaths(4) ::: levelPaths(5) ::: excludePaths ::: restrictPaths ::: includePaths).distinct
    val categoryWords = allCategories.flatMap(_.split("/")).map(_.trim.toLowerCase).filter(_ != "")
    
    var debug = false
    
    //  players: "zoomPlayer" "mediaPlayerClassic" "vlc"
    //val player = "zoomPlayer"
    trait Player; object ZoomPlayer extends Player; object MediaPlayerClassic extends Player; object VLC extends Player
    
    val player: Player = ZoomPlayer
	val zoomPlayerExe = """c:\Program Files (x86)\Zoom Player\zplayer.exe"""
	val mediaPlayerClassicExe = ""
	val vlcExe = """C:\Program Files (x86)\VLC\vlc.exe"""
}