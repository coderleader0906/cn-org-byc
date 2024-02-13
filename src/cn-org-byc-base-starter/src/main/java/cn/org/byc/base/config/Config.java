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

package cn.org.byc.base.config;

import cn.org.byc.base.util.ResourceUtils;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Config类提供了一个中央配置管理功能，允许从不同来源加载和合并配置属性。
 * 它支持环境特定的配置属性加载，属性占位符的解析，以及配置属性的动态更新。
 *
 * @author ken
 */
public final class Config {
    // 环境配置对象，用于存储和管理应用的配置属性。
    private static ConfigurableEnvironment ENV = new StandardEnvironment();
    // 系统运行环境的配置键。
    private static final String RUN_ENV_KEY = "sys.runEnv";
    // 属性占位符帮助器，用于解析配置字符串中的占位符。
    private static final PropertyPlaceholderHelper HELPER = new PropertyPlaceholderHelper(
            SystemPropertyUtils.PLACEHOLDER_PREFIX, SystemPropertyUtils.PLACEHOLDER_SUFFIX,
            SystemPropertyUtils.VALUE_SEPARATOR, true);

    private Config() {
    }
    /**
     * 释放当前环境配置对象，用于重新初始化或清理资源。
     */
    public static void release() {
        ENV = null;
    }

    /**
     * 使用给定的环境配置对象初始化配置管理器。
     * 如果已经存在一个环境配置对象，则将给定环境的属性合并到现有环境中。
     *
     * @param env 新的环境配置对象。
     */
    public static void init(ConfigurableEnvironment env) {
        if (env != null) {
            if (ENV == null) {
                ENV = env;
            } else {
                env.merge(ENV);
                ENV = env;
            }
        }
    }

    /**
     * 使用给定的属性映射初始化环境配置。
     * 这允许从Map对象中加载配置属性。
     *
     * @param props 属性映射。
     */
    public static void init(Map<String, String> props) {
        init(props, ENV);
    }

