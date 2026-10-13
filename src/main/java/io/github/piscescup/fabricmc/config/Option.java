package io.github.piscescup.fabricmc.config;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

/**
 *
 * @author REN YuanTong
 * @since
 */
public interface Option extends StringRepresentable {

    String getValue();

    @NonNull
    @Override
    default String getSerializedName() {
        return getValue();
    }
}
