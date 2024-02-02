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

package cn.org.byc.base.support.beans;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

public class CustomBeanNameGenerator extends AnnotationBeanNameGenerator {
    /**
     * 自定义bean名称
     * @param definition the bean definition to generate a name for
     * @param registry   the bean definition registry that the given definition
     *                   is supposed to be registered with
     * @return
     */
    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        String beanName = super.generateBeanName(definition, registry);
        if (beanName.endsWith("Impl")){
            beanName = beanName.substring(0, beanName.length() - 4);
        }
        return beanName;
    }
}
