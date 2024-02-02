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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.*;

public class UniversalBeanRegistration implements BeanPostProcessor, ApplicationContextAware {

    private List<BeansPropertyEditor> editorList = Collections.emptyList();
    private List<Converter<?,?>> converterList = Collections.emptyList();
    private List<GenericConverter> genericConverterList = Collections.emptyList();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        initialRegistration(applicationContext);
    }

    public void initialRegistration(ApplicationContext applicationContext) {
        Collection<BeansPropertyEditor> beansPropertyEditors = getFromContext(BeansPropertyEditor.class, applicationContext);
        editorList = new ArrayList<>();
        editorList.addAll(beansPropertyEditors);

        Iterator<BeansPropertyEditor> beansPropertyEditorIterator = ServiceLoader.load(BeansPropertyEditor.class).iterator();
        for (;beansPropertyEditorIterator.hasNext();){
            BeansPropertyEditor next = beansPropertyEditorIterator.next();
            if (!editorList.contains(next)){
                editorList.add(next);
            }
        }

        Collection<Converter> converters = getFromContext(Converter.class, applicationContext);
        converterList = new ArrayList<>();
        for (Converter converter : converters) {
            converterList.add(converter);
        }
        Iterator<Converter> converterIterator = ServiceLoader.load(Converter.class).iterator();
        for (;converterIterator.hasNext();){
            Converter next = converterIterator.next();
            if (!converterList.contains(next)){
                converterList.add(next);
            }
        }

        Collection<GenericConverter> genericConverters = getFromContext(GenericConverter.class, applicationContext);
        genericConverterList = new ArrayList<>();
        for (GenericConverter genericConverter : genericConverters) {
            genericConverterList.add(genericConverter);
        }

        Iterator<GenericConverter> genericConverterIterator = ServiceLoader.load(GenericConverter.class).iterator();
        for(;genericConverterIterator.hasNext();){
            GenericConverter next = genericConverterIterator.next();
            if (!genericConverterList.contains(next)){
                genericConverterList.add(next);
            }
        }
    }

    private <T> Collection<T> getFromContext(Class<T> clazz, ApplicationContext applicationContext) {
        return applicationContext != null ? applicationContext.getBeansOfType(clazz).values() : Collections.emptyList();
    }
}
