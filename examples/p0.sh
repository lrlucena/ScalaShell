#!/bin/bash
exec scala "$0" "$@"
!#

import scala.io.StdIn._
import sys.process._

"date".!
"df".!
"w".!
