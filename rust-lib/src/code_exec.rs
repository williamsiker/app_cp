use log::{debug, error};
use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct ExecutionResult {
    pub output: String,
    pub compile_output: Option<String>,
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

pub async fn execute_code_detailed(
    code: &str,
    language_name: &str,
    input: &str,
) -> Result<ExecutionResult, String> {
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

            if let Some(ref c) = response.compile {
                debug!("Compilation: {}", c.output);
            }

            debug!("Output: {}", response.run.output);
            Ok(ExecutionResult {
                output: response.run.output,
                compile_output: response.compile.map(|c| c.output),
            })
        }
        Err(e) => {
            error!("Error: {}", e);
            Err(e.to_string())
        }
    }
}
