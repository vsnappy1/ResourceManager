package dev.randos.resourcemanager.utils

import java.io.File
import java.nio.file.Paths

object MockFileReader {
    fun read(fileName: String): String {
        val projectRoot = Paths.get(System.getProperty("user.dir")).toFile()
        val mockDirectory = File(projectRoot, "src/test/kotlin/dev/randos/resourcemanager/mock")
        return File(mockDirectory, fileName).readText()
    }
}