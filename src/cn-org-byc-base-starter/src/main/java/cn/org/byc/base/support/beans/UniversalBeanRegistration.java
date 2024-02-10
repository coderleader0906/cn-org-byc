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

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.*;

/**
 * 实现{@link BeanPostProcessor}和{@link ApplicationContextAware}接口的类，
 * 用于在Spring容器中对bean实例进行自定义的后处理操作，并能够访问和操作应用上下文。
 * 此类负责在bean初始化后注册自定义的属性编辑器、转换器、序列化器和反序列化器。
 *
 * @author ken
 */
public class UniversalBeanRegistration implements BeanPostProcessor, ApplicationContextAware {

    /**
     * 类的单例实例
     */
    public final static UniversalBeanRegistration INSTANCE = new UniversalBeanRegistration();

    // 标记是否已初始化，防止重复初始化。
    private boolean isInitialized;

    // 存储自定义的bean属性编辑器、转换器和序列化/反序列化器的列表。
    private List<BeansPropertyEditor> editorList = Collections.emptyList();
    private List<Converter<?, ?>> converterList = Collections.emptyList();
    private List<GenericConverter> genericConverterList = Collections.emptyList();
    private List<JsonDeserializer<?>> jsonDeserializerList = Collections.emptyList();
    private List<JsonSerializer<?>> jsonSerializerList = Collections.emptyList();


