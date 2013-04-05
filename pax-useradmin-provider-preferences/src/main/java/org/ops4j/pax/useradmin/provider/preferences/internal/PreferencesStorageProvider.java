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

package org.ops4j.pax.useradmin.provider.preferences.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.ops4j.pax.useradmin.provider.preferences.ConfigurationConstants;
import org.ops4j.pax.useradmin.service.PaxUserAdminConstants;
import org.ops4j.pax.useradmin.service.spi.CredentialProvider;
import org.ops4j.pax.useradmin.service.spi.Decryptor;
import org.ops4j.pax.useradmin.service.spi.Encryptor;
import org.ops4j.pax.useradmin.service.spi.StorageException;
import org.ops4j.pax.useradmin.service.spi.StorageProvider;
import org.ops4j.pax.useradmin.service.spi.UserAdminFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * A PreferencesService based <code>StorageProvider</code> service.
 * 
 * @author Matthias Kuespert
 * @since 08.07.2009
 */
public class PreferencesStorageProvider implements StorageProvider, CredentialProvider {

    private static final String                  PATH_SEPARATOR         = "/";

    private static final String                  PREFERENCE_USER        = "Pax UserAdmin";

    private static final String                  NODE_TYPE              = "type";

    private static final String                  MEMBERS_NODE           = "members";
    private static final String                  REQUIRED_MEMBER_STRING = "required";
    private static final String                  BASIC_MEMBER_STRING    = "basic";

    private static final String                  PROPERTIES_NODE        = "properties";
    private static final String                  CREDENTIALS_NODE       = "credentials";
    private static final String                  TYPES_NODE             = "types";

    private PreferencesService                   m_preferencesService   = null;

    private Preferences                          m_rootNode             = null;

    private final Long                           trackedServiceID;

    private ServiceRegistration<StorageProvider> serviceRegistration;

    public PreferencesStorageProvider(PreferencesService preferencesService, Long trackedServiceID) throws StorageException {
        m_preferencesService = preferencesService;
        this.trackedServiceID = trackedServiceID;

        //
        // create the anonymous user if it does not exist
        //
        Preferences node = getRootNode();
        try {
            if (!node.nodeExists(Role.USER_ANYONE)) {
                Preferences anyoneNode = node.node(Role.USER_ANYONE);
                anyoneNode.putInt(NODE_TYPE, Role.USER);
            }
        } catch (BackingStoreException e) {
            throw new StorageException("Error creating anonymous role '" + Role.USER_ANYONE, e);
        }
    }

    private PreferencesService getPreferencesService() {
        return m_preferencesService;
    }

