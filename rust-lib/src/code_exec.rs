use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use log::{debug, error};
use tokio::runtime::Builder;


#[unsafe(no_mangle)]
pub extern "system" fn Java_com_example_lancelot_rust_RustBridge_executeCode(
    mut env: JNIEnv,
    _class: JClass,
    code: JString,
    language_name: JString,
    input: JString,
) -> jstring {
    let code: String = env.get_string(&code).unwrap().into();
    let language_name: String = env.get_string(&language_name).unwrap().into();
    let input: String = env.get_string(&input).unwrap().into();

    // Creamos un runtime local para ejecutar async
    let rt = Builder::new_current_thread()
        .enable_all()
        .build()
        .unwrap();
    let result = rt.block_on(async {
        execute_code(&code, &language_name, &input).await
    });

    // Convertimos resultado a jstring para devolver a Java
    let output = result.unwrap_or_else(|err| err);

    env.new_string(output).unwrap().into_raw()
}

pub async fn execute_code(
    code: &str,
    language_name: &str,
    input: &str,
) -> Result<String, String> {
    let client = piston_rs::Client::new();
    let executor = piston_rs::Executor::new()
        .set_language(language_name)
        .set_version("*")
        .set_stdin(input)
        .add_file(
            piston_rs::File::default()
                .set_name("main.cpp")
                .set_content(code),
        );

    match client.execute(&executor).await {
        Ok(response) => {
            debug!("Language: {}", response.language);
            debug!("Version: {}", response.version);
            
            if let Some(c) = response.compile {
                debug!("Compilation: {}", c.output);
            }

            debug!("Output: {}", response.run.output);
            Ok(response.run.output)
        }
        Err(e) => {
            error!("Error: {}", e);
            Err(e.to_string())
        }
    }
}
