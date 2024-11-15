package com.hothand.extension.p6spy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties(prefix = "sqllogging")
@Data
public class SqlLogProperties {

    /**
     * use for formatting log messages
     */
    private final String logMessageFormat = "com.kuaishou.sqllog.p6spy.SmartLineFormat";

    /**
     * JDBC drivers to load and register
     */
    private String driverlist = System.getProperty("sqllogging.p6spy.driverlist", "com.mysql.jdbc.Driver,org.h2.Driver");

    /**
     * appender to use for logging
     */
    private String appender = System.getProperty("sqllogging.p6spy.appender", "com.p6spy.engine.spy.appender.Slf4JLogger");


    /**
     * sql param's length limit, only for in/not in(....)
     */
    private int paramAbbrLimit = Integer.parseInt(System.getProperty("sqllogging.paramAbbrLimit", "500"));

    /**
     * sql sentence's length limit
     */
    private int sqlAbbrLimit = Integer.parseInt(System.getProperty("sqllogging.sqlAbbrLimit", "5000"));

    /**
     * whether to enable sql abbr
     */
    private boolean enableSqlAbbr = Boolean.parseBoolean(System.getProperty("sqllogging.enableSqlAbbr", "true"));


    /**
     * whether to enable perf
     */
    private boolean enablePerf = Boolean.parseBoolean(System.getProperty("sqllogging.perf.enable", "true"));

    /**
     * whether to enable perf
     */
    private String pertNamespace = System.getProperty("sqllogging.perf.namespace", "ad.sql.logging");
    private String pertBizDef = System.getProperty("sqllogging.perf.bizDef", "ks.ad.infra");

    public void populate(Properties properties) {
        properties.setProperty("p6spy.config.driverlist", driverlist);
        properties.setProperty("p6spy.config.appender", appender);
        properties.setProperty("p6spy.config.logMessageFormat", logMessageFormat);
    }


}
