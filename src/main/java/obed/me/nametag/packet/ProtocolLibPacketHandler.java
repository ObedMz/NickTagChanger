package obed.me.nametag.packet;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import obed.me.nametag.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import obed.me.nametag.utils.Reflect;

@SuppressWarnings("ALL")
public class ProtocolLibPacketHandler extends PacketAdapter implements IPacketHandler {
    private static final Class<?> ENTITY_PLAYER = (Class) Reflect.getNMSClass("EntityPlayer").getOrThrow();

    private static final Method GET_HANDLE = (Method)Reflect.getMethod((Class)Reflect.getCBClass("entity.CraftPlayer").getOrThrow(), "getHandle", new Class[0]).getOrThrow();

    private static final Field PING = (Field)Reflect.getField(ENTITY_PLAYER, "ping").getOrThrow();

    private static final int CREATE_SCOREBOARD_TEAM_MODE = 0;

    private static final int JOIN_SCOREBOARD_TEAM_MODE = 3;

    private static final int LEAVE_SCOREBOARD_TEAM_MODE = 4;

    public ProtocolLibPacketHandler(Plugin plugin) {
        super(plugin, new PacketType[] { PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.SCOREBOARD_TEAM });
        ProtocolLibrary.getProtocolManager().addPacketListener((PacketListener)this);
    }

    public void onPacketSending(PacketEvent e) {
        if (Main.INSTANCE.sendingPackets)
            return;
        if (e.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            List<PlayerInfoData> list = Lists.newArrayList();
            boolean modified = false;
            for (PlayerInfoData infoData : e.getPacket().getPlayerInfoDataLists().read(0)) {
                if (Main.INSTANCE.gameProfiles.containsKey(infoData.getProfile().getUUID())) {
                    UUID uuid = infoData.getProfile().getUUID();
                    Player player = Bukkit.getPlayer(uuid);
                    WrappedChatComponent displayName = (infoData.getDisplayName() == null) ? WrappedChatComponent.fromText((player == null) ? infoData.getProfile().getName() : player.getPlayerListName()) : infoData.getDisplayName();
                    WrappedGameProfile gameProfile = getProtocolLibProfileWrapper(Main.INSTANCE.gameProfiles.get(uuid));
                    PlayerInfoData newInfoData = new PlayerInfoData(gameProfile, infoData.getLatency(), infoData.getGameMode(), displayName);
                    list.add(newInfoData);
                    modified = true;
                    continue;
                }
                list.add(infoData);
            }
            if (modified)
                e.getPacket().getPlayerInfoDataLists().write(0, list);
        } else {
            int mode = ((Integer)e.getPacket().getIntegers().read(1)).intValue();
            if (mode == 0 || mode == 4 || mode == 3) {
                Collection<String> entriesToAdd = (Collection<String>)e.getPacket().getSpecificModifier(Collection.class).read(0);
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
    }

    public void sendTabListRemovePacket(Player playerToRemove, Player seer) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        PlayerInfoData playerInfoData = new PlayerInfoData(WrappedGameProfile.fromPlayer(playerToRemove), 0, EnumWrappers.NativeGameMode.NOT_SET, null);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(seer, packet);
        } catch (InvocationTargetException e) {
            logMessage(Level.SEVERE, "Failed to send tab list remove packet!", e);
        }
    }

