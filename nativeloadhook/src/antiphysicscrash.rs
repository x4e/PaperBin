use rs_jvm_bindings::jvmti::{jvmtiEnv, jvmtiError_JVMTI_ERROR_NONE};
use rs_jvm_bindings::jni::{JNIEnv, jobject, jmethodID};
use crate::utils;

pub unsafe fn anti_physics_crash(env: *mut JNIEnv, jvmti: *mut jvmtiEnv) {
	let enabled = {
		let transformer = (**env).FindClass.unwrap()(env, cstr!("dev/binclub/paperbin/transformers/AntiPhysicsCrash"));
		assert!(!transformer.is_null());
		let meth = (**env).GetStaticMethodID.unwrap()(
			env,
			transformer,
			cstr!("enabled"),
			cstr!("()Z")
		);
		assert!(!meth.is_null());
		(**env).CallStaticBooleanMethod.unwrap()(env, transformer, meth) == 1
	};
	println!("Physics enabled: {}", enabled);
	
	if enabled {
	
	}
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_binclub_paperbin_native_NativeAccessor_registerAntiPhysicsCrash(
	env: *mut JNIEnv, _this: jobject,
	method: jobject
) {
	let jvmti: *mut jvmtiEnv = utils::get_jvmti(utils::get_vm(env));
	
	let method_id: jmethodID = (**env).FromReflectedMethod.unwrap()(env, method);
	
	// add breakpoint at bytecode index 0 (start of method)
	assert_eq!((**jvmti).SetBreakpoint.unwrap()(jvmti, method_id, 0), jvmtiError_JVMTI_ERROR_NONE, "Couldn't add breakpoint");
	
	println!("Set Breakpoint");
}
