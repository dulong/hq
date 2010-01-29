package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

import junit.framework.TestCase;

/**
 * Unit test of the {@link RecoveryConditionEvaluator}
 * @author jhickey
 * 
 */
public class RecoveryConditionEvaluatorTest
    extends TestCase
{

    private ExecutionStrategy executionStrategy;

    private Map events = new LinkedHashMap();

    private static final Integer TEST_ALERT_DEF_ID = Integer.valueOf(1234);

    public void setUp() throws Exception {
        super.setUp();
        this.executionStrategy = EasyMock.createMock(ExecutionStrategy.class);

    }

    /**
     * Verifies successful initialize of the evaluator with persisted state
     */
    public void testInitialize() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();
        MockEvent mockEvent = new MockEvent(2l, 3);
        // Recovery condition occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);
        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);
        evaluator.initialize(triggerFired);
        assertEquals(triggerFired, evaluator.getState());
    }

    /**
     * Verifies nothing blows up initializing with a non-TriggerFiredEvent
     */
    public void testInitializeNotTFE() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);
        evaluator.initialize(null);
        assertNull(evaluator.getState());
    }

    /**
     * Verifies that trigger is fired if newer recovery conditions come in after
     * alert is fired
     */
    public void testMoreRecoveryConditionsAfterAlertFired() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // Recovery condition occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 4 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        MockEvent mockEvent2 = new MockEvent(12l, 13);
        // Recovery condition occurred 2 minutes ago
        mockEvent2.setTimestamp(System.currentTimeMillis() - (2 * 60 * 1000));
        TriggerFiredEvent triggerFired2 = new TriggerFiredEvent(trigger2Id, mockEvent2);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(),
                                                                                  new TriggerFiredEvent[] { triggerFired,
                                                                                                           alertTriggerFired });

        executionStrategy.conditionsSatisfied(event);
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerFired(alertTriggerFired);
        evaluator.triggerFired(triggerFired2);
        EasyMock.verify(executionStrategy);
        Map expectedEvents = new LinkedHashMap();
        expectedEvents.put(trigger2Id, triggerFired2);
        assertEquals(expectedEvents, events);
        assertNull(evaluator.getState());
    }

    /**
     * Verifies that trigger will not fire if older alert occurs after trigger
     * is fired
     */
    public void testOlderAlertFiredAfterClear() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // Recovery condition occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 4 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        MockEvent alertFired2 = new MockEvent(10l, 11);
        // alert fired 5 minutes ago
        alertFired2.setTimestamp(System.currentTimeMillis() - (5 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired2 = new TriggerFiredEvent(alertTriggerId, alertFired2);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(),
                                                                                  new TriggerFiredEvent[] { triggerFired,
                                                                                                           alertTriggerFired });

        executionStrategy.conditionsSatisfied(event);
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerFired(alertTriggerFired);
        evaluator.triggerFired(alertTriggerFired2);
        EasyMock.verify(executionStrategy);
        assertTrue(events.isEmpty());
        assertEquals(alertTriggerFired2, evaluator.getState());
    }

    /**
     * Verifies that last alert fired is not updated by an older alert
     */
    public void testOlderAlertFiredBeforeClear() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 4 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        MockEvent alertFired2 = new MockEvent(10l, 11);
        // alert fired 5 minutes ago
        alertFired2.setTimestamp(System.currentTimeMillis() - (5 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired2 = new TriggerFiredEvent(alertTriggerId, alertFired2);

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(alertTriggerFired);
        evaluator.triggerFired(alertTriggerFired2);
        EasyMock.verify(executionStrategy);
        assertTrue(events.isEmpty());
        assertEquals(alertTriggerFired, evaluator.getState());
    }

    /**
     * Verifies that trigger fires when recovery conditions not fired is
     * received after recovery conditions fired, but recovery conditions fired
     * is more recent
     */
    public void testRecoveryAlertConditionsNotFiredAfterFired() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // Recovery condition occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent mockEvent2 = new MockEvent(Long.valueOf(4l), Integer.valueOf(5));
        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(trigger2Id);
        // Recovery condition unoccurred 5 minutes ago
        triggerNotFired.setTimestamp(System.currentTimeMillis() - (5 * 60 * 1000));

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 4 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(),
                                                                                  new TriggerFiredEvent[] { triggerFired,
                                                                                                           alertTriggerFired });

        executionStrategy.conditionsSatisfied(event);

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerNotFired(triggerNotFired);
        evaluator.triggerFired(alertTriggerFired);
        EasyMock.verify(executionStrategy);

        assertTrue(events.isEmpty());
        assertNull(evaluator.getState());
    }

    /**
     * Verifies the trigger fires when recovery conditions are met
     */
    public void testRecoveryCondition() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // Recovery condition occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 4 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(),
                                                                                  new TriggerFiredEvent[] { triggerFired,
                                                                                                           alertTriggerFired });

        executionStrategy.conditionsSatisfied(event);
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerFired(alertTriggerFired);
        EasyMock.verify(executionStrategy);
        assertTrue(events.isEmpty());
        assertNull(evaluator.getState());
    }

    /**
     * Verifies that trigger is fired if recovery conditions are received before
     * the alert, but the alert is less recent
     */
    public void testRecoveryConditionReceivedBeforeAlertFired() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent mockEvent = new MockEvent(Long.valueOf(2l), Integer.valueOf(3));
        // Recovery condition occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 4 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(),
                                                                                  new TriggerFiredEvent[] { triggerFired,
                                                                                                           alertTriggerFired });

        executionStrategy.conditionsSatisfied(event);

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerFired(alertTriggerFired);
        EasyMock.verify(executionStrategy);

        Map expectedEvents = new LinkedHashMap();
        expectedEvents.put(trigger2Id, triggerFired);

        assertTrue(events.isEmpty());
        assertNull(evaluator.getState());
    }

    /**
     * Verifies that trigger does not fire if timestamps on any of the required
     * recovery conditions are older than the alert fired
     */
    public void testRecoveryTriggerFiredBeforeAlert() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // Recovery condition occurred 4 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 3 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerFired(alertTriggerFired);
        EasyMock.verify(executionStrategy);

        Map expectedEvents = new LinkedHashMap();
        expectedEvents.put(trigger2Id, triggerFired);

        assertEquals(expectedEvents, events);
        assertEquals(alertTriggerFired, evaluator.getState());
    }

    /**
     * Verifies that trigger is fired if recovery conditions are met before and
     * after alert is fired
     */
    public void testRecoveryTriggerFiredBeforeAndAfterAlert() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent mockEvent = new MockEvent(Long.valueOf(2l), Integer.valueOf(3));
        // Recovery condition occurred 4 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 3 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        MockEvent mockEvent2 = new MockEvent(Long.valueOf(4l), Integer.valueOf(5));
        // Recovery condition occurred 1 minute ago
        mockEvent2.setTimestamp(System.currentTimeMillis() - (1 * 60 * 1000));
        TriggerFiredEvent triggerFired2 = new TriggerFiredEvent(trigger2Id, mockEvent2);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(),
                                                                                  new TriggerFiredEvent[] { triggerFired2,
                                                                                                           alertTriggerFired });

        executionStrategy.conditionsSatisfied(event);
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerFired(alertTriggerFired);
        evaluator.triggerFired(triggerFired2);
        EasyMock.verify(executionStrategy);

        assertTrue(events.isEmpty());
        assertNull(evaluator.getState());
    }

    /**
     * Verifies that trigger will fire if older alert comes in after newer one
     */
    public void testUpdateAlertFired() {
        Integer alertTriggerId = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        RecoveryConditionEvaluator evaluator = new RecoveryConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                              alertTriggerId,
                                                                              conditions,
                                                                              executionStrategy,
                                                                              events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // Recovery condition occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent alertFired = new MockEvent(3l, 4);
        // alert fired 4 minutes ago
        alertFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired = new TriggerFiredEvent(alertTriggerId, alertFired);

        MockEvent alertFired2 = new MockEvent(10l, 11);
        // alert fired 2 minutes ago
        alertFired2.setTimestamp(System.currentTimeMillis() - (2 * 60 * 1000));
        TriggerFiredEvent alertTriggerFired2 = new TriggerFiredEvent(alertTriggerId, alertFired2);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(),
                                                                                  new TriggerFiredEvent[] { triggerFired,
                                                                                                           alertTriggerFired });

        executionStrategy.conditionsSatisfied(event);
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerFired(alertTriggerFired);
        evaluator.triggerFired(alertTriggerFired2);
        EasyMock.verify(executionStrategy);
        assertTrue(events.isEmpty());
        assertEquals(alertTriggerFired2, evaluator.getState());
    }

}