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

package org.hyperic.util.timer;

/**
 * A singleton factory class that returns {@link Clock} instances for 
 * keeping time.
 */
public class ClockFactory {
    
    private static final ClockFactory INSTANCE = new ClockFactory();
    
    /**
     * Singletons should have a private constructor.
     */
    private ClockFactory() {
    }
    
    /**
     * @return The clock factory instance.
     */
    public static ClockFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * @return A {@link Clock} for keeping time.
     */
    public Clock getClock() {
        return SystemTimeClock.getInstance();
    }
    

}