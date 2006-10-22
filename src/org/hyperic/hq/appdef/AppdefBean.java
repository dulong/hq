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

package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;

import java.io.Serializable;

import org.hyperic.hibernate.PersistedObject;

/**
 * This is the base abstract class for all appdef pojos.
 * This is modeled after the AppdefEntityBean less the EJB code.
 */
public abstract class AppdefBean 
    extends PersistedObject
    implements Serializable
{
    // XXX -- Can we make these private?  We have accessors.  -- JMT
    protected Long creationTime;
    protected Long modifiedTime;

    // legacy stuff, do we really need this?
    protected Integer cid;

    protected AppdefBean() {
        super();
    }

    protected AppdefBean(Integer id)
    {
        super();
        setId(id);
    }

    public long getCreationTime()
    {
        return creationTime != null ? creationTime.longValue() : 0;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }

    public long getModifiedTime()
    {
        return modifiedTime != null ? modifiedTime.longValue() : 0;
    }

    public void setModifiedTime(Long modifiedTime)
    {
        this.modifiedTime = modifiedTime;
    }

    // for legacy EJB assessor
    /**
     * @deprecated
     * @return
     */
    public Long getCTime()
    {
        return creationTime;
    }
    /**
     * @deprecated
     * @return
     */
    public Long getMTime()
    {
        return modifiedTime;
    }
    // end legacy EJB assessors
    public Integer getCid()
    {
        return cid;
    }

    public void setCid(Integer cid)
    {
        this.cid = cid;
    }

    /**
     * legacy EJB entity bean code
     * @param obj
     * @return
     */
    public boolean matchesValueObject(AppdefResourceValue obj)
    {
        boolean matches = true;
        if (obj.getId() != null) {
            matches = (obj.getId().intValue() == this.getId().intValue());
        } else {
            matches = (this.getId() == null);
        }
        if (obj.getCTime() != null) {
            matches = (obj.getCTime().floatValue() == getCTime().floatValue());
        } else {
            matches = (this.getCreationTime() == 0);
        }
        return matches;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof AppdefBean) || !super.equals(obj)) {
            return false;
        }
        AppdefBean o = (AppdefBean)obj;
        return
            ((creationTime == o.getCTime()) ||
             (creationTime!=null && o.getCTime()!=null &&
              creationTime.equals(o)));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result +
                 (creationTime != null ? creationTime.hashCode() : 0);

        return result;
    }

}
