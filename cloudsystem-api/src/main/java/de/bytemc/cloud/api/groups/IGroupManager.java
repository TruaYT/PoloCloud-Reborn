package de.bytemc.cloud.api.groups;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface IGroupManager {

    /**
     * gets all cached service groups
     * @return the cached service groups
     */
    @NotNull List<IServiceGroup> getAllCachedServiceGroups();

    /**
     * adds a service group
     * @param serviceGroup the service group to add
     */
    void addServiceGroup(@NotNull IServiceGroup serviceGroup);

    /**
     * removes a service group
     * @param serviceGroup the service group to remove
     */
    void removeServiceGroup(@NotNull IServiceGroup serviceGroup);

    /**
     * gets a service group
     * @param name the name of the service group
     * @return the service group in an optional
     */
    default @NotNull Optional<IServiceGroup> getServiceGroupByName(@NotNull String name) {
        return this.getAllCachedServiceGroups().stream().filter(it -> it.getName().equalsIgnoreCase(name)).findAny();
    }

    /**
     * gets a service group
     * @param name the name of the service group
     * @return the service group or null when it not exists
     */
    default @Nullable IServiceGroup getServiceGroupByNameOrNull(@NotNull String name) {
        return this.getServiceGroupByName(name).orElse(null);
    }

    /**
     * checks if a group exists
     * @param name the name of the group
     * @return true if the group exists
     */
    default boolean isServiceGroupExists(@NotNull String name) {
        return this.getServiceGroupByNameOrNull(name) != null;
    }

    /**
     * gets all service groups by node
     * @param node the node
     * @return all services of the node
     */
    default @NotNull List<IServiceGroup> getServiceGroup(@NotNull String node) {
        return this.getAllCachedServiceGroups().stream()
            .filter(it -> it.getNode().equalsIgnoreCase(node))
            .collect(Collectors.toList());
    }

    /**
     * update a group
     * @param serviceGroup the group to update
     */
    void updateServiceGroup(@NotNull IServiceGroup serviceGroup);

}
