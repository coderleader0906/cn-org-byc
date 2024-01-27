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

import cn.org.byc.base.util.ReflectionUtils;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 扩展{@link BeanWrapperImpl} 类
 *
 * @author ken
 * @see BeanWrapperImpl
 * @since 1.0.0
 */
public class CustomBeanWrapper extends BeanWrapperImpl {

    public CustomBeanWrapper(Object object) {
        super(object);
        registerCustomEditor(Date.class, new CustomDateEditor());
        registerCustomEditor(Array.newInstance(String.class,0).getClass(),new CustomStringArrayEditor());
    }

    public CustomBeanWrapper(Class<?> clazz) {
        super(clazz);
        registerCustomEditor(Date.class, new CustomDateEditor());
        registerCustomEditor(Array.newInstance(String.class,0).getClass(),new CustomStringArrayEditor());
    }

    /**
     * 根据声明顺序获取属性描述符
     * @return 属性描述符数组 PropertyDescriptor[]
     */
    public PropertyDescriptor[] getPropertyDescriptorsInDeclaringOrder() {
        // 获取父类属性描述符
        PropertyDescriptor[] propertyDescriptors = super.getPropertyDescriptors();
        // 获取方法集合
        List<Method> methodsInDeclaringOrder = ReflectionUtils.getMethodsInDeclaringOrder(getWrappedClass());
        Set<PropertyDescriptor> resultPropertyDescriptors = new LinkedHashSet<>();

        for (Method method : methodsInDeclaringOrder) {
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (method.equals(propertyDescriptor.getReadMethod()) || method.equals(propertyDescriptor.getWriteMethod())){
                    resultPropertyDescriptors.add(propertyDescriptor);
                    break;
                }
            }
        }

        return resultPropertyDescriptors.toArray(new PropertyDescriptor[resultPropertyDescriptors.size()]);
    }

    public void copyPropertiesTo(Object destinationBean, List<String> propertyNames){
        CustomBeanWrapper destinationBeanWrapper = new CustomBeanWrapper(destinationBean);
        for (String propertyName : propertyNames) {
            destinationBeanWrapper.setPropertyValue(propertyName, getPropertyValue(propertyName));
        }
    }

    /**
     * 递归获取属性值
     * @param propertyName 属性名称
     * @return 属性值 PropertyValue
     */
    public Object getPropertyValueRecursively(String propertyName){
        int dotIndex = propertyName.indexOf(".");
        if (dotIndex == -1){
            return getPropertyValue(propertyName);
        }

        Object propertyBean = getPropertyValue(propertyName.substring(0, dotIndex));
        if (propertyBean == null){
            return null;
        }
        return new CustomBeanWrapper(propertyBean)
                .getPropertyValueRecursively(propertyName.substring(dotIndex+1));
    }

    /**
     * 递归获取属性类型
     * @param propertyName 属性名称
     * @return 属性类型 PropertyType
     */
    public Class<?> getPropertyTypeRecursively(String propertyName) {
        int dotIndex = propertyName.indexOf(".");
        if (dotIndex == -1){
            return getPropertyType(propertyName);
        }

        Object propertyBean = getPropertyValue(propertyName.substring(0, dotIndex));
        if (propertyBean == null){
            return null;
        }

        return new CustomBeanWrapper(propertyBean)
                .getPropertyTypeRecursively(propertyName.substring(dotIndex + 1));
    }
}
