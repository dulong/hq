package org.hyperic.hq.inventory.data;

import org.hyperic.hq.inventory.domain.ResourceType;

/**
 * 
 * Repository for lookup of {@link ResourceType}s
 * @author jhickey
 * @author dcruchfield
 * 
 */
public interface ResourceTypeDao extends GenericDao<ResourceType> {

    /**
     * 
     * @return The root ResourceType to use when making associations (for
     *         traversal of rootless objs)
     */
    ResourceType findRoot();

    /**
     * Persists a new ResourceType
     * @param resourceType The new resource type
     */
    void persist(ResourceType resourceType);

    /**
     * Persists a new ResourceType to use as the Root ResourceType for traversal
     * of rootless objects
     * @param resource The new root ResourceType
     */
    void persistRoot(ResourceType resourceType);
}
