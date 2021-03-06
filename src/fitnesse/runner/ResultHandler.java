// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.runner;

import fitnesse.testsystems.TestSummary;

import java.io.IOException;

public interface ResultHandler {
    void acceptResult(PageResult result) throws IOException;

    void acceptFinalCount(TestSummary testSummary) throws IOException;
}
