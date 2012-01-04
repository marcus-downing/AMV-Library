package bas.tools.xml

import scala.xml._
import org.joda.time._

object JodaConversions {
    implicit def enhanceNode (node: Node) = new {
        def toInt: Int = node.text.toInt
        def toDate: LocalDate = new LocalDate(node.text.toString)
        def toPeriod: Period = new Period(node.text.toString).normalizedStandard()
    }

    implicit def enhanceNodeSeq (nodes: NodeSeq) = new {
        def toInt: Int = nodes.text.toInt
        def toDate: LocalDate = new LocalDate(nodes.text.toString)
        def toPeriod: Period = new Period(nodes.text.toString)
    }

    implicit def enhancePeriod (period: Period) = new EnhancedPeriod (period)

    class EnhancedPeriod (period: Period) {
        def >= (other: Period) = period.toStandardSeconds.getSeconds >= other.toStandardSeconds.getSeconds
        def > (other: Period) = period.toStandardSeconds.getSeconds > other.toStandardSeconds.getSeconds
        def <= (other: Period) = period.toStandardSeconds.getSeconds <= other.toStandardSeconds.getSeconds
        def < (other: Period) = period.toStandardSeconds.getSeconds < other.toStandardSeconds.getSeconds
        def - (other: Period) = period.minus(other)
        def + (other: Period) = period.plus(other)
        def * (factor: Double) = {
            val secs = Math.round(period.toStandardSeconds.getSeconds * factor).toInt
            new Period().withSeconds(secs)
//            new Period().
//                withSeconds(Math.ceil(period.getSeconds.toDouble * factor).toInt).
//                withMinutes(Math.ceil(period.getMinutes.toDouble * factor).toInt).
//                withHours(Math.ceil(period.getHours.toDouble * factor).toInt).
//                withDays(Math.ceil(period.getDays.toDouble * factor).toInt).
//                withMonths(period.getMonths.toDouble * factor).withYears(period.getYears.toDouble * factor)
        }
        def / (factor: Double) = period * (1/factor)
    }

    implicit def enhanceLocalDate (date: LocalDate) = new {
        def >= (other: LocalDate) = !date.isBefore(other)
        def > (other: LocalDate) = date.isAfter(other)
        def <= (other: LocalDate) = !date.isAfter(other)
        def < (other: LocalDate) = date.isBefore(other)
    }
}