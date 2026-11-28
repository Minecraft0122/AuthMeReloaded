package fr.xephi.authme.platform;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Resolves client UI capabilities without treating protocol translation as native client support. */
public class PaperClientCapabilities {

    // Dialogs were introduced in Java Edition 1.21.6.
    private static final int FIRST_DIALOG_PROTOCOL = 771;

    public boolean supportsDialogs(Player player) {
        int protocol = isViaEnabled()
            ? ViaClientProtocol.forPlayer(player.getUniqueId(), player.getConnection().getAddress())
            : player.getProtocolVersion();
        return supportsDialogs(protocol);
    }

    public boolean supportsDialogs(PlayerConfigurationConnection connection) {
        // Before join Via's UUID index need not be populated. Match the actual transport
        // address (including port), never just a name, IP, or forwarded client address.
        int protocol = isViaEnabled()
            ? ViaClientProtocol.forConnection(connection.getAddress())
            : Bukkit.getUnsafe().getProtocolVersion();
        return supportsDialogs(protocol);
    }

    static boolean supportsDialogs(int protocol) {
        // An unknown translated protocol falls back to command authentication, not a
        // blocking dialog which the client may be unable to answer.
        return protocol >= FIRST_DIALOG_PROTOCOL;
    }

    private boolean isViaEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
    }
}
