package dev.binclub.paperbin.utils

/**
 * @author cookiedragon234 06/Aug/2020
 */
class NopSet<T: Any?>: MutableSet<T> {
	override fun add(element: T): Boolean = false
	override fun addAll(elements: Collection<T>): Boolean = false
	override fun clear() {}
	override fun iterator(): MutableIterator<T> = object: MutableIterator<T> {
		override fun hasNext(): Boolean = false
		override fun next(): T = throw NoSuchElementException()
		override fun remove() = throw NoSuchElementException()
	}
	override fun remove(element: T): Boolean = false
	override fun removeAll(elements: Collection<T>): Boolean = false
	override fun retainAll(elements: Collection<T>): Boolean = false
	override val size: Int = 0
	override fun contains(element: T): Boolean = false
	override fun containsAll(elements: Collection<T>): Boolean = false
	override fun isEmpty(): Boolean = true
}
