package bas.library.playlists

import org.joda.time.{Period, LocalDate}
import org.joda.time.format.DateTimeFormat
import bas.library.Settings
import java.io.{FileInputStream, File}
import org.mozilla.universalchardet.UniversalDetector
//import scala.tools.nsc.io.Streamable
import io.{BufferedSource, Source}
import collection.mutable.ListBuffer
import java.util.Arrays
import bas.store.xml.Controller
import bas.store.{AMV, Viewing, Playlist}
import bas.tools.log.Log

object PlaylistFile {
    val dateFormat = DateTimeFormat.forPattern("yyyy MM dd")
    def date2name(date: LocalDate): String = date.toString(dateFormat)
    def name2date(name: String): LocalDate = dateFormat.parseDateTime(name).toLocalDate

    def fromPlaylist(playlist: Playlist): PlaylistFile = fromName(dateFormat.print(playlist.date)).get
    def fromName(name: String, folder: String = Settings.playlistFolder): Option[PlaylistFile] =
        if (name.endsWith(".zpl") || name.endsWith(".m3u"))
            fromFile(new File(folder+name))
        else try {
            val zpl = new File(folder+name+".zpl")
            val m3u = new File(folder+name+".m3u")

            if (zpl.exists) Some(new ZoomPlayerPlaylist(zpl))
            else if (m3u.exists) Some(new M3UPlaylist(m3u))
            else fromFile(new File(folder + name))
        } catch { case x =>
            x.printStackTrace
            None
        }

    def fromFile(file: File): Option[PlaylistFile] = try {
        if (file.exists) {
            val name = file.getName
            if (name.endsWith(".zpl")) Some(new ZoomPlayerPlaylist(file))
            else if (name.endsWith(".m3u")) Some(new M3UPlaylist(file))
            else None
        } else None
    } catch { case x =>
        x.printStackTrace
        None
    }
    
    def slurp(file: File): Array[Byte] = {
    	var len = file.length.toInt
    	val data = new Array[Byte](len)
    	val fis = new FileInputStream(file)
        try {
            var read = fis.read(data)
            var pos = read
            len -= read
            while (read > -1 && len > 0) {
                read = fis.read(data, pos, len)
        		pos += read
        		len -= read
            }
        } finally {
            fis.close();
        }
        data
    }

    def source(file: File): Source = {
        val detector = new UniversalDetector(null)
        val data: Array[Byte] = slurp(file) 
        	//Stream.continually(fis.read).takeWhile(-1 !=).map(_.toByte).toArray 
        	//new Streamable.Bytes { def inputStream = new FileInputStream(file) } toByteArray()
        detector.handleData(data, 0, data.length)
        detector.dataEnd
        val charset = detector.getDetectedCharset
        val bom = charset match {
            case "UTF-32LE" if data.startsWith(Array[Byte](0, 0, -1, -2)) => 4
            case "UTF-32BE" if data.startsWith(Array[Byte](0, 0, -2, -1)) => 4
            case "UTF-16LE" if data.startsWith(Array[Byte](-1, -2)) => 2
            case "UTF-16BE" if data.startsWith(Array[Byte](-2, -1)) => 2
            case "UTF-8" if data.startsWith(Array[Byte](0xEF.asInstanceOf[Byte], 0xBB.asInstanceOf[Byte], 0xBF.asInstanceOf[Byte])) => 3
            case _ => 0
        }
        val data2 = if (bom == 0) data else Arrays.copyOfRange(data, bom, data.length - bom)
        Source.fromBytes(data2, charset)
    }
}

abstract class PlaylistFile (val file: File) {
    def name: String = file.getName
    def date: LocalDate = {
        val nm = name.lastIndexOf('.') match { case -1 => name; case i => name.substring(0, i) }
        PlaylistFile.name2date(nm)
    }
    def write(destinationBase: String, entries: List[PlaylistEntry]) = throw new UnsupportedOperationException
    def read: List[PlaylistEntry]

    def copy(destination: PlaylistFile) = {
        val entries = read
        val entries2 = entries.map { from =>
            val topath = from.path // TRANSFORM PATH
            val to = new PlaylistEntry(topath, from.duration)
            from.copy(to)
            to
        }
        destination.write("", entries2)
        throw new UnsupportedOperationException
    }

    def scan = {
        val playlist = new Playlist(date)
        Log ! "Scanning " + name
        playlist.viewings = for {
            entry <- read
            amv <- entry.amv
        } yield {
            for (duration <- entry.duration if duration != new Period())
                amv.duration = duration
            Log ! amv.path
            new Viewing(amv, date)
        }
        Log ! "Scanned " + name + " - " + playlist.viewings.length + " AMVs"
        Controller.addPlaylist(playlist)
        Controller.saveData
    }
}

class ZoomPlayerPlaylist (file: File) extends PlaylistFile (file) {
    def read = {
        val entries = new ListBuffer[PlaylistEntry]
        var path: String = null
        var duration: Option[Period] = None
        for (line <- PlaylistFile.source(file).getLines) {
            if (line.startsWith("nm="))
                path = line.substring(3)
            else if (line.startsWith("dr="))
                duration = Some(Period.seconds(line.substring(3).toInt))
            else if (line == "br!") {
                if (path != null)
                    entries += new PlaylistEntry(path, duration)
                duration = None
            }
        }
        entries.toList
    }
}

class M3UPlaylist (file: File) extends PlaylistFile (file) {
    def read = {
        val entries = new ListBuffer[PlaylistEntry]
        var path: String = null
        var duration: Option[Period] = None
        for (line <- PlaylistFile.source(file).getLines) {
            if (line.startsWith("#EXTINF:")) {
                val d = line.substring(8, line.indexOf(',', 8))
                duration = Some(Period.seconds(d.toInt))
            } else if (!line.startsWith("#")) {
                entries += new PlaylistEntry(line, duration)
                duration = None
            }
        }
        entries.toList
    }
}

case class PlaylistEntry (path: String, duration: Option[Period]) {
    def file = new File(path)
    def copy (destination: PlaylistEntry) = {
        val from = file
        val to = destination.file
        0
    }
    def amv: Option[AMV] = {
		val path2 = for {
			base <- Settings.basePath :: Settings.altPath
			if path.startsWith(base)
		} yield path.substring(base.length)
		
		path2.headOption flatMap { p =>
		  	val p2 = p.replace(Settings.delim, "/")
			Controller.getAMV(p2) 
		}
    }
}