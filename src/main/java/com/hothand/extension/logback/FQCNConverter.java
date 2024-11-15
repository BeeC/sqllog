package com.hothand.extension.logback;

import ch.qos.logback.classic.pattern.NamedConverter;
import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class FQCNConverter extends NamedConverter {

    private String[] postfixes;

    public FQCNConverter(String... postfixes) {
        this.postfixes = postfixes;
    }

    public FQCNConverter() {
        this("Logger", "Log");
    }

    @Override
    protected String getFullyQualifiedName(ILoggingEvent event) {
        StackTraceElement[] cda = event.getCallerData();
        if (cda != null && cda.length > 0) {
            for (StackTraceElement stack : cda) {
                if (!fit(stack.getClassName())) {
                    return stack.getClassName() + "." + stack.getMethodName() + ":" + stack.getLineNumber();
                }
            }
            return cda[0].getClassName() + "." + cda[0].getMethodName() + ":" + cda[0].getLineNumber();
        } else {
            return CallerData.CALLER_DATA_NA;
        }
    }

    private boolean fit(String name) {
        for (String fix : postfixes) {
            if (name.endsWith(fix)) {
                return true;
            }
        }
        return false;
    }
}
