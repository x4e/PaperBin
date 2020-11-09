use rs_jvm_bindings::jni::{JavaVM, JNI_OK, JNIEnv, JNI_VERSION_1_8};
use std::ptr::null_mut;
use std::borrow::BorrowMut;
use rs_jvm_bindings::jvmti::{jvmtiEnv, JVMTI_VERSION, jvmtiCapabilities};
use std::os::raw::c_void;
use std::mem::zeroed;

pub unsafe fn get_vm(env: *mut JNIEnv) -> *mut JavaVM {
	let mut vm: *mut JavaVM = null_mut();
	{
		let result = (**env).GetJavaVM.unwrap()(env, vm.borrow_mut());
		if result != JNI_OK as i32 {
			panic!("Couldn't fetch vm instance ({})", result);
		}
	}
	vm
}

pub unsafe fn get_jni(vm: *mut JavaVM) -> *mut JNIEnv {
	let mut jni_ptr: *mut c_void = null_mut();
	{
		let result = (**vm).GetEnv.unwrap()(vm, jni_ptr.borrow_mut(), JNI_VERSION_1_8 as i32);
		if result != JNI_OK as i32 {
			panic!("Couldn't fetch jvmti instance ({})", result);
		}
	}
	jni_ptr as *mut JNIEnv
}

pub unsafe fn get_jvmti(vm: *mut JavaVM) -> *mut jvmtiEnv {
	let mut jvmti_ptr: *mut c_void = null_mut();
	{
		let result = (**vm).GetEnv.unwrap()(vm, jvmti_ptr.borrow_mut(), JVMTI_VERSION as i32);
		if result != JNI_OK as i32 {
			panic!("Couldn't fetch jvmti instance ({})", result);
		}
	}
	jvmti_ptr as *mut jvmtiEnv
}

pub unsafe fn print_caps(jvmti: *mut jvmtiEnv) {
	let mut available: jvmtiCapabilities = zeroed();
	(**jvmti).GetPotentialCapabilities.unwrap()(jvmti, &mut available);
	println!("can_tag_objects: {}", available.can_tag_objects());
	println!("can_generate_field_modification_events: {}", available.can_generate_field_modification_events());
	println!("can_generate_field_access_events: {}", available.can_generate_field_access_events());
	println!("can_get_bytecodes: {}", available.can_get_bytecodes());
	println!("can_get_synthetic_attribute: {}", available.can_get_synthetic_attribute());
	println!("can_get_owned_monitor_info: {}", available.can_get_owned_monitor_info());
	println!("can_get_current_contended_monitor: {}", available.can_get_current_contended_monitor());
	println!("can_get_monitor_info: {}", available.can_get_monitor_info());
	println!("can_pop_frame: {}", available.can_pop_frame());
	println!("can_redefine_classes: {}", available.can_redefine_classes());
	println!("can_signal_thread: {}", available.can_signal_thread());
	println!("can_get_source_file_name: {}", available.can_get_source_file_name());
	println!("can_get_line_numbers: {}", available.can_get_line_numbers());
	println!("can_get_source_debug_extension: {}", available.can_get_source_debug_extension());
	println!("can_access_local_variables: {}", available.can_access_local_variables());
	println!("can_maintain_original_method_order: {}", available.can_maintain_original_method_order());
	println!("can_generate_single_step_events: {}", available.can_generate_single_step_events());
	println!("can_generate_exception_events: {}", available.can_generate_exception_events());
	println!("can_generate_frame_pop_events: {}", available.can_generate_frame_pop_events());
	println!("can_generate_breakpoint_events: {}", available.can_generate_breakpoint_events());
	println!("can_suspend: {}", available.can_suspend());
	println!("can_redefine_any_class: {}", available.can_redefine_any_class());
	println!("can_get_current_thread_cpu_time: {}", available.can_get_current_thread_cpu_time());
	println!("can_generate_method_entry_events: {}", available.can_generate_method_entry_events());
	println!("can_generate_all_class_hook_events: {}", available.can_generate_all_class_hook_events());
}
