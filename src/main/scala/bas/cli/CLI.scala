package bas.cli

import org.joda.time.LocalDate
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

import scala.Console

object CLI {
	var latest: List[AMV] = Nil
    private var in: BufferedReader = null

    object Nothing

    def main (args: Array[String]): Unit = {
        in = new BufferedReader(new InputStreamReader(System.in))
        while (true) {
            print(Console.GREEN+Console.BOLD+"bas"+"> "+Console.WHITE)
            Console.flush()
            val line = in.readLine()
            print(Console.RESET)
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
    private val playOne = "play (.*)" r
    private val plusLine = "\\+(.*)" r

    private val dateFormat = DateTimeFormat.forPattern("yyyy MM dd")

    def doCommand (line: String) = {
        val input = line.trim
        input match {
            case "?" =>
                Source.fromFile("help.txt").getLines foreach { line: String => println(input) }
                
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
            case playOne(line) =>
                val rules = parseRules(line)
                val amvs = rules.find
                Player.play(amvs)
            case "+" => Player.queue(latest)
            case plusLine(line) =>
                val rules = parseRules(line)
                val amvs = rules.find
                Player.queue(amvs)
            case scoreAMVs(judge, cmd) =>
              	val rules = parseRules(cmd+" all")
              	scoreAMVs(rules, judge, false)
            case rescoreAMVs(judge, cmd) =>
              	val rules = parseRules(cmd+" all")
              	scoreAMVs(rules, judge, true)
            
            case _ => input match {
                case "exit" => 
                    exit()
                    None

                case "show settings" =>
                    val settings: ShowingSettings = Controller.showingSettings
                    printSettings(settings)

                case "*" =>
                  	latest = Controller.amvs
                  	writeAMVs(latest)
                  	
                case _ =>
                    val rules = parseRules(input)
                    val amvs = rules.find
                    latest = amvs
                    writeAMVs(amvs)
            }
        }
    }

    def printSettings (settings: ShowingSettings) = {
        
    }

    def exit() {
        println(Console.RESET+Console.BOLD+"bye")
        System.setSecurityManager(null)
        System.exit(0)
        println(Console.RED+"You still here?")
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
        println(Console.RESET+"  Name:         "+Console.BOLD+Console.GREEN+amv.name+Console.RESET+"        ("+amv.location+")");
        if (amv.duration != new Period())
            println("  Duration:     " + amv.duration.normalizedStandard.toString(durationFormat))
        
        val dateThreshold = new LocalDate().withYear(2007)
        val (viewings, oldViewings) = amv.viewings.partition { _.date.isAfter(dateThreshold) }
        viewings.reverse match {
          	case latest :: tail =>
	            println("  Last shown:   "+latest.date)
	            val previous = tail.map(_.date)
	            if (!previous.isEmpty) {
                    println("  Also shown:   "+previous.mkString(", ")+(if (oldViewings.isEmpty) "" else " and long ago"))
	            } else if (!oldViewings.isEmpty)
	            	println("  Also shown:   long ago")
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
        val allAMVs = util.Random.shuffle(RuleSet(rules.rules, 1000000).find)
        def report() {
            val scoredAMVs = allAMVs.filter(_.hasScoreBy(judge))
            println()
            println(Console.CYAN+Console.BOLD+"Scored "+(scoredAMVs.length)+" of "+(allAMVs.length)+" AMVs")
            if (allAMVs.length == scoredAMVs.length)
                println("Nothing left to score")
            println(Console.RESET)
        }

		val toScore = if (rescore) {
			val scored = allAMVs.filter(_.hasScoreBy(judge))
			if (scored.isEmpty) {
				println(Console.RED+"You haven't yet scored any of these AMVs"+Console.RESET)
				return
			}
			println("Revisiting the AMVs you've already scored")
			scored
		} else {
			val unscored = allAMVs.filter(!_.hasScoreBy(judge))
			if (unscored.isEmpty) {
				if (allAMVs.isEmpty) println(Console.RED+"No AMVs found to score"+Console.RESET)
				else println(Console.RED+"You have already scored all these AMVs"+Console.RESET)
                report()
				return
			}
			unscored
		}
		
		println(Console.RESET+Console.CYAN+"Score AMVs with a number from "+Console.BOLD+"0"+Console.RESET+Console.CYAN+" (it sucks) to "+Console.BOLD+"9"+Console.RESET+Console.CYAN+" (it rocks). A score of "+Console.BOLD+"5"+Console.RESET+Console.CYAN+" is average.")
		println("Hit enter to skip, "+Console.BOLD+"quit"+Console.RESET+Console.CYAN+" or "+Console.BOLD+"q"+Console.RESET+Console.CYAN+" to finish scoring, or "+Console.BOLD+"exit"+Console.RESET+Console.CYAN+" to end the program.")
        println()

		for (amv <- toScore) {
	  		writeAMV(amv)
  			Player.playOne(amv)
			print(Console.GREEN+Console.BOLD+"score 0-9 or quit> "+Console.WHITE);
	  		val line = in.readLine().trim
            println(Console.RESET)
	  		line match {
	  		  	case "" => 
	  		  	case "exit" => 
                    report()
                    Player.quit
                    exit()
                    None
	  		  	case "quit" | "q" => 
                    report()
                    return
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

        report()
    }
    
}
