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

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * ResourceUtils类提供静态工具方法，用于简化资源访问。
 * 该类利用Spring框架的资源解析能力，允许以统一的方式访问不同来源的资源。
 *
 * @author ken
 */
public final class ResourceUtils {
    // 使用CachingMetadataReaderFactory来提高元数据读取的性能。
    private final static MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

    /**
     * 根据给定的路径模式获取资源。
     * 该方法使用ResourcePatternResolver来解析路径模式，并返回对应的资源。
     *
     * @param pattern 资源路径模式，支持直接路径、"classpath:"前缀、"file:"前缀等。
     * @return 匹配的资源。如果资源不存在，具体行为取决于底层资源加载器的实现。
     */
    public static Resource getResource(String pattern) {
        ResourcePatternResolver resourcePatternResolver = getResourcePatternResolver();
        // 调用ResourcePatternResolver的getResource方法获取资源。
        return resourcePatternResolver.getResource(pattern);
    }

    /**
     * 获取资源模式解析器实例。
     * 本方法创建并返回一个PathMatchingResourcePatternResolver实例，
     * 用于解析资源路径模式，包括支持Ant风格的路径匹配。
     *
     * @return ResourcePatternResolver实例，用于解析资源路径模式。
     */
    public static ResourcePatternResolver getResourcePatternResolver() {
        // 返回新的PathMatchingResourcePatternResolver实例。
        return new PathMatchingResourcePatternResolver();
    }

}
