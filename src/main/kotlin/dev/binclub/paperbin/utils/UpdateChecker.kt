package dev.binclub.paperbin.utils

import com.google.gson.JsonParser
import dev.binclub.paperbin.PaperBinInfo
import java.net.URL
import java.util.logging.Level

/**
 * @author cookiedragon234 19/May/2020
 */
fun checkForUpdate() {
	try {
		val localVersion = PaperBinInfo.version
		val remoteVersion = latestVersion
		
		if (localVersion < remoteVersion) {
			PaperBinInfo.logger.warning("You are running an outdated version of PaperBin! (Running $localVersion while latest is $remoteVersion")
			PaperBinInfo.logger.warning("Download $remoteVersion here: $latestUrl")
		}
	} catch (t: Throwable) {
		PaperBinInfo.logger.log(Level.WARNING, "Error while checking for update", t)
	}
}

private val url = URL("https://api.github.com/repos/cookiedragon234/paperbin/releases/latest")
private val json by lazy { JsonParser().parse(url.readText()).asJsonObject }
private val latestVersion by lazy { json.get("name").asString.toFloat() }
private val latestUrl by lazy { json.get("html_url").asString }
