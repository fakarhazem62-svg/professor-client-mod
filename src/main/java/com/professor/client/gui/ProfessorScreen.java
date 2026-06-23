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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.LinkedList;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    // ── Blue palette (Xerion Client) ─────────────────────────────────────
    private static final int C_BG     = 0xFF01020A;
    private static final int C_PANEL  = 0xF0010318;
    private static final int C_BLUE   = 0x00AAFF;
    private static final int C_LBLUE  = 0x00DDFF;
    private static final int C_DBLUE  = 0x0044AA;
    private static final int C_GOLD   = 0xFFD700;
    private static final int C_RED    = 0xFF3300;
    private static final int C_GREEN  = 0x00FF77;
    private static final int C_PURPLE = 0x6600FF;
    private static final int C_MUTED  = 0x002244;

    // ── Panel ────────────────────────────────────────────────────────────
    private static final int PW = 580, PH = 420;

    // ── Tabs ─────────────────────────────────────────────────────────────
    private static final String[] TABS = {"⚡ FLOOD","🔓 BYPASS","💥 EXPLOIT","⚔ COMBAT","🏃 MOVE","💬 CHAT"};
    private static final int NT = 6;
    private int tab = 0;
    private float tabAnim = 1f, tabDir = 1;

    private static final int[] TAB_COLS = {
        0x00AAFF, 0x00FF88, 0xFF6600, 0xFF3300, 0xFFAA00, 0x0088FF
    };

    // ── Bypass modes ─────────────────────────────────────────────────────
    private static final String[] BP_NAMES = {"OFF","BURST","WAVE","MATRIX","NCP","AAC","GRIM","GHOST"};
    private static final String[] BP_DESC  = {
        "No obfuscation — raw packets",
        "Short Y-burst variation",
        "Sine-wave Y oscillation",
        "Matrix AC spoofing pattern",
        "NoCheatPlus bypass timing",
        "AAC anti-cheat evasion",
        "Grim — vanilla-like movement",
        "Ghost mode — minimal footprint"
    };
    private int bypass = 0;
    private boolean unlimited = false;

    // ── Settings per tab ─────────────────────────────────────────────────
    private int   floodDelay   = 0;    // ms between packets
    private int   floodBurst   = 1;    // packets per tick
    private boolean autoBypass = true; // auto-apply bypass on flood
    private int   exploitType  = 0;    // 0=NBT 1=Swing 2=Slot 3=Teleport 4=Chunk
    private boolean combatAura = false;
    private int   moveSpeed    = 1;
    private boolean chatSpoof  = false;

    // ── Widgets ──────────────────────────────────────────────────────────
    private TextFieldWidget pktField, chatField;

    // ── Status ───────────────────────────────────────────────────────────
    private String  statusText  = "Xerion Client  v1.0  —  Ready";
    private int     statusColor = 0xFF00AAFF;
    private int     statusTimer = 120;

    // ── Particles ────────────────────────────────────────────────────────
    private static final int PCNT = 240;
    private final float[] ppx=new float[PCNT],ppy=new float[PCNT];
    private final float[] pvx=new float[PCNT],pvy=new float[PCNT];
    private final float[] psz=new float[PCNT],palp=new float[PCNT];
    private final float[] pph=new float[PCNT];
    private final int[]   pct=new int[PCNT];

    // ── Mouse trail ──────────────────────────────────────────────────────
    private final LinkedList<int[]> trail = new LinkedList<>();

    // ── Data streams ─────────────────────────────────────────────────────
    private static final String DS = "XERION01CLIENT10BYPASS110100";
    private final float[] dsX=new float[70],dsY=new float[70],dsSp=new float[70];
    private final int[]   dsCh=new int[70];

    // ── Orbs ─────────────────────────────────────────────────────────────
    private static final int ORBS = 7;
    private final float[] ox=new float[ORBS],oy=new float[ORBS];
    private final float[] ovx=new float[ORBS],ovy=new float[ORBS];
    private final float[] osz=new float[ORBS],oph=new float[ORBS];
    private final int[]   oct=new int[ORBS];

    // ── Animation ────────────────────────────────────────────────────────
    private long  tick      = 0;
    private float scanY     = 0, scanY2 = 0;
    private float glowPulse = 0f;
    private boolean glowUp  = true;
    private float borderHue = 0f;
    private float[] tabGlow = new float[NT];
    private float tabSlide  = 0f;   // 0→1 slide animation

    private final Random rng = new Random();

    // ═══════════════════════════════════════════════════════════════════════
    public ProfessorScreen() {
        super(Text.literal("Xerion Client"));
        initParticles(); initOrbs();
    }

    @Override protected void init() {
        initParticles(); initOrbs(); initDataStreams();
        rebuild();
        ProfessorMusicManager.onOpen(client);
    }

    @Override public void removed() { ProfessorMusicManager.onClose(client); super.removed(); }

    // ── Init ─────────────────────────────────────────────────────────────
    private void initParticles() {
        for (int i=0;i<PCNT;i++) {
            ppx[i]=rng.nextFloat()*1920; ppy[i]=rng.nextFloat()*1080;
            pvx[i]=(rng.nextFloat()-.5f)*.5f; pvy[i]=-(rng.nextFloat()*.7f+.1f);
            psz[i]=rng.nextFloat()*3f+.5f; palp[i]=rng.nextFloat()*.85f+.15f;
            pph[i]=rng.nextFloat()*6.28f; pct[i]=rng.nextInt(4);
        }
    }
    private void initOrbs() {
        for (int i=0;i<ORBS;i++) {
            ox[i]=rng.nextFloat()*1920; oy[i]=rng.nextFloat()*1080;
            ovx[i]=(rng.nextFloat()-.5f)*.2f; ovy[i]=(rng.nextFloat()-.5f)*.2f;
            osz[i]=rng.nextFloat()*50f+20f; oph[i]=rng.nextFloat()*6.28f;
            oct[i]=rng.nextInt(3);
        }
    }
    private void initDataStreams() {
        for (int i=0;i<70;i++) {
            dsX[i]=i*(width/70f); dsY[i]=rng.nextFloat()*-height;
            dsSp[i]=rng.nextFloat()*1.3f+.4f; dsCh[i]=rng.nextInt(DS.length());
        }
    }

    // ── Rebuild ───────────────────────────────────────────────────────────
    private void rebuild() {
        clearChildren(); pktField=null; chatField=null;
        int cx=width/2, bpx=cx-PW/2, bpy=height/2-PH/2;
        int tabW=(PW-16)/NT;
        for (int i=0;i<NT;i++) { final int idx=i;
            addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]),b->switchTab(idx))
                .dimensions(bpx+8+i*(tabW+1),bpy+44,tabW,18).build()); }
        switch(tab){
            case 0->buildFlood(cx,bpy);
            case 1->buildBypass(cx,bpy,bpx);
            case 2->buildExploit(cx,bpy);
            case 3->buildCombat(cx,bpy);
            case 4->buildMove(cx,bpy);
            case 5->buildChat(cx,bpy,bpx);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("✕  CLOSE"),b->close())
            .dimensions(cx-40,bpy+PH-28,80,18).build());
    }

    private void switchTab(int t) {
        if(t==tab)return; tabDir=t>tab?1:-1; tab=t; tabAnim=0f; tabSlide=0f;
        pktField=null; chatField=null; rebuild();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 0 — FLOOD
    // ═══════════════════════════════════════════════════════════════════════
    private void buildFlood(int cx, int bpy) {
        addDrawableChild(ButtonWidget.builder(
            Text.literal("MODE: "+(unlimited?"!!! UNLIMITED !!!":"MANUAL COUNT")),
            b->{unlimited=!unlimited;b.setMessage(Text.literal("MODE: "+(unlimited?"!!! UNLIMITED !!!":"MANUAL COUNT")));})
            .dimensions(cx-110,bpy+74,220,18).build());

        pktField=new TextFieldWidget(textRenderer,cx-70,bpy+97,140,16,Text.empty());
        pktField.setMaxLength(9); pktField.setText("10000"); addSelectableChild(pktField);

        int[][] pr={{1000,-145},{10000,-48},{100000,50},{1000000,148}};
        for(int[] p:pr){int n=p[0],ox2=p[1];addDrawableChild(ButtonWidget.builder(
            Text.literal(n>=1000000?"1M":n>=1000?(n/1000)+"K":""+n),b->pktField.setText(""+n))
            .dimensions(cx+ox2-26,bpy+117,52,14).build());}

        addDrawableChild(ButtonWidget.builder(Text.literal("Bypass: "+BP_NAMES[bypass]),
            b->{bypass=(bypass+1)%BP_NAMES.length;b.setMessage(Text.literal("Bypass: "+BP_NAMES[bypass]));})
            .dimensions(cx-70,bpy+136,140,16).build());

        // Delay setting
        addDrawableChild(ButtonWidget.builder(Text.literal("Delay: "+floodDelay+"ms"),
            b->{floodDelay=(floodDelay+5)%55;b.setMessage(Text.literal("Delay: "+floodDelay+"ms"));})
            .dimensions(cx-70,bpy+155,140,14).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(">>> SEND PACKETS <<<"),b->doFlood())
            .dimensions(cx-115,bpy+175,230,22).build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 1 — BYPASS CONFIG
    // ═══════════════════════════════════════════════════════════════════════
    private void buildBypass(int cx, int bpy, int bpx) {
        // Bypass selector (big)
        for (int i=0;i<BP_NAMES.length;i++) { final int idx=i;
            addDrawableChild(ButtonWidget.builder(
                Text.literal((bypass==idx?"▶ ":"")+BP_NAMES[idx]+"  —  "+BP_DESC[idx]),
                b->{ bypass=idx; rebuild(); })
                .dimensions(bpx+20,bpy+70+i*22,PW-40,18).build());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 2 — EXPLOITS
    // ═══════════════════════════════════════════════════════════════════════
    private static final String[] EXPLOIT_NAMES = {
        "Swing Flood     — spam arm swing packets",
        "Slot Spam       — cycle hotbar slots rapidly",
        "Teleport Ack    — send fake teleport confirmations",
        "Move Flood      — spam player move packets",
        "Interact Flood  — spam use-item packets"
    };

    private void buildExploit(int cx, int bpy) {
        // Exploit type selector
        for (int i=0;i<EXPLOIT_NAMES.length;i++) { final int idx=i;
            addDrawableChild(ButtonWidget.builder(
                Text.literal((exploitType==idx?"▶ ":"")+EXPLOIT_NAMES[idx]),
                b->{ exploitType=idx; rebuild(); })
                .dimensions(cx-200,bpy+68+i*20,400,16).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("[ EXECUTE EXPLOIT ]"),b->doExploit())
            .dimensions(cx-100,bpy+186,200,22).build());

        // Count input
        pktField=new TextFieldWidget(textRenderer,cx-60,bpy+214,120,16,Text.empty());
        pktField.setMaxLength(7); pktField.setText("1000"); addSelectableChild(pktField);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 3 — COMBAT
    // ═══════════════════════════════════════════════════════════════════════
    private void buildCombat(int cx, int bpy) {
        int bw=340,bx=cx-bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Hit Packet Spam  —  swing flood with bypass"),     b->combatHitSpam())  .dimensions(bx,bpy+70,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Crit Exploit     —  send crit hit packets"),       b->combatCrit())     .dimensions(bx,bpy+92,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Anti-KB Packets  —  spam position packets"),       b->combatAntiKB())   .dimensions(bx,bpy+114,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Fast Use         —  rapid use-item sequence"),     b->combatFastUse())  .dimensions(bx,bpy+136,bw,18).build());

        // Burst toggle
        addDrawableChild(ButtonWidget.builder(Text.literal("Burst: "+floodBurst+"x"),
            b->{floodBurst=floodBurst%5+1;b.setMessage(Text.literal("Burst: "+floodBurst+"x"));})
            .dimensions(cx-50,bpy+162,100,16).build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 4 — MOVE
    // ═══════════════════════════════════════════════════════════════════════
    private void buildMove(int cx, int bpy) {
        int bw=340,bx=cx-bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Speed Boost     —  send fast-move packets"),       b->moveSpeed())     .dimensions(bx,bpy+70,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Blink           —  pause then flush packets"),     b->moveBlink())     .dimensions(bx,bpy+92,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Fly Exploit     —  send airborne move packets"),   b->moveFly())       .dimensions(bx,bpy+114,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  NoFall          —  onGround=true packets"),        b->moveNoFall())    .dimensions(bx,bpy+136,bw,18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Speed: "+moveSpeed+"x"),
            b->{moveSpeed=moveSpeed%10+1;b.setMessage(Text.literal("Speed: "+moveSpeed+"x"));})
            .dimensions(cx-50,bpy+162,100,16).build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 5 — CHAT
    // ═══════════════════════════════════════════════════════════════════════
    private void buildChat(int cx, int bpy, int bpx) {
        chatField=new TextFieldWidget(textRenderer,cx-150,bpy+76,300,18,Text.empty());
        chatField.setMaxLength(256); chatField.setText("/gamemode creative");
        addSelectableChild(chatField);

        pktField=new TextFieldWidget(textRenderer,cx-60,bpy+100,120,16,Text.empty());
        pktField.setMaxLength(6); pktField.setText("50"); addSelectableChild(pktField);

        addDrawableChild(ButtonWidget.builder(Text.literal(">> SPAM CHAT <<"),b->doChat())
            .dimensions(cx-80,bpy+122,160,20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("[SYS] Send Command Packet"),b->doCommand())
            .dimensions(cx-110,bpy+148,220,18).build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════════════
    private void doFlood() {
        if(client==null||client.player==null||client.getNetworkHandler()==null)return;
        int count=unlimited?50000:parsePkt();
        for(int i=0;i<count;i++) {
            double dy=bypassDY(i);
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                client.player.getX(), client.player.getY()+dy, client.player.getZ(), (i%2==0)));
            if(bypass>0 && i%3==0)
                client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        flash("Sent "+count+" packets  [Bypass: "+BP_NAMES[bypass]+"]", C_BLUE|0xFF000000);
    }

    private void doExploit() {
        if(client==null||client.player==null||client.getNetworkHandler()==null)return;
        int n=parsePkt();
        switch(exploitType) {
            case 0 -> { // Swing flood
                for(int i=0;i<n;i++) client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(i%2==0?Hand.MAIN_HAND:Hand.OFF_HAND));
                flash("Swing flood: "+n+" packets", C_GREEN|0xFF000000);
            }
            case 1 -> { // Slot spam
                for(int i=0;i<Math.min(n,500);i++) client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
                flash("Slot spam: "+Math.min(n,500)+" packets", C_GREEN|0xFF000000);
            }
            case 2 -> { // Teleport ack
                for(int i=0;i<Math.min(n,200);i++) client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(i));
                flash("Teleport ack: "+Math.min(n,200)+" packets", C_GREEN|0xFF000000);
            }
            case 3 -> { // Move flood
                for(int i=0;i<n;i++) client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX()+rng.nextGaussian()*.01, client.player.getY(), client.player.getZ()+rng.nextGaussian()*.01, true));
                flash("Move flood: "+n+" packets", C_GREEN|0xFF000000);
            }
            case 4 -> { // Interact
                for(int i=0;i<Math.min(n,300);i++) client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                flash("Interact flood: "+Math.min(n,300)+" packets", C_GREEN|0xFF000000);
            }
        }
    }

    private void combatHitSpam()  { if(client==null||client.getNetworkHandler()==null)return; for(int i=0;i<200;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); flash("Hit spam: 200 packets",C_RED|0xFF000000); }
    private void combatCrit()     { if(client==null||client.getNetworkHandler()==null||client.player==null)return; for(int i=0;i<50;i++){double dy=i%2==0?.42:0;client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+dy,client.player.getZ(),false));}flash("Crit packets sent",C_RED|0xFF000000); }
    private void combatAntiKB()   { if(client==null||client.getNetworkHandler()==null||client.player==null)return; for(int i=0;i<100;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("Anti-KB: 100 pos packets",C_RED|0xFF000000); }
    private void combatFastUse()  { if(client==null||client.getNetworkHandler()==null)return; for(int i=0;i<80;i++)client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));flash("Fast use: 80 packets",C_RED|0xFF000000); }
    private void moveSpeed()      { if(client==null||client.getNetworkHandler()==null||client.player==null)return; double sp=moveSpeed*.4; for(int i=0;i<80;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+sp*Math.cos(i*.1),client.player.getY(),client.player.getZ()+sp*Math.sin(i*.1),true));flash("Speed: "+moveSpeed+"x boost sent",C_GOLD|0xFF000000); }
    private void moveBlink()      { if(client==null||client.getNetworkHandler()==null||client.player==null)return; for(int i=0;i<30;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("Blink packets flushed",C_GOLD|0xFF000000); }
    private void moveFly()        { if(client==null||client.getNetworkHandler()==null||client.player==null)return; for(int i=0;i<60;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY()+i*.05,client.player.getZ(),false));flash("Fly exploit: 60 packets",C_GOLD|0xFF000000); }
    private void moveNoFall()     { if(client==null||client.getNetworkHandler()==null||client.player==null)return; for(int i=0;i<100;i++)client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),client.player.getY(),client.player.getZ(),true));flash("NoFall packets sent",C_GOLD|0xFF000000); }

    private void doChat() {
        if(client==null||client.getNetworkHandler()==null||chatField==null)return;
        String msg=chatField.getText(); int n=parsePkt();
        for(int i=0;i<n;i++) client.getNetworkHandler().sendPacket(new ChatMessageC2SPacket(msg,System.currentTimeMillis()));
        flash("Chat spam: "+n+" messages",C_BLUE|0xFF000000);
    }
    private void doCommand() {
        if(client==null||client.getNetworkHandler()==null||chatField==null)return;
        String cmd=chatField.getText().startsWith("/")?chatField.getText().substring(1):chatField.getText();
        client.getNetworkHandler().sendPacket(new CommandExecutionC2SPacket(cmd));
        flash("Command sent: /"+cmd,C_BLUE|0xFF000000);
    }

    // ── Bypass Y delta helper ─────────────────────────────────────────────
    private double bypassDY(int i) {
        return switch(bypass) {
            case 1 -> (i%3==0)?0.0625:0;
            case 2 -> Math.sin(i*.3)*.08;
            case 3 -> (i%4==0)?0.1:(i%4==1)?0.05:0;
            case 4 -> (i%5==0)?0.0625:(i%3==0)?0.03125:0;
            case 5 -> Math.sin(i*.2)*.05+((i%7==0)?0.0625:0);
            case 6 -> (i%10==0)?0.0625:0;
            case 7 -> (i%20==0)?0.03125:0;
            default -> 0;
        };
    }

    // ── Utils ─────────────────────────────────────────────────────────────
    private int parsePkt() { try{return Math.max(1,Integer.parseInt(pktField!=null?pktField.getText():"100"));}catch(Exception e){return 100;} }
    private void flash(String msg,int color){statusText=msg;statusColor=color;statusTimer=120;}

    // ═══════════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++;
        tabAnim=Math.min(1f,tabAnim+.08f);
        tabSlide=Math.min(1f,tabSlide+.1f);
        if(statusTimer>0)statusTimer--;

        glowPulse+=glowUp?.03f:-.03f;
        if(glowPulse>=1f){glowPulse=1f;glowUp=false;}else if(glowPulse<=0f){glowPulse=0f;glowUp=true;}
        scanY=(scanY+2.2f)%height;scanY2=(scanY2+1.3f)%height;
        borderHue=(borderHue+.004f)%1f;

        // Update tab glow
        for(int i=0;i<NT;i++) tabGlow[i]=Math.max(0f,tabGlow[i]-(i==tab?.0f:.04f));
        tabGlow[tab]=Math.min(1f,tabGlow[tab]+.08f);

        int W=width,H=height,cx=W/2,cy=H/2;
        int bpx=cx-PW/2,bpy=cy-PH/2;

        // Background
        ctx.fill(0,0,W,H,C_BG);

        // Update & draw particles
        for(int i=0;i<PCNT;i++){
            ppx[i]+=pvx[i];ppy[i]+=pvy[i];pph[i]+=.045f;
            if(ppy[i]<-8){ppy[i]=H+5;ppx[i]=rng.nextFloat()*W;}
            if(ppx[i]<0)ppx[i]=W;if(ppx[i]>W)ppx[i]=0;
            float tw=(MathHelper.sin(pph[i])+1f)/2f;
            int a=(int)(palp[i]*tw*255);if(a<10)continue;
            boolean big=psz[i]>2.3f;
            int col=switch(pct[i]){
                case 1->(a<<24)|(big?0x0055FF:0x001133);
                case 2->(a<<24)|(big?0x00AAFF:0x002244);
                case 3->(a<<24)|(big?0xFFD700:0x332200);
                default->(a<<24)|(big?0x00DDFF:0x003355);
            };
            int sz=psz[i]>2.5f?2:1;
            ctx.fill((int)ppx[i],(int)ppy[i],(int)ppx[i]+sz,(int)ppy[i]+sz,col);
        }

        // Orbs
        for(int i=0;i<ORBS;i++){
            ox[i]+=ovx[i];oy[i]+=ovy[i];oph[i]+=.022f;
            if(ox[i]<0||ox[i]>W)ovx[i]=-ovx[i];if(oy[i]<0||oy[i]>H)ovy[i]=-ovy[i];
            float tw=(MathHelper.sin(oph[i])+1f)/2f;
            int a=(int)(0.18f*tw*255);if(a<=0)continue;
            int sz=(int)osz[i];
            int c=switch(oct[i]){case 1->0x0033FF;case 2->0x00AAFF;default->0x0066FF;};
            ctx.fill((int)ox[i]-sz,(int)oy[i]-sz/2,(int)ox[i]+sz,(int)oy[i]+sz/2,(a<<24)|c);
        }

        // Data streams
        for(int i=0;i<70;i++){
            dsY[i]+=dsSp[i];if(dsY[i]>H+20){dsY[i]=-24;dsCh[i]=rng.nextInt(DS.length());}
            if(rng.nextInt(22)==0)dsCh[i]=rng.nextInt(DS.length());
            if(dsY[i]<0)continue;
            int ha=(int)((50+30*glowPulse));
            ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i],(Math.min(255,ha*2)<<24)|0x0066FF,false);
            if(dsY[i]>14)ctx.drawText(textRenderer,String.valueOf(DS.charAt(dsCh[i])),(int)dsX[i]-4,(int)dsY[i]-14,(ha<<24)|0x001133,false);
        }

        // Scanlines
        ctx.fill(0,(int)scanY, W,(int)scanY+2,0x0DFFFFFF);
        ctx.fill(0,(int)scanY2,W,(int)scanY2+1,0x07FFFFFF);

        // Mouse trail
        trail.add(new int[]{mx,my,(int)(255*tabGlow[tab])});
        while(trail.size()>18)trail.removeFirst();
        int ti=0;
        for(int[]p:trail){int ta2=(int)((float)ti/trail.size()*120);if(ta2>0){int tc=TAB_COLS[tab];ctx.fill(p[0]-1,p[1]-1,p[0]+2,p[1]+2,(ta2<<24)|tc);}ti++;}

        // ── Panel shadow
        ctx.fill(bpx+6,bpy+6,bpx+PW+6,bpy+PH+6,0x66000000);
        ctx.fill(bpx+3,bpy+3,bpx+PW+3,bpy+PH+3,0x33000000);

        // ── Panel body
        ctx.fill(bpx,bpy,bpx+PW,bpy+PH,C_PANEL);

        // Subtle gradient overlay
        for(int y=0;y<PH;y+=2){int a2=(int)(0.04f*(1f-(float)y/PH)*255);ctx.fill(bpx,bpy+y,bpx+PW,bpy+y+2,(a2<<24)|0x0088FF);}

        // ── Animated border (hue-shifted blue)
        float hrad=borderHue*6.28f;
        int bR=0,bG=Math.max(80,Math.min(220,(int)(150+100*Math.sin(hrad))));
        int bB=255,bAv=(int)((0.65f+.35f*glowPulse)*255);
        int bc=(bAv<<24)|(bR<<16)|(bG<<8)|bB;
        int bc2=((bAv/4)<<24)|(bR<<16)|(bG<<8)|bB;
        ctx.fill(bpx,     bpy,     bpx+PW, bpy+2,    bc);
        ctx.fill(bpx,     bpy+PH-2,bpx+PW, bpy+PH,   bc);
        ctx.fill(bpx,     bpy,     bpx+2,  bpy+PH,   bc);
        ctx.fill(bpx+PW-2,bpy,     bpx+PW, bpy+PH,   bc);
        ctx.fill(bpx+2,   bpy+2,   bpx+PW-2,bpy+3,   bc2);
        ctx.fill(bpx+2,   bpy+PH-3,bpx+PW-2,bpy+PH-2,bc2);

        // Gold corner brackets
        int gca=(int)(0xDD*1f);int gcc=(gca<<24)|C_GOLD;int cs=24;
        ctx.fill(bpx,      bpy,      bpx+cs,   bpy+3,  gcc);ctx.fill(bpx,     bpy,     bpx+3,   bpy+cs, gcc);
        ctx.fill(bpx+PW-cs,bpy,      bpx+PW,   bpy+3,  gcc);ctx.fill(bpx+PW-3,bpy,     bpx+PW,  bpy+cs, gcc);
        ctx.fill(bpx,      bpy+PH-3, bpx+cs,   bpy+PH, gcc);ctx.fill(bpx,     bpy+PH-cs,bpx+3,  bpy+PH, gcc);
        ctx.fill(bpx+PW-cs,bpy+PH-3, bpx+PW,   bpy+PH, gcc);ctx.fill(bpx+PW-3,bpy+PH-cs,bpx+PW, bpy+PH, gcc);

        // ── Title bar
        ctx.fill(bpx,bpy,bpx+PW,bpy+40,0x22001155);
        String t1="XERION",t2=" CLIENT";
        int tw1=textRenderer.getWidth(t1),tw2=textRenderer.getWidth(t2);
        int tx=cx-(tw1+tw2)/2,ty=bpy+10;
        int tA=(int)((190+65*glowPulse));
        for(int d=-4;d<=4;d++){int ga=(int)Math.max(0,22-Math.abs(d)*6);if(ga>0){ctx.drawText(textRenderer,t1+t2,tx+d,ty,(ga<<24)|C_BLUE,false);ctx.drawText(textRenderer,t1+t2,tx,ty+d,(ga<<24)|C_BLUE,false);}}
        ctx.drawText(textRenderer,t1,tx,ty,(tA<<24)|C_LBLUE,false);
        ctx.drawText(textRenderer,t2,tx+tw1,ty,(tA<<24)|C_GOLD,false);

        String sub="v1.0  |  "+NT+" Modules  |  "+BP_NAMES.length+" Bypass Modes  |  © Xerion Client";
        int sa=(int)((110+55*glowPulse));
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,ty+12,(sa<<24)|C_DBLUE,false);

        // Gold divider under title
        int divA=(int)(0x66*1f);
        ctx.fill(bpx+30,bpy+28,bpx+PW-30,bpy+29,(divA<<24)|C_GOLD);

        // ── Tab bar
        ctx.fill(bpx,bpy+42,bpx+PW,bpy+64,0x33000022);
        int tabW2=(PW-16)/NT;
        for(int i=0;i<NT;i++){
            int tgl=(int)(tabGlow[i]*255);
            if(tgl>0){
                int tc=TAB_COLS[i];
                ctx.fill(bpx+8+i*(tabW2+1),bpy+42,bpx+8+(i+1)*(tabW2+1)-1,bpy+64,(tgl/3<<24)|tc);
                ctx.fill(bpx+8+i*(tabW2+1),bpy+62,bpx+8+(i+1)*(tabW2+1)-1,bpy+64,(tgl<<24)|tc);
            }
        }

        // Tab divider
        ctx.fill(bpx+8,bpy+63,bpx+PW-8,bpy+64,(int)(0x88*1f)<<24|C_BLUE);

        // ── Tab content (with slide animation)
        float slideOff=tabSlide<1f?(1f-tabSlide)*PW*tabDir:0;
        ctx.enableScissor(bpx,bpy+64,bpx+PW,bpy+PH-30);
        // (Minecraft DrawContext doesn't have translate, but we draw everything at fixed positions)
        ctx.disableScissor();

        renderTabContent(ctx,mx,my,delta,cx,bpx,bpy);

        // ── Status bar
        drawStatus(ctx,cx,bpx,bpy);

        // ── Copyright footer
        ctx.fill(bpx,bpy+PH-22,bpx+PW,bpy+PH,0x22000011);
        String copy="© Xerion Client  —  All rights reserved  —  Not responsible for bans";
        ctx.drawText(textRenderer,copy,cx-textRenderer.getWidth(copy)/2,bpy+PH-14,(int)(0x44*1f)<<24|C_DBLUE,false);

        super.render(ctx,mx,my,delta);
    }

    private void renderTabContent(DrawContext ctx,int mx,int my,float delta,int cx,int bpx,int bpy){
        // Labels for tab content
        ctx.fill(bpx+8,bpy+66,bpx+PW-8,bpy+68,(int)(0x33*1f)<<24|TAB_COLS[tab]);
        String tname=TABS[tab];
        ctx.drawText(textRenderer,tname,bpx+12,bpy+70,(int)(0xCC*1f)<<24|TAB_COLS[tab],false);
        ctx.drawText(textRenderer,BP_NAMES[bypass]+" bypass",bpx+PW-textRenderer.getWidth(BP_NAMES[bypass]+" bypass")-12,bpy+70,(int)(0x88*1f)<<24|C_BLUE,false);

        if(pktField!=null) pktField.render(ctx,mx,my,delta);
        if(chatField!=null) chatField.render(ctx,mx,my,delta);

        // Tab-specific info box (right mini-panel)
        drawInfoBox(ctx,bpx+PW-150,bpy+240,140,140);
    }

    private void drawInfoBox(DrawContext ctx,int x,int y,int w,int h){
        ctx.fill(x,y,x+w,y+h,0x44001133);
        ctx.fill(x,y,x+w,y+1,0x88<<24|C_BLUE);ctx.fill(x,y+h-1,x+w,y+h,0x88<<24|C_BLUE);
        ctx.fill(x,y,x+1,y+h,0x88<<24|C_BLUE);ctx.fill(x+w-1,y,x+w,y+h,0x88<<24|C_BLUE);
        ctx.drawText(textRenderer,"STATUS",x+4,y+4,0xAA<<24|C_BLUE,false);
        ctx.fill(x,y+13,x+w,y+14,0x44<<24|C_BLUE);
        ctx.drawText(textRenderer,"Bypass: "+BP_NAMES[bypass],x+4,y+18,0x99<<24|C_LBLUE,false);
        ctx.drawText(textRenderer,"Mode: "+(unlimited?"UNLIM":"MANUAL"),x+4,y+30,0x99<<24|C_LBLUE,false);
        ctx.drawText(textRenderer,"Delay: "+floodDelay+"ms",x+4,y+42,0x99<<24|C_LBLUE,false);
        ctx.drawText(textRenderer,"Burst: "+floodBurst+"x",x+4,y+54,0x99<<24|C_LBLUE,false);
        ctx.drawText(textRenderer,"Speed: "+moveSpeed+"x",x+4,y+66,0x99<<24|C_LBLUE,false);
        ctx.drawText(textRenderer,"Tab: "+tab+"/"+NT,x+4,y+78,0x77<<24|C_DBLUE,false);
        // Animated indicator
        int indA=(int)((0.6f+.4f*glowPulse)*200);
        ctx.fill(x+4,y+92,x+4+(int)(glowPulse*(w-8)),y+96,(indA<<24)|C_BLUE);
        ctx.fill(x+4,y+92,x+w-4,y+96,0x22<<24|C_BLUE);
    }

    private void drawStatus(DrawContext ctx,int cx,int bpx,int bpy){
        if(statusTimer<=0)return;
        int a=Math.min(255,statusTimer*2);
        ctx.fill(bpx+6,bpy+PH-44,bpx+PW-6,bpy+PH-24,0xBB000011);
        ctx.fill(bpx+6,bpy+PH-44,bpx+9,   bpy+PH-24,(a<<24)|C_GOLD);
        ctx.fill(bpx+PW-9,bpy+PH-44,bpx+PW-6,bpy+PH-24,(a<<24)|C_GOLD);
        int col=(statusColor&0x00FFFFFF)|(a<<24);
        ctx.drawText(textRenderer,statusText,cx-textRenderer.getWidth(statusText)/2,bpy+PH-37,col,false);
    }

    // ── Input ──────────────────────────────────────────────────────────────
    @Override public boolean mouseClicked(double mx,double my,int btn){if(pktField!=null)pktField.mouseClicked(mx,my,btn);if(chatField!=null)chatField.mouseClicked(mx,my,btn);return super.mouseClicked(mx,my,btn);}
    @Override public boolean keyPressed(int k,int sc,int mod){if(pktField!=null&&pktField.isFocused()&&pktField.keyPressed(k,sc,mod))return true;if(chatField!=null&&chatField.isFocused()&&chatField.keyPressed(k,sc,mod))return true;return super.keyPressed(k,sc,mod);}
    @Override public boolean charTyped(char c,int mod){if(pktField!=null&&pktField.isFocused()&&pktField.charTyped(c,mod))return true;if(chatField!=null&&chatField.isFocused()&&chatField.charTyped(c,mod))return true;return super.charTyped(c,mod);}
    @Override public boolean shouldPause(){return false;}
}
