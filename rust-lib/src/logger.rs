use jni::JNIEnv;
use jni::objects::JClass;
use android_logger::Config;
use log::LevelFilter;

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_example_lancelot_rust_RustBridge_initLogger(
    _env: JNIEnv,
    _class: JClass,
) {
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Debug)
            .with_tag("RustBridge"),
    );
}
