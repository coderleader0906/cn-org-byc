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

package cn.org.byc.base.model;

import cn.org.byc.base.support.CustomBeanWrapper;
import cn.org.byc.base.util.DateUtils;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Date;

public abstract class BaseModel implements Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getClass().getName());
        result.append("{");

        CustomBeanWrapper beanWrapper = new CustomBeanWrapper(this);
        PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptorsInDeclaringOrder();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String propertyName = propertyDescriptor.getName();
            if ("class".equals(propertyName)){
                continue;
            }
            if (!beanWrapper.isReadableProperty(propertyName)){
                continue;
            }
            result.append(propertyName);
            result.append("=");
            Object propertyValue = beanWrapper.getPropertyValue(propertyName);
            if (propertyValue instanceof String){
                propertyValue = "\"" + propertyValue + "\"";
            }else if (propertyValue instanceof Date date){
                propertyValue = DateUtils.formatDateTime(date);
            }
            result.append(propertyValue);
            result.append(", ");
        }
        if (propertyDescriptors.length>0){
            result.setLength(result.length() - 2);
        }
        return result.toString();
    }
}
