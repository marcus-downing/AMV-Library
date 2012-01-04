package bas.cli

import bas.library._
import bas.store._

import scala.sys.process._

object Player {
	val player: VideoPlayer = {
		Settings.player match {
		  	case Settings.ZoomPlayer => new ZoomPlayer(Settings.zoomPlayerExe)
		  	//case "mediaPlayerClassic" => new MediaPlayerClassic(Settings.mediaPlayerClassicExe)
		  	case Settings.VLC => new VLC(Settings.vlcExe)
		  	case _ => null
		}
	}
	
	def playOne(amv: AMV) = player.playOne(amv)
	def play(amvs: List[AMV]) = player.play(amvs)
	def queue(amvs: List[AMV]) = player.queue(amvs)
	def quit = player.quit
}

trait VideoPlayer {
	def playOne(amv: AMV)
	def play(amvs: List[AMV])
	def queue(amvs: List[AMV])
	def quit
	
	def invoke(exe: String, wait: Boolean, arguments: String*) = {
		if (Settings.debug)
			println("Invoking: "+exe+"   "+arguments.mkString("   "))
		val process = Process(exe, arguments.toList)
		if (wait) process.!
		else process.run
	}
}

class ZoomPlayer(exe: String) extends VideoPlayer {
	def playOne(amv: AMV) = invoke(exe, false, amv.file.getAbsolutePath, "/F")
	def play(amvs: List[AMV]) = for (amv <- amvs)  invoke(exe, true, amv.file.getAbsolutePath, "/F")
	def queue(amvs: List[AMV]) = invoke(exe, false, amvs.map("/queue:"+_.file.getAbsolutePath):_*)
	def quit = invoke(exe, true, "/Close")
}

/*
class MediaPlayerClassic(exe: String) extends VideoPlayer {
	def playOne(amv: AMV) = invoke(exe, false, )
}*/

class VLC(exe: String) extends VideoPlayer {
	def playOne(amv: AMV) = invoke(exe, false, "file:///"+amv.file.getAbsolutePath, "--fullscreen", "--play-and-exit")
	def play(amvs: List[AMV]) = for(amv <- amvs) invoke(exe, true, "file:///"+amv.file.getAbsolutePath, "--fullscreen", "--play-and-exit")
	def queue(amvs: List[AMV]) = invoke(exe, false, "--playlist-enqueue"::amvs.map("file:///"+_.file.getAbsolutePath):_*)
	def quit = invoke(exe, false, "vlc://quit")
}