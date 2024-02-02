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

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * i18n 工具类
 *
 * @author ken
 */
public class MessageUtils {

    private static final MessageSource messageSource = ContextUtils.getBeanOfType(MessageSource.class);

    public static String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    public static String getMessageDefault(String code, String defaultMessage, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    public static String getMessage(Locale locale, String code, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }

    public static String getMessageDefault(Locale locale, String code, String defaultMessage, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }
}
