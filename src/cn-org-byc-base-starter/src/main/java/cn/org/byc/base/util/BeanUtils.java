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

import org.springframework.cglib.beans.BeanMap;
import org.springframework.util.ClassUtils;

import java.util.Map;

public class BeanUtils {

    /**
     * 将Java Bean对象转换为Map。
     * 该方法自动识别Bean的实际类并委托到另一个重载的toMap方法。
     *
     * @param valueBean 需要转换成Map的Java Bean对象。如果对象是null，则直接返回null。
     * @return 转换后的Map，其中键是Bean的属性名，值是对应的属性值。如果输入是null，则返回null。
     */
    public static Map<String, Object> toMap(Object valueBean) {
        if (valueBean == null) {
            return null;
        }
        // 使用ClassUtils.getUserClass方法获取Bean的实际类，以处理可能的CGLIB代理类等情况。
        Class<?> clazz = ClassUtils.getUserClass(valueBean);
        return toMap(valueBean, clazz);
    }

    /**
     * 将Java Bean对象转换为Map，允许显式指定Bean的类。
     * 如果valueBean已经是Map类型，则直接返回该Map。
     *
     * @param valueBean 需要转换成Map的Java Bean对象。如果对象是null，或beanClass是null，则直接返回null。
     * @param beanClass Java Bean对象的类，用于指导如何提取属性值。
     * @return 转换后的Map，其中键是Bean的属性名，值是对应的属性值。如果输入是null，则返回null。
     */
    public static Map<String, Object> toMap(Object valueBean, Class<?> beanClass) {
        if (valueBean == null || beanClass == null) {
            return null;
        }

        // 检查valueBean是否已经是Map实例，如果是，则直接返回。
        if (valueBean instanceof Map<?, ?>) {
            return (Map<String, Object>) valueBean;
        }

        // 使用BeanMap的Generator来转换Bean到Map。BeanMap是一个特殊的Map实现，能够自动同步Bean和Map之间的数据。
        BeanMap.Generator generator = new BeanMap.Generator() {
            // 重写setNamePrefix方法以添加自定义前缀，可以用于特定的命名策略或标识。
            @Override
            protected void setNamePrefix(String namePrefix) {
                super.setNamePrefix(namePrefix.concat("$ByEaf"));
            }

        };

        // 配置Generator使用缓存和尝试加载类，提高转换效率。
        generator.setUseCache(true);
        generator.setAttemptLoad(true);

        // 设置要转换的Bean及其类。
        generator.setBean(valueBean);
        generator.setBeanClass(beanClass);

        // 创建并返回Bean的Map表示。
        return generator.create();
    }
}
