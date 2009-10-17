/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.events.server.session;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.EdgePermCheck;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.dao.HibernateDAOFactory;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
@Repository
public class AlertDefinitionDAO extends HibernateDAO<AlertDefinition> {
    
      private PermissionManager permissionManager;
      private  ActionDAO actDAO;
      private TriggerDAO tDAO;
    
       private AlertConditionDAO alertConditionDAO;
   
       private ResourceDAO rDao;
       
      @Autowired 
    public AlertDefinitionDAO(
                              SessionFactory f,
                              PermissionManager permissionManager,
                              ActionDAO actDAO,
                              TriggerDAO tDAO,
                              AlertConditionDAO alertConditionDAO,
                              ResourceDAO rDao)
    {
        super(AlertDefinition.class, f);
        this.permissionManager = permissionManager;
        this.actDAO = actDAO;
        this.tDAO = tDAO;
        this.alertConditionDAO = alertConditionDAO;
        this.rDao = rDao;
    }

    private static final String[] MANAGE_ALERTS_OPS = new String[] {
        AuthzConstants.platformOpManageAlerts,
        AuthzConstants.serverOpManageAlerts,
        AuthzConstants.serviceOpManageAlerts
    };

    public AlertDefinitionDAO(SessionFactory sessionFactory) {
        super(AlertDefinition.class, sessionFactory);
    }

    void remove(AlertDefinition def) {
        super.remove(def.getAlertDefinitionState());
        super.remove(def);
    }

    
    public List<AlertDefinition> findAllByResource(Resource r) {
        return createCriteria().add(Restrictions.eq("resource", r)).list();
    }

    public List<AlertDefinition> findAllDeletedResources() {
        return createCriteria()
            .add(Restrictions.isNull("resource"))
            .add(Restrictions.eq("deleted", Boolean.TRUE))
            .list();
    }

    /**
     * Find the alert def for a given appdef entity and is child of the parent
     * alert def passed in
     * @param ent      Entity to find alert defs for
     * @param parentId ID of the parent
     */
    public AlertDefinition findChildAlertDef(Resource res, Integer parentId) {
        String sql = "FROM AlertDefinition a WHERE " +
            "a.resource = :res AND a.deleted = false AND a.parent.id = :parent";

        List defs = getSession().createQuery(sql)
            .setParameter("res", res)
            .setInteger("parent", parentId.intValue())
            .list();

        if (defs.size() == 0) {
            return null;
        }

        return (AlertDefinition) defs.get(0);
    }

    /**
     * Find the alert def for a given appdef entity that is the child of the
     * parent alert def passed in, allowing for the query to return a stale copy
     * of the alert definition (for efficiency reasons).
     *
     * @param ent
     * @param parentId
     * @param allowStale <code>true</code> to allow stale copies of an alert
     *                   definition in the query results; <code>false</code> to
     *                   never allow stale copies, potentially always forcing a
     *                   sync with the database.
     * @return The alert definition or <code>null</code>.
     */
    public AlertDefinition findChildAlertDef(Resource res,
                                             Integer parentId,
                                             boolean allowStale) {
        Session session = this.getSession();
        FlushMode oldFlushMode = session.getFlushMode();

        try {
            if (allowStale) {
                session.setFlushMode(FlushMode.MANUAL);
            }

            return findChildAlertDef(res, parentId);
        } finally {
            session.setFlushMode(oldFlushMode);
        }
    }

    public AlertDefinition findById(Integer id) {
        return (AlertDefinition) super.findById(id);
    }

    /**
     * Find an alert definition by Id, loading from the given session.
     *
     * @param id The alert definition Id.
     * @param session The session to use for loading the alert definition.
     * @return The alert definition.
     * @throws ObjectNotFoundException if no alert definition with the give Id exists.
     */
    public AlertDefinition findById(Integer id, Session session) {
        return (AlertDefinition)session.load(getPersistentClass(), id);
    }

    /**
     * Find an alert definition by Id, loading from the current session.
     *
     * @param id The alert definition Id.
     * @return The alert definition or <code>null</code> if no alert definition
     *         exists with the given Id.
     */
    public AlertDefinition get(Integer id) {
        return (AlertDefinition)super.get(id);
    }

    private List<AlertDefinition> findByResource(Resource res, String sort, boolean asc) {
        String sql = "from AlertDefinition a where a.resource = :res and " +
            "a.deleted = false order by a." + sort + (asc ? " ASC" : " DESC");

        return getSession().createQuery(sql)
            .setParameter("res", res)
            .list();
    }

