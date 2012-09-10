package bas.store

import org.joda.time._
class Playlist (val date: LocalDate) {
    var viewings: List[Viewing] = Nil

    def this (date: LocalDate, viewings: Seq[Viewing]) = {
      this(date)
      this.viewings = viewings.toList
    }
    //  ...
}