    public void sendTabListAddPacket(Player playerToAdd, GameProfileWrapper newProfile, Player seer) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        int ping = ((Integer)Reflect.getFieldValue(Reflect.invokeMethod(playerToAdd, GET_HANDLE, new Object[0]).getOrThrow(), PING).getOrThrow()).intValue();
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        PlayerInfoData playerInfoData = new PlayerInfoData(getProtocolLibProfileWrapper(newProfile), ping, EnumWrappers.NativeGameMode.fromBukkit(playerToAdd.getGameMode()), WrappedChatComponent.fromText(playerToAdd.getPlayerListName()));
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(seer, packet);
        } catch (InvocationTargetException e) {
            logMessage(Level.SEVERE, "Failed to send tab list add packet!", e);
        }
    }

    public void sendEntityDestroyPacket(Player playerToDestroy, Player seer) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntegerArrays().write(0, new int[] { playerToDestroy.getEntityId() });
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(seer, packet);
        } catch (InvocationTargetException e) {
            logMessage(Level.SEVERE, "Failed to send entity destroy packet!", e);
        }
    }

    public void sendNamedEntitySpawnPacket(Player playerToSpawn, Player seer) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        packet.getIntegers().write(0, Integer.valueOf(playerToSpawn.getEntityId()));
        packet.getUUIDs().write(0, playerToSpawn.getUniqueId());
        if (Reflect.isVersionHigherThan(1, 8, 8)) {
            packet.getDoubles().write(0, Double.valueOf(playerToSpawn.getLocation().getX()));
            packet.getDoubles().write(1, Double.valueOf(playerToSpawn.getLocation().getY()));
            packet.getDoubles().write(2, Double.valueOf(playerToSpawn.getLocation().getZ()));
        } else {
            packet.getIntegers().write(0, Integer.valueOf((int)Math.floor(playerToSpawn.getLocation().getX() * 32.0D)));
            packet.getIntegers().write(1, Integer.valueOf((int)Math.floor(playerToSpawn.getLocation().getY() * 32.0D)));
            packet.getIntegers().write(2, Integer.valueOf((int)Math.floor(playerToSpawn.getLocation().getZ() * 32.0D)));
        }
        packet.getBytes().write(0, Byte.valueOf((byte)(int)(playerToSpawn.getLocation().getYaw() * 256.0F / 360.0F)));
        packet.getBytes().write(1, Byte.valueOf((byte)(int)(playerToSpawn.getLocation().getPitch() * 256.0F / 360.0F)));
        packet.getDataWatcherModifier().write(0, WrappedDataWatcher.getEntityWatcher((Entity)playerToSpawn));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(seer, packet);
        } catch (InvocationTargetException e) {
            logMessage(Level.SEVERE, "Failed to send named entity spawn packet!", e);
        }
    }

    public void sendEntityEquipmentPacket(Player playerToSpawn, Player seer) {
        int entityID = playerToSpawn.getEntityId();
        if (playerToSpawn.getInventory().getItemInMainHand() != null && playerToSpawn.getInventory().getItemInMainHand().getType() != Material.AIR)
            doEquipmentPacketSend(entityID, EnumWrappers.ItemSlot.MAINHAND, playerToSpawn.getInventory().getItemInMainHand(), seer);
        if (playerToSpawn.getInventory().getItemInOffHand() != null && playerToSpawn.getInventory().getItemInOffHand().getType() != Material.AIR)
            doEquipmentPacketSend(entityID, EnumWrappers.ItemSlot.OFFHAND, playerToSpawn.getInventory().getItemInOffHand(), seer);
        if (playerToSpawn.getInventory().getBoots() != null && playerToSpawn.getInventory().getBoots().getType() != Material.AIR)
            doEquipmentPacketSend(entityID, EnumWrappers.ItemSlot.FEET, playerToSpawn.getInventory().getBoots(), seer);
        if (playerToSpawn.getInventory().getLeggings() != null && playerToSpawn.getInventory().getLeggings().getType() != Material.AIR)
            doEquipmentPacketSend(entityID, EnumWrappers.ItemSlot.LEGS, playerToSpawn.getInventory().getLeggings(), seer);
        if (playerToSpawn.getInventory().getChestplate() != null && playerToSpawn.getInventory().getChestplate().getType() != Material.AIR)
            doEquipmentPacketSend(entityID, EnumWrappers.ItemSlot.CHEST, playerToSpawn.getInventory().getChestplate(), seer);
        if (playerToSpawn.getInventory().getHelmet() != null && playerToSpawn.getInventory().getHelmet().getType() != Material.AIR)
            doEquipmentPacketSend(entityID, EnumWrappers.ItemSlot.HEAD, playerToSpawn.getInventory().getHelmet(), seer);
    }

    private void doEquipmentPacketSend(int entityID, EnumWrappers.ItemSlot slot, ItemStack itemStack, Player recipient) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, Integer.valueOf(entityID));
        packet.getItemSlots().write(0, slot);
        packet.getItemModifier().write(0, itemStack);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(recipient, packet);
        } catch (InvocationTargetException e) {
            logMessage(Level.SEVERE, "Failed to send equipment packet!", e);
        }
    }

    public void sendScoreboardRemovePacket(String playerToRemove, Player seer, String team) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(seer, getScoreboardPacket(team, playerToRemove, 4));
        } catch (InvocationTargetException e) {
            logMessage(Level.SEVERE, "Failed to send scoreboard remove packet!", e);
        }
    }

    public void sendScoreboardAddPacket(String playerToAdd, Player seer, String team) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(seer, getScoreboardPacket(team, playerToAdd, 3));
        } catch (InvocationTargetException e) {
            logMessage(Level.SEVERE, "Failed to send scoreboard add packet!", e);
        }
    }

    private PacketContainer getScoreboardPacket(String team, String entryToAdd, int mode) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, team);
        packet.getIntegers().write(1, Integer.valueOf(mode));
        ((Collection<String>)packet.getSpecificModifier(Collection.class).read(0)).add(entryToAdd);
        return packet;
    }

    public GameProfileWrapper getDefaultPlayerProfile(Player player) {
        WrappedGameProfile wrappedGameProfile = WrappedGameProfile.fromPlayer(player);
        GameProfileWrapper wrapper = new GameProfileWrapper(wrappedGameProfile.getUUID(), wrappedGameProfile.getName());
        for (Map.Entry<String, Collection<WrappedSignedProperty>> entry : (Iterable<Map.Entry<String, Collection<WrappedSignedProperty>>>)wrappedGameProfile.getProperties().asMap().entrySet()) {
            for (WrappedSignedProperty wrappedSignedProperty : entry.getValue())
                wrapper.getProperties().put(entry.getKey(), new GameProfileWrapper.PropertyWrapper(wrappedSignedProperty.getName(), wrappedSignedProperty.getValue(), wrappedSignedProperty.getSignature()));
        }
        return wrapper;
    }

    private static WrappedGameProfile getProtocolLibProfileWrapper(GameProfileWrapper wrapper) {
        WrappedGameProfile wrappedGameProfile = new WrappedGameProfile(wrapper.getUUID(), wrapper.getName());
        for (Map.Entry<String, Collection<GameProfileWrapper.PropertyWrapper>> entry : (Iterable<Map.Entry<String, Collection<GameProfileWrapper.PropertyWrapper>>>)wrapper.getProperties().asMap().entrySet()) {
            for (GameProfileWrapper.PropertyWrapper propertyWrapper : entry.getValue())
                wrappedGameProfile.getProperties().put(entry.getKey(), new WrappedSignedProperty(propertyWrapper.getName(), propertyWrapper.getValue(), propertyWrapper.getSignature()));
        }
        return wrappedGameProfile;
    }

    public void shutdown() {
        ProtocolLibrary.getProtocolManager().removePacketListener((PacketListener)this);
    }

    private void logMessage(Level level, String message, Exception e) {
        if (level == Level.SEVERE) {
            System.err.println("[NameTagChanger] " + message);
        } else {
            Main.INSTANCE.printMessage(message);
        }
        if (e != null)
            e.printStackTrace();
    }
}
