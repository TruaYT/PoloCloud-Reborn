package de.bytemc.cloud.groups;

import de.bytemc.cloud.Base;
import de.bytemc.cloud.api.CloudAPI;
import de.bytemc.cloud.api.groups.IServiceGroup;
import de.bytemc.cloud.api.groups.impl.AbstractGroupManager;

import java.util.stream.Collectors;

public class SimpleGroupManager extends AbstractGroupManager {

    public SimpleGroupManager(){
        getAllCachedServiceGroups().addAll(Base.getInstance().getDatabaseManager().getDatabase().getAllServiceGroups());
        CloudAPI.getInstance().getLoggerProvider().logMessage("§7Loading following groups: §b" +
            String.join(", ", getAllCachedServiceGroups().stream().map(it -> it.getGroup()).collect(Collectors.joining())));
    }

    @Override
    public void addServiceGroup(IServiceGroup serviceGroup) {
        Base.getInstance().getDatabaseManager().getDatabase().addGroup(serviceGroup);
        super.addServiceGroup(serviceGroup);
    }


    @Override
    public void removeServiceGroup(IServiceGroup serviceGroup) {
        Base.getInstance().getDatabaseManager().getDatabase().removeGroup(serviceGroup);
        super.removeServiceGroup(serviceGroup);
    }
}
