package codes.som.anthony.noverify

import sun.misc.Unsafe

val unsafe by lazy {
	Unsafe::class.java
		.getDeclaredField("theUnsafe")
		.also { it.isAccessible = true }
		.get(null) as Unsafe
}

private val findNativeMethod by lazy {
	ClassLoader::class.java
		.getDeclaredMethod("findNative", ClassLoader::class.java, String::class.java)
		.also { it.isAccessible = true }
}

fun findNative(name: String, classLoader: ClassLoader? = null): Long {
	return findNativeMethod.invoke(null, classLoader, name) as Long
}

fun Unsafe.getString(addr: Long): String? {
	if (addr == 0L) return null
	
	return buildString {
		var offset = 0
		
		while (true) {
			val ch = getByte(addr + offset++).toChar()
			if (ch == '\u0000') break
			append(ch)
		}
	}
}

fun disableBytecodeVerifier() {
	val flags = getFlags(getTypes(getStructs()))
	
	for (flag in flags) {
		if (flag.name == "BytecodeVerificationLocal"
			|| flag.name == "BytecodeVerificationRemote")   {
			unsafe.putByte(flag.address, 0)
		}
	}
}

data class JVMFlag(val name: String, val address: Long)

fun getFlags(types: Map<String, JVMType>): List<JVMFlag> {
	val jvmFlags = mutableListOf<JVMFlag>()
	
	val flagType =
		types["Flag"] ?: types["JVMFlag"] ?:
		error("Could not resolve type 'Flag'")
	
	val flagsField =
		flagType.fields["flags"] ?:
		error("Could not resolve field 'Flag.flags'")
	val flags = unsafe.getAddress(flagsField.offset)
	
	val numFlagsField =
		flagType.fields["numFlags"] ?:
		error("Could not resolve field 'Flag.numFlags'")
	val numFlags = unsafe.getInt(numFlagsField.offset)
	
	val nameField =
		flagType.fields["_name"] ?:
		error("Could not resolve field 'Flag._name'")
	
	val addrField =
		flagType.fields["_addr"] ?:
		error("Could not resolve field 'Flag._addr'")
	
	for (i in 0 until numFlags) {
		val flagAddress = flags + (i * flagType.size)
		val flagNameAddress = unsafe.getAddress(flagAddress + nameField.offset)
		val flagValueAddress = unsafe.getAddress(flagAddress + addrField.offset)
		
		val flagName = unsafe.getString(flagNameAddress)
		if (flagName != null) {
			val flag = JVMFlag(flagName, flagValueAddress)
			jvmFlags.add(flag)
		}
	}
	
	return jvmFlags
}

data class JVMStruct(val name: String) {
	val fields = mutableMapOf<String, Field>()
	operator fun get(f: String) = fields.getValue(f)
	operator fun set(f: String, value: Field) { fields[f] = value }
	
	data class Field(
		val name: String, val type: String?,
		val offset: Long, val static: Boolean
	)
}

fun getStructs(): Map<String, JVMStruct> {
	val structs = mutableMapOf<String, JVMStruct>()
	
	fun symbol(name: String) = unsafe.getLong(findNative(name))
	fun offsetSymbol(name: String) = symbol("gHotSpotVMStructEntry${name}Offset")
	fun derefReadString(addr: Long) = unsafe.getString(unsafe.getLong(addr))
	
	var currentEntry = symbol("gHotSpotVMStructs")
	val arrayStride = symbol("gHotSpotVMStructEntryArrayStride")
	
	while (true) {
		val typeName = derefReadString(currentEntry + offsetSymbol("TypeName"))
		val fieldName = derefReadString(currentEntry + offsetSymbol("FieldName"))
		if (typeName == null || fieldName == null)
			break
		
		val typeString = derefReadString(currentEntry + offsetSymbol("TypeString"))
		val static = unsafe.getInt(currentEntry + offsetSymbol("IsStatic")) != 0
		
		val offsetOffset = if (static) offsetSymbol("Address") else offsetSymbol("Offset")
		val offset = unsafe.getLong(currentEntry + offsetOffset)
		
		val struct = structs.getOrPut(typeName, { JVMStruct(typeName) })
		struct[fieldName] = JVMStruct.Field(fieldName, typeString, offset, static)
		
		currentEntry += arrayStride
	}
	
	return structs
}

data class JVMType(
	val type: String, val superClass: String?, val size: Int,
	val oop: Boolean, val int: Boolean, val unsigned: Boolean) {
	val fields = mutableMapOf<String, JVMStruct.Field>()
}

fun getTypes(structs: Map<String, JVMStruct>): Map<String, JVMType> {
	fun symbol(name: String) = unsafe.getLong(findNative(name))
	fun offsetSymbol(name: String) = symbol("gHotSpotVMTypeEntry${name}Offset")
	fun derefReadString(addr: Long) = unsafe.getString(unsafe.getLong(addr))
	
	var entry = symbol("gHotSpotVMTypes")
	val arrayStride = symbol("gHotSpotVMTypeEntryArrayStride")
	
	val types = mutableMapOf<String, JVMType>()
	
	while (true) {
		val typeName = derefReadString(entry + offsetSymbol("TypeName"))
		if (typeName == null) break
		
		val superClassName = derefReadString(entry + offsetSymbol("SuperclassName"))
		
		val size = unsafe.getInt(entry + offsetSymbol("Size"))
		val oop = unsafe.getInt(entry + offsetSymbol("IsOopType")) != 0
		val int = unsafe.getInt(entry + offsetSymbol("IsIntegerType")) != 0
		val unsigned = unsafe.getInt(entry + offsetSymbol("IsUnsigned")) != 0
		
		val structFields = structs[typeName]?.fields
		types[typeName] = JVMType(
			typeName, superClassName, size,
			oop, int, unsigned
		).apply {
			if (structFields != null)
				this.fields.putAll(structFields)
		}
		
		entry += arrayStride
	}
	
	return types
}
