# HelloWorldFabric

This is a basic Java application which utilizes the Fabric mod loader to load and apply a Mixin. That sentence sounds like I'm talking about Minecraft, but this is actually an application in pure Java, and you could load any Java application through this.

This project contains three subprojects:

- `app`: A Hello World application.
- `modLoader`: A Fabric mod loader built to act as a class loader for the Hello World application.
- `exampleMod`: A Fabric mod for the Hello World application, which has a mixin.

## How to Test

1. Perform `gradle buildAndCopy` on `app/`. This will put `helloworld.jar`, the JAR we plan to inject into, into `./run`.
2. Perform `gradle buildAndCopy` on `modLoader/`. This will put `modloader.jar`, the JAR which overrides class loading for HelloWorld, into `./run`.
3. Perform `gradle buildAndCopy` on `exampleMod/`. This will put `examplemod.jar`, the JAR containing a mod with Mixin that affects HelloWorld, into `./run/mods`.
4. Run `./launch.bat`.

What happens here is:
- Fabric Loader is launched via the script
- Fabric Loader detects our ModLoader on the class path, and executes its GameProvider to receive the class path to modify and the game folder to load mods from.
- Fabric Loader loads any applicable mods, in this case our ExampleMod, and initializes it and applies its tranformers.
    - At this point, the behavior of `Main.class` in the HelloWorld app is overridden by the Mixin.
- Fabric Loader then executes the classpath of the applicattion.
    - This invokes `Launch.class`, which in turn invokes `Main.class`.

Without the mod installed, "Hello World" is printed, but with it installed, "Goodbye World" is printed.

## Additional Resources

- https://fabricmc.net/wiki/documentation:fabric_loader
- https://github.com/PseudoDistant/Game_Providers_List/blob/main/index.md