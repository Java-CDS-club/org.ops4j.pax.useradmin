/**
 * Copyright 2009 OPS4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ops4j.pax.useradmin.service.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.ops4j.pax.useradmin.service.spi.SPIRole;
import org.ops4j.pax.useradmin.service.spi.StorageException;
import org.ops4j.pax.useradmin.service.spi.StorageProvider;
import org.osgi.service.log.LogService;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * Implementation of the <code>Group</code> interface as specified in the OSGi
 * companion specification.
 * 
 * @see <a href=
 *      "http://www.osgi.org/javadoc/r4v42/org/osgi/service/useradmin/Group.html"
 *      >Group</a>
 * @author Matthias Kuespert
 * @since 02.07.2009
 */
public class GroupImpl extends UserImpl implements Group {

    /**
     * Constructor.
     * 
     * @see UserImpl#UserImpl(String, PaxUserAdmin, Map, Map)
     */
    protected GroupImpl(String name, PaxUserAdmin admin, Map<String, Object> properties) {
        super(name, admin, properties);
    }

    /**
     * @see Group#addMember(Role)
     */
    @Override
    public boolean addMember(Role role) {
        if (role != null) {
            try {
                getAdmin().checkAdminPermission();
                StorageProvider storageProvider = getAdmin().getStorageProvider();
                return storageProvider.addMember(this, role);
                // TODO: verify that we really don't need to fire an event here
                // - the spec doesn't mention anything
            } catch (StorageException e) {
                getAdmin().logMessage(this, LogService.LOG_ERROR, "error when adding basic member to group '" + getName() + "':" + e.getMessage());
            }
        }
        return false;
    }

    /**
     * @see Group#addRequiredMember(Role)
     */
    @Override
    public boolean addRequiredMember(Role role) {
        if (role != null) {
            try {
                getAdmin().checkAdminPermission();
                StorageProvider storageProvider = getAdmin().getStorageProvider();
                return storageProvider.addRequiredMember(this, role);
            } catch (StorageException e) {
                getAdmin().logMessage(this, LogService.LOG_ERROR, "error when adding required member to group '" + getName() + "':" + e.getMessage());
            }
        }
        return false;
    }

    /**
     * @see Group#removeMember(Role)
     */
    @Override
    public boolean removeMember(Role role) {
        if (role != null) {
            try {
                getAdmin().checkAdminPermission();
                StorageProvider storageProvider = getAdmin().getStorageProvider();
                return storageProvider.removeMember(this, role);
            } catch (StorageException e) {
                getAdmin().logMessage(this, LogService.LOG_ERROR, "error when removing member from group '" + getName() + "':" + e.getMessage());
            }
        }
        return false;
    }

    /**
     * @see Group#getMembers()
     */
    @Override
    public Role[] getMembers() {
        try {
            StorageProvider storageProvider = getAdmin().getStorageProvider();
            Collection<Role> roles = storageProvider.getMembers(getAdmin(), this);
            if (roles == null || !roles.isEmpty()) {
                return roles.toArray(new Role[roles.size()]);
            }
        } catch (StorageException e) {
            getAdmin().logMessage(this, LogService.LOG_ERROR, "error when retrieving basic members of group '" + getName() + "':" + e.getMessage());
        }
        return null;
    }

    /**
     * @see Group#getRequiredMembers()
     */
    @Override
    public Role[] getRequiredMembers() {
        try {
            StorageProvider storageProvider = getAdmin().getStorageProvider();
            Collection<Role> roles = storageProvider.getRequiredMembers(getAdmin(), this);
            if (roles == null || !roles.isEmpty()) {
                return roles.toArray(new Role[roles.size()]);
            }
        } catch (StorageException e) {
            getAdmin().logMessage(this, LogService.LOG_ERROR, "error when retrieving required members of group '" + getName() + "':" + e.getMessage());
        }
        return null;
    }

    /**
     * @see Group#getType()
     */
    @Override
    public int getType() {
        return Role.GROUP;
    }

    /**
     * Checks if this group is implied by the given role.
     * 
     * @param role
     *            The role to check.
     * @param checkedRoles
     *            Used for loop detection.
     * @return True if this role is implied by the given one, false otherwise.
     */
    @Override
    public ImplicationResult isImpliedBy(SPIRole role, Collection<String> checkedRoles) {
        // check if this group is implied
        ImplicationResult isImplied = super.isImpliedBy(role, checkedRoles);
        if (ImplicationResult.IMPLIEDBY_NO != isImplied) {
            return isImplied;
        } else {
            // check if all required members are implied
            Role[] members = getRequiredMembers();
            if (null != members) {
                Collection<String> localCheckedRoles = new ArrayList<String>(checkedRoles);
                for (Role member : members) {
                    if (member instanceof SPIRole) {
                        isImplied = ((SPIRole) member).isImpliedBy(role, localCheckedRoles);
                        if (ImplicationResult.IMPLIEDBY_YES != isImplied) {
                            // not implied because not all required members are
                            // implied or a loop was detected
                            return isImplied;
                        }
                    } else {
                        if (member != null) {
                            getAdmin().logMessage(GroupImpl.class.getSimpleName(), LogService.LOG_WARNING, "RequiredMember " + member.getName()
                                    + " is ignored because " + member.getClass().getName() + " does not implement the SPIRole interface");
                        }
                    }
                }
            }
            members = getMembers();
            if (null != members) {
                Collection<String> localCheckedRoles = new ArrayList<String>(checkedRoles);
                for (Role member : members) {
                    if (member instanceof SPIRole) {
                        isImplied = ((SPIRole) member).isImpliedBy(role, localCheckedRoles);
                        if (ImplicationResult.IMPLIEDBY_YES == isImplied) {
                            // implied because one basic member is implied
                            return isImplied;
                        }
                    } else {
                        if (member != null) {
                            getAdmin().logMessage(GroupImpl.class.getSimpleName(), LogService.LOG_WARNING, "BasicMember " + member.getName()
                                    + " is ignored because " + member.getClass().getName() + " does not implement the SPIRole interface");
                        }
                    }
                }
            }
        }
        return ImplicationResult.IMPLIEDBY_NO;
    }

    @Override
    public String toString() {
        return "Group-" + getName();
    }
}
