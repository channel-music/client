package com.kalouantonis.channel

import cats.effect.IO

object FileUtil {
  def getDirectoryFiles(path: String): IO[Seq[java.io.File]] = IO {
    val directory = new java.io.File(path)
    val files = directory.listFiles
    files.flatMap { file =>
      if (file.isFile)
        Seq(file)
      else
        // the algorithm is a little easier with eager evaluation, however i should
        // consider not doing it at all at some point.
        getDirectoryFiles(file.getPath).unsafeRunSync()
    }.toSeq
  }
}
