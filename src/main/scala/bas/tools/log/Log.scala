package bas.tools.log

import _root_.scala.actors.Actor
import java.io.{PrintWriter, FileWriter}
import bas.library.Settings

case object Close

case class Cmd(cmd: String)

object Log extends Actor {
    def act() {
        while (true) {
            receive {
                case Close => out.flush; out.close
                case cmd:Cmd => log(cmd.cmd, false)
                case line:String => log(line, true)
                case Some(line: String) => log(line, true)
                case None =>
                case Nil =>
                case obj:Any => log(obj.toString, true)
            }
        }
    }

    lazy val out = {
        val fw = new FileWriter("bas.log", true)
        new PrintWriter(fw)
    }

    def log (line: String, feedback: Boolean) = {
        out.println(line)
        out.flush

        if (Settings.debug && feedback)
            println(line)
    }
}