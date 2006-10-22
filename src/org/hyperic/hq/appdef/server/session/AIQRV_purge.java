/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.List;

import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.dao.DAOFactory;

import org.apache.commons.logging.Log;

/**
 * The AIQueueConstants.Q_DECISION_PURGE means to remove the resource
 * from the ai queue.
 */
public class AIQRV_purge implements AIQResourceVisitor {

    public void visitPlatform ( AIPlatform aiplatform,
                                AuthzSubjectValue subject, 
                                Log log, 
                                PlatformManagerLocal pmLocal,
                                ConfigManagerLocal configMgr,
                                CPropManagerLocal cpropMgr,
                                List createdResources  ) 
        throws AIQApprovalException, PermissionException {

        log.info("(purge) visiting platform: " + aiplatform.getId()
                 + " fqdn=" + aiplatform.getFqdn());
        DAOFactory.getDAOFactory().getAIPlatformDAO()
            .remove(aiplatform);
    }

    public void visitIp ( AIIp aiip,
                          AuthzSubjectValue subject, 
                          Log log, 
                          PlatformManagerLocal pmLocal )
        throws AIQApprovalException, PermissionException {
        log.info("(purge) visiting ip: " + aiip.getId()
                 + " addr=" + aiip.getAddress());
        DAOFactory.getDAOFactory().getAIIpDAO()
            .remove(aiip);
    }

    public void visitServer ( AIServer aiserver,
                              AuthzSubjectValue subject, 
                              Log log, 
                              PlatformManagerLocal pmLocal,
                              ServerManagerLocal smLocal,
                              ConfigManagerLocal configMgr,
                              CPropManagerLocal cpropMgr,
                              List createdResources )
        throws AIQApprovalException, PermissionException {
        log.info("(purge) visiting server: " + aiserver.getId()
                 + " AIID=" + aiserver.getAutoinventoryIdentifier());
        DAOFactory.getDAOFactory().getAIServerDAO()
            .remove(aiserver);
    }
}
