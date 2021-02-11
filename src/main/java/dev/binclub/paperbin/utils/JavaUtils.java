package dev.binclub.paperbin.utils;

/**
 * Somethings just arent possible in kotlin
 *
 * This fixes that
 *
 * @author cookiedragon234 24/May/2020
 */
public class JavaUtils {
	public static void wait(Object obj) throws InterruptedException {
		obj.wait();
	}
	public static void wait(Object obj, long timeout) throws InterruptedException {
		obj.wait(timeout);
	}
	public static void notify(Object obj) {
		obj.notify();
	}
}
