use std::sync::Arc;
use anyhow::Error;
use jni::{objects::{GlobalRef, JClass, JString, JValue}, sys::jobject, JNIEnv, JavaVM};
use once_cell::sync::OnceCell;
use tokio::runtime::{Runtime, Builder};
use crate::code_exec::execute_code;
// Globally store the JavaVM and Tokio runtime
static JVM: OnceCell<Arc<JavaVM>> = OnceCell::new();
static TOKIO_RT: OnceCell<Runtime> = OnceCell::new();

#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(
    vm: JavaVM,
    _reserved: *mut std::ffi::c_void,
) -> jni::sys::jint {
    JVM.set(Arc::new(vm)).expect("Failed to set JavaVM");
    //TOKIO_RT.get_or_init(|| Runtime::new().expect("Failed to create Tokio runtime"));
    TOKIO_RT.get_or_init(|| Builder::new_multi_thread()
        .enable_all()
        .worker_threads(1)
        .build()
        .expect("Error building runtime")
    );
    jni::sys::JNI_VERSION_1_6
}

#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnUnload(_vm: *mut jni::sys::JavaVM, _reserved: *mut std::ffi::c_void) {
    log::debug!("JNI_OnUnload called. Cleaning service")
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_example_lancelot_rust_RustBridge_ktFuture(
    mut env: JNIEnv,
    _class: JClass,
    code: JString,
    language_name: JString,
    input: JString,
) -> jobject {
    // 1. Extraer cadenas en el hilo JNI
    let code_str: Result<String, Error> = match env.get_string(&code) {
        Ok(java_str) => match java_str.to_str() {
            Ok(cs) => Ok(cs.to_string()),
            Err(e) => {
                let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Invalid code string: {:?}", e));
                return std::ptr::null_mut();
            }
        },
        Err(e) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Failed to get code string: {:?}", e));
            return std::ptr::null_mut();
        }
    };

    let lang_str: Result<String, Error> = match env.get_string(&language_name) {
        Ok(java_str) => match java_str.to_str() {
            Ok(cs) => Ok(cs.to_string()),
            Err(e) => {
                let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Invalid code string: {:?}", e));
                return std::ptr::null_mut();
            }
        },
        Err(e) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Failed to get code string: {:?}", e));
            return std::ptr::null_mut();
        }
    };
    let input_str: Result<String, Error> = match env.get_string(&input) {
        Ok(java_str) => match java_str.to_str() {
            Ok(cs) => Ok(cs.to_string()),
            Err(e) => {
                let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Invalid code string: {:?}", e));
                return std::ptr::null_mut();
            }
        },
        Err(e) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Failed to get code string: {:?}", e));
            return std::ptr::null_mut();
        }
    };

    // 2. Crear y registrar un CompletableFuture
    let future_obj = match env.new_object(
        "java/util/concurrent/CompletableFuture",
        "()V",
        &[],
    ) {
        Ok(o) => o,
        Err(e) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Failed to create CompletableFuture: {:?}", e));
            return std::ptr::null_mut();
        }
    };
    let future_ref = match env.new_global_ref(future_obj) {
        Ok(r) => Arc::new(r),
        Err(e) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Failed to create global ref: {:?}", e));
            return std::ptr::null_mut();
        }
    };
    let future_ref_clone = future_ref.clone();

    // 3. Capturar JavaVM y Runtime para el hilo de fondo
    let jvm = JVM.get().expect("JavaVM not initialized").clone();
    let code_owned = code_str.unwrap();
    let lang_owned = lang_str.unwrap();
    let input_owned = input_str.unwrap();

    // 4. Lanzar tarea en el runtime global
    TOKIO_RT.get().unwrap().spawn(async move {
        // Ejecutar lógica asíncrona
        let res: Result<String, String> = execute_code(&code_owned, &lang_owned, &input_owned).await;

        // Adjuntar el hilo a JVM y completar el future
        let env = match jvm.attach_current_thread_permanently() {
            Ok(e) => e,
            Err(err) => {
                log::error!("Failed to attach thread: {:?}", err);
                return;
            }
        };
        if let Err(e) = complete_future(env, &future_ref_clone, res) {
            log::error!("Error completing future: {:?}", e);
        }
    });

    // 5. Devolver el CompletableFuture al código Java (retorna jobject)
    future_ref.as_obj().as_raw()
}

// Helper para completar el future con resultado o excepción
fn complete_future(
    mut env: JNIEnv,
    future: &GlobalRef,
    result: Result<String, String>,
) -> Result<(), jni::errors::Error> {
    match result {
        Ok(output) => {
            let jstr = env.new_string(output)?;
            env.call_method(
                future.as_obj(),
                "complete",
                "(Ljava/lang/Object;)Z",
                &[JValue::Object(&jstr)],
            )?;
        }
        Err(err_msg) => {
            let jstr = env.new_string(err_msg)?;
            let exception = env.new_object(
                "java/util/concurrent/CompletionException",
                "(Ljava/lang/String;)V",
                &[JValue::Object(&jstr)],
            )?;
            env.call_method(
                future.as_obj(),
                "completeExceptionally",
                "(Ljava/lang/Throwable;)Z",
                &[JValue::Object(&exception)],
            )?;
        }
    }
    Ok(())
}
