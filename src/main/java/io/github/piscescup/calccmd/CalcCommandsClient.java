package io.github.piscescup.calccmd;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import io.github.piscescup.calccmd.commands.CalcCommands;

import static io.github.piscescup.calccmd.References.MOD_LOGGER;
import static io.github.piscescup.calccmd.References.MOD_NAME;
import static io.github.piscescup.calccmd.References.MOD_VERSION;

public final class CalcCommandsClient implements ClientModInitializer {
	/**
	 * Runs the mod initializer on the client environment.
	 */
	@Override
	public void onInitializeClient() {
		MOD_LOGGER.info(
			"Hello, {} {}, powered by Minecraft.",
			MOD_NAME, MOD_VERSION
		);
		ClientCommandRegistrationCallback.EVENT.register(CalcCommands::register);
		ClientLifecycleEvents.CLIENT_STARTED.register(CalcCommands::warmUp);
	}

}