    public List<AlertDefinition> findByResource(Resource res) {
        return findByResource(res, true);
    }

    public List<AlertDefinition> findByResource(Resource res, boolean asc) {
        return findByResource(res, "name", asc);
    }

    public List<AlertDefinition> findByResourceSortByCtime(Resource res, boolean asc) {
        return findByResource(res, "ctime", asc);
    }

    /**
     * Return all alert definitions for the given resource and its descendants
     * @param res the root resource
     * @return
     */
    public List<AlertDefinition> findByRootResource(AuthzSubject subject, Resource r) {
        EdgePermCheck wherePermCheck =
           permissionManager.makePermCheckHql("rez", true);
        String hql = "select ad from AlertDefinition ad join ad.resource rez "
                        + wherePermCheck
                        + " and ad.deleted = false and ad.resource is not null ";

        Query q = createQuery(hql);

        return wherePermCheck
            .addQueryParameters(q, subject, r, 0,
                                Arrays.asList(MANAGE_ALERTS_OPS)).list();
    }

    void save(AlertDefinition def) {
        super.save(def);

        // Make sure there's a valid alert definition state
        if (def.getAlertDefinitionState() == null) {
            AlertDefinitionState state = new AlertDefinitionState(def);
            def.setAlertDefinitionState(state);
            super.save(state);
        }
    }

    int deleteByAlertDefinition(AlertDefinition def) {
        String sql = "update AlertDefinition " +
        		     "set escalation = null, deleted = true, parent = null, " +
        		         "active = false where parent = :def";

        int ret = getSession().createQuery(sql).setParameter("def", def)
                              .executeUpdate();
        def.getChildrenBag().clear();

        return ret;
    }

    void setAlertDefinitionValue(AlertDefinition def, AlertDefinitionValue val)
    {
       
       

        // Set parent alert definition
        if (val.parentIdHasBeenSet() && val.getParentId() != null) {
            def.setParent(findById(val.getParentId()));
        }

        setAlertDefinitionValueNoRels(def, val);

        // def.set the resource based on the entity ID
     
        // Don't need to synch the Resource with the db since changes
        // to the Resource aren't cascaded on saving the AlertDefinition.
        Integer authzTypeId;
        if (EventConstants.TYPE_ALERT_DEF_ID.equals(val.getParentId())) {
            switch(val.getAppdefType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                authzTypeId = AuthzConstants.authzPlatformProto;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                authzTypeId = AuthzConstants.authzServerProto;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                authzTypeId = AuthzConstants.authzServiceProto;
                break;
            default:
                throw new IllegalArgumentException("Type " + val.getAppdefType()
                                                   + " is not a valid type");
            }
        }
        else {
            AppdefEntityID aeid = new AppdefEntityID(val.getAppdefType(),
                                                     val.getAppdefId());
            authzTypeId = aeid.getAuthzTypeId();
        }
        def.setResource(rDao.findByInstanceId(authzTypeId, val.getAppdefId(),
                                              true));

        for (Iterator i=val.getAddedTriggers().iterator(); i.hasNext(); ) {
            RegisteredTriggerValue tVal = (RegisteredTriggerValue) i.next();
            def.addTrigger(tDAO.findById(tVal.getId()));
        }

        for (Iterator i=val.getRemovedTriggers().iterator(); i.hasNext(); ) {
            RegisteredTriggerValue tVal = (RegisteredTriggerValue)i.next();
            def.removeTrigger(tDAO.findById(tVal.getId()));
        }

        for (Iterator i=val.getAddedConditions().iterator(); i.hasNext(); ) {
            AlertConditionValue cVal = (AlertConditionValue)i.next();
            def.addCondition(alertConditionDAO.findById(cVal.getId()));
        }

        for (Iterator i=val.getRemovedConditions().iterator(); i.hasNext(); ) {
            AlertConditionValue cVal = (AlertConditionValue)i.next();
            def.removeCondition(alertConditionDAO.findById(cVal.getId()));
        }

        for (Iterator i=val.getAddedActions().iterator(); i.hasNext(); ) {
            ActionValue aVal = (ActionValue)i.next();
            def.addAction(actDAO.findById(aVal.getId()));
        }

        for (Iterator i=val.getRemovedActions().iterator(); i.hasNext(); ) {
            ActionValue aVal = (ActionValue)i.next();
            def.removeAction(actDAO.findById(aVal.getId()));
        }
    }

