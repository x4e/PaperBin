package dev.binclub.paperbin

import org.apache.openjpa.enhance.InstrumentationFactory
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.Attributes
import java.util.jar.Attributes.Name.MAIN_CLASS
import java.util.jar.JarFile

/**
 * @author cookiedragon234 12/Apr/2020
 */
fun main(args: Array<String>) {
	if (args.isEmpty()) {
		error("Usage java -jar paperbin.jar paperclip.jar")
	}
	
	if (!ManagementFactory.getRuntimeMXBean().inputArguments.any {
			it.contains("noverify", true) || it.contains("Xverify", true)
		}) {
		error("Disable the verifier")
	}
	
	val file = File(args[0])
	val newArgs = args.drop(1).toTypedArray()
	
	InstrumentationFactory.getInstrumentation().addTransformer(PaperBinTransformer)
	println("Added transformer")
	
	val sysCl = ClassLoader.getSystemClassLoader() as URLClassLoader
	
	URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java).let {
		it.isAccessible = true
		it.invoke(sysCl, file.toURI().toURL())
	}
	
	val mainClass = run {
		JarFile(file).manifest.mainAttributes.getValue(MAIN_CLASS)
	}
	
	try {
		Class.forName(mainClass)!!.getDeclaredMethod("main", Array<String>::class.java)!!.invoke(null, newArgs)
	} catch (t: Throwable) {
		throw IllegalStateException("Please provide a valid paperclip jar", t)
	}
}
