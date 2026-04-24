package com.anvilautomation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.List;

public class AnvilAutomationController {

    public enum State {
        IDLE,
        // Sandıklardan malzeme al
        WALK_TO_CHEST0, OPEN_CHEST0, WAIT_CHEST0, GET_HELMET, CLOSE_CHEST0,
        WALK_TO_CHEST1, OPEN_CHEST1, WAIT_CHEST1, GET_MENDING, CLOSE_CHEST1,
        WALK_TO_CHEST2, OPEN_CHEST2, WAIT_CHEST2, GET_UNB3, CLOSE_CHEST2,
        WALK_TO_CHEST3, OPEN_CHEST3, WAIT_CHEST3, GET_BP4, CLOSE_CHEST3,
        // Örse git, 1. büyü: BP4
        WALK_TO_ANVIL,
        OPEN_ANVIL,   WAIT_ANVIL_OPEN,   SLOT1_HELMET,        SLOT2_BOOK_BP4,   WAIT_RESULT_BP4,   TAKE_RESULT_BP4,   CLOSE_ANVIL_1,
        // 2. büyü: Unb3
        OPEN_ANVIL_2, WAIT_ANVIL_OPEN_2, SLOT1_INTER_HELMET,  SLOT2_BOOK_UNB3,  WAIT_RESULT_UNB3,  TAKE_RESULT_UNB3,  CLOSE_ANVIL_2,
        // 3. büyü: Mending
        OPEN_ANVIL_3, WAIT_ANVIL_OPEN_3, SLOT1_INTER_HELMET_2,SLOT2_BOOK_MENDING,WAIT_RESULT_MENDING,TAKE_RESULT_MENDING,CLOSE_ANVIL_3,
        // Depolama sandığına götür
        WALK_TO_STORAGE, OPEN_STORAGE, WAIT_STORAGE_OPEN, STORE_HELMET, CLOSE_STORAGE,
        // XP bekle
        WAITING_XP,
        WAIT_NEW_ANVIL
    }

    private static final AnvilAutomationController INSTANCE = new AnvilAutomationController();
    public static AnvilAutomationController getInstance() { return INSTANCE; }

    private static final int TICK_DELAY        = 3;
    private static final int BATCH_SIZE        = 9;
    private static final double NEAR_DIST      = 3.5;
    private static final int WALK_TIMEOUT      = 200;
    private static final int ANVIL_WAIT_TIMEOUT= 100;

    private State state        = State.IDLE;
    private State stateAfterXP = State.IDLE;
    private boolean running    = false;
    private int tickCounter    = 0;
    private int walkTick       = 0;
    private int anvilWaitTick  = 0;
    private int currentHelmet  = 0;
    private int totalCompleted = 0;

    private BlockPos anvilPos       = null;
    private List<BlockPos> chestPosList  = new ArrayList<>();
    private BlockPos storageChestPos     = null;

    // --- Public API ---
    public boolean isRunning()         { return running; }
    public int getTotalCompleted()     { return totalCompleted; }
    public int getCurrentHelmet()      { return currentHelmet; }
    public int getMaterialChestCount() { return chestPosList.size(); }
    public boolean hasOutputChest()    { return storageChestPos != null; }
    public State getState()            { return state; }

    public void resetChests() {
        chestPosList.clear();
        storageChestPos = null;
    }

    public void start(MinecraftClient client) {
        if (running) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        State resume = detectResumeState(client);
        if (resume != State.IDLE) {
            player.sendMessage(Text.literal("[212SO] Bot kaldığı yerden devam ediyor... (State: " + resume + ")"), false);
            state = resume;
        } else {
            state = State.WALK_TO_CHEST0;
        }
        currentHelmet  = 0;
        totalCompleted = 0;
        running        = true;
        tickCounter    = 0;
    }

    public void stop(MinecraftClient client) {
        running = false;
        state   = State.IDLE;
        if (client.player != null)
            client.player.sendMessage(Text.literal("[212SO] Bot durduruldu."), false);
    }

    public void saveChest(MinecraftClient client) {
        BlockPos pos = getLookingAt(client);
        if (pos == null) {
            client.player.sendMessage(Text.literal("[212SO] Örse bak, sonra G bas!"), false);
            return;
        }
        if (anvilPos == null && isAnvil(client, pos)) {
            anvilPos = pos;
            client.player.sendMessage(Text.literal("[212SO] Örs kaydedildi: " + pos), false);
            return;
        }
        chestPosList.add(pos);
        int n = chestPosList.size();
        client.player.sendMessage(
            Text.literal("[212SO] " + n + " chest lazım, şu an: " + n + "/4  (Bot | Mending | Unb3 | BP4)"), false);
    }

