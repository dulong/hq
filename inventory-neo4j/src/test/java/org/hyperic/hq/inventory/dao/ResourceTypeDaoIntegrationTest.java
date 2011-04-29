package org.hyperic.hq.inventory.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional("neoTxManager")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INF/spring/neo4j-context.xml",
                                   "classpath:org/hyperic/hq/inventory/InventoryIntegrationTest-context.xml" })
public class ResourceTypeDaoIntegrationTest {

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    private ResourceType type1;

    private ResourceType type2;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        this.type1 = new ResourceType("TestGroupType");
        resourceTypeDao.persist(type1);
        this.type2 = new ResourceType("Another Type");
        resourceTypeDao.persist(type2);
    }

    @Test
    public void testFindById() {
        assertEquals(type1, resourceTypeDao.findById(type1.getId()));
    }

    @Test
    public void testFindByIdNonExistent() {
        assertNull(resourceTypeDao.findById(98765));
    }

    @Test
    public void testFindAll() {
        Set<ResourceType> expected = new HashSet<ResourceType>();
        expected.add(type1);
        expected.add(type2);
        Set<ResourceType> actual = new HashSet<ResourceType>(resourceTypeDao.findAll());
        assertTrue(actual.containsAll(expected));
    }

    @Test
    public void testFindPaged() {
        List<ResourceType> resourceTypes = resourceTypeDao.find(1, 2);
        assertEquals(1, resourceTypes.size());
    }

    @Test
    public void testFindPagedBigMaxResult() {
        List<ResourceType> resourceTypes = resourceTypeDao.find(0, 20);
        assertEquals(2, resourceTypes.size());
    }

    @Test
    public void testFindPagedFirstResultLargerThanSize() {
        List<ResourceType> resourceTypes = resourceTypeDao.find(14, 20);
        assertEquals(0, resourceTypes.size());
    }

    @Test
    public void testCount() {
        assertEquals(new Long(2), resourceTypeDao.count());
    }

    @Test
    public void testFindByName() {
        assertEquals(type1, resourceTypeDao.findByName("TestGroupType"));
    }

    @Test
    public void testFindByNameNonExistent() {
        assertNull(resourceTypeDao.findByName("Fake Type"));
    }
    
    
    @Test
    public void testPersistRoot() {
        ResourceType resourceType1 = new ResourceType("Some Type");
        resourceTypeDao.persistRoot(resourceType1);
        assertEquals(resourceType1,resourceTypeDao.findRoot());
    }
    
    @Test
    public void testFindRootNoRoot() {
        assertNull(resourceTypeDao.findRoot());
    }
}