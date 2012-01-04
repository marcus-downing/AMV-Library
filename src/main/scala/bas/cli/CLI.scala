package bas.cli

import org.joda.time.LocalDate
import bas.tools.log._
import bas.library._
import bas.library.commands._
import java.io._
import bas.library.playlists.PlaylistFile
import scala.io._
import org.joda.time.format._
import bas.store.{Viewing, AMV, Playlist}
import org.joda.time.{Period}
import bas.tools.xml.JodaConversions._
import bas.store.xml.{Controller, ShowingSettings}
import collection.mutable.ListBuffer

object CLI {
	var latest: List[AMV] = Nil
    private var in: BufferedReader = null

    object Nothing

    def main (args: Array[String]): Unit = {
        Log.start
        in = new BufferedReader(new InputStreamReader(System.in))
        while (true) {
            print("bas> ");
            val line = in.readLine()
            doCommand(line)
        }
    }

    private val getLine = "get (.*)" r
    private val buildIntervalLine = "build interval" r
    private val buildPlaylistLine = "build playlist (.*)" r
    private val readPlaylistLine = "read playlist (.*)" r
    private val scanPlaylistLine = "scan playlist (.*)" r
    private val backupPlaylistLine = "backup playlist (.*)" r
    private val backupPlaylistToDestinationLine = "backup playlist (.*) to (.*)" r
    private val backupToDestinationLine = "backup to (.*)" r
    private val countAMVs = "count (.*)" r
    private val scoreAMVs = "score @([a-zA-Z0-9]+)(.*)" r
    private val rescoreAMVs = "rescore @([a-zA-Z0-9]+)(.*)" r

    private val dateFormat = DateTimeFormat.forPattern("yyyy MM dd")

    def doCommand (line: String) = {
        Log ! Cmd("\n> " + line)
        line.trim match {
            case "?" =>
                Source.fromFile("help.txt").getLines foreach { line: String => println(line.trim) }
                
            case "debug on" =>
                Settings.debug = true
            case "debug off" =>
                Settings.debug = false
            case getLine(path) =>
                val amv = Controller.getAMV(path)
                writeAMVs(amv.toList)

            case "count" =>
                val c = Controller.amvs.length
                writeCount(c)
            case countAMVs(style) =>
                val rules = parseRules(style)
                val c = rules.count
                writeCount(c)

            case "playlists" =>
                for (playlist <- Controller.playlists; date = playlist.date) {
                    println(date)
                }

            case readPlaylistLine(path) => for {
            	pl <- PlaylistFile.fromName(path) 
                entry <- pl.read} {
                    println(entry.path+", "+entry.duration)
            }

            case scanPlaylistLine(path) => for (pl <- PlaylistFile.fromName(path)) pl.scan
            case "scan all playlists" => AMVLibrary.scanAllPlaylists
            case "scan playlists" => AMVLibrary.scanRecentPlaylists
            case "scan library" => AMVLibrary.scanLibrary
            case "scan" => AMVLibrary.scanLibrary; AMVLibrary.scanRecentPlaylists

//            case "backup" => AMVLibrary.backupRecentPlaylists(Settings.backupPath)
//            case backupToDestinationLine(path) => AMVLibrary.backupRecentPlaylists(path)
//            case backupPlaylistToDestinationLine(playlist, path) => AMVLibrary.backupPlaylist(playlist, path)
//            case backupPlaylistLine(playlist) => AMVLibrary.backupPlaylist(playlist, Settings.backupPath)

            case "play" => Player.play(latest)
            case "+" => Player.queue(latest)
            case scoreAMVs(judge, cmd) =>
              	val rules = parseRules(cmd+" all")
              	scoreAMVs(rules, judge, false)
            case rescoreAMVs(judge, cmd) =>
              	val rules = parseRules(cmd+" all")
              	scoreAMVs(rules, judge, true)
            
            case "exit" => Log ! Close; System.exit(0); None

            case "show settings" =>
                val settings: ShowingSettings = Controller.showingSettings
                printSettings(settings)

            case "*" =>
              	latest = Controller.amvs
              	writeAMVs(latest)
              	
            case _ =>
                val rules = parseRules(line)
                val amvs = rules.find
                latest = amvs
                writeAMVs(amvs)
        }
    }

    def printSettings (settings: ShowingSettings) = {
        
    }

    private val durationFormat =
            new PeriodFormatterBuilder().printZeroAlways.appendMinutes.
            appendSeparator(":").minimumPrintedDigits(2).appendSeconds.toFormatter()

    def writeAMVs (amvs: Seq[AMV]) {
        amvs foreach writeAMV
        if (amvs.length != 1) writeCount(amvs.length)
    }

    def writeCount (count: Int) = println("  " + count + " AMVs")

