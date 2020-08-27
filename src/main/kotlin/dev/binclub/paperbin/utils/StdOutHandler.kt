package dev.binclub.paperbin.utils

import java.io.*
import java.util.logging.*

/**
 * @author cookiedragon234 17/Aug/2020
 */
class StdOutHandler: Handler() {
	private fun writerForRecord(record: LogRecord): OutputStream
		= if (record.level == Level.WARNING || record.level == Level.SEVERE) System.err else System.out
	
	@Synchronized
	override fun publish(record: LogRecord?) {
		if (record != null && isLoggable(record)) {
			val text = try {
				formatter.format(record)
			} catch (e: Exception) {
				reportError(null, e, 5)
				return
			}
			try {
				writerForRecord(record).write(text.toByteArray(Charsets.UTF_8))
			} catch (e: Exception) {
				reportError(null, e, 1)
			}
		}
	}
	
	@Synchronized
	override fun flush() {
		System.out.flush()
		System.err.flush()
	}
	
	@Synchronized
	private fun flushAndClose() {
		this.flush()
	}
	
	@Synchronized
	override fun close() {
		flushAndClose()
	}
}
