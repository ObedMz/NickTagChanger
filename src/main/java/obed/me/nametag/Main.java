package obed.me.nametag;

import obed.me.nametag.object.Skin;
import obed.me.nametag.object.SkinCallBack;
import obed.me.nametag.packet.ChannelPacketHandler;
import obed.me.nametag.packet.GameProfileWrapper;
import obed.me.nametag.packet.IPacketHandler;
import obed.me.nametag.packet.ProtocolLibPacketHandler;
import obed.me.nametag.utils.MojangAPI;
import obed.me.nametag.utils.Reflect;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

@SuppressWarnings("All")
public final class Main {
    private static final LoadingCache<UUID, Skin> SKIN_CACHE = CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.MINUTES).build(new CacheLoader<UUID, Skin>() {
        public Skin load(UUID uuid) throws Exception {
            MojangAPI.Result<MojangAPI.SkinData> result = MojangAPI.getSkinData(uuid);
            if (result.wasSuccessful()) {
                if (result.getValue() != null) {
                    MojangAPI.SkinData data = (MojangAPI.SkinData)result.getValue();
                    if (data.getSkinURL() == null && data.getCapeURL() == null)
                        return Skin.EMPTY_SKIN;
                    return new Skin(data.getUUID(), data.getBase64(), data.getSignedBase64());
                }
            } else {
                throw result.getException();
            }
            return Skin.EMPTY_SKIN;
        }
    });

    public static final String VERSION = "1.1-SNAPSHOT";

    public static final Main INSTANCE = new Main();

    public boolean sendingPackets;

    private IPacketHandler packetHandler;

    public HashMap<UUID, GameProfileWrapper> gameProfiles = Maps.newHashMap();

    private Plugin plugin;

    private boolean enabled;

    private Main() {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.getClass().getProtectionDomain().getCodeSource().equals(getClass().getProtectionDomain().getCodeSource()))
                this.plugin = plugin;
        }
        enable();
    }

    public void setPlayerSkin(Player player, Skin skin) {
        Validate.isTrue(this.enabled, "NameTagChanger is disabled");
        Validate.notNull(player, "player cannot be null");
        Validate.notNull(skin, "skin cannot be null");
        Validate.isTrue(!skin.equals(getDefaultSkinFromPlayer(player)), "Skin cannot be the default skin of the player! If you intended to reset the skin, use resetPlayerSkin() instead.");
        GameProfileWrapper profile = this.gameProfiles.get(player.getUniqueId());
        if (profile == null)
            profile = this.packetHandler.getDefaultPlayerProfile(player);
        profile.getProperties().removeAll("textures");
        if (skin != Skin.EMPTY_SKIN)
            profile.getProperties().put("textures", new GameProfileWrapper.PropertyWrapper("textures", skin.getBase64(), skin.getSignedBase64()));
        this.gameProfiles.put(player.getUniqueId(), profile);
    }

    public void resetPlayerSkin(Player player) {
        Validate.isTrue(this.enabled, "NameTagChanger is disabled");
        if (player == null || !this.gameProfiles.containsKey(player.getUniqueId()))
            return;
        GameProfileWrapper profile = this.gameProfiles.get(player.getUniqueId());
        profile.getProperties().removeAll("textures");
        GameProfileWrapper defaultProfile = this.packetHandler.getDefaultPlayerProfile(player);
        if (defaultProfile.getProperties().containsKey("textures"))
            profile.getProperties().putAll("textures", defaultProfile.getProperties().get("textures"));
        checkForRemoval(player);
    }

    public void changePlayerName(Player player, String newName) {
        Validate.isTrue(this.enabled, "NameTagChanger is disabled");
        Validate.notNull(player, "player cannot be null");
        Validate.notNull(newName, "newName cannot be null");
        Validate.isTrue(!newName.equals(player.getName()), "The new name cannot be the same as the player's! If you intended to reset the player's name, use resetPlayerName()!");
        Validate.isTrue((newName.length() <= 16), "newName cannot be longer than 16 characters!");
        GameProfileWrapper profile = new GameProfileWrapper(player.getUniqueId(), newName);
        if (this.gameProfiles.containsKey(player.getUniqueId())) {
            profile.getProperties().putAll(((GameProfileWrapper)this.gameProfiles.get(player.getUniqueId())).getProperties());
        } else {
            profile.getProperties().putAll(this.packetHandler.getDefaultPlayerProfile(player).getProperties());
        }
        this.gameProfiles.put(player.getUniqueId(), profile);
        updatePlayer(player, player.getName());
    }

    public void resetPlayerName(Player player) {
        Validate.isTrue(this.enabled, "NameTagChanger is disabled");
        if (player == null || !this.gameProfiles.containsKey(player.getUniqueId()))
            return;
        GameProfileWrapper oldProfile = this.gameProfiles.get(player.getUniqueId());
        GameProfileWrapper newProfile = this.packetHandler.getDefaultPlayerProfile(player);
        newProfile.getProperties().removeAll("textures");
        if (oldProfile.getProperties().containsKey("textures"))
            newProfile.getProperties().putAll("textures", oldProfile.getProperties().get("textures"));
        this.gameProfiles.put(player.getUniqueId(), newProfile);
        updatePlayer(player, oldProfile.getName());
        checkForRemoval(player);
    }

    private void checkForRemoval(Player player) {
        if (((GameProfileWrapper)this.gameProfiles.get(player.getUniqueId())).equals(this.packetHandler.getDefaultPlayerProfile(player)))
            this.gameProfiles.remove(player.getUniqueId());
    }

    public String getChangedName(Player player) {
        Validate.isTrue(this.enabled, "NameTagChanger is disabled");
        GameProfileWrapper profile = this.gameProfiles.get(player.getUniqueId());
        return (profile == null || profile.getName().equals(player.getName())) ? null : profile.getName();
    }

    public Skin getChangedSkin(Player player) {
        Validate.isTrue(this.enabled, "NameTagChanger is disabled");
        GameProfileWrapper profile = this.gameProfiles.get(player.getUniqueId());
        if (profile == null)
            return null;
        Skin skin = getSkinFromGameProfile(profile);
        if (skin.equals(getDefaultSkinFromPlayer(player)))
            return null;
        return skin;
    }

    public Skin getDefaultSkinFromPlayer(Player player) {
        return getSkinFromGameProfile(this.packetHandler.getDefaultPlayerProfile(player));
    }

    public Skin getSkinFromGameProfile(GameProfileWrapper profile) {
        Validate.isTrue(this.enabled, "NameTagChanger is disabled");
        Validate.notNull(profile, "profile cannot be null");
        if (profile.getProperties().containsKey("textures")) {
            GameProfileWrapper.PropertyWrapper property = (GameProfileWrapper.PropertyWrapper)Iterables.getFirst(profile.getProperties().get("textures"), null);
            if (property == null)
                return Skin.EMPTY_SKIN;
            return new Skin(profile.getUUID(), property.getValue(), property.getSignature());
        }
        return Skin.EMPTY_SKIN;
    }

    public void updatePlayer(Player player) {
        updatePlayer(player, null);
    }

    private void updatePlayer(Player player, String oldName) {
        Validate.isTrue(this.enabled, "NameTagChanger is disabled");
        GameProfileWrapper newProfile = this.gameProfiles.get(player.getUniqueId());
        if (newProfile == null)
            newProfile = this.packetHandler.getDefaultPlayerProfile(player);
        List<Team> scoreboardTeamsToUpdate = Lists.newArrayList();
        this.sendingPackets = true;
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (otherPlayer.equals(player)) {
                if (otherPlayer.getScoreboard().getEntryTeam(player.getName()) != null)
                    scoreboardTeamsToUpdate.add(otherPlayer.getScoreboard().getEntryTeam(player.getName()));
                continue;
            }
            if (otherPlayer.canSee(player)) {
                this.packetHandler.sendTabListRemovePacket(player, otherPlayer);
                this.packetHandler.sendTabListAddPacket(player, newProfile, otherPlayer);
                if (otherPlayer.getWorld().equals(player.getWorld())) {
                    this.packetHandler.sendEntityDestroyPacket(player, otherPlayer);
                    this.packetHandler.sendNamedEntitySpawnPacket(player, otherPlayer);
                    this.packetHandler.sendEntityEquipmentPacket(player, otherPlayer);
                }
            }
            if (otherPlayer.getScoreboard().getEntryTeam(player.getName()) != null)
                scoreboardTeamsToUpdate.add(otherPlayer.getScoreboard().getEntryTeam(player.getName()));
        }
        if (oldName != null) {
            String newName = newProfile.getName();
            for (Team team : scoreboardTeamsToUpdate) {
                Bukkit.getOnlinePlayers().stream().filter(p -> (p.getScoreboard() == team.getScoreboard())).forEach(p -> {
                    this.packetHandler.sendScoreboardRemovePacket(oldName, p, team.getName());
                    this.packetHandler.sendScoreboardAddPacket(newName, p, team.getName());
                });
            }
        }
        this.sendingPackets = false;
    }

    public Map<UUID, String> getChangedPlayers() {
        Map<UUID, String> changedPlayers = Maps.newHashMap();
        for (Map.Entry<UUID, GameProfileWrapper> entry : this.gameProfiles.entrySet())
            changedPlayers.put(entry.getKey(), ((GameProfileWrapper)entry.getValue()).getName());
        return Collections.unmodifiableMap(changedPlayers);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void disable() {
        Validate.isTrue(this.enabled, "NameTagChanger is already disabled");
        for (UUID uuid : this.gameProfiles.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                continue;
            resetPlayerName(player);
            resetPlayerSkin(player);
        }
        this.gameProfiles.clear();
        this.packetHandler.shutdown();
        this.packetHandler = null;
        this.enabled = false;
    }

    public void enable() {
        if (this.plugin == null)
            return;
        if (!Reflect.isVersionHigherThan(1, 7, 10))
            printMessage("NameTagChanger has detected that you are running 1.7 or lower. This probably means that NameTagChanger will not work or throw errors, but you are still free to try and use it.\nIf you are not a developer, please consider contacting the developer of " + this.plugin.getName() + " and informing them about this message.");
        ConfigurationSerialization.registerClass(Skin.class);
        Validate.isTrue(!this.enabled, "NameTagChanger is already enabled");
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            this.packetHandler = new ProtocolLibPacketHandler(this.plugin);
        } else {
            this.packetHandler = new ChannelPacketHandler(this.plugin);
        }
        this.enabled = true;
    }

    public void printMessage(String message) {
        System.out.println("[NameTagChanger] " + message);
    }

    public void setPlugin(Plugin plugin) {
        Validate.notNull(plugin, "plugin cannot be null");
        this.plugin = plugin;
    }

    public void getSkin(final String username, final SkinCallBack callBack) {
        (new BukkitRunnable() {
            public void run() {
                final MojangAPI.Result<Map<String, MojangAPI.Profile>> result = MojangAPI.getUUID(Collections.singletonList(username));
                if (result.wasSuccessful()) {
                    if (result.getValue() == null || ((Map)result.getValue()).isEmpty()) {
                        (new BukkitRunnable() {
                            public void run() {
                                callBack.callBack(Skin.EMPTY_SKIN, true, null);
                            }
                        }).runTask(Main.this.plugin);
                        return;
                    }
                    for (Map.Entry<String, MojangAPI.Profile> entry : (Iterable<Map.Entry<String, MojangAPI.Profile>>)((Map)result.getValue()).entrySet()) {
                        if (((String)entry.getKey()).equalsIgnoreCase(username)) {
                            Main.this.getSkin(((MojangAPI.Profile)entry.getValue()).getUUID(), callBack);
                            return;
                        }
                    }
                } else {
                    (new BukkitRunnable() {
                        public void run() {
                            callBack.callBack(null, false, result.getException());
                        }
                    }).runTask(Main.this.plugin);
                }
            }
        }).runTaskAsynchronously(this.plugin);
    }

    public void getSkin(final UUID uuid, final SkinCallBack callBack) {
        Map<UUID, Skin> asMap = SKIN_CACHE.asMap();
        if (asMap.containsKey(uuid)) {
            callBack.callBack(asMap.get(uuid), true, null);
        } else {
            (new BukkitRunnable() {
                public void run() {
                    try {
                        final Skin skin = (Skin)Main.SKIN_CACHE.get(uuid);
                        (new BukkitRunnable() {
                            public void run() {
                                callBack.callBack(skin, true, null);
                            }
                        }).runTask(Main.this.plugin);
                    } catch (ExecutionException e) {
                        (new BukkitRunnable() {
                            public void run() {
                                callBack.callBack(null, false, e);
                            }
                        }).runTask(Main.this.plugin);
                    }
                }
            }).runTaskAsynchronously(this.plugin);
        }
    }

    public void init() {}
}
