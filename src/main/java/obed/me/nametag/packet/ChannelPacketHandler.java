package obed.me.nametag.packet;

import com.google.common.collect.Lists;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;

import obed.me.nametag.Main;
import obed.me.nametag.utils.PacketInterceptor;
import obed.me.nametag.utils.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("All")
public class ChannelPacketHandler extends PacketInterceptor implements IPacketHandler {
    private static final Class<?> ENUM_GAMEMODE;

    static {
        if (Reflect.isVersionHigherThan(1, 9, 4)) {
            ENUM_GAMEMODE = (Class)Reflect.getNMSClass("EnumGamemode").getOrThrow();
        } else {
            ENUM_GAMEMODE = (Class)Reflect.getNMSClass("WorldSettings$EnumGamemode").getOrThrow();
        }
    }

    private static final Class<?> GAME_PROFILE_CLASS = (Class)Reflect.getClass("com.mojang.authlib.GameProfile").getOrThrow();

    private static final Class<?> PLAYER_INFO_DATA_CLASS = (Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo$PlayerInfoData").getOrThrow();

    private static final Field PLAYER_DATA_LIST = (Field)Reflect.getDeclaredFieldByType((Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo").getOrThrow(), List.class, 0, true).getOrThrow();

    private static final Method GET_GAME_PROFILE = (Method)Reflect.getMethodByType(PLAYER_INFO_DATA_CLASS, GAME_PROFILE_CLASS, 0).getOrThrow();

    private static final Constructor<?> PLAYER_INFO_DATA_CONSTRUCTOR = (Constructor)Reflect.getConstructor(PLAYER_INFO_DATA_CLASS, new Class[] { (Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo").getOrThrow(), GAME_PROFILE_CLASS, int.class, ENUM_GAMEMODE,
            (Class)Reflect.getNMSClass("IChatBaseComponent").getOrThrow() }).getOrThrow();

    private static final Method GET_LATENCY = (Method)Reflect.getMethodByType(PLAYER_INFO_DATA_CLASS, int.class, 0).getOrThrow();

    private static final Method GET_GAMEMODE = (Method)Reflect.getMethodByType(PLAYER_INFO_DATA_CLASS, ENUM_GAMEMODE, 0).getOrThrow();

    private static final Method GET_DISPLAY_NAME = (Method)Reflect.getMethodByType(PLAYER_INFO_DATA_CLASS, (Class)Reflect.getNMSClass("IChatBaseComponent").getOrThrow(), 0).getOrThrow();

    private static final Class<?> ENTITY_PLAYER = (Class)Reflect.getNMSClass("EntityPlayer").getOrThrow();

    private static final Constructor<?> PACKET_PLAYER_INFO_CONSTRUCTOR = (Constructor)Reflect.getConstructor((Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo").getOrThrow(), new Class[] { (Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getOrThrow(), Array.newInstance(ENTITY_PLAYER, 0).getClass() }).getOrThrow();

    private static final Object REMOVE_PLAYER_CONSTANT = Reflect.getEnumConstant((Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getOrThrow(), "REMOVE_PLAYER").getOrThrow();

    private static final Method GET_HANDLE = (Method)Reflect.getMethod((Class)Reflect.getCBClass("entity.CraftPlayer").getOrThrow(), "getHandle", new Class[0]).getOrThrow();

    private static final Field PLAYER_CONNECTION = (Field)Reflect.getFieldByType(ENTITY_PLAYER, (Class)Reflect.getNMSClass("PlayerConnection").getOrThrow(), 0).getOrThrow();

    private static final Method SEND_PACKET = (Method)Reflect.getMethod((Class)Reflect.getNMSClass("PlayerConnection").getOrThrow(), "sendPacket", new Class[] { (Class)Reflect.getNMSClass("Packet").getOrThrow() }).getOrThrow();

    private static final Constructor<?> PACKET_PLAYER_INFO_CONSTRUCTOR_EMPTY = (Constructor)Reflect.getConstructor((Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo").getOrThrow(), new Class[0]).getOrThrow();

    private static final Method ENTITY_PLAYER_GET_GAME_PROFILE = (Method)Reflect.getMethodByType(ENTITY_PLAYER, GAME_PROFILE_CLASS, 0).getOrThrow();

    private static final Field PING = (Field)Reflect.getField(ENTITY_PLAYER, "ping").getOrThrow();

    private static final Method GET_BY_ID = (Method)Reflect.getMethod(ENUM_GAMEMODE, "getById", new Class[] { int.class }).getOrThrow();

    private static final Constructor<?> CHAT_COMPONENT_TEXT_CONSTRUCTOR = (Constructor)Reflect.getConstructor((Class)Reflect.getNMSClass("ChatComponentText").getOrThrow(), new Class[] { String.class }).getOrThrow();

    private static final Field PLAYER_INFO_ACTION = (Field)Reflect.getDeclaredFieldByType((Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo").getOrThrow(), (Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getOrThrow(), 0, true).getOrThrow();

    private static final Object ADD_PLAYER_CONSTANT = Reflect.getEnumConstant((Class)Reflect.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getOrThrow(), "ADD_PLAYER").getOrThrow();

    private static final Constructor<?> PACKET_ENTITY_DESTROY_CONSTRUCTOR = (Constructor)Reflect.getConstructor((Class)Reflect.getNMSClass("PacketPlayOutEntityDestroy").getOrThrow(), new Class[] { int[].class }).getOrThrow();

    private static final Constructor<?> PACKET_NAMED_ENTITY_SPAWN_CONSTRUCTOR = (Constructor)Reflect.getConstructor((Class)Reflect.getNMSClass("PacketPlayOutNamedEntitySpawn").getOrThrow(), new Class[] { (Class)Reflect.getNMSClass("EntityHuman").getOrThrow() }).getOrThrow();

    private static final Class<?> ITEM_STACK_CLASS = (Class)Reflect.getNMSClass("ItemStack").getOrThrow();

    private static final Method AS_NMS_COPY = (Method)Reflect.getMethod((Class)Reflect.getCBClass("inventory.CraftItemStack").getOrThrow(), "asNMSCopy", new Class[] { ItemStack.class }).getOrThrow();

    private static final Class<?> ENUM_ITEM_SLOT_CLASS = (Class)Reflect.getNMSClass("EnumItemSlot").getOrThrow();

    private static final Method ENUM_ITEM_SLOT_BY_NAME;

    static {
        ENUM_ITEM_SLOT_BY_NAME = (Method)Reflect.getMethodByPredicate(ENUM_ITEM_SLOT_CLASS, (Predicate)(new Reflect.MethodPredicate()).withModifiers(new int[] { 1, 8 }).withParams(new Class[] { String.class } ).withReturnType(ENUM_ITEM_SLOT_CLASS).withPredicate(method -> !method.getName().equals("valueOf")), 0).getOrThrow();
    }

    private static final Constructor<?> PACKET_ENTITY_EQUIPMENT_CONSTRUCTOR = (Constructor)Reflect.getConstructor((Class)Reflect.getNMSClass("PacketPlayOutEntityEquipment").getOrThrow(), new Class[] { int.class, ENUM_ITEM_SLOT_CLASS, ITEM_STACK_CLASS }).getOrThrow();

    private static final Class<?> SCOREBOARD_TEAM_PACKET_CLASS = (Class)Reflect.getNMSClass("PacketPlayOutScoreboardTeam").getOrThrow();

    private static final Field SCOREBOARD_TEAM_PACKET_MODE = (Field)Reflect.getDeclaredField(SCOREBOARD_TEAM_PACKET_CLASS, "i", true).getOrThrow();

    private static final Field SCOREBOARD_TEAM_PACKET_ENTRIES_TO_ADD = (Field)Reflect.getDeclaredFieldByType(SCOREBOARD_TEAM_PACKET_CLASS, Collection.class, 0, true).getOrThrow();

    private static final int CREATE_SCOREBOARD_TEAM_MODE = 0;

    private static final int JOIN_SCOREBOARD_TEAM_MODE = 3;

    private static final int LEAVE_SCOREBOARD_TEAM_MODE = 4;

    private static final Constructor<?> SCOREBOARD_TEAM_PACKET_CONSTRUCTOR = (Constructor)Reflect.getConstructor(SCOREBOARD_TEAM_PACKET_CLASS, new Class[0]).getOrThrow();

    private static final Field SCOREBOARD_TEAM_PACKET_TEAM_NAME = (Field)Reflect.getDeclaredField(SCOREBOARD_TEAM_PACKET_CLASS, "a", true).getOrThrow();

    public ChannelPacketHandler(Plugin plugin) {
        super(plugin, new String[] { "PacketPlayOutPlayerInfo", "PacketPlayOutScoreboardTeam" });
    }

    public boolean packetSending(Player player, Object packet, String packetName) {
        if (Main.INSTANCE.sendingPackets)
            return true;
        if (packetName.equals("PacketPlayOutPlayerInfo")) {
            List<Object> list = Lists.newArrayList();
            boolean modified = false;
            Reflect.ReflectionResponse<Object> infoData = Reflect.getFieldValue(packet, PLAYER_DATA_LIST);

                GameProfileWrapper gameProfile = GameProfileWrapper.fromHandle(Reflect.invokeMethod(infoData, GET_GAME_PROFILE, new Object[0]).getOrThrow());
                UUID uuid = gameProfile.getUUID();
                if (Main.INSTANCE.gameProfiles.containsKey(uuid)) {
                    Object prevDisplayName = Reflect.invokeMethod(infoData, GET_DISPLAY_NAME, new Object[0]).getOrThrow();
                    Object displayName = (prevDisplayName == null) ? Reflect.invokeConstructor(CHAT_COMPONENT_TEXT_CONSTRUCTOR, new Object[] { (Bukkit.getPlayer(uuid) == null) ? gameProfile.getName() : Bukkit.getPlayer(uuid).getPlayerListName() }).getOrThrow() : Reflect.invokeMethod(infoData, GET_DISPLAY_NAME, new Object[0]).getOrThrow();
                    GameProfileWrapper newGameProfile = Main.INSTANCE.gameProfiles.get(uuid);
                    Object newInfoData = Reflect.invokeConstructor(PLAYER_INFO_DATA_CONSTRUCTOR, new Object[] { packet, newGameProfile.getHandle(), Reflect.invokeMethod(infoData, GET_LATENCY, new Object[0]).getOrThrow(), Reflect.invokeMethod(infoData, GET_GAMEMODE, new Object[0]).getOrThrow(), displayName }).getOrThrow();
                    list.add(newInfoData);
                    modified = true;
                }
                list.add(infoData);

            if (modified)
                Reflect.setFieldValue(packet, PLAYER_DATA_LIST, list);
        } else {
            int mode = ((Integer)Reflect.getFieldValue(packet, SCOREBOARD_TEAM_PACKET_MODE).getOrThrow()).intValue();
            if (mode == 0 || mode == 3 || mode == 4) {
                Collection<String> entriesToAdd = (Collection<String>)Reflect.getFieldValue(packet, SCOREBOARD_TEAM_PACKET_ENTRIES_TO_ADD).getOrThrow();
                Map<UUID, String> changedPlayerNames = Main.INSTANCE.getChangedPlayers();
                for (String entry : entriesToAdd) {
                    for (UUID uuid : changedPlayerNames.keySet()) {
                        Player changedPlayer = Bukkit.getPlayer(uuid);
                        if (changedPlayer != null && changedPlayer.getName().equals(entry)) {
                            entriesToAdd.remove(entry);
                            entriesToAdd.add(changedPlayerNames.get(uuid));
                        }
                    }
                }
            }
        }
        return true;
    }

    protected void logMessage(Level level, String message, Exception e) {
        if (level == Level.SEVERE) {
            System.err.println("[NameTagChanger] " + message);
        } else {
            Main.INSTANCE.printMessage(message);
        }
        if (e != null)
            e.printStackTrace();
    }

    public void sendTabListRemovePacket(Player playerToRemove, Player seer) {
        Object array = Array.newInstance(ENTITY_PLAYER, 1);
        Array.set(array, 0, Reflect.invokeMethod(playerToRemove, GET_HANDLE, new Object[0]).getOrThrow());
        Object packet = Reflect.invokeConstructor(PACKET_PLAYER_INFO_CONSTRUCTOR, new Object[] { REMOVE_PLAYER_CONSTANT, array }).getOrThrow();
        sendPacket(seer, packet);
    }

    public void sendTabListAddPacket(Player playerToAdd, GameProfileWrapper newProfile, Player seer) {
        Object packet = Reflect.invokeConstructor(PACKET_PLAYER_INFO_CONSTRUCTOR_EMPTY, new Object[0]).getOrThrow();
        Object entityPlayer = Reflect.invokeMethod(playerToAdd, GET_HANDLE, new Object[0]).getOrThrow();
        Object infoData = Reflect.invokeConstructor(PLAYER_INFO_DATA_CONSTRUCTOR, new Object[] { packet, newProfile.getHandle(), Reflect.getFieldValue(entityPlayer, PING).getOrThrow(), getEnumGameMode(playerToAdd.getGameMode()), Reflect.invokeConstructor(CHAT_COMPONENT_TEXT_CONSTRUCTOR, new Object[] { playerToAdd.getPlayerListName() }).getOrThrow() }).getOrThrow();
        Reflect.setFieldValue(packet, PLAYER_DATA_LIST, Collections.singletonList(infoData)).getOrThrow();
        Reflect.setFieldValue(packet, PLAYER_INFO_ACTION, ADD_PLAYER_CONSTANT).getOrThrow();
        sendPacket(seer, packet);
    }

    public void sendEntityDestroyPacket(Player playerToDestroy, Player seer) {

        Object packet = Reflect.invokeConstructor(PACKET_ENTITY_DESTROY_CONSTRUCTOR, new Object[] { playerToDestroy.getEntityId() }).getOrThrow();
        sendPacket(seer, packet);
    }

    public void sendNamedEntitySpawnPacket(Player playerToSpawn, Player seer) {
        Object packet = Reflect.invokeConstructor(PACKET_NAMED_ENTITY_SPAWN_CONSTRUCTOR, new Object[] { Reflect.invokeMethod(playerToSpawn, GET_HANDLE, new Object[0]).getOrThrow() }).getOrThrow();
        sendPacket(seer, packet);
    }

    public void sendEntityEquipmentPacket(Player playerToSpawn, Player seer) {
        int entityID = playerToSpawn.getEntityId();
        if (playerToSpawn.getInventory().getItemInMainHand() != null)
            doEquipmentPacketSend(entityID, getEnumItemSlot(EquipmentSlot.HAND), getNMSItemStack(playerToSpawn.getInventory().getItemInMainHand()), seer);
        if (playerToSpawn.getInventory().getItemInOffHand() != null)
            doEquipmentPacketSend(entityID, getEnumItemSlot(EquipmentSlot.OFF_HAND), getNMSItemStack(playerToSpawn.getInventory().getItemInOffHand()), seer);
        if (playerToSpawn.getInventory().getBoots() != null)
            doEquipmentPacketSend(entityID, getEnumItemSlot(EquipmentSlot.FEET), getNMSItemStack(playerToSpawn.getInventory().getBoots()), seer);
        if (playerToSpawn.getInventory().getLeggings() != null)
            doEquipmentPacketSend(entityID, getEnumItemSlot(EquipmentSlot.LEGS), getNMSItemStack(playerToSpawn.getInventory().getLeggings()), seer);
        if (playerToSpawn.getInventory().getChestplate() != null)
            doEquipmentPacketSend(entityID, getEnumItemSlot(EquipmentSlot.CHEST), getNMSItemStack(playerToSpawn.getInventory().getChestplate()), seer);
        if (playerToSpawn.getInventory().getHelmet() != null)
            doEquipmentPacketSend(entityID, getEnumItemSlot(EquipmentSlot.HEAD), getNMSItemStack(playerToSpawn.getInventory().getHelmet()), seer);
    }

    private void doEquipmentPacketSend(int entityID, Object enumItemSlot, Object itemStack, Player recipient) {
        Object packet = Reflect.invokeConstructor(PACKET_ENTITY_EQUIPMENT_CONSTRUCTOR, new Object[] { Integer.valueOf(entityID), enumItemSlot, itemStack }).getOrThrow();
        sendPacket(recipient, packet);
    }

    private Object getNMSItemStack(ItemStack itemStack) {
        return Reflect.invokeMethod(null, AS_NMS_COPY, new Object[] { itemStack }).getOrThrow();
    }

    private Object getEnumItemSlot(EquipmentSlot slot) {

        switch (slot) {
            case HAND:
                return  Reflect.invokeMethod(null, ENUM_ITEM_SLOT_BY_NAME, new Object[] { "mainhand" }).getOrThrow();
            case OFF_HAND:
                return  Reflect.invokeMethod(null, ENUM_ITEM_SLOT_BY_NAME, new Object[] { "offhand" }).getOrThrow();
            case FEET:
                return  Reflect.invokeMethod(null, ENUM_ITEM_SLOT_BY_NAME, new Object[] { "feet" }).getOrThrow();
            case LEGS:
                return  Reflect.invokeMethod(null, ENUM_ITEM_SLOT_BY_NAME, new Object[] { "legs" }).getOrThrow();
            case CHEST:
                return  Reflect.invokeMethod(null, ENUM_ITEM_SLOT_BY_NAME, new Object[] { "chest" }).getOrThrow();
            case HEAD:
                return  Reflect.invokeMethod(null, ENUM_ITEM_SLOT_BY_NAME, new Object[] { "head" }).getOrThrow();
        }
        logMessage(Level.SEVERE, "Unknown EquipmentSlot: " + slot, null);
        return null;
    }

    public void sendScoreboardRemovePacket(String playerToRemove, Player seer, String team) {
        sendPacket(seer, getScoreboardPacket(team, playerToRemove, 4));
    }

    public void sendScoreboardAddPacket(String playerToAdd, Player seer, String team) {
        sendPacket(seer, getScoreboardPacket(team, playerToAdd, 3));
    }

    private Object getScoreboardPacket(String team, String entryToAdd, int mode) {
        Object packet = Reflect.invokeConstructor(SCOREBOARD_TEAM_PACKET_CONSTRUCTOR, new Object[0]).getOrThrow();
        Reflect.setFieldValue(packet, SCOREBOARD_TEAM_PACKET_TEAM_NAME, team).getOrThrow();
        Reflect.setFieldValue(packet, SCOREBOARD_TEAM_PACKET_MODE, Integer.valueOf(mode)).getOrThrow();
        ((Collection<String>)Reflect.getFieldValue(packet, SCOREBOARD_TEAM_PACKET_ENTRIES_TO_ADD).getOrThrow()).add(entryToAdd);
        return packet;
    }

    public void shutdown() {
        close();
    }

    public GameProfileWrapper getDefaultPlayerProfile(Player player) {
        Object entityPlayer = Reflect.invokeMethod(player, GET_HANDLE, new Object[0]).getOrThrow();
        return GameProfileWrapper.fromHandle(Reflect.invokeMethod(entityPlayer, ENTITY_PLAYER_GET_GAME_PROFILE, new Object[0]).getOrThrow());
    }

    private void sendPacket(Player player, Object packet) {
        Object playerConnection = Reflect.getFieldValue(Reflect.invokeMethod(player, GET_HANDLE, new Object[0]).getOrThrow(), PLAYER_CONNECTION).getOrThrow();
        Reflect.invokeMethod(playerConnection, SEND_PACKET, new Object[] { packet }).getOrThrow();
    }

    private Object getEnumGameMode(GameMode bukkitGameMode) {
        int id = 0;
        switch (bukkitGameMode) {
            case CREATIVE:
                id = 1;
                break;
            case ADVENTURE:
                id = 2;
                break;
            case SPECTATOR:
                id = 3;
                break;
        }
        return Reflect.invokeMethod(null, GET_BY_ID, new Object[] { Integer.valueOf(id) }).getOrThrow();
    }
}