    /**
     * 使用给定的属性映射和环境配置对象初始化配置管理器。
     * 如果属性源不存在，则创建一个新的属性源并将其添加到环境配置中。
     *
     * @param props 属性映射。
     * @param env 环境配置对象。
     */
    public static void init(Map<String, String> props, ConfigurableEnvironment env) {
        init((ConfigurableEnvironment) null);
        if (!CollectionUtils.isEmpty(props)) {
            final String name = "bycProperties";
            MutablePropertySources sources = env.getPropertySources();
            PropertySource<?> source = sources.get(name);
            if (source instanceof MapPropertySource) {
                MapPropertySource mapSource = (MapPropertySource) source;
                mapSource.getSource().putAll(props);
            } else if (source == null) {
                Map map = props;
                MapPropertySource mapSource = new MapPropertySource(name, map);
                // 根据存在的属性源名称确定新属性源的添加位置。
                if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
                    sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mapSource);
                } else if (sources.contains(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
                    sources.addAfter(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mapSource);
                } else {
                    sources.addFirst(mapSource);
                }
            }
        }
    }
    /**
     * 根据运行环境加载对应的配置属性文件。
     * 支持从文件系统或类路径加载配置文件。
     *
     * @param env 环境配置对象。
     */
    public static void loadRunEnvProperties(ConfigurableEnvironment env) {
        // 获取运行环境标识，决定加载哪个配置文件。
        String runEnv = getRunEnv(env);
        final String propFile = "application-" + (runEnv) + (".properties");
        // 尝试从文件系统加载配置文件。
        File file = new File(propFile);
        Properties props = new Properties();
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        } else {
            // 尝试从类路径加载配置文件。
            Resource res = ResourceUtils.getResource("classpath:/" + propFile);
            if (res.exists()) {
                try (InputStream is = res.getInputStream()) {
                    props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        // 将加载的配置属性添加到环境配置中。
        if (!props.isEmpty()) {
            MutablePropertySources sources = env.getPropertySources();
            PropertiesPropertySource pps = new PropertiesPropertySource(runEnv, props);
            if (sources.contains("applicationConfig")) {
                sources.addBefore("applicationConfig", pps);
            } else if(sources.contains("systemEnvironment")) {
                sources.addAfter("systemEnvironment", pps);
            } else {
                sources.addFirst(pps);
            }
        }
    }

    /**
     * 使用Properties对象初始化配置管理器。
     * 这允许从Properties对象中加载配置属性。
     *
     * @param props Properties对象。
     */
    public static void init(Properties props) {
        Map map = props;
        init(map);
    }

    /**
     * 获取当前配置环境。
     *
     * @return 当前的可配置环境对象。
     */
    public static ConfigurableEnvironment env() {
        return ENV;
    }

    /**
     * 替换字符串中的占位符为实际的配置值。
     * 使用递归处理嵌套占位符的情况。
     *
     * @param str 待处理的字符串，可能包含一个或多个占位符。
     * @return 替换占位符后的字符串。
     */
    public static String replacePlaceholders(String str) {
        // 检查字符串是否为空或不包含占位符
        if (StringUtils.isEmpty(str) || str.indexOf('$') < 0) {
            return str;
        }
        // 替换占位符
        String value = HELPER.replacePlaceholders(str, Config::getProperty);
        // 如果替换后的值仍然包含占位符，递归处理以支持嵌套占位符
        if (StringUtils.hasText(value) && !value.equals(str) && value.contains("${")) {
            return replacePlaceholders(value);
        }
        return value;
    }

    /**
     * 获取当前运行环境的名称。
     *
     * @return 运行环境名称，如未设置，则默认为"dev"。
     */
    public static String getRunEnv() {
        return getRunEnv(ENV);
    }

    /**
     * 从环境配置中获取运行环境的名称。
     * 首先尝试从配置中直接获取，若未设置，则尝试从Spring激活的profiles中获取。
     *
     * @param env 可配置环境对象。
     * @return 运行环境名称。
     */
    private static String getRunEnv(ConfigurableEnvironment env) {
        String runEnv = env.getProperty(RUN_ENV_KEY);
        // 若直接获取为空，尝试解析Spring激活的profiles作为运行环境
        if (StringUtils.isEmpty(runEnv)) {
            runEnv = getSpringActiveProfilesAsRunEnv(env);
            if (StringUtils.isEmpty(runEnv)) {
                runEnv = "dev";
            }
        }
        return runEnv;
    }

    /**
     * 尝试从系统属性、环境变量及Spring激活的profiles中获取运行环境。
     *
     * @param env 可配置环境对象。
     * @return 运行环境名称，若未设置则返回null。
     */
    private static String getSpringActiveProfilesAsRunEnv(ConfigurableEnvironment env) {
        String temp = System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
        if (StringUtils.isEmpty(temp)) {
            temp = System.getenv(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
            if (StringUtils.isEmpty(temp)) {
                temp = env.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
            }
        }
        // 解析激活的profiles，取第一个作为运行环境
        if (StringUtils.hasText(temp)) {
            if (temp.indexOf(',') > -1) {
                String[] array = StringUtils.tokenizeToStringArray(temp, ",");
                if (array.length > 0) {
                    temp = array[0];
                }
            }
        }
        return temp;
    }

    /**
     * 根据键获取配置属性的值。
     *
     * @param key 配置键。
     * @return 配置值，若不存在则返回null。
     */
    public static String getProperty(String key) {
        if (RUN_ENV_KEY.equals(key)) {
            return getRunEnv();
        }
        return ENV.getProperty(key);
    }

    /**
     * 根据键获取配置属性的值，若不存在则返回默认值。
     *
     * @param key 配置键。
     * @param defaultValue 默认值。
     * @return 配置值，若不存在则返回默认值。
     */
    public static String getProperty(String key, String defaultValue) {
        return ENV.getProperty(key, defaultValue);
    }

    /**
     * 根据键获取配置属性的值，并转换为指定类型。
     *
     * @param key 配置键。
     * @param targetType 值的目标类型。
     * @param <T> 目标类型参数。
     * @return 配置值的目标类型实例，若不存在则返回null。
     */
    public static <T> T getProperty(String key, Class<T> targetType) {
        return ENV.getProperty(key, targetType);
    }

    /**
     * 从环境配置中获取指定键的属性值，并尝试将该值转换为指定的目标类型。
     * 如果指定键的属性不存在，或者无法将属性值转换为目标类型，则返回提供的默认值。
     *
     * @param key 想要获取的属性的键名。
     * @param targetType 属性值应该被转换成的目标类型的Class对象。
     * @param defaultValue 如果指定键的属性不存在或无法转换为目标类型时返回的默认值。
     * @param <T> 目标类型的泛型参数。
     * @return 转换为目标类型的属性值，或在无法获取指定键的属性时返回默认值。
     */
    public static <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        // 调用ENV.getProperty方法尝试获取并转换属性值。
        // 如果指定键不存在或不能被转换成目标类型，则返回提供的默认值。
        return ENV.getProperty(key, targetType, defaultValue);
    }
}
