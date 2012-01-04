package bas.store.xml

import org.joda.time.{Period, LocalDate}
import scala.xml._
import bas.library.Settings
import bas.store._
import bas.tools.xml.JodaConversions._

class ShowingSettings (val data: Node) {

    var _lastRecap: Option[LocalDate] = {
        (for (node <- data \ "lastrecap")
            yield node toDate).firstOption
    }
    
    def lastRecap: Option[LocalDate] = _lastRecap
    def lastRecap_= (date: LocalDate) = { _lastRecap = Some(date) }

    def save: NodeSeq  =
        <current>
        {
            _lastRecap match {
                case Some(date: LocalDate) => <lastrecap>{date.toString}</lastrecap>
                case None =>
            }
        }
        </current>

}