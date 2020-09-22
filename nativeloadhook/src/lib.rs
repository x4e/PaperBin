#[macro_use]
extern crate rs_jvm_bindings;
use rs_jvm_bindings::jni::{JavaVM, JNIEnv, jclass, jobject, jint, JNI_VERSION_1_8, jmethodID, jbyteArray, jstring, JNI_OK, jlong};
use rs_jvm_bindings::jvmti::{jvmtiEnv, jvmtiCapabilities, jvmtiError_JVMTI_ERROR_NONE, JVMTI_VERSION_1_2, jvmtiEventCallbacks, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_CLASS_FILE_LOAD_HOOK};
use rs_jvm_bindings::utils::*;

use std::os::raw::{c_void, c_int, c_char, c_uchar};
use std::borrow::BorrowMut;
use std::ptr::null_mut;
use std::mem::{zeroed, size_of};

#[no_mangle]
pub unsafe extern "system" fn JNI_OnLoad(_vm: *mut JavaVM, _reserved: &mut c_void) -> c_int {
	JNI_VERSION_1_8 as i32
}

/// (Static)
#[no_mangle]
pub unsafe extern "system" fn Java_dev_binclub_paperbin_native_NativeAccessor_registerClassLoadHook(
	env: *mut JNIEnv, _this: jobject,
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
	
	HOOK_OBJECT = Some((**env).NewGlobalRef.unwrap()(env, hook));
	HOOK_METH_ID = Some(hook_method);
	
	let mut vm: *mut JavaVM = null_mut();
	{
		let result = (**env).GetJavaVM.unwrap()(env, vm.borrow_mut());
		if result != JNI_OK as i32 {
			panic!("Couldn't fetch vm instance ({})", result);
		}
	}
	
	let mut jvmti_ptr: *mut c_void = null_mut();
	{
		let result = (**vm).GetEnv.unwrap()(vm, jvmti_ptr.borrow_mut(), JVMTI_VERSION_1_2 as i32);
		if result != JNI_OK as i32 {
			panic!("Couldn't fetch jvmti instance ({})", result);
		}
	}
	
	let jvmti: *mut jvmtiEnv = jvmti_ptr as *mut jvmtiEnv;
	{
		let mut capabilities: jvmtiCapabilities = zeroed();
		capabilities.set_can_retransform_classes(1);
		
		let result = (**jvmti).AddCapabilities.unwrap()(jvmti, &capabilities);
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't add jvmti capabilities ({})", result);
		}
	}
	
	{
		let mut callbacks: jvmtiEventCallbacks = zeroed();
		callbacks.ClassFileLoadHook = Some(load_hook_handler);
		
		let result = (**jvmti).SetEventCallbacks.unwrap()(jvmti, &callbacks, size_of::<jvmtiEventCallbacks>() as i32);
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't add jvmti callbacks ({})", result);
		}
	}
	
	{
		let result = (**jvmti).SetEventNotificationMode.unwrap()(jvmti, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null_mut());
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't enable jvmti callbacks ({})", result);
		}
	}
	assert_eq!((**env).ExceptionCheck.unwrap()(env), 0);
}

static mut HOOK_OBJECT: Option<jobject> = None;
static mut HOOK_METH_ID: Option<jmethodID> = None;

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
