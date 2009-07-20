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
package org.ops4j.pax.useradmin.itest.preferences;

import static org.ops4j.pax.exam.CoreOptions.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.useradmin.itest.AuthorizationManagement;
import org.osgi.framework.BundleContext;

/**
 * @author Matthias Kuespert
 * @since 12.07.2009
 */
@RunWith(JUnit4TestRunner.class)
public class AuthorizationManagementTest extends AuthorizationManagement {

    @Inject
    private BundleContext m_context;

    protected BundleContext getBundleContext() {
        return m_context;
    };

    @Configuration
    public static Option[] configure() {
        return options(getBasicFrameworkConfiguration(),
                       FrameworkConfiguration.get());
    }

    @Before
    public void setup() {
        super.setup();
    }

    @Test
    public void hasRole() {
        super.hasRole();
    }
}