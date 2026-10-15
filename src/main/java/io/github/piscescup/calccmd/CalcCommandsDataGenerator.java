package io.github.piscescup.calccmd;

import io.github.piscescup.calccmd.datagen.ZHCNLanguageProvider;
import io.github.piscescup.calccmd.datagen.ENUSLanguageProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class CalcCommandsDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(ENUSLanguageProvider::new);
		pack.addProvider(ZHCNLanguageProvider::new);
	}
}
