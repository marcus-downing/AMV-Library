package bas.tools.regex

import java.util.regex._

case class RegexLine (regex: String) {
    val pattern = Pattern.compile(regex)

    def unapply (value: String): Option[String] = {
        val matcher = pattern.matcher(value)
        if (!matcher.matches) None else Some(matcher.group(1))
    }
}

case class RegexLine2 (regex: String) {
    val pattern = Pattern.compile(regex)

    def unapply (value: String): Option[(String, String)] = {
        val matcher = pattern.matcher(value)
        if (!matcher.matches) None else Some((matcher.group(1), matcher.group(2)))
    }
}