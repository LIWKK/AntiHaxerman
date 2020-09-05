package me.tecnio.antihaxerman.checks;

import io.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketSendEvent;
import io.github.retrooper.packetevents.packet.PacketType;
import io.github.retrooper.packetevents.packetwrappers.in.useentity.WrappedPacketInUseEntity;
import me.tecnio.antihaxerman.AntiHaxerman;
import me.tecnio.antihaxerman.Config;
import me.tecnio.antihaxerman.playerdata.DataManager;
import me.tecnio.antihaxerman.playerdata.PlayerData;
import me.tecnio.antihaxerman.utils.LogUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;

public abstract class Check implements Listener {

    public double vl, maxVL;
    public boolean enabled, punishable;
    public String checkName, checkType, punishCommand;

    protected int preVL;

    public Check(){
        checkName = getCheckName();
        checkType = getCheckType();
        enabled = AntiHaxerman.getInstance().getConfig().getBoolean(checkName.toLowerCase() + ".enabled");
        punishable = AntiHaxerman.getInstance().getConfig().getBoolean(checkName.toLowerCase() + ".punishable");
        punishCommand = AntiHaxerman.getInstance().getConfig().getString(checkName.toLowerCase() + ".punish-command");
        maxVL = AntiHaxerman.getInstance().getConfig().getInt(checkName.toLowerCase() + ".max-vl");
        Bukkit.getPluginManager().registerEvents(this, AntiHaxerman.getInstance());
    }

    private String getCheckName() { return this.getClass().getAnnotation(CheckInfo.class).name(); }

    private String getCheckType() { return this.getClass().getAnnotation(CheckInfo.class).type(); }

    public void onPacketReceive(final PacketReceiveEvent e, final PlayerData data){}
    public void onPacketSend(final PacketSendEvent e, final PlayerData data){}
    public void onMove(final PlayerData data){}
    public void onAttack(final WrappedPacketInUseEntity packet, final PlayerData data){}

    protected void flag(final PlayerData data, final String information){
        assert data != null;
        if (enabled){
            vl++;
            data.setTotalFlags(data.getTotalFlags() + 1);

            data.setLegitTick(data.getTicks());

            TextComponent alertMessage = new TextComponent(ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "AntiHaxerman" + ChatColor.GRAY + "] " + ChatColor.DARK_GREEN + data.getPlayer().getName() + ChatColor.WHITE + " failed " + getCheckName() + " (" + getCheckType() + ") VL: " + vl);
            alertMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + data.getPlayer().getName()));
            alertMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7(Click to teleport)\n Info: " + information).create()));

            if (Config.ENABLE_LOGGING)LogUtils.logToFile(data.getLogFile(), "[AntiHaxerman] " + data.getPlayer().getName() + " failed " + getCheckName() + " (" + getCheckType() + ") [Info: " + information + "]");
            if (Config.LOG_TO_CONSOLE)Bukkit.getLogger().info("[AntiHaxerman] " + data.getPlayer().getName() + " failed " + getCheckName() + " (" + getCheckType() + ") [Info: " + information + "]");

            for (PlayerData staff : DataManager.INSTANCE.getUsers()) {
                if (staff.isAlerts()){
                    if (staff.getPlayer().isOp() || staff.getPlayer().hasPermission("antihaxerman.alerts")){
                        staff.getPlayer().spigot().sendMessage(alertMessage);
                    }
                }
            }

            if (punishable && vl >= maxVL){
                Bukkit.getServer().getScheduler().runTask(AntiHaxerman.getInstance(), () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), punishCommand.replace("%player%", data.getPlayer().getName()).replace("%check%", checkName)));
            }
        }
    }

    protected void flag(PlayerData data, String information, SetBackType setBackType){
        if (elapsed(time(), data.getLastSetBack()) > 200){
            data.setLastSetBack(time());
            /*
            switch (setBackType){
                case BACK: data.getPlayer().teleport(data.getLastLocation());break;
                case PULLDOWN: data.getPlayer().teleport(data.getLastOnGroundLocation());break;
                case LASTLEGIT: data.getPlayer().teleport(data.getLastLegitLocation());break;
            }
             */
        }
        flag(data, information);
    }

    protected long time(){ return System.nanoTime() / 1000000; }
    protected long elapsed(long num1, long num2){ return num1 - num2; }
    protected double elapsed(double num1, double num2){ return num1 - num2; }
    protected void Debug(Object object) { Bukkit.broadcastMessage(String.valueOf(object)); }
    protected void debug(Object object) { Bukkit.broadcastMessage(String.valueOf(object)); }
    protected boolean isFlyingPacket(PacketReceiveEvent event) { return PacketType.Client.Util.isInstanceOfFlying(event.getPacketId()); }
    protected boolean isPositionPacket(PacketReceiveEvent event) { return event.getPacketId() == PacketType.Client.POSITION || event.getPacketId() == PacketType.Client.POSITION_LOOK; }
    protected boolean isRotationPacket(PacketReceiveEvent event) { return event.getPacketId() == PacketType.Client.LOOK || event.getPacketId() == PacketType.Client.POSITION_LOOK; }
}
