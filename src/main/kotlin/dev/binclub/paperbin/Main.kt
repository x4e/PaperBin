package dev.binclub.paperbin

import dev.binclub.paperbin.utils.InstrumentationFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader

/**
 * @author cookiedragon234 12/Apr/2020
 */
fun main(args: Array<String>) {
	if (args.isEmpty()) {
		throw IllegalArgumentException("Usage java -jar paperbin.jar paperclip.jar")
	}
	
	val file = File(args[0])
	val newArgs = args.drop(1).toTypedArray()
	
	InstrumentationFactory.instrumentation.addTransformer(PaperBinTransformer)
	
	println("Added transformer")
	
	val sysCl = ClassLoader.getSystemClassLoader() as URLClassLoader
	
	URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java).let {
		it.isAccessible = true
		it.invoke(sysCl, file.toURI().toURL())
	}
	
	Class.forName("com.destroystokyo.paperclip.Main")?.getDeclaredMethod("main", Array<String>::class.java)?.also {
		it.invoke(null, newArgs)
	} ?: error("Please provide a valid paperclip jar")
}
