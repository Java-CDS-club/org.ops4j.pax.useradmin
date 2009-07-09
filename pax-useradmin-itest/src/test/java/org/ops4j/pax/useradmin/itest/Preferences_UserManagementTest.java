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

package org.ops4j.pax.useradmin.itest;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.*;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.useradmin.UserAdmin;

/**
 * @author Matthias Kuespert
 * @since  09.07.2009
 */
@RunWith(JUnit4TestRunner.class)
public class Preferences_UserManagementTest extends UserManagementTest {

    @Configuration
    public static Option[] configure() {

        return options(logProfile(),

                       // TODO: preferences service
                       
//                       mavenBundle().groupId("org.ops4j.pax.useradmin")
//                                    .artifactId("pax-useradmin-provider-preferences")
//                                    .version("0.0.1-SNAPSHOT"),
                       mavenBundle().groupId("org.ops4j.pax.useradmin")
                                    .artifactId("pax-useradmin-service")
                                    .version("0.0.1-SNAPSHOT")
                       );
    }

    @Test
    public void createUser() {
        
        testCreateUser();
//        UserAdmin userAdmin = getUserAdmin();
//        Assert.assertNotNull(userAdmin);
//        
//        Assert.fail("test");
        
    }

}