package com.hothand.extension;

import com.hothand.extension.p6spy.SqlLogProperties;
import com.hothand.extension.p6spy.SqlLogPropertiesProvider;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


@EnableConfigurationProperties(SqlLogProperties.class)
@AutoConfigureOrder(Integer.MIN_VALUE)
public class DataSourceLookup implements ApplicationContextInitializer, ApplicationContextAware, SqlLogPropertiesProvider {

    public static SqlLogProperties sqlLogProperties = new SqlLogProperties();
    private static AtomicBoolean installed = new AtomicBoolean(false);


    private static void testDriver(Properties properties) {
        String name = "p6spy.config.driverlist";
        String value = properties.getProperty(name, "");
        properties.setProperty(name, Arrays.stream(value.split(",")).filter(s -> {
            try {
                Class.forName(s);
                return true;
            } catch (Throwable e) {
                return false;
            }
        }).collect(Collectors.joining(",")));
        //load to system properties
        properties.forEach((k, v) -> System.setProperty((String) k, (String) v));

    }

    //load from spy.properties
    private static Properties loadSpyProperties() {
        Properties dest = new Properties();
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Properties src = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = resourceLoader.getResource("spy.properties").getInputStream();
            src.load(inputStream);
        } catch (Throwable e) {
            //ignore
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        src.forEach((k, v) -> dest.setProperty("p6spy.config." + k, (String) v));
        return dest;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        sqlLogProperties = applicationContext.getBean(SqlLogProperties.class);
        Properties spyProperties = loadSpyProperties();
        sqlLogProperties.populate(spyProperties);
        testDriver(spyProperties);
    }

    @Override
    public SqlLogProperties get() {
        return sqlLogProperties;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        install();
    }

    private static void install() {
        if (installed.compareAndSet(false, true)) {
            ByteBuddyAgent.install();
            ClassRedefine.redefine();
        }
    }

}
