curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y

source ~/.cargo/env


rustup default 1.88.0
rustup target add wasm32-unknown-unknown
rustup component add rust-src
rustup component add rust-analyzer


cargo install -f wasm-bindgen-cli --version 0.2.105
