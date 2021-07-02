package obed.me.nametag.packet;

import org.bukkit.entity.Player;

public interface IPacketHandler {
    void sendTabListRemovePacket(Player paramPlayer1, Player paramPlayer2);

    void sendTabListAddPacket(Player paramPlayer1, GameProfileWrapper paramGameProfileWrapper, Player paramPlayer2);

    void sendEntityDestroyPacket(Player paramPlayer1, Player paramPlayer2);

    void sendNamedEntitySpawnPacket(Player paramPlayer1, Player paramPlayer2);

    void sendEntityEquipmentPacket(Player paramPlayer1, Player paramPlayer2);

    void sendScoreboardRemovePacket(String paramString1, Player paramPlayer, String paramString2);

    void sendScoreboardAddPacket(String paramString1, Player paramPlayer, String paramString2);

    GameProfileWrapper getDefaultPlayerProfile(Player paramPlayer);

    void shutdown();
}
