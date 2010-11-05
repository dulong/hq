// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.hyperic.hq.inventory.domain;

import org.hyperic.hq.inventory.domain.PlatformTypeNGDataOnDemand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

privileged aspect PlatformTypeNGIntegrationTest_Roo_IntegrationTest {
    
    declare @type: PlatformTypeNGIntegrationTest: @RunWith(SpringJUnit4ClassRunner.class);
    
    declare @type: PlatformTypeNGIntegrationTest: @ContextConfiguration(locations = "classpath:/META-INF/spring/applicationContext.xml");
    
    declare @type: PlatformTypeNGIntegrationTest: @Transactional;
    
    @Autowired
    private PlatformTypeNGDataOnDemand PlatformTypeNGIntegrationTest.dod;
    
    @Test
    public void PlatformTypeNGIntegrationTest.testCountPlatformTypeNGs() {
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to initialize correctly", dod.getRandomPlatformTypeNG());
        long count = org.hyperic.hq.inventory.domain.PlatformTypeNG.countPlatformTypeNGs();
        org.junit.Assert.assertTrue("Counter for 'PlatformTypeNG' incorrectly reported there were no entries", count > 0);
    }
    
    @Test
    public void PlatformTypeNGIntegrationTest.testFindPlatformTypeNG() {
        org.hyperic.hq.inventory.domain.PlatformTypeNG obj = dod.getRandomPlatformTypeNG();
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to initialize correctly", obj);
        java.lang.Long id = obj.getId();
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to provide an identifier", id);
        obj = org.hyperic.hq.inventory.domain.PlatformTypeNG.findPlatformTypeNG(id);
        org.junit.Assert.assertNotNull("Find method for 'PlatformTypeNG' illegally returned null for id '" + id + "'", obj);
        org.junit.Assert.assertEquals("Find method for 'PlatformTypeNG' returned the incorrect identifier", id, obj.getId());
    }
    
    @Test
    public void PlatformTypeNGIntegrationTest.testFindAllPlatformTypeNGs() {
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to initialize correctly", dod.getRandomPlatformTypeNG());
        long count = org.hyperic.hq.inventory.domain.PlatformTypeNG.countPlatformTypeNGs();
        org.junit.Assert.assertTrue("Too expensive to perform a find all test for 'PlatformTypeNG', as there are " + count + " entries; set the findAllMaximum to exceed this value or set findAll=false on the integration test annotation to disable the test", count < 250);
        java.util.List<org.hyperic.hq.inventory.domain.PlatformTypeNG> result = org.hyperic.hq.inventory.domain.PlatformTypeNG.findAllPlatformTypeNGs();
        org.junit.Assert.assertNotNull("Find all method for 'PlatformTypeNG' illegally returned null", result);
        org.junit.Assert.assertTrue("Find all method for 'PlatformTypeNG' failed to return any data", result.size() > 0);
    }
    
    @Test
    public void PlatformTypeNGIntegrationTest.testFindPlatformTypeNGEntries() {
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to initialize correctly", dod.getRandomPlatformTypeNG());
        long count = org.hyperic.hq.inventory.domain.PlatformTypeNG.countPlatformTypeNGs();
        if (count > 20) count = 20;
        java.util.List<org.hyperic.hq.inventory.domain.PlatformTypeNG> result = org.hyperic.hq.inventory.domain.PlatformTypeNG.findPlatformTypeNGEntries(0, (int) count);
        org.junit.Assert.assertNotNull("Find entries method for 'PlatformTypeNG' illegally returned null", result);
        org.junit.Assert.assertEquals("Find entries method for 'PlatformTypeNG' returned an incorrect number of entries", count, result.size());
    }
    
    @Test
    public void PlatformTypeNGIntegrationTest.testFlush() {
        org.hyperic.hq.inventory.domain.PlatformTypeNG obj = dod.getRandomPlatformTypeNG();
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to initialize correctly", obj);
        java.lang.Long id = obj.getId();
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to provide an identifier", id);
        obj = org.hyperic.hq.inventory.domain.PlatformTypeNG.findPlatformTypeNG(id);
        org.junit.Assert.assertNotNull("Find method for 'PlatformTypeNG' illegally returned null for id '" + id + "'", obj);
        boolean modified =  dod.modifyPlatformTypeNG(obj);
        java.lang.Integer currentVersion = obj.getVersion();
        obj.flush();
        org.junit.Assert.assertTrue("Version for 'PlatformTypeNG' failed to increment on flush directive", (currentVersion != null && obj.getVersion() > currentVersion) || !modified);
    }
    
    @Test
    public void PlatformTypeNGIntegrationTest.testMerge() {
        org.hyperic.hq.inventory.domain.PlatformTypeNG obj = dod.getRandomPlatformTypeNG();
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to initialize correctly", obj);
        java.lang.Long id = obj.getId();
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to provide an identifier", id);
        obj = org.hyperic.hq.inventory.domain.PlatformTypeNG.findPlatformTypeNG(id);
        boolean modified =  dod.modifyPlatformTypeNG(obj);
        java.lang.Integer currentVersion = obj.getVersion();
        org.hyperic.hq.inventory.domain.PlatformTypeNG merged = (org.hyperic.hq.inventory.domain.PlatformTypeNG) obj.merge();
        obj.flush();
        org.junit.Assert.assertEquals("Identifier of merged object not the same as identifier of original object", merged.getId(), id);
        org.junit.Assert.assertTrue("Version for 'PlatformTypeNG' failed to increment on merge and flush directive", (currentVersion != null && obj.getVersion() > currentVersion) || !modified);
    }
    
    @Test
    public void PlatformTypeNGIntegrationTest.testPersist() {
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to initialize correctly", dod.getRandomPlatformTypeNG());
        org.hyperic.hq.inventory.domain.PlatformTypeNG obj = dod.getNewTransientPlatformTypeNG(Integer.MAX_VALUE);
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to provide a new transient entity", obj);
        org.junit.Assert.assertNull("Expected 'PlatformTypeNG' identifier to be null", obj.getId());
        obj.persist();
        obj.flush();
        org.junit.Assert.assertNotNull("Expected 'PlatformTypeNG' identifier to no longer be null", obj.getId());
    }
    
    @Test
    public void PlatformTypeNGIntegrationTest.testRemove() {
        org.hyperic.hq.inventory.domain.PlatformTypeNG obj = dod.getRandomPlatformTypeNG();
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to initialize correctly", obj);
        java.lang.Long id = obj.getId();
        org.junit.Assert.assertNotNull("Data on demand for 'PlatformTypeNG' failed to provide an identifier", id);
        obj = org.hyperic.hq.inventory.domain.PlatformTypeNG.findPlatformTypeNG(id);
        obj.remove();
        obj.flush();
        org.junit.Assert.assertNull("Failed to remove 'PlatformTypeNG' with identifier '" + id + "'", org.hyperic.hq.inventory.domain.PlatformTypeNG.findPlatformTypeNG(id));
    }
    
}