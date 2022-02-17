package de.polocloud.base.group;

import de.polocloud.api.network.packet.QueryPacket;
import de.polocloud.api.network.packet.group.ServiceGroupExecutePacket;
import de.polocloud.api.network.packet.group.ServiceGroupUpdatePacket;
import de.polocloud.base.Base;
import de.polocloud.api.CloudAPI;
import de.polocloud.api.event.group.CloudServiceGroupUpdateEvent;
import de.polocloud.api.groups.IServiceGroup;
import de.polocloud.api.groups.impl.AbstractGroupManager;
import de.polocloud.database.ICloudDatabaseProvider;
import de.polocloud.network.NetworkType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public final class SimpleGroupManager extends AbstractGroupManager {

    private final ICloudDatabaseProvider database;

    public SimpleGroupManager() {
        this.database = Base.getInstance().getDatabaseManager().getProvider();

        // loading all database groups
        this.getAllCachedServiceGroups().addAll(this.database.getAllServiceGroups());

        CloudAPI.getInstance().getPacketHandler().registerPacketListener(ServiceGroupExecutePacket.class, (channelHandlerContext, packet) -> {
            if (packet.getExecutorType().equals(ServiceGroupExecutePacket.Executor.CREATE)) {
                getAllCachedServiceGroups().add(packet.getGroup());
                Base.getInstance().getGroupTemplateService().createTemplateFolder(packet.getGroup());
                Base.getInstance().getQueueService().checkForQueue();
            } else {
                this.getAllCachedServiceGroups().remove(packet.getGroup());
            }
        });

        CloudAPI.getInstance().getEventHandler().registerEvent(CloudServiceGroupUpdateEvent.class, event ->
            Base.getInstance().getQueueService().checkForQueue());

        CloudAPI.getInstance().getLogger().log("§7Loading following groups: §b"
            + this.getAllCachedServiceGroups().stream().map(IServiceGroup::getName).collect(Collectors.joining("§7, §b")));
    }

    @Override
    public void addServiceGroup(final @NotNull IServiceGroup serviceGroup) {
        this.database.addGroup(serviceGroup);
        Base.getInstance().getNode().sendPacketToAll(new ServiceGroupExecutePacket(serviceGroup, ServiceGroupExecutePacket.Executor.CREATE));
        super.addServiceGroup(serviceGroup);
    }


    @Override
    public void removeServiceGroup(final @NotNull IServiceGroup serviceGroup) {
        this.database.removeGroup(serviceGroup);
        Base.getInstance().getNode().sendPacketToAll(new ServiceGroupExecutePacket(serviceGroup, ServiceGroupExecutePacket.Executor.REMOVE));
        super.removeServiceGroup(serviceGroup);
    }

    @Override
    public void updateServiceGroup(@NotNull IServiceGroup serviceGroup) {
        final ServiceGroupUpdatePacket packet = new ServiceGroupUpdatePacket(serviceGroup);
        // update all other nodes and this service groups
        Base.getInstance().getNode().sendPacketToType(new QueryPacket(packet, QueryPacket.QueryState.SECOND_RESPONSE), NetworkType.NODE);
        // update own service group caches
        Base.getInstance().getNode().sendPacketToType(packet, NetworkType.WRAPPER);

        Base.getInstance().getQueueService().checkForQueue();
    }

}
