use jvm_rs::jvmti::{jvmtiEnv, jvmtiError_JVMTI_ERROR_NONE, jvmtiEventCallbacks, jlocation, jthread, jvmtiEventMode_JVMTI_ENABLE, jvmtiEvent_JVMTI_EVENT_BREAKPOINT, jvmtiFrameInfo};
use jvm_rs::jni::{JNIEnv, jobject, jmethodID, jint};
use crate::{JVMTI, CALLBACKS};
use std::mem::{zeroed, size_of};
use std::ptr::null_mut;

static mut PHYSICS_METH: Option<jmethodID> = None;
static mut MAX_STACK_SIZE: usize = 500;

#[no_mangle]
pub unsafe extern "system" fn Java_dev_binclub_paperbin_native_NativeAccessor_registerAntiPhysicsCrash(
	env: *mut JNIEnv, _this: jobject,
	method: jobject, max_stack_size: jint
) {
	let jvmti: *mut jvmtiEnv = JVMTI.unwrap();
	
	let method_id: jmethodID = (**env).FromReflectedMethod.unwrap()(env, method);
	PHYSICS_METH = Some(method_id);
	MAX_STACK_SIZE = max_stack_size as usize;
	
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

static mut FRAMES: Option<Vec<jvmtiFrameInfo>> = None;

pub unsafe extern "C" fn breakpoint_hook(
	_jvmti_env: *mut jvmtiEnv,
	_jni_env: *mut JNIEnv,
	thread: jthread,
	method: jmethodID,
	_location: jlocation
) {
	if Some(method) != PHYSICS_METH {
		return
	}
	
	let jvmti: *mut jvmtiEnv = JVMTI.unwrap();
	
	if let None = FRAMES {
		FRAMES = Some(Vec::with_capacity(MAX_STACK_SIZE));
	}
	let max_frames = MAX_STACK_SIZE as jint;
	
	let frames: &mut Vec<jvmtiFrameInfo> = &mut FRAMES.as_mut().unwrap();
	let mut num_frames: jint = 0;
	
	(**jvmti).GetStackTrace.unwrap()(jvmti, thread, 0, max_frames, frames.as_mut_ptr(), &mut num_frames);
	
	if num_frames > max_frames {
		// just exit out of the method
		(**jvmti).ForceEarlyReturnVoid.unwrap()(jvmti, thread);
	}
}
