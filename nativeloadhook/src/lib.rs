mod antiphysicscrash;
mod utils;

use jvm_rs::jni::{JavaVM, JNIEnv, jclass, jobject, jint, JNI_VERSION_1_8, jmethodID, jbyteArray, jstring, JNI_OK, jlong, jboolean, JNINativeMethod};
use jvm_rs::jvmti::{jvmtiEnv, jvmtiCapabilities, jvmtiError_JVMTI_ERROR_NONE, jvmtiEventCallbacks, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, jthread, jvmtiEvent_JVMTI_EVENT_CLASS_PREPARE, jvmtiEvent_JVMTI_EVENT_VM_INIT};

use std::os::raw::{c_void, c_int, c_char, c_uchar};
use std::borrow::BorrowMut;
use std::ptr::null_mut;
use std::mem::{zeroed, size_of};
use crate::antiphysicscrash::Java_dev_binclub_paperbin_native_NativeAccessor_registerAntiPhysicsCrash;

static mut AGENT_LOADED: bool = false;
static mut CALLBACKS: Option<jvmtiEventCallbacks> = None;
static mut JVMTI: Option<*mut jvmtiEnv> = None;

#[no_mangle]
pub unsafe extern "system" fn Agent_OnLoad(vm: *mut JavaVM, _options: *const c_char, _reserved: &mut c_void) -> c_int {
	println!("Paperbin Agent Loaded");
	let jvmti = utils::get_jvmti(vm);
	JVMTI = Some(jvmti);
	
	{
		let mut capabilities: jvmtiCapabilities = zeroed();
		assert_eq!((**jvmti).GetCapabilities.unwrap()(jvmti, &mut capabilities), jvmtiError_JVMTI_ERROR_NONE);
		
		capabilities.set_can_retransform_classes(1);
		capabilities.set_can_generate_breakpoint_events(1);
		capabilities.set_can_generate_all_class_hook_events(1);
		
		assert_eq!((**jvmti).AddCapabilities.unwrap()(jvmti, &capabilities), jvmtiError_JVMTI_ERROR_NONE);
	}
	{
		let mut callbacks = match CALLBACKS {
			Some(x) => x,
			None => zeroed()
		};
		callbacks.VMInit = Some(vm_init);
		CALLBACKS = Some(callbacks);
		assert_eq!((**jvmti).SetEventCallbacks.unwrap()(jvmti, &callbacks, size_of::<jvmtiEventCallbacks>() as i32), jvmtiError_JVMTI_ERROR_NONE);
		assert_eq!((**jvmti).SetEventNotificationMode.unwrap()(jvmti, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_VM_INIT, null_mut()), jvmtiError_JVMTI_ERROR_NONE);
	}
	
	JNI_OK as i32
}

pub unsafe extern "C" fn vm_init(
	_jvmti: *mut jvmtiEnv, env: *mut JNIEnv, _thread: jthread
) {
	println!("VM INIT");
	let native_accessor = (**env).FindClass.unwrap()(env, cstr!("dev/binclub/paperbin/native/NativeAccessor"));
	assert!(!native_accessor.is_null());
	let methods = vec![
		JNINativeMethod {
			name: cstr!("registerClassLoadHook"),
			signature: cstr!("(Ldev/binclub/paperbin/native/PaperBinClassTransformer;)V"),
			fnPtr: registerClassLoadHook as *mut c_void
		},
		JNINativeMethod {
			name: cstr!("appendToClassloader"),
			signature: cstr!("(Ljava/lang/String;Z)V"),
			fnPtr: Java_dev_binclub_paperbin_native_NativeAccessor_appendToClassloader as *mut c_void
		},
		JNINativeMethod {
			name: cstr!("registerAntiPhysicsCrash"),
			signature: cstr!("(Ljava/lang/reflect/Method;I)V"),
			fnPtr: Java_dev_binclub_paperbin_native_NativeAccessor_registerAntiPhysicsCrash as *mut c_void
		},
	];
	assert_eq!((**env).RegisterNatives.unwrap()(env, native_accessor, methods.as_ptr(), methods.len() as i32), JNI_OK as i32);
	
	AGENT_LOADED = true;
}

#[no_mangle]
pub unsafe extern "system" fn JNI_OnLoad(_vm: *mut JavaVM, _reserved: &mut c_void) -> c_int {
	if !AGENT_LOADED {
		panic!("Paperbin loaded without agent, please pass -agentpath: argument, read the ReadMe for more details");
	}
	JNI_VERSION_1_8 as i32
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_binclub_paperbin_native_NativeAccessor_appendToClassloader(
	env: *mut JNIEnv, _this: jobject,
	url: jstring, bootloader: jboolean
) {
	let jvmti: *mut jvmtiEnv = utils::get_jvmti(utils::get_vm(env));
	
	let mut is_copy: jboolean = 0;
	let utf8chars = (**env).GetStringUTFChars.unwrap()(env, url, is_copy.borrow_mut());
	if !utf8chars.is_null() {
		let result = if bootloader == 1 {
			(**jvmti).AddToBootstrapClassLoaderSearch.unwrap()(jvmti, utf8chars)
		} else {
			(**jvmti).AddToSystemClassLoaderSearch.unwrap()(jvmti, utf8chars)
		};
		
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't add to classloader ({})", result);
		}
	}
}

