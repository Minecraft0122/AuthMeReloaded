package fr.xephi.authme.platform;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;

import java.net.SocketAddress;
import java.util.UUID;

/** Optional ViaVersion API linkage; loaded only when ViaVersion is enabled. */
final class ViaClientProtocol {

    private ViaClientProtocol() {
    }

    static int forPlayer(UUID playerId, SocketAddress address) {
        int protocol = forConnection(address);
        return protocol >= 0 ? protocol : Via.getAPI().getPlayerVersion(playerId);
    }

    static int forConnection(SocketAddress address) {
        if (address == null) {
            return -1;
        }
        for (UserConnection connection : Via.getManager().getConnectionManager().getConnections()) {
            if (!connection.isClientSide() && connection.getChannel() != null
                && address.equals(connection.getChannel().remoteAddress())) {
                return connection.getProtocolInfo().getProtocolVersion();
            }
        }
        return -1;
    }
}
