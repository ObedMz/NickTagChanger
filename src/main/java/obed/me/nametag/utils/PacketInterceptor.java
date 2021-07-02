package obed.me.nametag.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
@SuppressWarnings("ALL")
public abstract class PacketInterceptor implements Listener {
    private static final Method GET_HANDLE = (Method)Reflect.getMethod((Class)Reflect.getCBClass("entity.CraftPlayer").getOrThrow(), "getHandle", new Class[0]).getOrThrow();

    private static final Field PLAYER_CONNECTION = (Field)Reflect.getFieldByType((Class)Reflect.getNMSClass("EntityPlayer").getOrThrow(), (Class)Reflect.getNMSClass("PlayerConnection").getOrThrow(), 0).getOrThrow();

    private static final Class<?> NETWORK_MANAGER_CLASS = (Class)Reflect.getNMSClass("NetworkManager").getOrThrow();

    private static final Field NETWORK_MANAGER = (Field)Reflect.getFieldByType((Class)Reflect.getNMSClass("PlayerConnection").getOrThrow(), NETWORK_MANAGER_CLASS, 0).getOrThrow();

    private static final Field CHANNEL = (Field)Reflect.getFieldByType(NETWORK_MANAGER_CLASS, Channel.class, 0).getOrThrow();

    private static final Method GET_MINECRAFT_SERVER = (Method)Reflect.getMethodByType((Class)Reflect.getCBClass("CraftServer").getOrThrow(), (Class)Reflect.getNMSClass("MinecraftServer").getOrThrow(), 0).getOrThrow();

    private static final Class<?> SERVER_CONNECTION_CLASS = (Class)Reflect.getNMSClass("ServerConnection").getOrThrow();

    private static final Field SERVER_CONNECTION = (Field)Reflect.getDeclaredFieldByType((Class)Reflect.getNMSClass("MinecraftServer").getOrThrow(), SERVER_CONNECTION_CLASS, 0, true).getOrThrow();

    private static final Class<?> PACKET_LOGIN_START = (Class)Reflect.getNMSClass("PacketLoginInStart").getOrThrow();

    private static final Method GET_GAME_PROFILE = (Method)Reflect.getMethodByType(PACKET_LOGIN_START, GameProfile.class, 0).getOrThrow();

    private static Field NETWORK_MANAGERS = null;

    private static Field CHANNEL_FUTURES = null;

    private static int id = 0;

    private final Set<String> packets;

    private final boolean blackList;

    private final Plugin plugin;

    private final String handlerName;

    private final List<Channel> serverChannels = Lists.newArrayList();

    private final Map<String, Channel> injectedPlayerChannels = Maps.newHashMap();

    private ChannelInboundHandlerAdapter serverChannelHandler;

    private boolean syncWrite = doSyncWrite();

    private boolean syncRead = doSyncRead();

    public PacketInterceptor(Plugin plugin) {
        this(plugin, true, new String[0]);
    }

    public PacketInterceptor(Plugin plugin, String... packets) {
        this(plugin, false, packets);
    }

    public PacketInterceptor(Plugin plugin, boolean blackList, String... packets) {
        this.packets = (Set<String>)Arrays.<String>stream(packets).collect(Collectors.toSet());
        this.blackList = blackList;
        this.plugin = plugin;
        this.handlerName = "packet_interceptor_" + plugin.getName() + "_" + id++;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        injectServer();
        Bukkit.getOnlinePlayers().stream().filter(player -> !this.injectedPlayerChannels.containsKey(player.getName())).forEach(this::injectPlayer);
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent e) {
        injectPlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        if (this.injectedPlayerChannels.containsKey(e.getPlayer().getName()))
            this.injectedPlayerChannels.remove(e.getPlayer().getName());
    }

