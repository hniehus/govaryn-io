use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
struct ScenarioInput {
    seed: u64,
    constraints: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize)]
struct ScenarioResult {
    seed: u64,
    summary: String,
}

fn main() {
    let input = ScenarioInput { seed: 42, constraints: vec!["budget<=1.0m".into()] };
    let result = ScenarioResult { seed: input.seed, summary: "ok".into() };
    println!("{}", serde_json::to_string_pretty(&result).unwrap());
}
