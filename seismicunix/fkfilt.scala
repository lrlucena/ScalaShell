import scala.io.StdIn._
import sys.process._
import java.io.File

val tpow   = 1.7
val d2     = 50                // CMP spacing in meters
val indata = new File(args(0))
val tmp0   = new File("tmp0")

/*
 * Processos Seismic Unix
 */
val ganho    = Seq("sugain",  "agc=1", "wagc=1") // Seq("sugain", s"tpow=$tpow")

def suxwigb(title: String, xbox: Int = 10) = Seq("suxwigb", s"xbox=$xbox", "ybox=10",
               "wbox=400", "hbox=600", "label1=Traveltime [s]","label2=Offset [m]",
               s"title=$title", "verbose=0", "perc=99", "key=offset")

val suspecfk = Seq("suspecfk", s"dx=$d2")

def suximage(title: String) =  Seq("suximage", "xbox=420", "ybox=10", "wbox=400", "hbox=600", 
               "label1=Frequency [Hz]", "label2=Wavenumber [k]", s"title=$title", "cmap=hsv2",
               "legend=1", "perc=99", "verbose=0", "x1beg=0", "x1end=125" /*,"bclip=2e09", "wclip=0" */ )
               
def sudipfilt(slopes: String, pass: Boolean= true) = 
      Seq("sudipfilt", s"d2=$d2", s"slopes=$slopes", if (pass) "amps=0,1,1,0" else "amps=1,0,0,1")

def clean(file: String)  = Seq("rm", "-f", file).!
def sleep(time: Int) = Seq("sleep", s"$time").!
def copy(from: String, to: String) = Seq("cp", from, to).!
def exists(file: String) = Seq("test", "-e",file).! == 0

def askFilter() {
  println("""f-k Filter Test
            |Press A to add a FK filter
  	        |Press S to start over""".stripMargin('|'))
  readLine match {
    case "a"|"A" => copy(from="tmp2",to="tmp1")
                    println("Using filtred data")
    case _       => copy(from="tmp0", to="tmp1")
                    println("Using original data")
  }
}

def askSlopes() = {
  println("""Select the filter slopes:
            |Input: a,b,c,d  -  a:=cut   b:=pass
            |                   c:=pass  d:=cut
            |Where a < b < c < d; slopes between b band c pass""".stripMargin('|'))
  readLine
}

def askContinue() = {
  println("Press 1 for more f-k filter testing")
  println("Press 2 for PS output of current file and EXIT")
  readLine == "1" 
}

/*
 *  Principal
 */
println("f-k Filter Test")
clean("tmp2")

/*------------------------------------------------
 * Show the original Shotplot and Spectrum first..
 *------------------------------------------------*/
(ganho #< indata #> tmp0).!
(suxwigb(title="Original Shot Gather") #< tmp0).run
(suspecfk #< tmp0 #| suximage(title="f-k Spectrum before Filtering")).run

/*------------------------------------------------
 * f-k Filter Test...
 *------------------------------------------------*/
var again = true
copy(from="tmp0", to="tmp1")
while (again) {
  if (exists("tmp2")) {
  	askFilter
  }
  val slopes = askSlopes()
  (sudipfilt(slopes=slopes) #< new File("tmp1") #> new File("tmp2")).!

  /*------------------------------------------------)
   * Plot the filtered data...
   *------------------------------------------------*/
  (suxwigb(title="f-k Filtered Data") #< new File("tmp2")).run

  (suspecfk #< new File("tmp2") #| suximage(title="f-k Spectrum")).run

  (sudipfilt(slopes=slopes, pass=false) #< new File("tmp1") #|
  	suxwigb(title="Rejected Data", xbox=830)).run

  again = askContinue
}
clean("tmp*")