    /**
     * 在bean初始化之后调用，用于注册自定义编辑器、转换器、序列化器和反序列化器。
     *
     * @param bean     当前正在创建的bean实例。
     * @param beanName 当前bean的名称
     *
     * @return 返回可能经过处理的bean实例，也可能是原始的bean实例。
     *
     * @throws BeansException 如果在处理过程中发生错误。
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 如果尚未初始化，则进行初次注册。
        if (!isInitialized) {
            this.initialRegistration(null);
        }
        // 判断当前bean是否为ConverterRegistry或ObjectMapper的实例，以决定是否需要注册转换器或序列化/反序列化器。
        final boolean isConverterRegistry = (!converterList.isEmpty() || !genericConverterList.isEmpty())
                && bean instanceof ConverterRegistry;
        final boolean isObjectMapper = (!jsonDeserializerList.isEmpty() || !jsonSerializerList.isEmpty())
                && bean instanceof ObjectMapper;

        // 如果存在自定义编辑器并且bean是PropertyEditorRegistry的实例，则注册这些编辑器。
        if (!editorList.isEmpty() && bean instanceof PropertyEditorRegistry) {
            PropertyEditorRegistry registry = (PropertyEditorRegistry) bean;
            for (BeansPropertyEditor editor : editorList) {
                registry.registerCustomEditor(editor.supportClass(), editor);
            }
        } else if (isConverterRegistry) {  // 如果bean是ConverterRegistry的实例，则注册转换器。
            ConverterRegistry registry = (ConverterRegistry) bean;
            for (Converter<?, ?> conv : converterList) {
                registry.addConverter(conv);
            }
            for (GenericConverter conv : genericConverterList) {
                registry.addConverter(conv);
            }
        } else if (isObjectMapper) { // 如果bean是ObjectMapper的实例，则注册序列化/反序列化器。
            ObjectMapper mapper = (ObjectMapper) bean;
            SimpleModule module = new SimpleModule();
            for (JsonSerializer ser : jsonSerializerList) {
                module.addSerializer(ser.handledType(), ser);
            }
            for (JsonDeserializer der : jsonDeserializerList) {
                module.addDeserializer(der.handledType(), der);
            }
            mapper.registerModule(module);
        }
        return bean;
    }

    /**
     * 设置应用上下文，并在设置后进行自定义编辑器、转换器、序列化器和反序列化器的初始化注册
     *
     * @param applicationContext Spring应用上下文
     *
     * @throws BeansException 如果在设置应用上下文时发生错误
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        initialRegistration(applicationContext);
    }

    /**
     * 执行自定义编辑器、转换器、序列化器和反序列化器的初始化注册。
     * 该方法从应用上下文或通过ServiceLoader加载这些组件。
     *
     * @param applicationContext 可以为null，如果非null，则从中获取组件实例
     */
    public void initialRegistration(ApplicationContext applicationContext) {
        // 从应用上下文获取或通过ServiceLoader加载自定义编辑器，并添加到editorList。
        Collection<BeansPropertyEditor> beansPropertyEditors = getFromContext(BeansPropertyEditor.class, applicationContext);
        editorList = new ArrayList<>();
        editorList.addAll(beansPropertyEditors);

        // 重复上述过程，加载并添加自定义转换器到editorList。
        Iterator<BeansPropertyEditor> beansPropertyEditorIterator = ServiceLoader.load(BeansPropertyEditor.class).iterator();
        for (; beansPropertyEditorIterator.hasNext(); ) {
            BeansPropertyEditor next = beansPropertyEditorIterator.next();
            if (!editorList.contains(next)) {
                editorList.add(next);
            }
        }

        // 重复上述过程，加载并添加自定义转换器到converterList。
        Collection<Converter> converters = getFromContext(Converter.class, applicationContext);
        converterList = new ArrayList<>();
        for (Converter converter : converters) {
            converterList.add(converter);
        }
        Iterator<Converter> converterIterator = ServiceLoader.load(Converter.class).iterator();
        for (; converterIterator.hasNext(); ) {
            Converter next = converterIterator.next();
            if (!converterList.contains(next)) {
                converterList.add(next);
            }
        }

        // 加载并添加GenericConverter到genericConverterList。
        Collection<GenericConverter> genericConverters = getFromContext(GenericConverter.class, applicationContext);
        genericConverterList = new ArrayList<>();
        for (GenericConverter genericConverter : genericConverters) {
            genericConverterList.add(genericConverter);
        }

        Iterator<GenericConverter> genericConverterIterator = ServiceLoader.load(GenericConverter.class).iterator();
        for (; genericConverterIterator.hasNext(); ) {
            GenericConverter next = genericConverterIterator.next();
            if (!genericConverterList.contains(next)) {
                genericConverterList.add(next);
            }
        }

        // 加载并添加JsonSerializer到jsonSerializerList。
        Collection<JsonSerializer> jsonSerializers = getFromContext(JsonSerializer.class, applicationContext);
        jsonSerializerList = new ArrayList<>(3);
        for (JsonSerializer<?> jsonSerializer : jsonSerializers) {
            jsonSerializerList.add(jsonSerializer);
        }
        Iterator<JsonSerializer> jsonSerializerIterator = ServiceLoader.load(JsonSerializer.class).iterator();
        for (; jsonSerializerIterator.hasNext(); ) {
            JsonSerializer next = jsonSerializerIterator.next();
            if (!jsonSerializerList.contains(next)) {
                jsonSerializerList.add(next);
            }
        }

        // 加载并添加JsonDeserializer到jsonDeserializerList。
        Collection<JsonDeserializer> jsonDeserializers = getFromContext(JsonDeserializer.class, applicationContext);
        jsonDeserializerList = new ArrayList<>(3);
        for (JsonDeserializer<?> jsonDeserializer : jsonDeserializers) {
            jsonDeserializerList.add(jsonDeserializer);
        }

        Iterator<JsonDeserializer> jsonDeserializerIterator = ServiceLoader.load(JsonDeserializer.class).iterator();
        for (; jsonSerializerIterator.hasNext(); ) {
            JsonDeserializer<?> next = jsonDeserializerIterator.next();
            if (!jsonDeserializerList.contains(next)) {
                jsonDeserializerList.add(next);
            }
        }
    }

    /**
     * 从应用上下文中获取指定类型的bean集合。
     * 如果应用上下文为null，则返回一个空集合。
     *
     * @param clazz              要获取的bean的类型。
     * @param applicationContext Spring应用上下文。
     * @param <T>                bean的类型。
     *
     * @return 返回找到的bean实例的集合，或空集合。
     */
    private <T> Collection<T> getFromContext(Class<T> clazz, ApplicationContext applicationContext) {
        return applicationContext != null ? applicationContext.getBeansOfType(clazz).values() : Collections.emptyList();
    }
}
