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

package cn.org.byc.base.support;

import cn.org.byc.base.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.*;

/**
 * Spring 上下文注册器
 *
 * @author ken
 *
 * @see ContextUtils#setApplicationContext(ApplicationContext)
 */
@Slf4j
public class ApplicationContextRegister implements ApplicationContextAware {

    /**
     * 获取Spring上下文，设置到ContextUtils中
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ContextUtils.setApplicationContext(applicationContext);
        log.debug("ApplicationContext registed");
        if(log.isDebugEnabled()){
            for(String s :applicationContext.getBeanDefinitionNames()){
                log.debug(s);
            }
        }
    }
}