    /**
     * duplicates all the values obtained from master into clone.
     * the active and enabled fields are taken from master.getActive()
     * @param clone {@link AlertDefinition} set all of clone's values obtained
     * from master.
     * @param master {@link AlertDefinitionValue} object to retrieve values from
     * in order to update clone.  Object does not change.
     */
    void setAlertDefinitionValueNoRels(final AlertDefinition clone,
                                       final AlertDefinitionValue master) {
      

        clone.setName(master.getName());
        clone.setDescription(master.getDescription());

        // from bug http://jira.hyperic.com/browse/HQ-1636
        // setActiveStatus() should be governed by active NOT enabled field
        clone.setActiveStatus(master.getActive());

        clone.setWillRecover(master.getWillRecover());
        clone.setNotifyFiltered(master.getNotifyFiltered() );
        clone.setControlFiltered(master.getControlFiltered() );
        clone.setPriority(master.getPriority());

        clone.setFrequencyType(master.getFrequencyType());
        clone.setCount(new Long(master.getCount()));
        clone.setRange(new Long(master.getRange()));
        clone.setDeleted(master.getDeleted());
    }

    List<AlertDefinition> findDefinitions(AuthzSubject subj, AlertSeverity minSeverity,
                         Boolean enabled, boolean excludeTypeBased,
                         PageInfo pInfo)
    {
        String sql = PermissionManagerFactory.getInstance().getAlertDefsHQL();

        sql += " and d.deleted = false and d.resource is not null ";
        if (enabled != null) {
            sql += " and d.enabled = " +
                   (enabled.booleanValue() ? "true" : "false");
        }

        sql += " and (d.parent is null";
        if (excludeTypeBased) {
            sql += ") ";
        } else {
            sql += " or not d.parent.id = 0) ";
        }

        sql += getOrderByClause(pInfo);

        Query q = getSession().createQuery(sql)
            .setInteger("priority", minSeverity.getCode());

        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subj.getId().intValue())
             .setParameterList("ops", MANAGE_ALERTS_OPS);
        }

        return pInfo.pageResults(q).list();
    }

    private String getOrderByClause(PageInfo pInfo) {
        AlertDefSortField sort = (AlertDefSortField)pInfo.getSort();
        String res = " order by " + sort.getSortString("d", "r") +
            (pInfo.isAscending() ? "" : " DESC");

        if (!sort.equals(AlertDefSortField.CTIME)) {
            res += ", " + AlertDefSortField.CTIME.getSortString("d", "r") +
                   " DESC";
        }
        return res;
    }

    List<AlertDefinition> findTypeBased(Boolean enabled, PageInfo pInfo) {
        String sql = "from AlertDefinition d " +
            "where d.deleted = false and d.parent.id = 0 ";

        if (enabled != null) {
            sql += " and d.enabled = " +
                (enabled.booleanValue() ? "true" : "false");
        }
        sql += getOrderByClause(pInfo);

        Query q = getSession().createQuery(sql);

        return pInfo.pageResults(q).list();
    }

    List<AlertDefinition> getUsing(Escalation e) {
        return createCriteria().add(Restrictions.eq("escalation", e)).list();
    }

    boolean isEnabled(Integer id) {
        return ((Boolean) getSession()
            .createQuery("select enabled from AlertDefinition"+
            		     " where id = " + id)
            .uniqueResult()).booleanValue();
    }

    int setChildrenActive(AlertDefinition def, boolean active) {
        return createQuery("update AlertDefinition set active = :active, " +
                           "enabled = :active, mtime = :mtime " +
                           "where parent = :def")
            .setBoolean("active", active)
            .setLong("mtime", System.currentTimeMillis())
            .setParameter("def", def)
            .executeUpdate();
    }

    int getNumActiveDefs() {
        String hql = "select count(*) from AlertDefinition where active = true and deleted = false and " +
        		"(parent_id is null or parent_id > 0)";
        return ((Number)createQuery(hql)
            .setCacheable(true)
            .setCacheRegion("AlertDefinition.getNumActiveDefs")
            .uniqueResult()).intValue();
    }

    int setChildrenEscalation(AlertDefinition def, Escalation esc) {
        return createQuery("update AlertDefinition set escalation = :esc, " +
                           "mtime = :mtime where parent = :def")
            .setParameter("esc", esc)
            .setLong("mtime", System.currentTimeMillis())
            .setParameter("def", def)
            .executeUpdate();
    }

}
