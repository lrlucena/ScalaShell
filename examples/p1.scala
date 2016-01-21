import scala.io.StdIn._
import sys.process._

println("Data e hora:")
"date".!
println
println("Uso do disco:") 
"df".!
println
println("Usuários conectados:")
"w".!
