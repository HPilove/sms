package com.app;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.log4j.PropertyConfigurator;
import org.sms.process.sms.MessageQueueEx;
import smartlib.dictionary.Dictionary;
import smartlib.dictionary.DictionaryNode;
import smartlib.thread.FileThreadLister2;
import smartlib.thread.ProcessorListener;
import smartlib.thread.ThreadManager;
import smartlib.thread.ThreadProcessor;
import smartlib.util.Cache;
import smartlib.util.Global;
import smartlib.util.LogOutputStream;
import smartlib.util.StringUtil;

import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;
import java.util.Hashtable;
import java.util.Vector;

public class AppManager implements ProcessorListener {
    private static HikariDataSource datasource;
    private final Hashtable<Object, Object> mVariables = new Hashtable<>();
    private Dictionary dic = null;

    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("configuration/log4j.properties");
        AppManager appManager = new AppManager();
        try {
            appManager.initSystem();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String getParameter(String strKey, String strDefault) {
        String res = dic.getString(strKey);
        if (res == null || res.equals("")) {
            res = strDefault;
        }
        return res;
    }

    public String getParameter(String strKey) {
        return getParameter(strKey, "");
    }


    public Connection getConnection() throws Exception {
        return datasource.getConnection();
    }

    public void onCreate(ThreadProcessor processor) {
    }

    public void onOpen(ThreadProcessor processor) throws Exception {
        processor.mcnMain = getConnection();
    }

    public void initSystem() throws Exception {
        MessageQueueEx queueSubmit = new MessageQueueEx();
        setCommonVariable("QueueSubmit", queueSubmit);

        dic = new Dictionary("configuration/server.txt");
        String strNode = dic.getString("DefaultDatabase");
        int iConnectPoolSize = 20;
        try {
            iConnectPoolSize = Integer.parseInt(dic.getString("ConnectionPoolSize"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Cache.CACHE_DURATION = Long.parseLong(getParameter("CacheDuration")) * 1000L;
        } catch (Exception ignored) {
        }

        try {
            Cache.CHECK_INTERVAL = Long.parseLong(getParameter("CheckInterval")) * 1000L;
        } catch (Exception ignored) {
        }

        DictionaryNode ndConnection = dic.getChild("Connection." + strNode);
        int minimumIdle = Integer.parseInt(StringUtil.evl(ndConnection.getString("MinimumIdle"), "10"));
        long maxLifetime = Long.parseLong(StringUtil.evl(ndConnection.getString("MaxLifetime"), "1800000"));
        long idleTimeout = Long.parseLong(StringUtil.evl(ndConnection.getString("IdleTimeout"), "600000"));
        long connectionTimeout = Long.parseLong(StringUtil.evl(ndConnection.getString("ConnectionTimeout"), "30000"));
        DictionaryNode ndProperties = ndConnection.getChildIgnoreCase("Properties");
        //HikariCP
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(ndConnection.getString("Url"));
        config.setUsername(ndConnection.getString("UserName"));
        config.setPassword(ndConnection.getString("Password"));

        config.setMaximumPoolSize(iConnectPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setMaxLifetime(maxLifetime);
        config.setIdleTimeout(idleTimeout);
        config.setConnectionTimeout(connectionTimeout);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        if (ndProperties != null) {
            Vector<DictionaryNode> vtPropChild = ndProperties.getChildList();
            for (DictionaryNode nd : vtPropChild) {
                config.addDataSourceProperty(nd.mstrName, nd.mstrValue);
            }
        }
        datasource = new HikariDataSource(config);
        //End HikariCP

        String strLogFile = getParameter("ErrorLog", "error.log");
        String strWorkingDir = System.getProperty("user.dir");
        if (!strWorkingDir.endsWith("/") || !strWorkingDir.endsWith("\\")) {
            strWorkingDir += "/";
        }
        File fl = new File(strWorkingDir + strLogFile);
        if (fl.getParentFile() != null) {
            fl.getParentFile().mkdirs();
        }
        PrintStream ps = new PrintStream(new LogOutputStream(strWorkingDir + strLogFile));
        System.setOut(ps);
        System.setErr(ps);

        Global.APP_NAME = "SMS Thread Manager";

        int iPortID = 8338;
        try {
            iPortID = Integer.parseInt(getParameter("PortID"));
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }

        ThreadManager cs = new ThreadManager(iPortID, this) {
            @Override
            public void close() {
                super.close();
                AppManager.this.close();
            }
        };

        // Set action log file
        strLogFile = getParameter("ActionLog");
        fl = new File(strLogFile);
        if (fl.getParentFile() != null) {
            fl.getParentFile().mkdirs();
        }
        if (!strLogFile.equals("")) {
            cs.setActionLogFile(strLogFile);
        }
        // Set action log file
        strLogFile = getParameter("AlertLog");
        fl = new File(strLogFile);
        if (fl.getParentFile() != null) {
            fl.getParentFile().mkdirs();
        }
        if (!strLogFile.equals("")) {
            cs.setAlertLogFile(new File(strLogFile));
        }
        // Set max logfile size
        try {
            if (!getParameter("MaxLoggingSize").equals("")) {
                int iMaxLogFileSize = Integer.parseInt(getParameter("MaxLoggingSize"));
                if (iMaxLogFileSize > 0) {
                    cs.setMaxLogFileSize(iMaxLogFileSize);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set MaxLogContentSize
        try {
            if (!getParameter("MaxLogContentSize").equals("")) {
                int iMaxLogContentSize = Integer.parseInt(getParameter("MaxLogContentSize"));
                if (iMaxLogContentSize > 0) {
                    cs.setMaxLogContentSize(iMaxLogContentSize);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set max connection
        try {
            int iMaxConnectionAllowed = Integer.parseInt(getParameter("MaxConnectionAllowed"));
            if (iMaxConnectionAllowed > 0) {
                cs.setMaxConnectionAllowed(iMaxConnectionAllowed);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cs.getThreadListers().clear();
        cs.getThreadListers().add(new FileThreadLister2("configuration/thread/"));
        // Start manager
        cs.start();
    }

    private void close() {
        if (datasource != null) {
            datasource.close();
            datasource = null;
        }
    }

    public void setCommonVariable(Object name, Object value) {
        mVariables.put(name, value);
    }

    public Object getCommonVariable(Object object) {
        return mVariables.get(object);
    }
}
