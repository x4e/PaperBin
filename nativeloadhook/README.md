Native library used by paperbin for registering class load hooks.

## Building
First make sure you have cargo installed with all the toolchains you are going to use. 
https://wiki.archlinux.org/index.php/Rust is helpful for this.

Linux:
```
cargo build --release --target "x86_64-unknown-linux-gnu"
```
Windows:
```
cargo build --release --target "x86_64-pc-windows-gnu"
```
Mac:
```
cargo build --release --target "x86_64-apple-darwin"
```
