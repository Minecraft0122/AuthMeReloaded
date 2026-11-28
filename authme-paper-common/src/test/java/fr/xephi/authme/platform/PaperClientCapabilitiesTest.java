package fr.xephi.authme.platform;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaManager;
import com.viaversion.viaversion.api.connection.ConnectionManager;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.Channel;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PaperClientCapabilitiesTest {

    private final PaperClientCapabilities capabilities = new PaperClientCapabilities();

    @Test
    void rejectsLegacyAndUnknownProtocolsButAcceptsDialogBoundaryAndNewerClients() {
        for (int protocol : new int[] {-1, 0, 47, 107, 340, 764, 770}) {
            assertFalse(PaperClientCapabilities.supportsDialogs(protocol), "protocol " + protocol);
        }
        assertTrue(PaperClientCapabilities.supportsDialogs(771));
        assertTrue(PaperClientCapabilities.supportsDialogs(1000));
    }

    @Test
    void usesNativePlayerProtocolWithoutLoadingViaWhenPluginIsAbsent() {
        try (var bukkit = mockStatic(Bukkit.class)) {
            PluginManager plugins = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            Player player = mock(Player.class);
            when(player.getProtocolVersion()).thenReturn(771);
            assertTrue(capabilities.supportsDialogs(player));
            when(player.getProtocolVersion()).thenReturn(47);
            assertFalse(capabilities.supportsDialogs(player));
        }
    }

    @Test
    void findsLegacyConnectionBeforeUuidIndexAndDistinguishesPortsOnSameIp() {
        InetSocketAddress legacyAddress = new InetSocketAddress("127.0.0.1", 12345);
        InetSocketAddress modernAddress = new InetSocketAddress("127.0.0.1", 12346);
        UserConnection legacy = connection(legacyAddress, 47);
        UserConnection modern = connection(modernAddress, 771);
        try (var bukkit = mockStatic(Bukkit.class); var via = mockStatic(Via.class)) {
            PluginManager plugins = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            when(plugins.isPluginEnabled("ViaVersion")).thenReturn(true);
            ViaManager manager = mock(ViaManager.class);
            ConnectionManager connections = mock(ConnectionManager.class);
            via.when(Via::getManager).thenReturn(manager);
            when(manager.getConnectionManager()).thenReturn(connections);
            when(connections.getConnections()).thenReturn(Set.of(legacy, modern));

            PlayerConfigurationConnection configuring = mock(PlayerConfigurationConnection.class);
            when(configuring.getAddress()).thenReturn(legacyAddress);
            assertFalse(capabilities.supportsDialogs(configuring));
            when(configuring.getAddress()).thenReturn(modernAddress);
            assertTrue(capabilities.supportsDialogs(configuring));
            when(configuring.getAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12347));
            assertFalse(capabilities.supportsDialogs(configuring));

            Player player = mock(Player.class);
            PlayerGameConnection playing = mock(PlayerGameConnection.class);
            when(player.getConnection()).thenReturn(playing);
            when(playing.getAddress()).thenReturn(legacyAddress);
            // Bukkit may report the translated/native protocol; Via must take precedence.
            lenient().when(player.getProtocolVersion()).thenReturn(1000);
            assertFalse(capabilities.supportsDialogs(player));
            verify(player, never()).getProtocolVersion();
            via.verify(Via::getAPI, never());
        }
    }

    private UserConnection connection(InetSocketAddress address, int protocol) {
        UserConnection connection = mock(UserConnection.class);
        Channel channel = mock(Channel.class);
        ProtocolInfo info = mock(ProtocolInfo.class);
        when(connection.getChannel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(address);
        when(connection.getProtocolInfo()).thenReturn(info);
        when(info.getProtocolVersion()).thenReturn(protocol);
        return connection;
    }
}
