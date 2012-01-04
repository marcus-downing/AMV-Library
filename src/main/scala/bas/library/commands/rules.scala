package bas.library.commands
import java.text.DecimalFormat

import bas.store.AMV
import bas.store.xml.Controller
import collection.mutable.ListBuffer
import bas.library.Settings
import org.joda.time.{Days, LocalDate, Period}

trait Rule {
    def filter(amv: AMV) = true
    def score(amv: AMV): Double
    def name: String
}

case object RecentlyShownRule extends Rule{
    def score(amv: AMV) = {
        val today = new LocalDate
        val scores = Controller.viewings(amv).map { viewing =>
            val diff = Days.daysBetween(viewing.date, today).getDays.toDouble
            val prox = math.max(0, (720.0 - diff) / 600.0)
            val adjustment = -(prox*prox)
            adjustment
        }
        scores.sum
    }
    def name = "Recently shown"
}

case class DurationRule (duration: Period) extends Rule {
    def score(amv: AMV) = {
        val diffSecs = math.abs(duration.toStandardSeconds.getSeconds - amv.duration.toStandardSeconds.getSeconds)
        -(diffSecs.toDouble / 10.0)
    }
    def name = "Duration "+duration.toString
}

case class StyleRule (style: Int) extends Rule{
    def score(amv: AMV) = {
        val diff = math.abs(amv.level - style).toDouble
        -(diff * diff * diff / 4.0)
    }
    def name = "Style "+style
}

case class SearchRule (terms: List[String]) extends Rule {
    def score(amv: AMV) = amv.matchName(terms)
    override def filter(amv: AMV) = score(amv) > 0
    def name = "Search: "+terms.mkString(" ")
}

case class CategoryRule (terms: List[String]) extends Rule {
    def score(amv: AMV) = amv.matchCategory(terms)
    override def filter(amv: AMV) = score(amv) > 0
    def name = "Category: "+terms.mkString(", ")
}

case object CategoryScoreRule extends Rule {
    def score(amv: AMV) =
        if (amv.hasCategory("Gems")) 0.3 else 0
    def name = "Category: Gems"
}

case class ExcludeCategoriesRule (paths: List[String]) extends Rule {
    override def filter(amv: AMV) = !paths.exists { path => amv.path.startsWith(path) }
    def score(amv: AMV) = 0
    def name = "Exclude: "+paths.mkString(", ")
}

case object ScoreRule extends Rule {
	def score(amv: AMV) = amv.scoreAdjustment
	def name = "Score"
}

case class RuleSet (rules: List[Rule], limit: Int) {
	val fmt = new DecimalFormat("0.0##")
    def searchRules = rules.collect { case s: SearchRule => s }
	def single = RuleSet(rules, 1)

    def find: List[AMV] = {
		//if (Settings.debug) println("Rules:\n"+rules.map(_.name).mkString(", "))
		
        val amvs = Controller.amvs.filter { amv => !rules.exists { rule => !rule.filter(amv) } }
        val amvs2 = amvs.map { amv =>
            val score = rules.map(_.score(amv)).sum
            (amv, score)
        }
        
        if (Settings.debug) {
        	for ((amv, score) <- amvs2) {
        		println(fmt.format(score)+"    "+amv.path)
        		for (rule <- rules; v = rule.score(amv); if v != 0)
        			println("        "+fmt.format(v)+"  "+rule.name)
        	}
        	println("Collection: "+amvs2.size+" AMVs")
        }
            
        if (amvs2.isEmpty) Nil else {
            val threshold = amvs2.map(_._2).max-1.0
            val pool = amvs2.filter(_._2 >= threshold).toBuffer
            
            if (Settings.debug) {
            	println("Threshold: "+fmt.format(threshold))
            	println("Pool: "+pool.size+" AMVs")
            }

            val found = new ListBuffer[AMV]
            val rand = new scala.util.Random()
            while (found.length < limit && !pool.isEmpty) {
                val i = rand.nextInt(pool.length)
                val (amv, score) = pool(i)
                if (score + math.random >= threshold) {
                    pool.remove(i)
                    found.append(amv)
                }
            }

            found.toList
        }
    }

    def count: Int = {
        val amvs = Controller.amvs.filter { amv => !rules.exists { rules => !rules.filter(amv)} }
        amvs.length
    }
}