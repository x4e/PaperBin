package dev.binclub.paperbin.utils

/**
 * Kotlinifies the functions located in JavaUtils
 *
 * @author cookiedragon234 24/May/2020
 */
fun Any.wait() = JavaUtils.wait(this)
fun Any.wait(timeout: Long) = JavaUtils.wait(this, timeout)
fun Any.notify() = JavaUtils.notify(this)
