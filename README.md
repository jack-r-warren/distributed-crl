# Distributed CRL
> ### Carter Codell, Edward Shen, Ivan Chen, Jack Warren
>

## Project Configuration
IntelliJ should pick up the presence of Gradle within the project and ask to import it. After it is done, you'll want to use the `generateProto` Gradle task or use the "Regenerate Protobuf Code" IntelliJ run configuration I made.

Everything here generally assumes you have Java 13, though any Kotlin bytecode is Java 8 compatible. If you're using an older version you might hit API issues if we use something that isn't available for you.

Right now LibSodium is brought in as a dependency. You'll only need to have it installed if you try to run code that uses it (right now there's nothing that does).

## Project Layout
Subproject's folders won't show up on Git until they have something in them. The subproject folder names are in bold in the section below this.

Each subproject contains a `src` folder that contains the project's code.

Source code is in `src/main`, and test code is in `src/test`.

Within each of those folders, code is organized by language. For example:
- Kotlin source code would be in `src/main/kotlin`
- Java test code would be in `src/test/java`
- Protobuf files would be in `src/main/proto`

All subprojects have `java` enabled. The **proto** subproject is the only one to have the `proto` source type enabled (don't worry, you can use the generated code from anywhere). All subprojects *other than* **proto** have the `kotlin` source type enabled (this helps prevent circular dependencies).


### Applications
Executable applications here include:
- **certificate-authority**, our representation of a CA participant
- **intermediary**, our representation of a non-CA participant
- **client**, our representation of some non-participant client
- **discovery-server**, a bootstrapping server to assist in this proof-of-concept

Each application subproject has its entrypoint currently set as the `fun main()` in `src/main/kotlin/Main.kt`
### Others
Other libraries here include:
- **shared**, a library of shared logic between the applications
- **proto**, a store of Protobuf files for the entire project