    def writeAMV (amv: AMV) {
        println("  Name:            " + amv.name + "        (" + amv.location + ")");
        if (amv.duration != new Period())
            println("  Duration:       " + amv.duration.normalizedStandard.toString(durationFormat))
        
        val dateThreshold = new LocalDate().withYear(2007)
        val (viewings, oldViewings) = amv.viewings.partition { _.date.isAfter(dateThreshold) }
        viewings.reverse match {
          	case latest :: tail =>
	            println("  Last shown:   " + latest.date)
	            val previous = tail.map(_.date)
	            if (!previous.isEmpty) {
                    println("  Also shown:  " + previous.mkString(", ") + (if (oldViewings.isEmpty) "" else " and long ago"))
	            } else if (!oldViewings.isEmpty)
	            	println("  Also shown:  long ago")
          	case Nil => println("  Never shown")
        }
        if (Settings.debug) {
        	for (score <- amv.scores)
        		println("  Rated "+score.score+" by "+score.judge)
        }
        println()
    }

    def parseRules (cmd: String): RuleSet = {
        var limit = 1
        var style = 0
        var showAll = false
        val words = new ListBuffer[String]
        val quotedWords = new ListBuffer[String]
        val tokens = cmd.toLowerCase.split("\\s+").toList.map(_.trim).filter(_ != "")

        def parse(tokens: List[String]): List[Rule] = tokens match {
            case "all" :: tail =>
                showAll = true
                parse(tail)

            case "duration" :: d :: tail =>
                val format = new PeriodFormatterBuilder().appendMinutes.appendLiteral(":").appendSeconds.toFormatter
                val period = format.parsePeriod(d)
                DurationRule(period) :: parse(tail)

            case "style" :: n :: tail =>
                if (n == "any")
                    style = 0
                else
                    style = n.toInt
                parse(tail)

            case t :: tail if t.startsWith("\"") =>
                def followQuote(tokens: List[String]): List[String] = {
                    tokens match {
                        case t :: tail if t.endsWith("\"") =>
                            quotedWords += t.substring(0, t.length - 1)
                            tail
                        case t :: tail =>
                            quotedWords += t
                            followQuote(tail)
                        case Nil =>
                            Nil
                    }
                }
                val tail2: List[String] = followQuote(t.substring(1) :: tail)
                parse(tail2)
                
            case t :: tail =>
                try {
                    limit = t.toInt
                    parse(tail)
                } catch { case _ =>
                    words += t
                    parse(tail)
                }

            case Nil => Nil
        }

        val rules = parse(tokens).toBuffer
        val (categoryTerms, searchTerms) = {
            val categoryWords = Settings.categoryWords
            words.toList.partition(w => categoryWords.contains(w))
        }
        val searchTerms2 = searchTerms ::: quotedWords.toList
        if (!searchTerms2.isEmpty)
            rules += SearchRule(searchTerms2)
        if (!categoryTerms.isEmpty) {
            rules += CategoryRule(categoryTerms)
            //rules += CategoryRule(categoryTerms - Settings.restrictPaths)
        } else {
            rules += CategoryScoreRule
            rules += ExcludeCategoriesRule(Settings.restrictPaths)
            if (style == 0) style = 4
        }
        if (style != 0)
            rules += StyleRule(style)
        if (!showAll) {
        	rules += ScoreRule
            rules += RecentlyShownRule
            rules += ExcludeCategoriesRule(Settings.excludePaths)
        }

        new RuleSet(rules.toList, limit)
    }
    
    def scoreAMVs (rules: RuleSet, judge: String, rescore: Boolean): Unit = {
		val amvs = RuleSet(rules.rules, 1000).find
		val toScore = if (rescore) {
			val scored = amvs.filter(_.hasScoreBy(judge))
			if (scored.isEmpty) {
				println("You haven't yet scored any of these AMVs")
				return
			}
			println("Revisiting the AMVs you've already scored")
			scored
		} else {
			val unscored = amvs.filter(!_.hasScoreBy(judge))
			if (unscored.isEmpty) {
				if (amvs.isEmpty) println("No AMVs found to score")
				else println("You have already scored all these AMVs")
				return
			}
			unscored
		}
		
		println("Score AMVs with a number from 0 (it sucks) to 9 (it rocks). A score of 5 is average.")
		println("Hit enter to skip, 'quit' or 'q' to finish scoring, or 'exit' to end the program.")

		for (amv <- toScore) {
	  		writeAMV(amv)
  			Player.playOne(amv)
			print("score 0-9 or quit> ");
	  		val line = in.readLine().trim
	  		line match {
	  		  	case "" => 
	  		  	case "exit" => Player.quit; Log ! Close; System.exit(0)	
	  		  	case "quit" => return
	  		  	case "q" => return
	  		  	case _ =>
	  		  	  	try {
		  		  		val score = line.split(" ").head.toInt
		  		  		amv.addScore(judge, score)
		  		  		Controller.saveData
	  		  	  	} catch {
	  		  	  	  case _ => println("huh?")
	  		  	  	}
	  		}
		}
    }
    
}
