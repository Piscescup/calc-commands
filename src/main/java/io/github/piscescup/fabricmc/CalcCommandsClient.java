package io.github.piscescup.fabricmc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import io.github.piscescup.fabricmc.commands.CalcCommands;

import static io.github.piscescup.fabricmc.References.*;

public class CalcCommandsClient implements ClientModInitializer {
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
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> CalcCommands.warmUp());
	}

}
