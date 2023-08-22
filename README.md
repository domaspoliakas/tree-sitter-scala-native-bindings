# Tree-sitter for scala native

A library to make it easy for you to use treesitter from scala native. Will probably go nowhere but who knows!

## Synchronizing grammars

### YAML

scala-native only picks up `.cpp` extensions, and not `.cc`. As such `scanner.cc` needs to be renamed `scanner.cpp`. However, do not rename `schema.generated.cc`; it already gets `#include`ed in the `scanner.cc`.
### Testing

In order to test it you need to initialise the `tree-sitter` submodule and build it: `git submodule init && git submodule update && cd tree-sitter && make`
