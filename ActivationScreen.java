package com.anvilautomation.gui;

import com.anvilautomation.key.KeyManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ActivationScreen extends Screen {

    private static final int COL_BG      = 0xFF0E0E12;
    private static final int COL_HEADER  = 0xFF1A1A24;
    private static final int COL_ACCENT  = 0xFF7B5CF0;
    private static final int COL_GREEN   = 0xFF3ECF8E;
    private static final int COL_RED     = 0xFFE05C5C;
    private static final int COL_SUBTEXT = 0xFF8888AA;

    private TextFieldWidget keyField;
    private String message      = "";
    private int    messageColor = COL_SUBTEXT;

    public ActivationScreen() {
        super(Text.literal("Kowalski Aktivasyon"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        keyField = new TextFieldWidget(textRenderer, cx - 100, cy - 10, 200, 20, Text.literal("Lisans Anahtarı"));
        keyField.setMaxLength(64);
        keyField.setText("Kowalski-SensizKalamam");
        keyField.setEditable(true);
        addDrawableChild(keyField);

        addDrawableChild(ButtonWidget.builder(Text.literal("  Aktive Et  "), btn -> tryActivate())
            .dimensions(cx - 50, cy + 20, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("İptal  "), btn -> close())
            .dimensions(cx - 50, cy + 46, 100, 18).build());
    }

    private void tryActivate() {
        String key = keyField.getText().trim();
        if (key.isEmpty()) { message = "Anahtar boş olamaz!"; messageColor = COL_RED; return; }
        boolean ok = KeyManager.getInstance().activate(key);
        if (ok) {
            message = "Aktivasyon başarılı!";
            messageColor = COL_GREEN;
            client.execute(() -> client.setScreen(new BotScreen()));
        } else {
            message = "Geçersiz anahtar!";
            messageColor = COL_RED;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = width / 2;
        int cy = height / 2;
        int w = 240, h = 160;
        int x = cx - w / 2, y = cy - h / 2;

        ctx.fill(x, y, x + w, y + h, COL_BG);
        // border
        ctx.fill(x, y, x + w, y + 1, COL_ACCENT);
        ctx.fill(x, y + h - 1, x + w, y + h, COL_ACCENT);
        ctx.fill(x, y, x + 1, y + h, COL_ACCENT);
        ctx.fill(x + w - 1, y, x + w, y + h, COL_ACCENT);

        ctx.fill(x, y, x + w, y + 24, COL_HEADER);
        ctx.drawText(textRenderer, "  Kowalski  ", x + 8, y + 8, 0xFFE8E8F0, false);
        ctx.drawText(textRenderer, "  Aktivasyon  ", cx - textRenderer.getWidth("  Aktivasyon  ") / 2, y + 32, COL_SUBTEXT, false);
        ctx.drawText(textRenderer, "Lisans anahtarı girin", cx - textRenderer.getWidth("Lisans anahtarı girin") / 2, cy - 28, COL_SUBTEXT, false);

        if (!message.isEmpty())
            ctx.drawText(textRenderer, message, cx - textRenderer.getWidth(message) / 2, cy + 70, messageColor, false);

        ctx.drawText(textRenderer, "Anahtar için: discord.gg/212so", cx - textRenderer.getWidth("Anahtar için: discord.gg/212so") / 2, y + h - 12, COL_SUBTEXT, false);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
