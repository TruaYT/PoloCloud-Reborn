package de.bytemc.cloud.plugin.events.proxy;

import com.google.common.collect.Lists;
import de.bytemc.cloud.api.CloudAPI;
import de.bytemc.cloud.api.fallback.FallbackHandler;
import de.bytemc.cloud.api.player.ICloudPlayer;
import de.bytemc.cloud.api.player.ICloudPlayerManager;
import de.bytemc.cloud.api.services.IService;
import de.bytemc.cloud.api.services.utils.ServiceState;
import de.bytemc.cloud.api.services.utils.ServiceVisibility;
import de.bytemc.cloud.wrapper.service.ServiceManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProxyEvents implements Listener {

    private final ICloudPlayerManager playerManager;

    public ProxyEvents() {
        this.playerManager = CloudAPI.getInstance().getCloudPlayerManager();
    }

    //TODO WHITELIST
    private static final List<String> whitelistedPlayers = Lists.newArrayList(
        "HttpMarco", "Siggii", "xImNoxh", "BauHD", "FallenBreak", "ipommes", "SilenceCode", "outroddet_", "Forumat", "Einfxch", "NervigesLilli");

    @EventHandler
    public void handle(PreLoginEvent event) {

        final String name = event.getConnection().getName();

        if (name.startsWith("BauHD")) return;

        if (!whitelistedPlayers.contains(event.getConnection().getName())) {
            event.setCancelReason(new TextComponent("§cDu besitzt momentan keinen Zuganng, um das §nNetzwerk §czu betreten."));
            event.setCancelled(true);
            return;
        }

        if (!FallbackHandler.isFallbackAvailable()) {
            event.setCancelReason(new TextComponent("§cEs konnte kein passender Fallback gefunden werden."));
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void handle(LoginEvent event) {
        playerManager.registerCloudPlayer(event.getConnection().getUniqueId(), event.getConnection().getName());
    }

    @EventHandler
    public void handle(ServerConnectEvent event) {
        ICloudPlayer cloudPlayer = CloudAPI.getInstance().getCloudPlayerManager().getCloudPlayerByNameOrNull(event.getPlayer().getName());
        if (event.getTarget().getName().equalsIgnoreCase("fallback")) {
            FallbackHandler.getLobbyFallbackOrNull().ifPresentOrElse(it -> {
                event.setTarget(ProxyServer.getInstance().getServerInfo(it.getName()));
                assert cloudPlayer != null;
                cloudPlayer.setServer(it);
                cloudPlayer.setProxyServer(((ServiceManager) CloudAPI.getInstance().getServiceManager()).thisService());
                cloudPlayer.update();
            }, () -> {
                event.getPlayer().disconnect(new TextComponent("§cEs konnte kein passender Fallback gefunden werden."));
            });
        } else {
            assert cloudPlayer != null;
            cloudPlayer.setServer(Objects.requireNonNull(CloudAPI.getInstance().getServiceManager().getServiceByNameOrNull(event.getTarget().getName())));
            cloudPlayer.setProxyServer(((ServiceManager) CloudAPI.getInstance().getServiceManager()).thisService());
            cloudPlayer.update();
        }
    }

    @EventHandler
    public void handle(PlayerDisconnectEvent event) {
        playerManager.unregisterCloudPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void handle(ProxyPingEvent event) {
        final ServerPing response = event.getResponse();
        final ServerPing.Players players = response.getPlayers();

        response.setPlayers(new ServerPing.Players(((ServiceManager) CloudAPI.getInstance().getServiceManager()).thisService().getMaxPlayers(), playerManager.getCloudPlayerOnlineAmount(), players.getSample()));
        event.setResponse(response);
    }

    @EventHandler
    public void handle(final ServerKickEvent event) {
        this.getFallback(event.getPlayer()).ifPresent(serverInfo -> {
            event.setCancelled(true);
            event.setCancelServer(serverInfo);
        });
    }

    private Optional<ServerInfo> getFallback(final ProxiedPlayer player) {
        return CloudAPI.getInstance().getServiceManager().getAllCachedServices().stream()
            .filter(service -> service.getServiceState() == ServiceState.ONLINE)
            .filter(service -> service.getServiceVisibility() == ServiceVisibility.VISIBLE)
            .filter(service -> !service.getServiceGroup().getGameServerVersion().isProxy())
            .filter(service -> service.getServiceGroup().isFallbackGroup())
            .filter(service -> (player.getServer() == null || !player.getServer().getInfo().getName().equals(service.getName())))
            .min(Comparator.comparing(IService::getOnlinePlayers))
            .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()));
    }

}