    private Map<String, Object> loadAttributes(Preferences node) throws BackingStoreException {
        Preferences propertyTypes = node.node(TYPES_NODE);
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : node.keys()) {
            if (propertyTypes.getBoolean(key, true)) {
                properties.put(key, node.get(key, ""));
            } else {
                properties.put(key, node.getByteArray(key, "".getBytes()));
            }
        }
        return properties;
    }

    private void storeAttribute(Preferences node, String key, String value) throws BackingStoreException {
        Preferences propertyTypes = node.node(TYPES_NODE);
        propertyTypes.putBoolean(key, true);
        node.put(key, value);
        node.flush();
    }

    private void storeAttribute(Preferences node, String key, byte[] value) throws BackingStoreException {
        Preferences propertyTypes = node.node(TYPES_NODE);
        propertyTypes.putBoolean(key, false);
        node.putByteArray(key, value);
        node.flush();
    }

    private Role loadRole(UserAdminFactory factory, String name, Filter filter) throws BackingStoreException, StorageException {
        if (!getRootNode().nodeExists(name)) {
            return null;
        }
        Preferences node = getRootNode().node(name);
        //
        Map<String, Object> properties = null;
        if (node.nodeExists(PROPERTIES_NODE)) {
            properties = loadAttributes(node.node(PROPERTIES_NODE));
        }
        //
        if (null != filter) {
            if (null == properties) {
                return null;
            }
            Dictionary<String, Object> dict = new Hashtable<String, Object>(properties);
            if (!filter.match(dict)) {
                return null;
            }
        }
        //
        Map<String, Object> credentials = null;
        if (node.nodeExists(CREDENTIALS_NODE)) {
            credentials = loadAttributes(node.node(CREDENTIALS_NODE));
        }
        //
        int type = new Integer(node.get(NODE_TYPE, "666"));
        Role role = null;
        switch (type) {
            case User.USER:
                role = factory.createUser(name, properties);
                break;
            case User.GROUP:
                role = factory.createGroup(name, properties);
                break;
            default:
                throw new StorageException("Invalid role type for role '" + name + " / " + node.name() + "': " + type);
        }
        return role;
    }

    private Collection<Role> loadRoles(UserAdminFactory factory, Filter filter) throws BackingStoreException, StorageException {
        String[] roleNames = getRootNode().childrenNames();
        Collection<Role> roles = new ArrayList<Role>();
        for (String name : roleNames) {
            Role role = loadRole(factory, name, filter);
            if (null != role) {
                roles.add(role);
            }
        }
        return roles;
    }

    protected Collection<Role> loadMembers(UserAdminFactory factory, Group group, String memberType) throws BackingStoreException, StorageException {
        Collection<Role> members = new ArrayList<Role>();
        Preferences node = getRootNode().node(group.getName());
        if (node.nodeExists(MEMBERS_NODE)) {
            Preferences membersNode = node.node(MEMBERS_NODE);
            for (String name : membersNode.keys()) {
                if (memberType.equals(membersNode.get(name, ""))) {
                    Role role = loadRole(factory, name, null);
                    if (null != role) {
                        members.add(role);
                    }
                }
            }
        }
        return members;
    }

    // TODO: use when removing users - check & test
    private void removeFromGroups(String memberName) throws BackingStoreException, StorageException {
        String[] roleNames = getRootNode().childrenNames();
        for (String name : roleNames) {
            Preferences node = getRootNode().node(name);
            if (node.nodeExists(MEMBERS_NODE)) {
                Preferences membersNode = node.node(MEMBERS_NODE);
                membersNode.remove(memberName);
            }
        }
    }

    private Preferences getRootNode() throws StorageException {
        if (null == m_rootNode) {
            PreferencesService service = getPreferencesService();
            m_rootNode = service.getUserPreferences(PREFERENCE_USER);
        }
        return m_rootNode;
    }

    @Override
    public User createUser(UserAdminFactory factory, String name) throws StorageException {
        Preferences node = getRootNode().node(name);
        node.putInt(NODE_TYPE, Role.USER);
        User user = factory.createUser(name, null);
        try {
            node.flush();
        } catch (BackingStoreException e) {
            throw new StorageException("Error flush()ing node '" + node.name(), e);
        }
        return user;
    }

    @Override
    public Group createGroup(UserAdminFactory factory, String name) throws StorageException {
        Preferences node = getRootNode().node(name);
        node.putInt(NODE_TYPE, Role.GROUP);
        Group group = factory.createGroup(name, null);
        try {
            node.flush();
        } catch (BackingStoreException e) {
            throw new StorageException("Error flush()ing node '" + node.name(), e);
        }
        return group;
    }

    @Override
    public boolean deleteRole(Role role) throws StorageException {
        try {
            if (getRootNode().nodeExists(role.getName())) {
                removeFromGroups(role.getName());
                getRootNode().node(role.getName()).removeNode();
                getRootNode().flush();
                return true;
            }
        } catch (BackingStoreException e) {
            throw new StorageException("Error removing node '" + role.getName(), e);
        }
        return false;
    }

    @Override
    public Collection<Role> getMembers(UserAdminFactory factory, Group group) throws StorageException {
        try {
            return loadMembers(factory, group, BASIC_MEMBER_STRING);
        } catch (BackingStoreException e) {
            throw new StorageException("Error retrieving basic members of group '" + group.getName(), e);
        }
    }

    @Override
    public Collection<Role> getRequiredMembers(UserAdminFactory factory, Group group) throws StorageException {
        try {
            return loadMembers(factory, group, REQUIRED_MEMBER_STRING);
        } catch (BackingStoreException e) {
            throw new StorageException("Error retrieving required members of group '" + group.getName(), e);
        }
    }

    @Override
    public boolean addMember(Group group, Role role) throws StorageException {
        Preferences node = getRootNode().node(group.getName() + PATH_SEPARATOR + MEMBERS_NODE);
        if (!"".equals(node.get(role.getName(), ""))) {
            return false; // member already exists
        }
        //
        node.put(role.getName(), BASIC_MEMBER_STRING);
        try {
            node.flush();
        } catch (BackingStoreException e) {
            throw new StorageException("Error flush()ing node '" + node.name(), e);
        }
        return true;
    }

    @Override
    public boolean addRequiredMember(Group group, Role role) throws StorageException {
        Preferences node = getRootNode().node(group.getName() + PATH_SEPARATOR + MEMBERS_NODE);
        if (!"".equals(node.get(role.getName(), ""))) {
            return false; // member already exists
        }
        //
        node.put(role.getName(), REQUIRED_MEMBER_STRING);
        try {
            node.flush();
        } catch (BackingStoreException e) {
            throw new StorageException("Error flush()ing node '" + node.name(), e);
        }
        return true;
    }

    @Override
    public boolean removeMember(Group group, Role role) throws StorageException {
        Preferences node = getRootNode().node(group.getName() + PATH_SEPARATOR + MEMBERS_NODE);
        if ("".equals(node.get(role.getName(), ""))) {
            return false; // member does not exist
        }
        //
        node.remove(role.getName());
        try {
            getRootNode().flush();
        } catch (BackingStoreException e) {
            throw new StorageException("Error flush()ing node '" + node.name(), e);
        }
        return true;
    }

    @Override
    public void setRoleAttribute(Role role, String key, Object value) throws StorageException {
        try {
            Preferences node = getRootNode().node(role.getName() + PATH_SEPARATOR + PROPERTIES_NODE);
            if (value instanceof String) {
                storeAttribute(node, key, (String) value);
            } else if (value instanceof byte[]) {
                storeAttribute(node, key, (byte[]) value);
            } else {
                throw new StorageException("Invalid value type '" + value.getClass().getName() + "' - only String or byte[] are allowed.");
            }
            node.flush();
        } catch (BackingStoreException e) {
            throw new StorageException("Error storing attribute '" + key + "' = '" + value + "' for role '" + role.getName(), e);
        }
    }

    @Override
    public void removeRoleAttribute(Role role, String key) throws StorageException {
        try {
            Preferences node = getRootNode().node(role.getName() + PATH_SEPARATOR + CREDENTIALS_NODE);
            node.remove(key);
            getRootNode().flush();
        } catch (IllegalStateException e) {
            throw new StorageException("Error removing attribute from role '" + role.getName(), e);
        } catch (BackingStoreException e) {
            throw new StorageException("Error removing attribute from role '" + role.getName(), e);
        }
    }

    @Override
    public void clearRoleAttributes(Role role) throws StorageException {
        try {
            Preferences node = getRootNode().node(role.getName());
            if (node.nodeExists(PROPERTIES_NODE)) {
                node.node(PROPERTIES_NODE).removeNode();
                node.flush();
            }
        } catch (BackingStoreException e) {
            throw new StorageException("Error clearing attributes of role '" + role.getName(), e);
        }
    }

    @Override
    public void setUserCredential(Encryptor encryptor, User user, String key, Object value) throws StorageException {
        try {
            Preferences node = getRootNode().node(user.getName() + PATH_SEPARATOR + CREDENTIALS_NODE);
            if (value instanceof String) {
                storeAttribute(node, key, (String) value);
            } else if (value instanceof byte[]) {
                storeAttribute(node, key, (byte[]) value);
            } else {
                throw new StorageException("Invalid value type '" + value.getClass().getName() + "' - only String or byte[] are allowed.");
            }
            node.flush();
        } catch (BackingStoreException e) {
            throw new StorageException("Error storing credential for user '" + user.getName(), e);
        }
    }

    @Override
    public void removeUserCredential(User user, String key) throws StorageException {
        try {
            Preferences node = getRootNode().node(user.getName() + PATH_SEPARATOR + CREDENTIALS_NODE);
            node.remove(key);
            getRootNode().flush();
        } catch (IllegalStateException e) {
            throw new StorageException("Error removing credential from user '" + user.getName(), e);
        } catch (BackingStoreException e) {
            throw new StorageException("Error removing credential from user '" + user.getName(), e);
        }
    }

    @Override
    public void clearUserCredentials(User user) throws StorageException {
        try {
            Preferences node = getRootNode().node(user.getName());
            if (node.nodeExists(CREDENTIALS_NODE)) {
                node.node(CREDENTIALS_NODE).removeNode();
                node.flush();
            }
        } catch (BackingStoreException e) {
            throw new StorageException("Error clearing credentials of user '" + user.getName(), e);
        }
    }

    @Override
    public Role getRole(UserAdminFactory factory, String name) throws StorageException {
        try {
            return loadRole(factory, name, null);
        } catch (BackingStoreException e) {
            throw new StorageException("Error loading role '" + name, e);
        }
    }

    @Override
    public User getUser(UserAdminFactory factory, String key, String value) throws StorageException {
        try {
            Filter filter = FrameworkUtil.createFilter("(" + key + "=" + value + ")");
            Collection<Role> roles = loadRoles(factory, filter);
            Collection<User> users = new ArrayList<User>();
            for (Role role : roles) {
                if (Role.USER == role.getType()) {
                    users.add((User) role);
                }
            }
            //
            if (users.size() == 1) {
                return users.iterator().next();
            }
        } catch (InvalidSyntaxException e) {
            throw new StorageException("Invalid filter '" + e.getFilter(), e);
        } catch (BackingStoreException e) {
            throw new StorageException("Error retrieving user with attribute '" + key + " = " + value, e);
        } catch (StorageException e) {
            throw e;
        }
        return null;
    }

    @Override
    public Collection<Role> findRoles(UserAdminFactory factory, String filterString) throws StorageException {
        try {
            Filter filter = null;
            if (null != filterString) {
                filter = FrameworkUtil.createFilter(filterString);
            }
            Collection<Role> roles = loadRoles(factory, filter);
            return roles;
        } catch (InvalidSyntaxException e) {
            throw new StorageException("Invalid filter '" + e.getFilter(), e);
        } catch (BackingStoreException e) {
            throw new StorageException("Error retrieving roles for filter '" + filterString + "'", e);
        }
    }

    /**
     * @param context
     */
    public synchronized void register(BundleContext context) {
        if (serviceRegistration != null) {
            throw new IllegalStateException("This object is already registered under another bundle context!");
        }
        //Propagate the properties of the EMF service...
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        //Set service PID
        properties.put(Constants.SERVICE_PID, ConfigurationConstants.SERVICE_PID);
        //Set stoarage provider type
        properties.put(PaxUserAdminConstants.STORAGEPROVIDER_TYPE, ConfigurationConstants.STORAGEPROVIDER_TYPE);
        //set the service id of the underlying service
        properties.put(ConfigurationConstants.TRACKED_SERVICE_ID, trackedServiceID);
        serviceRegistration = context.registerService(StorageProvider.class, this, properties);
    }

    public synchronized void unregister() {
        if (serviceRegistration == null) {
            throw new IllegalStateException("This object is not registered!");
        }
        serviceRegistration.unregister();
    }

    @Override
    public Object getUserCredential(Decryptor decryptor, User user, String key) throws StorageException {
        Preferences node = getRootNode().node(user.getName());
        try {
            if (node.nodeExists(CREDENTIALS_NODE)) {
                //TODO: Just load the mentioned property
                return loadAttributes(node.node(CREDENTIALS_NODE)).get(key);
            }
        } catch (BackingStoreException e) {
            throw new StorageException("loading credentials failed", e);
        }
        return null;
    }

    @Override
    public boolean hasUserCredential(Decryptor decryptor, User user, String key, Object value) throws StorageException {
        return value.equals(getUserCredential(null, user, key));
    }

    @Override
    public CredentialProvider getCredentialProvider() {
        return this;
    }
}
