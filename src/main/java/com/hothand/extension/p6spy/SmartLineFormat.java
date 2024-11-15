package com.hothand.extension.p6spy;

import com.hothand.extension.DataSourceLookup;
import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

public class SmartLineFormat implements MessageFormattingStrategy {


    private SqlLogPropertiesProvider propertiesProvider = new DataSourceLookup();

    private static boolean detectPerf;

    static {
        try {
            Class.forName("com.kuaishou.infra.framework.common.internal.CommonPerfs");
            detectPerf = true;
        } catch (Throwable e) {
            detectPerf = false;
        }
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        String db = getDbFromUrl(url);
        String[] operation = new String[1];
        String table = getTableFromSql(prepared, operation);
        String[] dbTable = getTableFromSql(prepared, operation).split("\\.", 2);
        if (dbTable.length > 1) {
            db = dbTable[0];
            table = dbTable[1];
        }
        String sqlSchema = sqlSchema(prepared);
        boolean hasError = category.equalsIgnoreCase(Category.ERROR.getName());
        StringBuilder sb = new StringBuilder("sqllog@")
                .append(now)
                .append(" | ")
                .append(operation[0])
                .append(" | ")
                .append(db)
                .append("@")
                .append(table)
                .append(" | ")
                .append(hasError ? "failed" : "success")
                .append(" | ")
                .append(elapsed)
                .append("ms")
                .append(" | ")
                .append(url)
                .append("\n")
                .append(sqlSchema)
                .append("\n")
                .append(abbrSql(sql, propertiesProvider.get()))
                .append(";");
        if (detectPerf && propertiesProvider.get().isEnablePerf()) {
            /*CommonPerfs.commonPerf().perfContext(propertiesProvider.get().getPertNamespace(), db, table, sqlSchema)
                    .bizDef(propertiesProvider.get().getPertBizDef()).millis(elapsed)
                    .logLocalAndRemote();*/
        }
        return sb.toString();
    }


    /**
     * 从URL中获取数据库名
     *
     * @param url
     * @return
     */
    private String getDbFromUrl(String url) {
        return url.replaceAll(".*\\/([^?/]*).*", "$1");
    }

    /**
     * @param sql
     * @return
     */
    public static String getTableFromSql(String sql) {
        return getTableFromSql(sql, new String[1]);
    }

    /**
     * 从SQL中获取表名
     *
     * @param sql
     * @return
     */
    private static String getTableFromSql(String sql, String[] operation) {
        String select = "(?is).*?select.+?from\\s+([0-9a-zA-Z$_.]+).*";
        String delete = "(?is).*?delete.+?from\\s+([0-9a-zA-Z$_.]+).*";
        String update = "(?is).*?update\\s+([0-9a-zA-Z$_.]+).*";
        String insert = "(?is).*?insert.+?into\\s+([0-9a-zA-Z$_.]+).*";
        if (sql.matches(select)) {
            operation[0] = "select";
            return sql.replaceAll(select, "$1");
        } else if (sql.matches(delete)) {
            operation[0] = "delete";
            return sql.replaceAll(delete, "$1");
        } else if (sql.matches(update)) {
            operation[0] = "update";
            return sql.replaceAll(update, "$1");
        } else if (sql.matches(insert)) {
            operation[0] = "insert";
            return sql.replaceAll(insert, "$1");
        } else {
            operation[0] = "other";
            return "unknown";
        }
    }

    /**
     * SQL模板化，对于动态参数只保留一个占位符
     *
     * @param sql
     * @return
     */
    private String sqlSchema(String sql) {
        return abbrSql(sql, 1);
    }


    /**
     * 渲染SQL
     *
     * @param sql
     * @param properties
     * @return
     */
    private String abbrSql(String sql, SqlLogProperties properties) {
        //SQL超长时缩略打印
        if (properties.isEnableSqlAbbr() && sql.length() > properties.getSqlAbbrLimit()) {
            sql = abbrSql(sql, properties.getParamAbbrLimit());
            //参数缩略后SQL仍超长，则进行截断
            if (sql.length() > properties.getSqlAbbrLimit()) {
                sql = sql.substring(0, properties.getParamAbbrLimit());
            }
        }
        return sql;
    }

    /**
     * SQL缩略打印
     *
     * @param sql
     * @param paramAbbrLimit
     * @return
     */
    public static String abbrSql(String sql, int paramAbbrLimit) {
        String[] sqlAbbrPatterns = sqlAbbrPatterns(paramAbbrLimit);
        // 优先缩略参数列表显示
        try {
            return sql.replaceAll(sqlAbbrPatterns[0], sqlAbbrPatterns[1]);
        } catch (Exception e) {
            //ignore
        }
        return sql;
    }


    /**
     * SQL缩略打印正则表达式
     *
     * @param paramAbbrLimit
     * @return
     */
    private static String[] sqlAbbrPatterns(int paramAbbrLimit) {
        //(?s) 使.匹配包括换行在内的所有字符
        return new String[]{"(?is)(in\\s*\\(([^,]+,){" + paramAbbrLimit + "})([^)]*)(\\))", "$1...$4"};
    }

}
