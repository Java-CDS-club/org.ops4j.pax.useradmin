/*
 * Copyright 2009 Matthias Kuespert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.useradmin.service.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.ops4j.pax.useradmin.service.spi.StorageException;
import org.ops4j.pax.useradmin.service.spi.StorageProvider;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Testing the RoleImpl and RoleProperties classes.
 * 
 * @author Matthias Kuespert
 * @since  11.07.2009
 */
public class RoleImplTest {

    private static final String NAME = "someRole";
    private static final String KEY1 = "test";
    private static final String VALUE1 = "someValue1";
    
    private Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(KEY1, VALUE1);
        return properties;
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void constructNoName() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        new RoleImpl(null, userAdmin, getProperties());
        Assert.fail("No exception when creating role with invalid name");
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructNoAdmin() {
        new RoleImpl(NAME, null, getProperties());
        Assert.fail("No exception when creating role with invalid UserAdmin instance");
    }

    @Test
    public void constructNullProperties() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        Assert.assertNotNull("Could not create RoleImpl instance", role);
        Assert.assertEquals("Mismatching name", NAME, role.getName());
        Assert.assertEquals("Invalid type", Role.ROLE, role.getType());
        Assert.assertEquals("Invalid UserAdmin instance", userAdmin, role.getAdmin());
        Assert.assertNotNull("Invalid properties", role.getProperties());
        Assert.assertEquals("Too many initial properties", 0, role.getProperties().size());
    }

    @Test
    public void constructEmptyProperties() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, new HashMap<String, String>());
        Assert.assertNotNull("Could not create RoleImpl instance", role);
        Assert.assertEquals("Mismatching name", NAME, role.getName());
        Assert.assertEquals("Invalid type", Role.ROLE, role.getType());
        Assert.assertEquals("Invalid UserAdmin instance", userAdmin, role.getAdmin());
        Assert.assertNotNull("Invalid properties", role.getProperties());
        Assert.assertEquals("Too many initial properties", 0, role.getProperties().size());
    }

    @Test
    @SuppressWarnings(value = "unchecked")
    public void constructOk() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, getProperties());
        Assert.assertNotNull("Could not create RoleImpl instance", role);
        Assert.assertEquals("Mismatching name", NAME, role.getName());
        Assert.assertEquals("Invalid type", Role.ROLE, role.getType());
        Assert.assertEquals("Invalid UserAdmin instance", userAdmin, role.getAdmin());
        Dictionary properties = role.getProperties(); 
        Assert.assertNotNull("Invalid properties", properties);
        Assert.assertEquals("Mismatching property count", 1, properties.size());
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void getPropertyNullKey() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().get(null);
        EasyMock.verify(userAdmin, sp);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void getPropertyWrongKeyType() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().get(666);
        EasyMock.verify(userAdmin, sp);
    }
    
    @Test
    public void getPropertyOk() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, getProperties());
        EasyMock.replay(userAdmin);
        //
        Assert.assertEquals("Mismatching value", VALUE1, role.getProperties().get(KEY1));
        EasyMock.verify(userAdmin);
    }
    
    @Test (expected = IllegalArgumentException.class)
    @SuppressWarnings(value = "unchecked")
    public void addPropertyNullKey() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().put(null, VALUE1);
        EasyMock.verify(userAdmin, sp);
    }

    @Test (expected = IllegalArgumentException.class)
    @SuppressWarnings(value = "unchecked")
    public void addPropertyWrongKeyType() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().put(666, VALUE1);
        EasyMock.verify(userAdmin, sp);
    }

    @Test (expected = IllegalArgumentException.class)
    @SuppressWarnings(value = "unchecked")
    public void addPropertyNullValue() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().put(KEY1, null);
        EasyMock.verify(userAdmin, sp);
    }

    @Test (expected = IllegalArgumentException.class)
    @SuppressWarnings(value = "unchecked")
    public void addPropertyWrongValueType() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().put(KEY1, 666);
        EasyMock.verify(userAdmin, sp);
    }

    @Test
    @SuppressWarnings(value = "unchecked")
    public void addPropertyOk() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
            sp.setRoleAttribute(role, KEY1, VALUE1);
            userAdmin.checkPermission(KEY1, UserAdminPermission.CHANGE_PROPERTY);
            userAdmin.fireEvent(UserAdminEvent.ROLE_CHANGED, role);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().put(KEY1, VALUE1);
        EasyMock.verify(userAdmin, sp);
    }

    @Test (expected = IllegalArgumentException.class)
    public void removePropertyNullKey() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().remove(null);
        EasyMock.verify(userAdmin, sp);
    }

    @Test (expected = IllegalArgumentException.class)
    public void removePropertyWrongKeyType() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, null);
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().remove(666);
        EasyMock.verify(userAdmin, sp);
    }

    @Test
    public void removePropertyOk() {
        UserAdminImpl userAdmin = EasyMock.createMock(UserAdminImpl.class);
        RoleImpl role = new RoleImpl(NAME, userAdmin, getProperties());
        StorageProvider sp = EasyMock.createMock(StorageProvider.class);
        //
        try {
            EasyMock.expect(userAdmin.getStorageProvider()).andReturn(sp);
            sp.removeRoleAttribute(role, KEY1);
            userAdmin.checkPermission(KEY1, UserAdminPermission.CHANGE_PROPERTY);
            userAdmin.fireEvent(UserAdminEvent.ROLE_CHANGED, role);
        } catch (StorageException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        EasyMock.replay(userAdmin, sp);
        //
        role.getProperties().remove(KEY1);
        EasyMock.verify(userAdmin, sp);
    }

    // Note: a test for the clear() method is not needed since the Dictionary
    // class does not provide a clear method
}