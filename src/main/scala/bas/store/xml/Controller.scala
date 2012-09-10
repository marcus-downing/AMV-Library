package bas.store.xml

import bas.store._
import scala.xml._
import org.joda.time._
import bas.tools.xml.JodaConversions._

object Controller {
    var data = XML.loadFile("data.xml")
    var amvs: List[AMV] = data \ "amvs" \ "amv" map { node => node2amv(node) } toList
    var playlists: List[Playlist] = data \ "playlists" \ "playlist" map { node =>
            val date = node \ "date" toDate
            val amvs1 = node \ "viewing"
            val amvs2 = amvs1.flatMap { n =>
                    val amv = getAMV(n text)
                    amv match {
                        case Some(amv2) => Some(new Viewing(amv2, date))
                        case _ => None
                    }
            }
            val amvs3 = amvs2.toArray
            new Playlist(date, amvs3)
        } toList

    val currentData: ShowingSettings = new ShowingSettings(data)

    //  access the data
    implicit def node2amv (node: Node): AMV = {
        val path = (node \ "filename").text.toString
        val duration = (node \ "duration").toPeriod
        val level = (node \ "level").text.toString.toInt
        val scores = (node \ "score").map { scoreNode =>
        	val user: String = (scoreNode \ "judge").text.toString
        	val score: Int = (scoreNode \ "score").text.toString.toInt
        	new AMVScore(user, AMVScore.normalise(score))
        }
        val amv = new AMV(path, duration, level, scores.toList)
        amv
    }

    def amvsByPath: Map[String, AMV] = Map(amvs map { amv => amv.path -> amv } : _*)

    def isEmpty = amvs.isEmpty

    def addAMV (amv: AMV) = {
        amvs = amvs.filterNot(_.path == amv.path)
        amvs = amv :: amvs
    }

    def removeAMV (amv: AMV) = {
        amvs = amvs.filterNot(_.path == amv.path)
    }

    def transferAMV (from: AMV, to: AMV) = {
        for (playlist <- playlists) {
            playlist.viewings = playlist.viewings.map { viewing =>
                if (viewing.amv == from)
                    new Viewing(to, viewing.date)
                else
                    viewing
            }
        }
    }

    def getAMV (path: String): Option[AMV] = amvs filter { amv => amv.path == path } firstOption

    def randomAMV: AMV = {
        val num = Math.floor(Math.random * amvs.length).toInt
        amvs(num)
    }

    def getPlaylist (date: LocalDate): Option[Playlist] =
        playlists filter (_.date == date) firstOption

    def latestPlaylist: Playlist =
      playlists.sort((a: Playlist, b: Playlist) => a.date < b.date).last

    def getPlaylists (from: Option[LocalDate], to: Option[LocalDate]): Seq[Playlist] = {
        (from, to) match {
            case (Some(from2: LocalDate), Some(to2: LocalDate)) =>
                playlists filter ( p => p.date >= from2 && p.date < to2 )
            case _ =>
        }
        //playlists filter ()
        Nil
    }          

    def viewings: Seq[Viewing] = playlists flatMap { playlist => playlist.viewings }

    def viewings (amv: AMV): Seq[Viewing] = {
        val viewings = playlists flatMap { _.viewings } filter { _.amv == amv }
        viewings.sort ((a: Viewing, b: Viewing) => a.date.compareTo(b.date) < 1)
    }

    def addPlaylist (playlist: Playlist) {
        playlists = playlists.remove { _.date == playlist.date }
        playlists = playlist :: playlists
    }

    def showingSettings = currentData

    //  write the data
    def saveData {
        data = <library>
            <amvs>{ amvs map { amv =>
                <amv>
                    <filename>{amv.path}</filename>
                    <duration>{amv.duration.toString}</duration>
                    <level>{amv.level.toString}</level>
                    { amv.scores map { score =>
                      	<score>
                    		<judge>{score.judge}</judge>
                    		<score>{score.score}</score>
                    	</score>
                    } }
                </amv>
            } }</amvs>
            <playlists>{ playlists map { playlist =>
                <playlist>
                    <date>{playlist.date.toString}</date>
                    { playlist.viewings map { v =>
                        <viewing>{v.amv.path}</viewing>
                    } }
                </playlist>
            } }</playlists>
            {  currentData.save }
        </library>

        XML.save("data.xml", data, "utf-8")
    }
}