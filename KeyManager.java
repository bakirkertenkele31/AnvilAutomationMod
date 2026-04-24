package com.anvilautomation.gui;

import com.anvilautomation.AnvilAutomationController;
import com.anvilautomation.key.KeyManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BotScreen extends Screen {

    private static final String CLIENT_NAME    = "Kowalski";
    private static final String CLIENT_VERSION = "v3.0.0";
    private static final String DISCORD        = "discord.gg/212so";

    private static final int COL_BG      = 0xFF0E0E12;
    private static final int COL_HEADER  = 0xFF1A1A24;
    private static final int COL_ACCENT  = 0xFF7B5CF0;
    private static final int COL_ACCENT2 = 0xFF5B3FD0;
    private static final int COL_GREEN   = 0xFF3ECF8E;
    private static final int COL_RED     = 0xFFE05C5C;
    private static final int COL_TEXT    = 0xFFE8E8F0;
    private static final int COL_SUBTEXT = 0xFF8888AA;

    public BotScreen() {
        super(Text.literal("Kowalski Bot"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        // Kapat
        addDrawableChild(ButtonWidget.builder(Text.literal("  Kapat"), btn -> close())
            .dimensions(cx + 70, cy + 80, 60, 18).build());

        // Sandıkları sıfırla
        addDrawableChild(ButtonWidget.builder(Text.literal("  Sandıklar"), btn -> {
            AnvilAutomationController.getInstance().resetChests();
            close();
        }).dimensions(cx - 130, cy + 80, 90, 18).build());

        // Başlat / Durdur
        addDrawableChild(ButtonWidget.builder(
            Text.literal(AnvilAutomationController.getInstance().isRunning() ? "  Durdur" : "  Başla"),
            btn -> {
                var ctrl = AnvilAutomationController.getInstance();
                if (ctrl.isRunning()) ctrl.stop(client);
                else ctrl.start(client);
                close();
            }).dimensions(cx - 30, cy + 80, 70, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = width / 2;
        int cy = height / 2;
        int w = 260, h = 200;
        int x = cx - w / 2, y = cy - h / 2;

        // Arka plan
        ctx.fill(x, y, x + w, y + h, COL_BG);
        drawBorder(ctx, x, y, w, h, 1);

        // Header
        ctx.fill(x, y, x + w, y + 28, COL_HEADER);
        String title   = "  " + CLIENT_NAME + "  " + CLIENT_VERSION;
        String time    = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        int timeW      = textRenderer.getWidth(time);
        ctx.drawText(textRenderer, title, x + 8, y + 8, COL_TEXT, false);
        ctx.drawText(textRenderer, time,  x + w - timeW - 8, y + 8, COL_SUBTEXT, false);

        var ctrl    = AnvilAutomationController.getInstance();
        boolean running = ctrl.isRunning();

        // Durum kartı
        drawCard(ctx, x + 8, y + 36, w - 16, 40);
        int statusCol = running ? COL_GREEN : COL_RED;
        String statusStr = running ? "ÇALIŞIYOR" : "DURDURULDU";
        ctx.drawText(textRenderer, "Durum", x + 16, y + 42, COL_SUBTEXT, false);
        ctx.drawText(textRenderer, statusStr, x + 16, y + 54, statusCol, false);
        int done    = ctrl.getTotalCompleted();
        int current = ctrl.getCurrentHelmet();
        String doneStr = "Tamamlanan: " + done + " kask  (" + current + "/" + 3 + " büyü)";
        ctx.drawText(textRenderer, doneStr, x + 100, y + 54, COL_TEXT, false);

        // Sandık kartı
        drawCard(ctx, x + 8, y + 84, w - 16, 70);
        ctx.drawText(textRenderer, "Sandık Durumu", x + 16, y + 90, COL_SUBTEXT, false);
        int chestCount = ctrl.getMaterialChestCount();
        boolean hasOut = ctrl.hasOutputChest();
        String[] names = { "Kask", "BP4 Kitabı", "Unb3 Kitabı", "Mending" };
        for (int i = 0; i < 4; i++) {
            boolean ok   = i < chestCount;
            int col      = ok ? COL_GREEN : COL_RED;
            String mark  = ok ? "✔" : "✗";
            ctx.drawText(textRenderer, mark + " " + names[i], x + 16 + (i % 2) * 115, y + 102 + (i / 2) * 12, col, false);
        }
        String outStr   = hasOut ? "✔ Output Sandığı" : "✗ Output Sandığı";
        int outColor    = hasOut ? COL_GREEN : COL_RED;
        ctx.drawText(textRenderer, outStr, x + 16, y + 128, outColor, false);

        // Tuş ipucu
        String masked  = KeyManager.getInstance().getMaskedKey();
        String keyHint = "J = Sandık  |  K = Output  |  G = Başlat";
        ctx.drawText(textRenderer, keyHint, x + 8, y + h - 28, COL_SUBTEXT, false);
        ctx.drawText(textRenderer, "Lisans: " + masked, x + 8, y + h - 18, COL_SUBTEXT, false);
        ctx.drawText(textRenderer, DISCORD + "  |  xmari3162_", x + 8, y + h - 8, COL_SUBTEXT, false);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int t) {
        ctx.fill(x, y, x + w, y + t, COL_ACCENT);
        ctx.fill(x, y + h - t, x + w, y + h, COL_ACCENT);
        ctx.fill(x, y, x + t, y + h, COL_ACCENT);
        ctx.fill(x + w - t, y, x + w, y + h, COL_ACCENT);
    }

    private void drawCard(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, COL_HEADER);
    }

    private String getToggleLabel() {
        return AnvilAutomationController.getInstance().isRunning() ? "  Durdur" : "  Başla";
    }

    @Override
    public boolean shouldPause() { return false; }
}
