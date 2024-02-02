/*
 * Copyright 2023-2024 the Ken.Zhang
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

package cn.org.byc.base.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * spring 上下文工具类
 *
 * @author ken
 *
 * @see ContextUtils#notifyAll()
 * @see ContextUtils#wait(long)
 */
@Slf4j
public class ContextUtils {
    private static ApplicationContext applicationContext;

    /**
     * 等待/通知机制来处理线程间同步
     * 设置applicationContext
     * @param applicationContext
     */
    public static void setApplicationContext(ApplicationContext applicationContext){
        synchronized (ContextUtils.class) {
            log.debug("setApplicationContext, notify all");
            ContextUtils.applicationContext = applicationContext;
            ContextUtils.class.notifyAll();
        }
    }

    /**
     * 获取applicationContext
     *
     * @see ContextUtils#setApplicationContext(ApplicationContext)
     * @return
     */
    public static ApplicationContext getApplicationContext() {
        synchronized (ContextUtils.class) {
            while (applicationContext == null) {
                try {
                    log.debug("getApplicationContext, wait...");
                    ContextUtils.class.wait(60000);
                    if (applicationContext == null) {
                        log.warn("Have been waiting for ApplicationContext to be set for 1 minute", new Exception());
                    }
                } catch (InterruptedException ex) {
                    log.debug("getApplicationContext, wait interrupted");
                }
            }
            return applicationContext;
        }
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    public static <T> T getBean(String name, Class<T> type) {
        return (T) getApplicationContext().getBean(name);
    }

    public static Class<?> getType(String name) {
        return getApplicationContext().getType(name);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        return getApplicationContext().getBeansOfType(type);
    }

    public static <T> T getBeanOfType(Class<T> type) {
        Map<String, T> beans = getBeansOfType(type);
        if (beans.size() == 0) {
            throw new NoSuchBeanDefinitionException(type,
                    "Unsatisfied dependency of type [" + type + "]: expected at least 1 matching bean");
        }
        if (beans.size() > 1) {
            throw new NoSuchBeanDefinitionException(type,
                    "expected single matching bean but found " + beans.size() + ": " + beans.keySet());
        }
        return beans.values().iterator().next();
    }
}
