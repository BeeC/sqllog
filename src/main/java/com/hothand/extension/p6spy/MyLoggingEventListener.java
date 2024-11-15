package com.hothand.extension.p6spy;

import com.p6spy.engine.common.Loggable;
import com.p6spy.engine.logging.Category;
import com.p6spy.engine.logging.LoggingEventListener;

import java.sql.SQLException;

public class MyLoggingEventListener extends LoggingEventListener {

    @Override
    protected void logElapsed(Loggable loggable, long timeElapsedNanos, Category category, SQLException e) {
        super.logElapsed(loggable, timeElapsedNanos, e != null ? Category.ERROR : category, e);
    }
}
