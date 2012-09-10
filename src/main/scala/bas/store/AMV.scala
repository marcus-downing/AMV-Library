package bas.store

import org.joda.time._
import bas.library.{Settings, AMVLibrary}
import java.io.File
import bas.store.xml.Controller

class AMV (val path: String, var duration: Period, var level: Int, var scores: List[AMVScore]) {
    def file = new File(Settings.basePath + path)
    
	lazy val name: String = {
		val filename = path.split("/").last
		val index = filename.lastIndexOf('.')
		index match {
			case -1 => filename
			case _ => filename.substring(0, index)
		}
	}
    override def toString = name

	lazy val location: String = {
		val index = path.lastIndexOf('/')
		index match {
			case -1 => path
			case _ => path.substring(0, index)
		}
	}

    def splitWords(str: String) = str.split("[^a-zA-Z0-9]").map(_.toLowerCase.trim)
    lazy val nameWords = splitWords(name)
    lazy val categoryWords = splitWords(location)
    def hasCategory(word: String) = categoryWords.contains(word)
	def isChristmas = hasCategory("christmas")

	def viewings: Seq[Viewing] = Controller.viewings(this)

	private val dateThreshold = new LocalDate().withYear(2007)
	def realViewings = viewings.filter { _.date.isAfter(dateThreshold) }
	def lastViewing = realViewings.toList.lastOption
	def hasViewing = !realViewings.isEmpty

	def matchName (terms: Seq[String]) = matchWords(terms, nameWords, name) * 1.5

    def matchCategory (terms: Seq[String]) = matchWords(terms, categoryWords, path) + matchWords(terms, nameWords, name) * 0.00001

    def matchWords (terms: Seq[String], words: Seq[String], base: String): Int = {
        lazy val base2 = base.toLowerCase.replaceAll("\\s+", " ").trim
        val matched = terms.map{ term =>
            if (term.indexOf(' ') == -1)
                if (words.contains(term)) 1 else 0
            else
                if (base2.indexOf(term) != -1) 1 else 0
        }.foldLeft(0)(_+_)

        if (matched == terms.length)
            matched * 2
        else
            matched
    }

	def affinityAdjustment (previous: AMV): Double = 0 // Controller.getAffinity(previous, this)

    def similarity (other: AMV) = {
        val nameSimilarity = {
            val sharedWords = nameWords.filter(w => other.nameWords.contains(w))
            2.0 * sharedWords.length.toDouble / (nameWords.length + other.nameWords.length).toDouble
        }
        val pathSimilarity = if (location.equals(other.location)) 0.5 else 0.0
        val durationSimilarity = {
            if (duration == Period.ZERO || other.duration == Period.ZERO) 
                0.0
            else if (duration == other.duration)
                0.5
            else
                -1.0
        }

        nameSimilarity + pathSimilarity + durationSimilarity
    }
	
	def scoreAdjustment: Double = AMVScore.adjustment(scores)
	def hasScoreBy(judge: String) = scores.exists(_.judge == judge)
	def addScore(judge: String, score: Int) = {
		scores = scores.filterNot(_.judge == judge)
		scores = new AMVScore(judge, score) :: scores
	}
}

object AMVScore {
	def normalise(score: Int): Int = score match {
	  	case score if score > 9 => 9
	  	case score if score < 0 => 0
	  	case score => score
	}
	def adjustment(scores: List[AMVScore]): Double = {
		if (scores.isEmpty) 0.0
		else scores.map(_.adjustment).sum / (scores.length+1)
	}
}

class AMVScore (val judge: String, val score: Int) {
	def adjustment: Double = (score.toDouble - 5.0) / 5.0
}