    protected void injectServer() {
        Object minecraftServer = Reflect.invokeMethod(Bukkit.getServer(), GET_MINECRAFT_SERVER, new Object[0]).getOrThrow();
        Object serverConnection = Reflect.getFieldValue(minecraftServer, SERVER_CONNECTION).getOrThrow();
        for (int i = 0; NETWORK_MANAGERS == null || CHANNEL_FUTURES == null; i++) {
            Field field = (Field)Reflect.getDeclaredFieldByType(SERVER_CONNECTION_CLASS, List.class, i, true).getOrThrow();
            List<Object> list = (List<Object>)Reflect.getFieldValue(serverConnection, field).getOrThrow();
            for (Object object : list) {
                if (NETWORK_MANAGERS == null && NETWORK_MANAGER_CLASS.isInstance(object))
                    NETWORK_MANAGERS = field;
                if (CHANNEL_FUTURES == null && ChannelFuture.class.isInstance(object))
                    CHANNEL_FUTURES = field;
            }
            if (CHANNEL_FUTURES != null && NETWORK_MANAGERS == null)
                NETWORK_MANAGERS = field;
        }
        final List<Object> networkManagers = (List<Object>)Reflect.getFieldValue(serverConnection, NETWORK_MANAGERS).getOrThrow();
        List<ChannelFuture> channelFutures = (List<ChannelFuture>)Reflect.getFieldValue(serverConnection, CHANNEL_FUTURES).getOrThrow();
        final ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) throws Exception {
                try {
                    synchronized (networkManagers) {
                        if (Reflect.isVersionHigherThan(1, 11, 2)) {
                            channel.eventLoop().submit(() -> PacketInterceptor.this.injectChannel(channel, null));
                        } else {
                            PacketInterceptor.this.injectChannel(channel, null);
                        }
                    }
                } catch (Exception e) {
                    PacketInterceptor.this.logMessage(Level.SEVERE, "Failed to inject Channel " + channel + "!", e);
                }
            }
        };
        final ChannelInitializer<Channel> channelPreInitializer = new ChannelInitializer<Channel>() {
            private Object val$channelInitializer;

            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(new ChannelHandler[] { (ChannelHandler) this.val$channelInitializer });
            }
        };
        this.serverChannelHandler = new ChannelInboundHandlerAdapter() {
            private Object val$channelPreInitializer;

            public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                Channel channel = (Channel)message;
                channel.pipeline().addFirst(new ChannelHandler[] { (ChannelHandler)this.val$channelPreInitializer });
                context.fireChannelRead(message);
            }
        };
        for (ChannelFuture channelFuture : channelFutures) {
            Channel channel = channelFuture.channel();
            this.serverChannels.add(channel);
            channel.pipeline().addFirst(new ChannelHandler[] { (ChannelHandler)this.serverChannelHandler });
        }
    }

    protected void injectPlayer(Player player) {
        Channel channel;
        if (this.injectedPlayerChannels.containsKey(player.getName())) {
            channel = this.injectedPlayerChannels.get(player.getName());
        } else {
            Object handle = Reflect.invokeMethod(player, GET_HANDLE, new Object[0]).getOrThrow();
            Object playerConnection = Reflect.getFieldValue(handle, PLAYER_CONNECTION).getOrThrow();
            if (playerConnection == null) {
                logMessage(Level.WARNING, "Failed to inject Channel for player " + player.getName() + "!", null);
                return;
            }
            Object networkManager = Reflect.getFieldValue(playerConnection, NETWORK_MANAGER).getOrThrow();
            channel = (Channel)Reflect.getFieldValue(networkManager, CHANNEL).getOrThrow();
        }
        injectChannel(channel, player);
        if (!this.injectedPlayerChannels.containsKey(player.getName()))
            this.injectedPlayerChannels.put(player.getName(), channel);
    }

    protected void injectChannel(Channel channel, Player player) {
        ChannelInterceptor handler = (ChannelInterceptor)channel.pipeline().get(this.handlerName);
        if (handler == null) {
            handler = new ChannelInterceptor();
            channel.pipeline().addBefore("packet_handler", this.handlerName, (ChannelHandler)handler);
        }
        if (player != null)
            handler.player = player;
    }

    public void close() {
        for (Iterator<Channel> chanel = this.injectedPlayerChannels.values().iterator(); chanel.hasNext(); ) {
            Channel channel = chanel.next();

            try {
                channel.eventLoop().execute(() -> channel.pipeline().remove(this.handlerName));
            } catch (NoSuchElementException noSuchElementException) {}
        }
        this.injectedPlayerChannels.clear();
        for (Channel channel : this.serverChannels)
            channel.pipeline().remove((ChannelHandler)this.serverChannelHandler);
        HandlerList.unregisterAll(this);
    }

    protected boolean doSyncRead() {
        try {
            return (getClass().getMethod("packetReading", new Class[] { Player.class, Object.class, String.class }).getDeclaringClass() != PacketInterceptor.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected boolean doSyncWrite() {
        try {
            return (getClass().getMethod("packetSending", new Class[] { Player.class, Object.class, String.class }).getDeclaringClass() != PacketInterceptor.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected void logMessage(Level level, String message, Exception e) {
        if (e != null) {
            this.plugin.getLogger().log(level, message, e);
        } else {
            this.plugin.getLogger().log(level, message);
        }
    }

    public boolean packetSendingAsync(Player player, Object packet, String packetName) {
        return true;
    }

    public boolean packetReadingAsync(Player player, Object packet, String packetName) {
        return true;
    }

    public boolean packetSending(Player player, Object packet, String packetName) {
        return true;
    }

    public boolean packetReading(Player player, Object packet, String packetName) {
        return true;
    }

    protected class ChannelInterceptor extends ChannelDuplexHandler {
        protected Player player;

        public void write(ChannelHandlerContext context, final Object message, ChannelPromise promise) throws Exception {
            if ((PacketInterceptor.this.blackList && PacketInterceptor.this.packets.contains(message.getClass().getSimpleName())) || (!PacketInterceptor.this.blackList && !PacketInterceptor.this.packets.contains(message.getClass().getSimpleName()))) {
                super.write(context, message, promise);
                return;
            }
            if (PacketInterceptor.this.syncWrite) {
                final boolean[] result = new boolean[2];
                (new BukkitRunnable() {
                    public void run() {
                        try {
                            result[0] = PacketInterceptor.this.packetSending(PacketInterceptor.ChannelInterceptor.this.player, message, message.getClass().getSimpleName());
                        } catch (Exception e) {
                            PacketInterceptor.this.logMessage(Level.SEVERE, "An error occurred while handling packet " + message.getClass().getSimpleName() + "!", e);
                            Reflect.ReflectionResponse<String> response = Reflect.getStringRepresentation(message, false, new Class[0]);
                            if (response.hasResult()) {
                                PacketInterceptor.this.logMessage(Level.SEVERE, "Packet dump: " + (String)response.getValue(), null);
                            } else {
                                PacketInterceptor.this.logMessage(Level.SEVERE, "Failed to retrieve packet dump!", response.getException());
                            }
                            result[0] = true;
                        }
                        result[1] = true;
                        synchronized (result) {
                            result.notifyAll();
                        }
                    }
                }).runTask(PacketInterceptor.this.plugin);
                synchronized (result) {
                    while (!result[1])
                        result.wait();
                }
                if (result[0])
                    super.write(context, message, promise);
            } else {
                try {
                    if (PacketInterceptor.this.packetSendingAsync(this.player, message, message.getClass().getSimpleName()))
                        super.write(context, message, promise);
                } catch (Exception e) {
                    PacketInterceptor.this.logMessage(Level.SEVERE, "An error occurred while handling packet " + message.getClass().getSimpleName() + "!", e);
                    Reflect.ReflectionResponse<String> response = Reflect.getStringRepresentation(message, false, new Class[0]);
                    if (response.hasResult()) {
                        PacketInterceptor.this.logMessage(Level.SEVERE, "Packet dump: " + (String)response.getValue(), null);
                    } else {
                        PacketInterceptor.this.logMessage(Level.SEVERE, "Failed to retrieve packet dump!", response.getException());
                    }
                    super.write(context, message, promise);
                }
            }
        }

        public void channelRead(ChannelHandlerContext context, final Object message) throws Exception {
            if (PacketInterceptor.PACKET_LOGIN_START.isInstance(message))
                PacketInterceptor.this.injectedPlayerChannels.put(((GameProfile)Reflect.invokeMethod(message, PacketInterceptor.GET_GAME_PROFILE, new Object[0]).getOrThrow()).getName(), context.channel());
            if ((PacketInterceptor.this.blackList && PacketInterceptor.this.packets.contains(message.getClass().getSimpleName())) || (!PacketInterceptor.this.blackList && !PacketInterceptor.this.packets.contains(message.getClass().getSimpleName()))) {
                super.channelRead(context, message);
                return;
            }
            if (PacketInterceptor.this.syncRead) {
                final boolean[] result = new boolean[2];
                (new BukkitRunnable() {
                    public void run() {
                        try {
                            result[0] = PacketInterceptor.this.packetReading(PacketInterceptor.ChannelInterceptor.this.player, message, message.getClass().getSimpleName());
                        } catch (Exception e) {
                            PacketInterceptor.this.logMessage(Level.SEVERE, "An error occurred while handling packet " + message.getClass().getSimpleName() + "!", e);
                            Reflect.ReflectionResponse<String> response = Reflect.getStringRepresentation(message, false, new Class[0]);
                            if (response.hasResult()) {
                                PacketInterceptor.this.logMessage(Level.SEVERE, "Packet dump: " + (String)response.getValue(), null);
                            } else {
                                PacketInterceptor.this.logMessage(Level.SEVERE, "Failed to retrieve packet dump!", response.getException());
                            }
                            result[0] = true;
                        }
                        result[1] = true;
                        synchronized (result) {
                            result.notifyAll();
                        }
                    }
                }).runTask(PacketInterceptor.this.plugin);
                synchronized (result) {
                    while (!result[1])
                        result.wait();
                }
                if (result[0])
                    super.channelRead(context, message);
            } else {
                try {
                    if (PacketInterceptor.this.packetReadingAsync(this.player, message, message.getClass().getSimpleName()))
                        super.channelRead(context, message);
                } catch (Exception e) {
                    PacketInterceptor.this.logMessage(Level.SEVERE, "An error occurred while handling packet " + message.getClass().getSimpleName() + "!", e);
                    Reflect.ReflectionResponse<String> response = Reflect.getStringRepresentation(message, false, new Class[0]);
                    if (response.hasResult()) {
                        PacketInterceptor.this.logMessage(Level.SEVERE, "Packet dump: " + (String)response.getValue(), null);
                    } else {
                        PacketInterceptor.this.logMessage(Level.SEVERE, "Failed to retrieve packet dump!", response.getException());
                    }
                    super.channelRead(context, message);
                }
            }
        }
    }
}