    public void saveStorageChest(MinecraftClient client) {
        BlockPos pos = getLookingAt(client);
        if (pos == null) {
            client.player.sendMessage(Text.literal("[212SO] Output chest kaydedilmedi! K ile kaydet."), false);
            return;
        }
        storageChestPos = pos;
        client.player.sendMessage(Text.literal("[212SO] Output sandığı kaydedildi: " + pos), false);
    }

    // --- Tick ---
    public void tick(MinecraftClient client) {
        if (!running || client.player == null) return;
        if (++tickCounter < TICK_DELAY) return;
        tickCounter = 0;

        ClientPlayerEntity player = client.player;

        switch (state) {

            // === SANDIK 0: Kask ===
            case WALK_TO_CHEST0 -> {
                if (chestPosList.size() < 4) {
                    player.sendMessage(Text.literal("[212SO] 4 sandık gerekli! J ile kaydet."), false);
                    stop(client); return;
                }
                walkTo(client, chestPosList.get(0));
                if (isNear(player, chestPosList.get(0))) state = State.OPEN_CHEST0;
            }
            case OPEN_CHEST0 -> { interactBlock(client, chestPosList.get(0)); state = State.WAIT_CHEST0; walkTick = 0; }
            case WAIT_CHEST0 -> {
                if (client.currentScreen != null) { state = State.GET_HELMET; return; }
                if (++walkTick > 30) { state = State.OPEN_CHEST0; walkTick = 0; }
            }
            case GET_HELMET -> {
                if (!(client.player.currentScreenHandler instanceof GenericContainerScreenHandler sh)) {
                    state = State.WALK_TO_CHEST0; return;
                }
                int slot = findCleanHelmet(sh);
                if (slot < 0) {
                    player.sendMessage(Text.literal("[212SO] Temiz Diamond Kask bulunamadı!"), false);
                    stop(client); return;
                }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.QUICK_MOVE, player);
                state = State.CLOSE_CHEST0;
            }
            case CLOSE_CHEST0 -> { client.player.closeHandledScreen(); state = State.WALK_TO_CHEST1; }

            // === SANDIK 1: Mending ===
            case WALK_TO_CHEST1 -> {
                walkTo(client, chestPosList.get(1));
                if (isNear(player, chestPosList.get(1))) state = State.OPEN_CHEST1;
            }
            case OPEN_CHEST1  -> { interactBlock(client, chestPosList.get(1)); state = State.WAIT_CHEST1; walkTick = 0; }
            case WAIT_CHEST1  -> {
                if (client.currentScreen != null) { state = State.GET_MENDING; return; }
                if (++walkTick > 30) { state = State.OPEN_CHEST1; walkTick = 0; }
            }
            case GET_MENDING  -> {
                if (!(client.player.currentScreenHandler instanceof GenericContainerScreenHandler sh)) {
                    state = State.WALK_TO_CHEST1; return;
                }
                int slot = findBook(sh, "mending");
                if (slot < 0) {
                    player.sendMessage(Text.literal("[212SO] Mending kitabı chest'e ulaşılamadı"), false);
                    stop(client); return;
                }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.QUICK_MOVE, player);
                state = State.CLOSE_CHEST1;
            }
            case CLOSE_CHEST1 -> { client.player.closeHandledScreen(); state = State.WALK_TO_CHEST2; }

