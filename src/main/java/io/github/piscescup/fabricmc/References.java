package io.github.piscescup.fabricmc;


import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class References {
    public static final String MOD_ID = "calc-commands";

    private static final ModMetadata METADATA = FabricLoader.getInstance()
        .getModContainer(MOD_ID)
        .orElseThrow()
        .getMetadata();

    public static final String MOD_NAME = METADATA
        .getName();

    public static final String MOD_VERSION = METADATA
        .getVersion()
        .getFriendlyString();

    public static final Logger MOD_LOGGER = LogManager.getLogger(MOD_ID);

    private References() {}
}
