package com.anvilautomation;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.anvilautomation.key.KeyManager;

public class AnvilAutomationMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AnvilAutomationKeys.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AnvilAutomationKeys.handleKeys(client, AnvilAutomationController.getInstance());
            AnvilAutomationController.getInstance().tick(client);
        });
    }
}
