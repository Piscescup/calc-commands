package io.github.piscescup.fabricmc;


import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class References {
    public static final String MOD_ID = "calc-commands";

    public static final FabricLoader FABRIC_LOADER =
        FabricLoader.getInstance();

    public static final ModContainer CALC_CMDS_CONTAINER = FABRIC_LOADER.getModContainer(MOD_ID)
        .orElseThrow();

    public static final ModMetadata CALC_CMDS_METADATA = CALC_CMDS_CONTAINER.getMetadata();

    public static final String MOD_NAME = CALC_CMDS_METADATA
        .getName();

    public static final String MOD_VERSION = CALC_CMDS_METADATA
        .getVersion()
        .getFriendlyString();

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger MOD_LOGGER = LogManager.getLogger(MOD_NAME);


    @NonNull
    @Contract("_ -> new")
    public static Identifier fromPath(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