            // === SANDIK 2: Unbreaking III ===
            case WALK_TO_CHEST2 -> {
                walkTo(client, chestPosList.get(2));
                if (isNear(player, chestPosList.get(2))) state = State.OPEN_CHEST2;
            }
            case OPEN_CHEST2  -> { interactBlock(client, chestPosList.get(2)); state = State.WAIT_CHEST2; walkTick = 0; }
            case WAIT_CHEST2  -> {
                if (client.currentScreen != null) { state = State.GET_UNB3; return; }
                if (++walkTick > 30) { state = State.OPEN_CHEST2; walkTick = 0; }
            }
            case GET_UNB3     -> {
                if (!(client.player.currentScreenHandler instanceof GenericContainerScreenHandler sh)) {
                    state = State.WALK_TO_CHEST2; return;
                }
                int slot = findBook(sh, "unbreaking");
                if (slot < 0) { stop(client); return; }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.QUICK_MOVE, player);
                state = State.CLOSE_CHEST2;
            }
            case CLOSE_CHEST2 -> { client.player.closeHandledScreen(); state = State.WALK_TO_CHEST3; }

            // === SANDIK 3: Blast Protection IV ===
            case WALK_TO_CHEST3 -> {
                walkTo(client, chestPosList.get(3));
                if (isNear(player, chestPosList.get(3))) state = State.OPEN_CHEST3;
            }
            case OPEN_CHEST3  -> { interactBlock(client, chestPosList.get(3)); state = State.WAIT_CHEST3; walkTick = 0; }
            case WAIT_CHEST3  -> {
                if (client.currentScreen != null) { state = State.GET_BP4; return; }
                if (++walkTick > 30) { state = State.OPEN_CHEST3; walkTick = 0; }
            }
            case GET_BP4      -> {
                if (!(client.player.currentScreenHandler instanceof GenericContainerScreenHandler sh)) {
                    state = State.WALK_TO_CHEST3; return;
                }
                int slot = findBook(sh, "blast_protection");
                if (slot < 0) { stop(client); return; }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.QUICK_MOVE, player);
                state = State.CLOSE_CHEST3;
            }
            case CLOSE_CHEST3 -> { client.player.closeHandledScreen(); state = State.WALK_TO_ANVIL; }

            // === ÖRSE GİT ===
            case WALK_TO_ANVIL -> {
                if (anvilPos == null) { anvilPos = findNearbyAnvil(client); }
                if (anvilPos == null) {
                    player.sendMessage(Text.literal("[212SO] Örs bulunamadı! G ile kaydet."), false);
                    stop(client); return;
                }
                walkTo(client, anvilPos);
                if (isNear(player, anvilPos)) state = State.OPEN_ANVIL;
            }

            // === BÜYÜ 1: Patlama Koruması IV ===
            case OPEN_ANVIL -> {
                int xpNeeded = 8;
                if (!hasXP(player, xpNeeded)) { warnXP(player, xpNeeded); stateAfterXP = State.OPEN_ANVIL; state = State.WAITING_XP; return; }
                interactBlock(client, anvilPos); state = State.WAIT_ANVIL_OPEN; anvilWaitTick = 0;
            }
            case WAIT_ANVIL_OPEN -> {
                if (client.player.currentScreenHandler instanceof AnvilScreenHandler) { state = State.SLOT1_HELMET; return; }
                if (++anvilWaitTick > ANVIL_WAIT_TIMEOUT) { state = State.WAIT_NEW_ANVIL; return; }
            }
            case SLOT1_HELMET -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                int slot = findCleanHelmetInv(player);
                if (slot < 0) { stop(client); return; }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(sh.syncId, 0, 0, SlotActionType.PICKUP, player);
                state = State.SLOT2_BOOK_BP4;
            }
            case SLOT2_BOOK_BP4 -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                int slot = findBookInInv(player, "blast_protection");
                if (slot < 0) { stop(client); return; }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(sh.syncId, 1, 0, SlotActionType.PICKUP, player);
                state = State.WAIT_RESULT_BP4;
            }
            case WAIT_RESULT_BP4 -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                if (!sh.getSlot(2).getStack().isEmpty()) state = State.TAKE_RESULT_BP4;
            }
            case TAKE_RESULT_BP4 -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                client.interactionManager.clickSlot(sh.syncId, 2, 0, SlotActionType.QUICK_MOVE, player);
                state = State.CLOSE_ANVIL_1;
            }
            case CLOSE_ANVIL_1 -> { client.player.closeHandledScreen(); state = State.OPEN_ANVIL_2; }

            // === BÜYÜ 2: Kırılmazlık III ===
            case OPEN_ANVIL_2 -> {
                int xpNeeded = 6;
                if (!hasXP(player, xpNeeded)) { warnXP(player, xpNeeded); stateAfterXP = State.OPEN_ANVIL_2; state = State.WAITING_XP; return; }
                interactBlock(client, anvilPos); state = State.WAIT_ANVIL_OPEN_2; anvilWaitTick = 0;
            }
            case WAIT_ANVIL_OPEN_2 -> {
                if (client.player.currentScreenHandler instanceof AnvilScreenHandler) { state = State.SLOT1_INTER_HELMET; return; }
                if (++anvilWaitTick > ANVIL_WAIT_TIMEOUT) { state = State.WAIT_NEW_ANVIL; return; }
            }
            case SLOT1_INTER_HELMET -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                int slot = findHelmetWithEnchant(player, "blast_protection");
                if (slot < 0) { stop(client); return; }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(sh.syncId, 0, 0, SlotActionType.PICKUP, player);
                state = State.SLOT2_BOOK_UNB3;
            }
            case SLOT2_BOOK_UNB3 -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                int slot = findBookInInv(player, "unbreaking");
                if (slot < 0) { stop(client); return; }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(sh.syncId, 1, 0, SlotActionType.PICKUP, player);
                state = State.WAIT_RESULT_UNB3;
            }
            case WAIT_RESULT_UNB3 -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                if (!sh.getSlot(2).getStack().isEmpty()) state = State.TAKE_RESULT_UNB3;
            }
            case TAKE_RESULT_UNB3 -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                client.interactionManager.clickSlot(sh.syncId, 2, 0, SlotActionType.QUICK_MOVE, player);
                state = State.CLOSE_ANVIL_2;
            }
            case CLOSE_ANVIL_2 -> { client.player.closeHandledScreen(); state = State.OPEN_ANVIL_3; }

            // === BÜYÜ 3: Onarım (Mending) ===
            case OPEN_ANVIL_3 -> {
                int xpNeeded = 9;
                if (!hasXP(player, xpNeeded)) { warnXP(player, xpNeeded); stateAfterXP = State.OPEN_ANVIL_3; state = State.WAITING_XP; return; }
                interactBlock(client, anvilPos); state = State.WAIT_ANVIL_OPEN_3; anvilWaitTick = 0;
            }
            case WAIT_ANVIL_OPEN_3 -> {
                if (client.player.currentScreenHandler instanceof AnvilScreenHandler) { state = State.SLOT1_INTER_HELMET_2; return; }
                if (++anvilWaitTick > ANVIL_WAIT_TIMEOUT) { state = State.WAIT_NEW_ANVIL; return; }
            }
            case SLOT1_INTER_HELMET_2 -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                int slot = findHelmetWithEnchant(player, "unbreaking");
                if (slot < 0) { stop(client); return; }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(sh.syncId, 0, 0, SlotActionType.PICKUP, player);
                state = State.SLOT2_BOOK_MENDING;
            }
            case SLOT2_BOOK_MENDING -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                int slot = findBookInInv(player, "mending");
                if (slot < 0) { stop(client); return; }
                client.interactionManager.clickSlot(sh.syncId, slot, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(sh.syncId, 1, 0, SlotActionType.PICKUP, player);
                state = State.WAIT_RESULT_MENDING;
            }
            case WAIT_RESULT_MENDING -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                if (!sh.getSlot(2).getStack().isEmpty()) state = State.TAKE_RESULT_MENDING;
            }
            case TAKE_RESULT_MENDING -> {
                AnvilScreenHandler sh = (AnvilScreenHandler) client.player.currentScreenHandler;
                client.interactionManager.clickSlot(sh.syncId, 2, 0, SlotActionType.QUICK_MOVE, player);
                state = State.CLOSE_ANVIL_3;
            }
            case CLOSE_ANVIL_3 -> { client.player.closeHandledScreen(); state = State.WALK_TO_STORAGE; }

            // === DEPOLAMA ===
            case WALK_TO_STORAGE -> {
                if (storageChestPos == null) {
                    player.sendMessage(Text.literal("[212SO] Output chest kaydedilmedi! K ile kaydet."), false);
                    stop(client); return;
                }
                walkTo(client, storageChestPos);
                if (isNear(player, storageChestPos)) state = State.OPEN_STORAGE;
            }
            case OPEN_STORAGE -> { interactBlock(client, storageChestPos); state = State.WAIT_STORAGE_OPEN; walkTick = 0; }
            case WAIT_STORAGE_OPEN -> {
                if (client.currentScreen != null) { state = State.STORE_HELMET; return; }
                if (++walkTick > 30) { state = State.OPEN_STORAGE; walkTick = 0; }
            }
            case STORE_HELMET -> {
                if (!(client.player.currentScreenHandler instanceof GenericContainerScreenHandler sh)) {
                    state = State.WALK_TO_STORAGE; return;
                }
                int invSlot = findFullHelmet(player);
                if (invSlot >= 0) {
                    int screenSlot = invSlotToChestScreen(sh, invSlot);
                    client.interactionManager.clickSlot(sh.syncId, screenSlot, 0, SlotActionType.QUICK_MOVE, player);
                }
                state = State.CLOSE_STORAGE;
            }
            case CLOSE_STORAGE -> {
                client.player.closeHandledScreen();
                totalCompleted++;
                currentHelmet = 0;
                player.sendMessage(Text.literal("[212SO] Kask #" + totalCompleted + " tamamlandı!"), false);
                state = State.WALK_TO_CHEST0;
            }

            // === XP / ÖRS BEKLEME ===
            case WAITING_XP -> {
                int needed = xpForState(stateAfterXP);
                if (hasXP(player, needed)) {
                    state = stateAfterXP;
                } else {
                    if (tickCounter % 40 == 0)
                        player.sendMessage(Text.literal("[212SO] XP bekleniyor... Gereken: " + needed + ", şu an: " + player.experienceLevel), false);
                }
            }
            case WAIT_NEW_ANVIL -> {
                BlockPos found = findNearbyAnvil(client);
                if (found != null) {
                    anvilPos = found;
                    player.sendMessage(Text.literal("[212SO] Yeni örs bulundu!"), false);
                    state = State.WALK_TO_ANVIL;
                } else {
                    player.sendMessage(Text.literal("[212SO] Örs kırıldı! Yeni örs bekleniyor..."), false);
                }
            }
            default -> {}
        }
    }

    // --- Yardımcı metodlar ---

    private State detectResumeState(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        boolean hasBP    = countInvBooks(p, "blast_protection") > 0;
        boolean hasUnb   = countInvBooks(p, "unbreaking") > 0;
        boolean hasMend  = countInvBooks(p, "mending") > 0;
        int helmets      = countInvHelmets(p, false, false, false);
        int bp4Helmets   = countInvHelmets(p, true, false, false);
        int unb3Helmets  = countInvHelmets(p, true, true, false);
        int fullHelmets  = countInvHelmets(p, true, true, true);

        if (fullHelmets > 0)   return State.WALK_TO_STORAGE;
        if (unb3Helmets > 0)   return State.OPEN_ANVIL_3;
        if (bp4Helmets > 0)    return State.OPEN_ANVIL_2;
        if (helmets > 0 && hasBP && hasUnb && hasMend) return State.WALK_TO_ANVIL;
        return State.IDLE;
    }

    private int xpForState(State s) {
        return switch (s) {
            case OPEN_ANVIL   -> 8;
            case OPEN_ANVIL_2 -> 6;
            case OPEN_ANVIL_3 -> 9;
            default -> 0;
        };
    }

    private boolean hasXP(ClientPlayerEntity p, int needed) {
        return p.experienceLevel >= needed;
    }

    private void warnXP(ClientPlayerEntity p, int needed) {
        p.sendMessage(Text.literal("[212SO] XP bekleniyor... Gereken: " + needed + ", şu an: " + p.experienceLevel), false);
    }

    private BlockPos getLookingAt(MinecraftClient client) {
        var hit = client.crosshairTarget;
        if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr) return bhr.getBlockPos();
        return null;
    }

    private boolean isAnvil(MinecraftClient client, BlockPos pos) {
        var state = client.world.getBlockState(pos);
        return state.isOf(Blocks.ANVIL) || state.isOf(Blocks.CHIPPED_ANVIL) || state.isOf(Blocks.DAMAGED_ANVIL);
    }

    private BlockPos findNearbyAnvil(MinecraftClient client) {
        if (client.player == null || client.world == null) return null;
        BlockPos origin = client.player.getBlockPos();
        for (int dx = -5; dx <= 5; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -5; dz <= 5; dz++) {
                    BlockPos p = origin.add(dx, dy, dz);
                    if (isAnvil(client, p)) return p;
                }
        return null;
    }

    private void interactBlock(MinecraftClient client, BlockPos pos) {
        if (client.world == null || client.player == null) return;
        var blockState = client.world.getBlockState(pos);
        client.interactionManager.interactBlock(client.player,
            net.minecraft.util.Hand.MAIN_HAND,
            new net.minecraft.util.hit.BlockHitResult(
                net.minecraft.util.math.Vec3d.ofCenter(pos),
                net.minecraft.util.math.Direction.UP, pos, false));
    }

    private void walkTo(MinecraftClient client, BlockPos pos) {
        if (client.player == null) return;
        double dx = pos.getX() + 0.5 - client.player.getX();
        double dz = pos.getZ() + 0.5 - client.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < NEAR_DIST) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        client.player.setYaw(yaw);
        client.options.forwardKey.setPressed(true);
        if (++walkTick > WALK_TIMEOUT) {
            client.options.forwardKey.setPressed(false);
            walkTick = 0;
        }
    }

    private boolean isNear(ClientPlayerEntity player, BlockPos pos) {
        double dx = pos.getX() + 0.5 - player.getX();
        double dy = pos.getY()       - player.getY();
        double dz = pos.getZ() + 0.5 - player.getZ();
        return Math.sqrt(dx*dx + dy*dy + dz*dz) < NEAR_DIST;
    }

    // --- Envanter arama ---

    private int findCleanHelmet(GenericContainerScreenHandler sh) {
        for (int i = 0; i < sh.getRows() * 9; i++) {
            ItemStack s = sh.getSlot(i).getStack();
            if (s.getItem() == Items.DIAMOND_HELMET) {
                var enc = s.get(DataComponentTypes.ENCHANTMENTS);
                if (enc == null || enc.isEmpty()) return i;
            }
        }
        return -1;
    }

    private int findCleanHelmetInv(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == Items.DIAMOND_HELMET) {
                var enc = s.get(DataComponentTypes.ENCHANTMENTS);
                if (enc == null || enc.isEmpty()) return i;
            }
        }
        return -1;
    }

    private int findFullHelmet(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == Items.DIAMOND_HELMET && isFullHelmet(s)) return i;
        }
        return -1;
    }

    private boolean isFullHelmet(ItemStack s) {
        return hasEnchant(s, "blast_protection") && hasEnchant(s, "unbreaking") && hasEnchant(s, "mending");
    }

    private int findHelmetWithEnchant(ClientPlayerEntity player, String enchId) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == Items.DIAMOND_HELMET && hasEnchant(s, enchId)) return i;
        }
        return -1;
    }

    private int findBook(GenericContainerScreenHandler sh, String enchId) {
        for (int i = 0; i < sh.getRows() * 9; i++) {
            ItemStack s = sh.getSlot(i).getStack();
            if (s.getItem() == Items.ENCHANTED_BOOK && isBook(s, enchId)) return i;
        }
        return -1;
    }

    private int findBookInInv(ClientPlayerEntity player, String enchId) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == Items.ENCHANTED_BOOK && isBook(s, enchId)) return i;
        }
        return -1;
    }

    private boolean isBook(ItemStack s, String enchId) {
        var stored = s.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (stored == null) return false;
        for (var entry : stored.getEnchantments()) {
            String id = entry.getKey().map(e -> e.value().toString()).orElse("");
            if (id.contains(enchId)) return true;
        }
        return false;
    }

    private boolean hasEnchant(ItemStack s, String enchId) {
        var enc = s.get(DataComponentTypes.ENCHANTMENTS);
        if (enc == null) return false;
        for (var entry : enc.getEnchantments()) {
            String id = entry.getKey().map(e -> e.value().toString()).orElse("");
            if (id.contains(enchId)) return true;
        }
        return false;
    }

    private int countInvHelmets(ClientPlayerEntity p, boolean hasBP, boolean hasUnb, boolean hasMend) {
        int count = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.getItem() != Items.DIAMOND_HELMET) continue;
            if (hasBP   && !hasEnchant(s, "blast_protection")) continue;
            if (hasUnb  && !hasEnchant(s, "unbreaking"))       continue;
            if (hasMend && !hasEnchant(s, "mending"))          continue;
            count++;
        }
        return count;
    }

    private int countInvBooks(ClientPlayerEntity p, String enchId) {
        int count = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.getItem() == Items.ENCHANTED_BOOK && isBook(s, enchId)) count++;
        }
        return count;
    }

    private int invSlotToChestScreen(GenericContainerScreenHandler sh, int invSlot) {
        return sh.getRows() * 9 + invSlot;
    }
}