#[no_mangle]
pub unsafe extern "system" fn registerClassLoadHook(
	env: *mut JNIEnv,
	_obj: jobject,
	hook: jobject
) {
	assert!(!hook.is_null());
	
	let hook_class: jclass = (**env).GetObjectClass.unwrap()(env, hook);
	assert!(!hook_class.is_null());
	
	let hook_method: jmethodID = (**env).GetMethodID.unwrap()(
		env,
		hook_class,
		cstr!("onClassLoad"),
		cstr!("(Ljava/lang/Class;Ljava/lang/ClassLoader;Ljava/lang/String;[B)[B")
	);
	assert!(!hook_method.is_null());
	
	let prepare_method: jmethodID = (**env).GetMethodID.unwrap()(
		env,
		hook_class,
		cstr!("onClassPrepare"),
		cstr!("(Ljava/lang/Class;)V")
	);
	assert!(!prepare_method.is_null());
	
	HOOK_OBJECT = Some((**env).NewGlobalRef.unwrap()(env, hook));
	HOOK_METH_ID = Some(hook_method);
	HOOK_PREPARE_ID = Some(prepare_method);
	
	let jvmti: *mut jvmtiEnv = JVMTI.unwrap();
	
	{
		let mut callbacks = match CALLBACKS {
			Some(x) => x,
			None => zeroed()
		};
		callbacks.ClassFileLoadHook = Some(load_hook_handler);
		callbacks.ClassPrepare = Some(prepare_class_handler);
		CALLBACKS = Some(callbacks);
		
		let result = (**jvmti).SetEventCallbacks.unwrap()(jvmti, &callbacks, size_of::<jvmtiEventCallbacks>() as i32);
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't add jvmti callbacks ({})", result);
		}
	}
	
	// enable load hook
	{
		let result = (**jvmti).SetEventNotificationMode.unwrap()(jvmti, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null_mut());
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't enable jvmti callbacks ({})", result);
		}
	}
	// enable prepare hook
	{
		let result = (**jvmti).SetEventNotificationMode.unwrap()(jvmti, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_CLASS_PREPARE, null_mut());
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't enable jvmti callbacks ({})", result);
		}
	}
	assert_eq!((**env).ExceptionCheck.unwrap()(env), 0);
}

static mut HOOK_OBJECT: Option<jobject> = None;
static mut HOOK_METH_ID: Option<jmethodID> = None;
static mut HOOK_PREPARE_ID: Option<jmethodID> = None;

pub unsafe extern "C" fn load_hook_handler(
	jvmti: *mut jvmtiEnv,
	env: *mut JNIEnv,
	class_being_redefined: jclass,
	loader: jobject,
	name: *const c_char,
	_protection_domain: jobject,
	class_data_len: jint,
	class_data: *const c_uchar,
	new_class_data_len: *mut jint,
	new_class_data: *mut *mut c_uchar) {
	let class_arr: jbyteArray = (**env).NewByteArray.unwrap()(env, class_data_len);
	(**env).SetByteArrayRegion.unwrap()(env, class_arr, 0, class_data_len, class_data.cast());
	
	let class_name: jstring = if name.is_null() {
		null_mut()
	} else {
		(**env).NewStringUTF.unwrap()(env, name)
	};
	
	let new_class_arr: jbyteArray = (**env).CallObjectMethod.unwrap()(
		env,
		HOOK_OBJECT.unwrap(),
		HOOK_METH_ID.unwrap(),
		class_being_redefined,
		loader,
		class_name,
		class_arr
	);
	
	if new_class_arr.is_null() {
		return;
	}
	
	*new_class_data_len = (**env).GetArrayLength.unwrap()(env, new_class_arr);
	check_jni!((**jvmti).Allocate.unwrap()(jvmti, *new_class_data_len as jlong, new_class_data));
	
	(**env).GetByteArrayRegion.unwrap()(env, new_class_arr, 0, *new_class_data_len, *new_class_data as *mut i8);
}

pub unsafe extern "C" fn prepare_class_handler(
	_jvmti: *mut jvmtiEnv,
	env: *mut JNIEnv,
	_thread: jthread,
	class_being_redefined: jclass,
) {
	(**env).CallObjectMethod.unwrap()(
		env,
		HOOK_OBJECT.unwrap(),
		HOOK_PREPARE_ID.unwrap(),
		class_being_redefined
	);
}
