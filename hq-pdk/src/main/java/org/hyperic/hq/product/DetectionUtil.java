/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2012], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
 *
 */
package org.hyperic.hq.product;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.config.ConfigResponse;

public class DetectionUtil {
    private static Log log =
            LogFactory.getLog(DetectionUtil.class.getName());
	private static final String OS_TYPE;
	private static final boolean IS_WINDOWS;  
	private static final boolean IS_UNIX;
	static {
		OS_TYPE = System.getProperty("os.name").toLowerCase();
		IS_WINDOWS = OS_TYPE.contains("win");
		IS_UNIX = OS_TYPE.contains("nix")
				|| OS_TYPE.contains("nux")
				|| OS_TYPE.contains("sunos")
				|| OS_TYPE.contains("mac os x");
	}
	
	/**
	 * This method finds all the ports the provided pid listens on and adds them as a list
	 * to the provided ConfigResponse instance
	 * @param pid - the process id for which we want to get the listening ports
	 * @param cf - usually the product config
	 * @param recursive - if true the population of the listening port will use all the child processes of the pid
	 */
	public static void populateListeningPorts(long pid, ConfigResponse cf, boolean recursive){
		Set<Long> pids = new HashSet<Long>();
		pids.add(pid);
		if (recursive) {
			pids.addAll(getAllChildPid(pid));
		}
		populateListeningPorts(pids, cf);
	}

	/**
	 * This method finds all the ports the provided pids are listening on and adds them as a list
	 * to the provided ConfigResponse instance
	 * @param pids - the ids of the processes we want to get the listening ports for
	 * @param cf - usually the product config
	 */
	public static void populateListeningPorts(Set<Long> pids, ConfigResponse cf){
		if (pids.isEmpty()) {
			return;
		}
		if (IS_WINDOWS) {
			populatePortsOnWindows(pids, cf);
		}
		else if (IS_UNIX) {
			populatePortsOnUnix(pids, cf);
		}
	}

	/**
	 * This method finds the listening ports for the provided pids on
	 * Unix platform by using lsof 
	 * @param pids
	 * @param cf
	 */
	private static void populatePortsOnUnix(Set<Long> pids, ConfigResponse cf) {
		//build a string of all the provided pids
		StringBuilder pidsStr = new StringBuilder();
		for (Long pid : pids) {
			pidsStr.append(',').append(pid);
		}
		
		//build the lsof command with the provided pids
		String cmd = "lsof -p " + pidsStr.substring(1) + " -P";
		Set<String> ports = new HashSet<String>();
		String line;
		try {
		    // Run lsof
		    Process process = Runtime.getRuntime().exec(cmd);
		    BufferedReader input = new BufferedReader(new InputStreamReader(process
					.getInputStream()));

			//Go over the results and find the listening ports
			while ((line = input.readLine()) != null) {
				if (!line.trim().contains("(LISTEN)")) {
					continue;
				}
				try{
					//parse the line so we will get the port number
					line = line.substring(line.lastIndexOf(":") + 1).trim();
					line = line.substring(0, line.indexOf(" ")).trim();
					//check that we got a number and if true add it to the ports set
					if (isNumber(line)) {
						ports.add(line);
					}
				}
				catch (Exception e) {
					continue;
				}
			}
			input.close();
		} catch (Exception e) {
		  log.warn("Error populating ports for '" + pidsStr + "' ", e);
		}
		updatePortsInConfigResponse(cf, ports);
	}

	private static void populatePortsOnWindows(Set<Long> pids, ConfigResponse cf) {
		String cmd = "netstat -ano";
		//build a string of all the provided pids
		StringBuilder pidsStr = new StringBuilder();
		for (Long pid : pids) {
			pidsStr.append(',').append(pid);
		}
		Set<String> ports = new HashSet<String>();	
		String line;
		try {
			// Run netstat
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
	
			while ((line = input.readLine()) != null) {
				if (!(line.trim().startsWith("TCP") || line.startsWith("UDP")) 
						|| !line.contains("LISTENING")) {
					continue;
				}
				try{
					//check that the pid that listens on this port is one of the provided pids
					long pid = Long.valueOf(line.trim().substring(line.trim().lastIndexOf(" ")).trim());
					if (!pids.contains(pid)) {
						continue;
					}
					//get the port number
					if (line.contains("[::")) {
						line = line.replaceAll("[::", "").trim();
					}
					line = line.substring(line.indexOf(":") + 1);
					line = line.substring(0, line.indexOf(" "));
					line = line.trim();
					if (isNumber(line)) {
						ports.add(line);
					}
				}
				catch (Exception e) {
					continue;
				}
			}
			input.close();
		} catch (Exception e) {
			log.warn("Error populating ports for '" + pidsStr + "' ", e);
		}

		updatePortsInConfigResponse(cf, ports);
	}

	/**
	 * This method updates the ConfigResponse with the listening ports (if there are any)
	 * @param cf
	 * @param ports
	 */
	private static void updatePortsInConfigResponse(ConfigResponse cf, Set<String> ports) {
		if (!ports.isEmpty()) {
			StringBuilder portsStr = new StringBuilder();
			for (String port : ports) {
				portsStr.append(',').append(port);
			}
			cf.setValue(Collector.LISTEN_PORTS, portsStr.substring(1));
		}
	}
	
	
	
	/**
	 * This method finds all the childs of the provided process
	 * @param parentPid
	 */
	public static Set<Long> getAllChildPid(long parentPid) {
		Set<Long> childPids = new HashSet<Long>();

		if (IS_UNIX) {
			String cmd = "ps -o pid --no-headers --ppid " + String.valueOf(parentPid);
			String line;
			try {
				Process process = Runtime.getRuntime().exec(cmd);
				BufferedReader input = new BufferedReader(new InputStreamReader(process
						.getInputStream()));
				while ((line = input.readLine()) != null) {
					if (!line.isEmpty()) {
						Long childPid = Long.valueOf(line.trim());
						childPids.addAll(getAllChildPid(childPid));
						childPids.add(childPid);
					}
				}
				input.close();
			} catch (Exception e) {

			}
		}
		else if (IS_WINDOWS) {
			final String cmd = "wmic process get processid,parentprocessid";
			String line;
			try {
			    Process process = Runtime.getRuntime().exec(cmd);
			    BufferedReader input = new BufferedReader(new InputStreamReader(process
						.getInputStream()));
				long lPpid = -1;
				String sPpid = "";
				String sCpid = "";
				long lCpid = -1;
				while ((line = input.readLine()) != null) {
					try {
						line = line.trim();
						sPpid = line.substring(0, line.indexOf(" "));
						lPpid = Long.valueOf(sPpid);
						if (parentPid == lPpid) {
							sCpid = line.substring(line.indexOf(" ")).trim();
							lCpid = Long.valueOf(sCpid);
							childPids.addAll(getAllChildPid(lCpid));
							childPids.add(lCpid);
						}
						else {
							continue;
						}
					}
					catch (Exception e) {
						continue;
					}					
				}
	
				input.close();
			} catch (Exception e) {
			}
		}
		return childPids;
	}
	
	/**
	 * Returns true if the provided string is a number (not a float number)
	 */
	private static boolean isNumber(String value) {
		try {
			Long.valueOf(value);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

}
