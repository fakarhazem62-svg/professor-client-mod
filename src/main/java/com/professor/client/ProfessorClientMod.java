package com.professor.client;

import com.professor.client.gui.ProfessorMusicManager;
import com.professor.client.gui.ProfessorScreen;
import com.professor.client.proxy.ProxyManager;
import com.professor.client.task.BackgroundTaskManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ProfessorClientMod implements ClientModInitializer {

    public static KeyBinding openGuiKey;
    public static KeyBinding proxyKey;

    public static final String MOD_ID      = "professorclient";
    public static final String CLIENT_NAME = "Xerion Client";
    public static final String VERSION     = "v1";

    // ── Packet queue ──────────────────────────────────────────────────────
    public static final ConcurrentLinkedQueue<Packet<?>> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    public static volatile int PACKETS_PER_TICK = 100;

    // Swing rate limiter (for manual calls)
    private static long lastSwingMs = 0;
    public static volatile int SWING_COOLDOWN_MS = 105;

    @Override
    public void onInitializeClient() {

        // ── Key: M → open Client GUI ──────────────────────────────────────
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.professorclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.professorclient.general"
        ));

        // ── Key: V → toggle proxy (background, NO screen change) ─────────
        proxyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.professorclient.proxy_toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.professorclient.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // ── M key → open ProfessorScreen directly ─────────────────────
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    playClickSound(client);
                    client.setScreen(new ProfessorScreen());
                }
            }

            // ── V key → toggle proxy in background ────────────────────────
            while (proxyKey.wasPressed()) {
                if (client.player != null) {
                    boolean nowEnabled = !ProxyManager.isEnabled();
                    ProxyManager.setEnabled(nowEnabled);
                    String msg;
                    if (nowEnabled && ProxyManager.count() > 0) {
                        msg = "§b❄ §fProxy §aON §7— §b" + ProxyManager.aliveCount()
                            + "§7/§b" + ProxyManager.count() + " §7active";
                    } else if (nowEnabled) {
                        msg = "§b❄ §fProxy §aON §7— §cadd proxies via §bM §cmenu";
                    } else {
                        msg = "§b❄ §fProxy §cOFF";
                    }
                    client.player.sendMessage(Text.literal(msg), true);
                }
            }

            // ── Module ticking ─────────────────────────────────────────────
            if (client.player != null && client.getNetworkHandler() != null) {

                // Auto-Swing: send swing packets continuously
                if (XerionModules.autoSwing && XerionModules.canAutoSwing()) {
                    try {
                        client.getNetworkHandler().sendPacket(
                            new HandSwingC2SPacket(Hand.MAIN_HAND));
                        XerionModules.totalPktsSent++;
                    } catch (Exception ignored) {}
                }

                // Anti-AFK: tiny nudge every ~28s to prevent kick
                if (XerionModules.antiAfk && XerionModules.shouldAfkMove()) {
                    try {
                        double px = client.player.getX();
                        double py = client.player.getY();
                        double pz = client.player.getZ();
                        client.getNetworkHandler().sendPacket(
                            new PlayerMoveC2SPacket.PositionAndOnGround(px + 0.0001, py, pz, true));
                        client.getNetworkHandler().sendPacket(
                            new PlayerMoveC2SPacket.PositionAndOnGround(px, py, pz, true));
                        XerionModules.totalPktsSent += 2;
                        client.player.sendMessage(
                            Text.literal("§b❄ §7Anti-AFK nudge"), true);
                    } catch (Exception ignored) {}
                }

                // NoFall Always: continuously send onGround=true
                if (XerionModules.noFallAlways) {
                    try {
                        client.getNetworkHandler().sendPacket(
                            new PlayerMoveC2SPacket.PositionAndOnGround(
                                client.player.getX(), client.player.getY(),
                                client.player.getZ(), true));
                        XerionModules.totalPktsSent++;
                    } catch (Exception ignored) {}
                }

                // Anti-KB Always: spam current position to resist knockback
                if (XerionModules.antiKbAlways) {
                    try {
                        for (int i = 0; i < 3; i++)
                            client.getNetworkHandler().sendPacket(
                                new PlayerMoveC2SPacket.PositionAndOnGround(
                                    client.player.getX(), client.player.getY(),
                                    client.player.getZ(), true));
                        XerionModules.totalPktsSent += 3;
                    } catch (Exception ignored) {}
                }
            }

            // ── Packet queue drain ─────────────────────────────────────────
            if (!PACKET_QUEUE.isEmpty() && client.getNetworkHandler() != null) {
                int sent = 0;
                while (!PACKET_QUEUE.isEmpty() && sent < PACKETS_PER_TICK) {
                    Packet<?> pkt = PACKET_QUEUE.poll();
                    if (pkt != null) {
                        try { client.getNetworkHandler().sendPacket(pkt); }
                        catch (Exception ignored) {}
                        sent++;
                        XerionModules.totalPktsSent++;
                    }
                }
                BackgroundTaskManager.setPacketProgress(
                    BackgroundTaskManager.getPacketsSent() + sent,
                    BackgroundTaskManager.getPacketsTotal()
                );
            }
        });

        // Title screen music
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                ProfessorMusicManager.playOnTitleScreen(client);
            }
        });
    }

    // ── Sounds ────────────────────────────────────────────────────────────
    public static void playClickSound(MinecraftClient client) {
        try {
            var id = Identifier.of("minecraft", "block.note_block.pling");
            var reg = net.minecraft.registry.Registries.SOUND_EVENT;
            if (reg.containsId(id))
                client.getSoundManager().play(PositionedSoundInstance.master(reg.get(id), 1.8f, 0.45f));
        } catch (Exception ignored) {}
    }

    // ── Packet queue ──────────────────────────────────────────────────────
    public static void queuePacket(Packet<?> pkt) {
        PACKET_QUEUE.offer(pkt);
        XerionModules.totalPktsSent++;
    }
    public static void clearQueue() { PACKET_QUEUE.clear(); }

    // ── Manual swing gate ─────────────────────────────────────────────────
    public static boolean canSwing() {
        long now = System.currentTimeMillis();
        if (now - lastSwingMs >= SWING_COOLDOWN_MS) { lastSwingMs = now; return true; }
        return false;
    }

    public static boolean proxyEnabled() {
        return ProxyManager.isEnabled() && ProxyManager.count() > 0;
    }
}
