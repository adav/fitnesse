// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.updates;

import fitnesse.wiki.PathParser;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdaterTest extends UpdateTestCase {

    public void setUp() throws Exception {
        super.setUp();
        UpdaterImplementation.testing = true;
        crawler.addPage(root, PathParser.parse("PageOne"));
    }

    @Test
    public void testProperties() throws Exception {
        File file = new File("TestDir/properties");
        assertFalse(file.exists());
        updater.updates = new Update[]{};
        updater.update();
        assertTrue(file.exists());
    }
}
