use rs_jvm_bindings::jvmti::{jvmtiEnv, jvmtiError_JVMTI_ERROR_NONE, jvmtiEventCallbacks, jlocation, jthread, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_BREAKPOINT};
use rs_jvm_bindings::jni::{JNIEnv, jobject, jmethodID};
use crate::{JVMTI, CALLBACKS};
use std::mem::zeroed;
use rs_jvm_bindings::jvm::JVM_CountStackFrames;

static mut PHYSICS_METH: Option<jmethodID> = None;

#[no_mangle]
pub unsafe extern "system" fn Java_dev_binclub_paperbin_native_NativeAccessor_registerAntiPhysicsCrash(
	env: *mut JNIEnv, _this: jobject,
	method: jobject
) {
	let jvmti: *mut jvmtiEnv = JVMTI.unwrap();
	
	let method_id: jmethodID = (**env).FromReflectedMethod.unwrap()(env, method);
	PHYSICS_METH = Some(method_id);
	
	// add breakpoint at bytecode index 0 (start of method)
	assert_eq!((**jvmti).SetBreakpoint.unwrap()(jvmti, method_id, 0), jvmtiError_JVMTI_ERROR_NONE, "Couldn't add breakpoint");
	
	{
		let mut callbacks = match CALLBACKS {
			Some(x) => x,
			None => zeroed()
		};
		callbacks.Breakpoint = Some(breakpoint_hook);
		CALLBACKS = Some(callbacks);
		
		let result = (**jvmti).SetEventCallbacks.unwrap()(jvmti, &callbacks, size_of::<jvmtiEventCallbacks>() as i32);
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't add jvmti callbacks ({})", result);
		}
	}
	
	// enable load hook
	{
		let result = (**jvmti).SetEventNotificationMode.unwrap()(jvmti, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_BREAKPOINT, null_mut());
		if result != jvmtiError_JVMTI_ERROR_NONE {
			panic!("Couldn't enable jvmti callbacks ({})", result);
		}
	}
}

pub unsafe extern "C" fn breakpoint_hook(
	_jvmti_env: *mut jvmtiEnv,
	_jni_env: *mut JNIEnv,
	_thread: jthread,
	method: jmethodID,
	_location: jlocation
) {
	if Some(method) != PHYSICS_METH {
		return
	}
	
	let jvmti: *mut jvmtiEnv = JVMTI.unwrap();
	(**jvmti).
	JVM_CountStackFrames()
}
