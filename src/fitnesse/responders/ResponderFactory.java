// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders;

import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.responders.editing.*;
import fitnesse.responders.files.*;
import fitnesse.responders.refactoring.*;
import fitnesse.responders.run.*;
import fitnesse.responders.search.ExecuteSearchPropertiesResponder;
import fitnesse.responders.search.SearchFormResponder;
import fitnesse.responders.search.SearchResponder;
import fitnesse.responders.search.WhereUsedResponder;
import fitnesse.responders.testHistory.*;
import fitnesse.responders.versions.RollbackResponder;
import fitnesse.responders.versions.VersionComparerResponder;
import fitnesse.responders.versions.VersionResponder;
import fitnesse.responders.versions.VersionSelectionResponder;
import fitnesse.wikitext.parser.WikiWordPath;
import util.StringUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ResponderFactory {
    private final String rootPath;
    private final Map<String, ResponderCreator> responderMap;

    private interface ResponderCreator {
        Responder create() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;
    }

    private static class ClassInstantiatingResponderCreator implements ResponderCreator {
        private final Class<?> classToInstantiate;
        private final String rootPath;

        private ClassInstantiatingResponderCreator(Class<?> classToInstantiate, String rootPath) {
            this.classToInstantiate = classToInstantiate;
            this.rootPath = rootPath;
        }

        @Override
        public Responder create() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
            return newResponderInstance(classToInstantiate, rootPath);
        }
    }

    public ResponderFactory(String rootPath) {
        this.rootPath = rootPath;
        responderMap = new HashMap<String, ResponderCreator>();
        addResponder("new", NewPageResponder.class);
        addResponder("edit", EditResponder.class);
        addResponder("saveData", SaveResponder.class);
        addResponder("search", SearchResponder.class);
        addResponder("searchForm", SearchFormResponder.class);
        addResponder("stoptest", StopTestResponder.class);
        addResponder("test", TestResponder.class);
        addResponder("suite", SuiteResponder.class);
        addResponder("proxy", SerializedPageResponder.class);
        addResponder("versions", VersionSelectionResponder.class);
        addResponder("viewVersion", VersionResponder.class);
        addResponder("rollback", RollbackResponder.class);
        addResponder("names", NameWikiPageResponder.class);
        addResponder("properties", PropertiesResponder.class);
        addResponder("saveProperties", SavePropertiesResponder.class);
        addResponder("executeSearchProperties", ExecuteSearchPropertiesResponder.class);
        addResponder("whereUsed", WhereUsedResponder.class);
        addResponder("refactor", RefactorPageResponder.class);
        addResponder("deletePage", DeletePageResponder.class);
        addResponder("renamePage", RenamePageResponder.class);
        addResponder("movePage", MovePageResponder.class);
        addResponder("pageData", PageDataWikiPageResponder.class);
        addResponder("createDir", CreateDirectoryResponder.class);
        addResponder("upload", UploadResponder.class);
        addResponder("socketCatcher", SocketCatchingResponder.class);
        addResponder("fitClient", FitClientResponder.class);
        addResponder("deleteFile", DeleteFileResponder.class);
        addResponder("renameFile", RenameFileResponder.class);
        addResponder("deleteConfirmation", DeleteConfirmationResponder.class);
        addResponder("renameConfirmation", RenameFileConfirmationResponder.class);
        addResponder("raw", RawContentResponder.class);
        addResponder("rss", RssResponder.class);
        addResponder("import", WikiImportingResponder.class);
        addResponder("files", FileResponder.class);
        addResponder("shutdown", ShutdownResponder.class);
        addResponder("format", TestResultFormattingResponder.class);
        addResponder("symlink", SymbolicLinkResponder.class);
        addResponder("importAndView", ImportAndViewResponder.class);
        addResponder("getPage", WikiPageResponder.class);
        addResponder("packet", PacketResponder.class);
        addResponder("testHistory", TestHistoryResponder.class);
        addResponder("pageHistory", PageHistoryResponder.class);
        addResponder("addChild", AddChildPageResponder.class);
        addResponder("purgeHistory", PurgeHistoryResponder.class);
        addResponder("compareHistory", HistoryComparerResponder.class);
        addResponder("replace", SearchReplaceResponder.class);
        addResponder("overview", SuiteOverviewResponder.class);
        addResponder("compareVersions", VersionComparerResponder.class);
    }

    public void addResponder(String key, String responderClassName) throws ClassNotFoundException {
        addResponder(key, Class.forName(responderClassName));
    }

    // only used by this class and tests
    public void addResponder(String key, Class<?> responderClass) {
        responderMap.put(key, new ClassInstantiatingResponderCreator(responderClass, rootPath));
    }

    /* exposed for test */ String getResponderKey(Request request) {
        String fullQuery;
        if (request.hasInput("responder"))
            fullQuery = (String) request.getInput("responder");
        else
            fullQuery = request.getQueryString();

        if (fullQuery == null)
            return null;

        int argStart = fullQuery.indexOf('&');
        return (argStart <= 0) ? fullQuery : fullQuery.substring(0, argStart);
    }

    public Responder makeResponder(Request request) throws InstantiationException {
        Responder responder;
        String resource = request.getResource();
        String responderKey = getResponderKey(request);
        if (usingResponderKey(responderKey))
            responder = lookupResponder(responderKey);
        else {
            if (StringUtil.isBlank(resource))
                responder = new WikiPageResponder();
            else if (resource.startsWith("files/") || resource.equals("files"))
                responder = FileResponder.makeResponder(request, rootPath);
            else if (WikiWordPath.isWikiWord(resource) || "root".equals(resource))
                responder = new WikiPageResponder();
            else
                responder = new NotFoundResponder();
        }

        return responder;
    }

    private Responder lookupResponder(String responderKey)
            throws InstantiationException {
        Class<?> responderClass = getResponderClass(responderKey);
        if (responderClass != null) {
            try {
                return newResponderInstance(responderClass, rootPath);
            } catch (Exception e) {
                e.printStackTrace();
                throw new InstantiationException("Unable to instantiate responder " + responderKey);
            }
        }
        throw new InstantiationException("No responder for " + responderKey);
    }

    private static Responder newResponderInstance(Class<?> responderClass, String rootPath)
            throws InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        try {
            Constructor<?> constructor = responderClass.getConstructor(String.class);
            return (Responder) constructor.newInstance(rootPath);
        } catch (NoSuchMethodException e) {
            Constructor<?> constructor = responderClass.getConstructor();
            return (Responder) constructor.newInstance();
        }
    }

    // only used by this class and tests
    public Class<?> getResponderClass(String responderKey) {
        try {
            return responderMap.get(responderKey).create().getClass();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception when looking up or creating responder", e);
        }
    }

    private boolean usingResponderKey(String responderKey) {
        return !("".equals(responderKey) || responderKey == null);
    }
}
