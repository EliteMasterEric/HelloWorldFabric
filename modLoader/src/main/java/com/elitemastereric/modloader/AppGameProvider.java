package com.elitemastereric.modloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

/*
 * A custom GameProvider which grants Fabric Loader the necessary information to launch the app.
 */
public class AppGameProvider implements GameProvider {
    public static final String CLIENT_ENTRYPOINT = "com.elitemastereric.helloworld.Launch";
    public static final String[] ENTRYPOINTS = { CLIENT_ENTRYPOINT };

    public static final String PROPERTY_APP_DIRECTORY = "appDirectory";

    private static final GameTransformer TRANSFORMER = new AppGameTransformer();

    private Arguments arguments;
    private Path appJar;

    /*
     * Display an identifier for the app.
     */ @Override
    public String getGameId() {
        return "helloworld";
    }

    /*
     * Display a readable name for the app.
     */ @Override
    public String getGameName() {
        return "Hello World";
    }

    /*
     * Display a raw version string that may include build numbers or git hashes.
     */ @Override
    public String getRawGameVersion() {
        return "1.0.0-abcdef";
    }

    /*
     * Display a clean version string for display.
     */ @Override
    public String getNormalizedGameVersion() {
        return "1.0.0";
    }

    /*
     * Provides built-in mods, for example a mod that represents the app itself so
     * that mods can depend on specific versions.
     */
    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        HashMap<String, String> contactMap = new HashMap<>();
        contactMap.put("homepage", "https://elitemastereric.com/");

        BuiltinModMetadata.Builder modMetadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
            .setName(getGameName())
            .addAuthor("EliteMasterEric", contactMap)
            .setContact(new ContactInformationImpl(contactMap))
            .setDescription("A simple Hello World app for Fabric Loader.");

        BuiltinMod mod = new BuiltinMod(Collections.singletonList(appJar), modMetadata.build());

        return Collections.singletonList(mod);
    }

    /*
     * Provides the full class name of the app's entrypoint.
     */
    @Override
    public String getEntrypoint() {
        return CLIENT_ENTRYPOINT;
    }

    /*
     * Provides the directory path where the app's resources (such as config) should
     * be located
     * This is where the `mods` folder will be located.
     */
    @Override
    public Path getLaunchDirectory() {
        if (arguments == null) {
            return Paths.get(".");
        }

        return getLaunchDirectory(arguments);
    }

    private static Path getLaunchDirectory(Arguments arguments) {
        return Paths.get(arguments.getOrDefault(PROPERTY_APP_DIRECTORY, "."));
    }

    /*
     * Return true if the app needs to be deobfuscated.
     */
    @Override
    public boolean isObfuscated() {
        return false;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /*
     * Parse the arguments, locate the game directory, and return true if the game
     * directory is valid.
     */
    @Override
    public boolean locateGame(FabricLauncher launcher, String[] args) {
        this.arguments = new Arguments();
        this.arguments.parse(args);

        // Build a list of possible locations for the app JAR.
        List<String> appLocations = new ArrayList<>();
        // Respect "fabric.gameJarPath" if it is set.
        if (System.getProperty(SystemProperties.GAME_JAR_PATH) != null) {
            appLocations.add(System.getProperty(SystemProperties.GAME_JAR_PATH));
        }
        // List out default locations.
        appLocations.add("./helloworld-" + getNormalizedGameVersion() + ".jar");

        // Filter the list of possible locations based on whether the file exists.
        List<Path> existingAppLocations = appLocations.stream().map(p -> Paths.get(p).toAbsolutePath().normalize())
                .filter(Files::exists).toList();

        // Filter the list of possible locations based on whether they contain the required entrypoints
        GameProviderHelper.FindResult result = GameProviderHelper.findFirst(existingAppLocations, new HashMap<>(), true, ENTRYPOINTS);

        if (result == null || result.path == null) {
            // Tell the user we couldn't find the app JAR.
            String appLocationsString = appLocations.stream().map(p -> (String.format("* %s", Paths.get(p).toAbsolutePath().normalize())))
                .collect(Collectors.joining("\n"));
            
            Log.error(LogCategory.GAME_PROVIDER, "Could not locate the application JAR! We looked in: \n" + appLocationsString);

            return false;
        }

        this.appJar = result.path;

        return true;
    }

    /*
     * Add additional configuration to the FabricLauncher, but do not launch your
     * app.
     */
    @Override
    public void initialize(FabricLauncher launcher) {
        TRANSFORMER.locateEntrypoints(launcher, appJar);
    }

    /*
     * Return a GameTransformer that does extra modification on the app's JAR.
     */
    @Override
    public GameTransformer getEntrypointTransformer() {
        return TRANSFORMER;
    }

    /*
     * Called after transformers were initialized and mods were detected and loaded
     * (but not initialized).
     */
    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        launcher.addToClassPath(appJar);
    }

    /*
     * Launch the app in this function. This MUST be done via reflection.
     */
    @Override
    public void launch(ClassLoader loader) {
        try {
            Class<?> main = loader.loadClass(this.getEntrypoint());
            Method method = main.getMethod("main", String[].class);

            method.invoke(null, (Object) this.arguments.toArray());
        } catch (InvocationTargetException e) {
            throw new FormattedException("App has crashed!", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new FormattedException("Failed to launch App", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Arguments getArguments() {
        return this.arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        if (arguments == null) return new String[0];

        String[] ret = arguments.toArray();
        return ret;
    }
}
