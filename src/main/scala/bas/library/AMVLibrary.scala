package bas.library

import bas.library.commands._
//import bas.library.select._
import java.io._
import org.joda.time._
import org.joda.time.format._
import playlists._
// import scala.actors.Actor
import bas.store.xml.Controller
//import unicode.UnicodeReader
import collection.mutable.ListBuffer
import bas.tools.xml.JodaConversions._
import bas.store._

object AMVLibrary {
    def recentPlaylists: List[String] = {
        var found = new ListBuffer[String]
        val startDate = new LocalDate().minusMonths(2)
        val endDate = new LocalDate().plusMonths(2)

        var date = startDate
        while (date < endDate) {
            def twoFigures(v: Int) = "%02d" format v
            found += date.getYear() + " " + twoFigures(date.getMonthOfYear()) + " " + twoFigures(date.getDayOfMonth())
            date = date.plusDays(1)
        }
        found.toList
    }

    def futurePlaylists: List[String] = {
        var found = new ListBuffer[String]
        val startDate = new LocalDate()
        val endDate = new LocalDate().plusMonths(2)

        var date = startDate
        while (date < endDate) {
            def twoFigures(v: Int) = "%02d" format v
            found += date.getYear() + " " + twoFigures(date.getMonthOfYear()) + " " + twoFigures(date.getDayOfMonth())
            date = date.plusDays(1)
        }
        found.toList
    }

    def backupPlaylist (playlistName: String, destinationPath: String): Int = {
        val playlist = PlaylistFile.fromName(playlistName).get
        backupPlaylist(playlist, Settings.basePath, destinationPath)
    }

    def backupFuturePlaylists (destinationPath: String): Int = {
        var backedUp = 0
        for (file <- futurePlaylists; playlist <- PlaylistFile.fromName(file))
            backedUp += backupPlaylist(playlist, Settings.basePath, destinationPath)
        return backedUp
    }

    def backupPlaylist(playlist: PlaylistFile, sourcePath: String, destinationPath: String) = {
        val zplDestination = new ZoomPlayerPlaylist(new File(destinationPath + "playlists/" + playlist.name + ".zpl"))
        val m3uDestination = new M3UPlaylist(new File(destinationPath + playlist.name + ".m3u"))
        val entries: List[PlaylistEntry] = playlist.read
        for (entry <- entries)
            backupFile(sourcePath, destinationPath, entry.path)
        zplDestination.write(destinationPath, entries)
        m3uDestination.write(destinationPath, entries)
        1
    }

    def backupFile (sourcePath: String, destinationPath: String, filePath: String) = {
        val source = new File(sourcePath + filePath)
        val destination = new File(destinationPath + filePath)
        if (source.exists) {
            if (destination.exists) {
                if (destination.length == source.length)
                    println(filePath + " (exists)")
                else {
                    copyFile(source, destination)
                    println(filePath + " (incomplete)")
                }
            } else {
                copyFile(source, destination)
                println(filePath)

            }

            //  also copy associated files
        }
    }

    def copyFile (source: File, destination: File) = {
        val in = new FileInputStream(source)
        if (!destination.exists) {
            destination.getParentFile.mkdirs
            destination.createNewFile
        }
        val out = new FileOutputStream(destination)
        out.getChannel.transferFrom(in.getChannel, 0, Long.MaxValue)
        in.close();
        out.close();
    }

    def scanLibrary = {
        val root = new File(Settings.basePath)
        val files = scanLibraryFolder(root, "")
        val added = new ListBuffer[AMV]
        val removed = new ListBuffer[AMV]

        files foreach { case (file: File, path: String) =>
            var level = 3
            (1 to 5) foreach { n =>
                if (Settings.levelPaths(n).exists(path.startsWith(_)))
                    level = n
            }
            val amv = new AMV(path, new Period(), level, Nil)
            Controller.addAMV(amv)
            added += amv
            println("Added AMV " + amv.path)
        }
        Controller.amvs.foreach { amv: AMV =>
            val file = new File(Settings.basePath + amv.path.replace("/", Settings.delim))
            if (!file.exists) {
                Controller.removeAMV(amv)
                removed += amv
                println("Removed AMV " + amv.path)
            }
        }

        for (r <- removed; a <- added) {
            if (r.similarity(a) >= 1.0) {
                Controller.transferAMV(r, a)
                println("Transferred AMV " + r.path + " to " + a.path)
            }
        }
        Controller.saveData
    }

    def scanLibraryFolder(folder: File, folderPath: String): Seq[(File, String)] = {
        //  get paths for children
        val files = folder.listFiles(null: FilenameFilter).flatMap{ file:File =>
            val path = if (folderPath == "") file.getName else folderPath + "/" + file.getName
            if (file.isDirectory) {
                scanLibraryFolder(file, path)
            } else {
                val excluded = Settings.excludePaths.exists(p => path.startsWith(p))
                val included = Settings.includePaths.exists(p => path.startsWith(p))
                val forbidden = Settings.excludePatterns.exists(p => file.getName.matches(p))
                if (forbidden || (excluded && !included))
                    None
                else {
                    val existing = Controller.getAMV(path)
                    existing match {
                        case Some(x:AMV) => None
                        case None => Some((file, path))
                    }
                }
            }
        }

        files
    }


    def scanAllPlaylists {
        val folder = new File(Settings.playlistFolder)
        scanPlaylistFolder(folder)
    }

    def scanRecentPlaylists {
		for {
		    name <- recentPlaylists
		    pl <- PlaylistFile.fromName(name)
		} pl.scan
	}

    def scanPlaylistFolder (folder: File) {
		if (folder.exists)
			for (file <- folder.listFiles(null: FilenameFilter)) {
				if (file.isDirectory)
					scanPlaylistFolder(file)
				else
					for (pl <- PlaylistFile.fromFile(file)) pl.scan
			}
	}
}
