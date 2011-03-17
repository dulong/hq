/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2011], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.product.server.session;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.session.AgentSynchronizer;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentPluginStatus;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusDAO;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusEnum;
import org.hyperic.hq.appdef.server.session.AgentPluginSyncRestartThrottle;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginDeployException;
import org.hyperic.hq.product.shared.PluginManager;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.emory.mathcs.backport.java.util.Collections;

@Service
@Transactional(readOnly=true)
public class PluginManagerImpl implements PluginManager, ApplicationContextAware {
    private static final Log log = LogFactory.getLog(PluginManagerImpl.class);
    
    private PluginDAO pluginDAO;
    private AgentPluginStatusDAO agentPluginStatusDAO;

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    private ApplicationContext ctx;

    private PermissionManager permissionManager;
    private AgentSynchronizer agentSynchronizer;
    private AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;

    @Autowired
    public PluginManagerImpl(PluginDAO pluginDAO, AgentPluginStatusDAO agentPluginStatusDAO,
                             PermissionManager permissionManager,
                             AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle,
                             AgentSynchronizer agentSynchronizer) {
        this.pluginDAO = pluginDAO;
        this.agentPluginStatusDAO = agentPluginStatusDAO;
        this.permissionManager = permissionManager;
        this.agentPluginSyncRestartThrottle = agentPluginSyncRestartThrottle;
        this.agentSynchronizer = agentSynchronizer;
    }
    
    public Plugin getByJarName(String jarName) {
        return pluginDAO.getByFilename(jarName);
    }
    
    @Transactional(readOnly=false)
    public void removePlugins(AuthzSubject subj, Collection<String> pluginFileNames)
    throws PermissionException {
        permissionManager.checkIsSuperUser(subj);
        final Collection<Agent> agents = agentPluginStatusDAO.getAutoUpdatingAgents();
        final File pluginDir = getPluginDir();
        for (final String filename : pluginFileNames) {
            final File plugin = new File(pluginDir.getAbsolutePath() + "/" + filename);
            if (!plugin.delete()) {
                log.warn("Could not remove plugin " + filename);
            }
        }
        final AgentPluginUpdater agentPluginUpdater = Bootstrap.getBean(AgentPluginUpdater.class);
        for (final Agent agent : agents) {
            agentPluginUpdater.queuePluginRemoval(agent.getId(), pluginFileNames);
        }
    }
    
    public Set<Integer> getAgentIdsInQueue() {
        final Set<Integer> rtn = new HashSet<Integer>();
        rtn.addAll(agentSynchronizer.getJobListByDescription(
            Arrays.asList(new String[]{AgentPluginUpdater.AGENT_PLUGIN_REMOVE,
                                       AgentPluginUpdater.AGENT_PLUGIN_TRANSFER})));
        rtn.addAll(agentPluginSyncRestartThrottle.getQueuedAgentIds());
        return rtn;
    }
    
    public Map<Integer, Long> getAgentIdsInRestartState() {
        return agentPluginSyncRestartThrottle.getAgentIdsInRestartState();
    }
    
    // XXX currently if one plugin validation fails all will fail.  Probably want to deploy the
    // plugins that are valid and return error status if any fail.
    public void deployPluginIfValid(AuthzSubject subj, Map<String, byte[]> pluginInfo)
    throws PluginDeployException {
        final Collection<File> files = new ArrayList<File>();
        for (final Entry<String, byte[]> entry : pluginInfo.entrySet()) {
            final String filename = entry.getKey();
            final byte[] bytes = entry.getValue();
            File file = null;
            if (filename.toLowerCase().endsWith(".jar")) {
                file = getFileAndValidateJar(filename, bytes);
            } else if (filename.toLowerCase().endsWith(".xml")) {
                file = getFileAndValidateXML(filename, bytes);
            } else {
                throw new PluginDeployException(
                    "cannot recognize file extension of " + filename + ", will not deploy plugin");
            }
            files.add(file);
        }
        deployPlugins(files);
    }
    
