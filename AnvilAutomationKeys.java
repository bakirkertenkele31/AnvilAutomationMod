package com.anvilautomation;

import com.anvilautomation.gui.BotScreen;
import com.anvilautomation.key.KeyManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AnvilAutomationKeys {

    public static KeyBinding KEY_START;
    public static KeyBinding KEY_STOP;
    public static KeyBinding KEY_SAVE_CHEST;
    public static KeyBinding KEY_SAVE_STORAGE;
    public static KeyBinding KEY_OPEN_GUI;
    public static KeyBinding KEY_TOGGLE_PANEL;

    public static void init() {
        KEY_START         = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.anvilautomation.start",        InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.anvilautomation"));
        KEY_STOP          = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.anvilautomation.stop",         InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.anvilautomation"));
        KEY_SAVE_CHEST    = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.anvilautomation.savechest",    InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.anvilautomation"));
        KEY_SAVE_STORAGE  = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.anvilautomation.savestoragechest", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.anvilautomation"));
        KEY_OPEN_GUI      = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.anvilautomation.opengui",     InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.anvilautomation"));
        KEY_TOGGLE_PANEL  = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.anvilautomation.togglepanel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.anvilautomation"));
    }

    public static void handleKeys(MinecraftClient client, AnvilAutomationController ctrl) {
        if (KEY_OPEN_GUI.wasPressed()) {
            if (KeyManager.getInstance().isActivated()) {
                client.setScreen(new BotScreen());
            } else {
                client.setScreen(new com.anvilautomation.gui.ActivationScreen());
            }
        }
        if (KEY_START.wasPressed())        ctrl.start(client);
        if (KEY_STOP.wasPressed())         ctrl.stop(client);
        if (KEY_SAVE_CHEST.wasPressed())   ctrl.saveChest(client);
        if (KEY_SAVE_STORAGE.wasPressed()) ctrl.saveStorageChest(client);
    }
}
