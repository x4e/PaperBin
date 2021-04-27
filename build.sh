#!/bin/bash

rm -rf build/libs

cd nativeloadhook
cargo build --release --target x86_64-unknown-linux-gnu || exit 1
cargo build --release --target x86_64-pc-windows-gnu || exit 1
cargo build --release --target x86_64-apple-darwin || exit 1
cd ..

./gradlew build || exit 1

mv nativeloadhook/target/x86_64-unknown-linux-gnu/release/libJvmClassHook.so build/libs
mv nativeloadhook/target/x86_64-pc-windows-gnu/release/JvmClassHook.dll build/libs
mv nativeloadhook/target/x86_64-apple-darwin/release/libJvmClassHook.dylib build/libs/

