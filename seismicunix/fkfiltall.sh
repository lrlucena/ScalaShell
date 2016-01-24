#!/bin/bash
exec scala "$0" "$@"
!#

import scala.io.StdIn._
import sys.process._
import java.io.File

val d2 = 50  // CMP spacing in meters
val slopes = List(-0.0005, -0.0003, 0.0003, 0.0005)

val indata = new File(args(0))
val destino = "final.su"
val outdata = new File(destino)
val output = "output"

// Limpar arquivos gerados: rm -f $destino ; rm -rf $output ; mkdir $output
val limpar = Seq("rm", "-f", destino) ###
             Seq("rm", "-rf", output) ###
             Seq("mkdir", output)
// Ganho: sugain agc=1 wagc=1
val ganho = Seq("sugain", "agc=1", "wagc=1")
// Split: suputgthr key=fldr dir=$output numlength=3
val split = Seq("suputgthr", "key=fldr", s"dir=$output", "numlength=3")
// Filtrar: sudipfilt ds=$d2 slopes=$slopes amps=0,1,1,0
val filtrar = Seq("sudipfilt", s"d2=$d2", s"slopes=${slopes.mkString(",")}", "amps=0,1,1,0")
// Obter arquivos gerados: ls $output/* | grep fldr
def tiros = (Seq("ls", s"$output/") #|
             Seq("grep", "fldr")).!!.trim.replace("fldr", s"$output/fldr").split("\n")

// Principal
limpar.!
println("Separando traços por tiro ...")
(split #< indata).!
println("Aplicando ganho e filtro ...")
for(tiro <- tiros) {
    print(".")
    (ganho #< new File(tiro) #| filtrar #>> outdata).!
}
println("fim")