    private File getFileAndValidateXML(String filename, byte[] bytes)
    throws PluginDeployException {
        FileWriter writer = null;
        File rtn = null;
        try {
            rtn = new File(TMP_DIR + File.separator + filename);
            final String str = new String(bytes);
            final StringReader buf = new StringReader(str);
            new SAXBuilder().build(buf);
            writer = new FileWriter(rtn);
            writer.write(str);
            return rtn;
        } catch (JDOMException e) {
            if (rtn != null && rtn.exists()) {
                rtn.delete();
            }
            throw new PluginDeployException(
                "could not parse xml doc, " + filename + " is not well-formed: " + e, e);
        } catch (IOException e) {
            if (rtn != null && rtn.exists()) {
                rtn.delete();
            }
            throw new PluginDeployException("could not open " + filename + ": " + e, e);
        } finally {
            close(writer);
        }
    }
    
// XXX probably want to get this from the ProductPluginDeployer
    private File getPluginDir() {
        try {
            return ctx.getResource("WEB-INF/hq-plugins").getFile();
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    private void deployPlugins(Collection<File> files) {
        File pluginDir = getPluginDir();
        if (!pluginDir.exists()) {
            throw new SystemException(pluginDir.getAbsolutePath() + " does not exist");
        }
        for (final File file : files) {
            final File dest = new File(pluginDir.getAbsolutePath() + "/" + file.getName());
            file.renameTo(dest);
        }
    }

    private File getFileAndValidateJar(String filename, byte[] bytes) throws PluginDeployException {
        ByteArrayInputStream bais = null;
        JarInputStream jis = null;
        FileOutputStream fos = null;
        String file = null;
        String currXml = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            jis = new JarInputStream(bais);
            final Manifest manifest = jis.getManifest();
            if (manifest == null) {
                throw new PluginDeployException(
                    "manifest does not exist in " + filename +
                    ", jar file could be corrupt.  Will not deploy.");
            }
            file = TMP_DIR + File.separator + filename;
            fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            final File rtn = new File(file);
            final URL url = new URL("jar", "", "file:" + file + "!/");
            final JarURLConnection jarConn = (JarURLConnection) url.openConnection();
            final JarFile jarFile = jarConn.getJarFile();
            final Enumeration<JarEntry> entries = jarConn.getJarFile().entries();
            while (entries.hasMoreElements()) {
                InputStream is = null;
                try {
                    final JarEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    if (!entry.getName().toLowerCase().endsWith(".xml")) {
                        continue;
                    }
                    currXml = entry.getName();
                    is = jarFile.getInputStream(entry);
                    new SAXBuilder().build(is);
                    currXml = null;
                } finally {
                    close(is);
                }
            }
            return rtn;
        } catch (IOException e) {
            final File toRemove = new File(file);
            if (toRemove != null && toRemove.exists()) {
                toRemove.delete();
            }
            throw new PluginDeployException("could not deploy " + filename + ": " + e, e);
        } catch (JDOMException e) {
            final File toRemove = new File(file);
            if (toRemove != null && toRemove.exists()) {
                toRemove.delete();
            }
            throw new PluginDeployException(
                "could not deploy " + filename + ", " + currXml + " is not well-formed" + e, e);
        } finally {
            close(jis);
            close(fos);
        }
    }

    public Map<Integer, Map<AgentPluginStatusEnum, Integer>> getPluginRollupStatus() {
        final Map<String, Plugin> pluginsByName = getAllPluginsByName();
        final List<AgentPluginStatus> statuses = agentPluginStatusDAO.findAll();
        final Map<Integer, Map<AgentPluginStatusEnum, Integer>> rtn =
            new HashMap<Integer, Map<AgentPluginStatusEnum, Integer>>();
        for (final AgentPluginStatus status : statuses) {
            final String name = status.getPluginName();
            final Plugin plugin = pluginsByName.get(name);
            if (plugin == null) {
                continue;
            }
            setPluginRollup(status, plugin.getId(), rtn);
        }
        return rtn;
    }
    
    private void setPluginRollup(AgentPluginStatus status, Integer pluginId,
                                 Map<Integer, Map<AgentPluginStatusEnum, Integer>> map) {
        Map<AgentPluginStatusEnum, Integer> tmp;
        if (null == (tmp = map.get(pluginId))) {
            tmp = new HashMap<AgentPluginStatusEnum, Integer>();
            tmp.put(AgentPluginStatusEnum.SYNC_FAILURE, 0);
            tmp.put(AgentPluginStatusEnum.SYNC_IN_PROGRESS, 0);
            tmp.put(AgentPluginStatusEnum.SYNC_SUCCESS, 0);
            map.put(pluginId, tmp);
        }
        final String lastSyncStatus = status.getLastSyncStatus();
        if (lastSyncStatus == null) {
// XXX need to handle this case
            return;
        }
        final AgentPluginStatusEnum e = AgentPluginStatusEnum.valueOf(lastSyncStatus);
        tmp.put(e, tmp.get(e)+1);
    }
    
    public Plugin getPluginById(Integer id) {
        return pluginDAO.get(id);
    }

    private Map<String, Plugin> getAllPluginsByName() {
        final List<Plugin> plugins = pluginDAO.findAll();
        final Map<String, Plugin> rtn = new HashMap<String, Plugin>(plugins.size());
        for (final Plugin plugin : plugins) {
            rtn.put(plugin.getName(), plugin);
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    public Collection<AgentPluginStatus> getErrorStatusesByPluginId(int pluginId) {
        final Plugin plugin = pluginDAO.get(pluginId);
        if (plugin == null) {
            return Collections.emptyList();
        }
        return agentPluginStatusDAO.getErrorPluginStatusByFileName(plugin.getPath());
    }
    
    public boolean isPluginDeploymentOff() {
// XXX need to implement
        return false;
    }

     public Map<Plugin, Collection<AgentPluginStatus>> getOutOfSyncAgentsByPlugin() {
         return agentPluginStatusDAO.getOutOfSyncAgentsByPlugin();
     }

     public List<Plugin> getAllPlugins() {
         return pluginDAO.findAll();
     }

     public Collection<String> getOutOfSyncPluginNamesByAgentId(Integer agentId) {
         return agentPluginStatusDAO.getOutOfSyncPluginNamesByAgentId(agentId);
     }
     
     @Transactional(propagation=Propagation.REQUIRES_NEW, readOnly=false)
     public void updateAgentPluginSyncStatusInNewTran(AgentPluginStatusEnum s, Integer agentId,
                                                      Collection<Plugin> plugins) {
         if (plugins == null || plugins.isEmpty()) {
             return;
         }
         final Map<String, AgentPluginStatus> statusMap =
             agentPluginStatusDAO.getStatusByAgentId(agentId);
         final long now = System.currentTimeMillis();
         for (final Plugin plugin : plugins) {
             AgentPluginStatus status = statusMap.get(plugin.getName());
             if (status == null) {
                 continue;
             }
             status.setLastSyncStatus(s.toString());
             status.setLastSyncAttempt(now);
         }
     }

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    private void close(OutputStream os) {
        if (os == null) {
            return;
        }
        try {
            os.close();
        } catch (IOException e) {
            log.debug(e,e);
        }
    }

    private void close(Writer writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException e) {
            log.debug(e,e);
        }
    }

    private void close(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
            log.debug(e,e);
        }
    }

    @Transactional(readOnly=false)
    public void removeAgentPluginStatuses(Integer agentId, Collection<String> pluginFileNames) {
        agentPluginStatusDAO.removeAgentPluginStatuses(agentId, pluginFileNames);
    }

}