package io.github.piscescup.fabricmc;

import io.github.piscescup.fabricmc.datagen.ZHCNLanguageProvider;
import io.github.piscescup.fabricmc.datagen.ENUSLanguageProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class CalcCommandsDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(ENUSLanguageProvider::new);
		pack.addProvider(ZHCNLanguageProvider::new);
	}
}
