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

import cn.org.byc.base.util.DateUtils;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期类型属性编辑器
 * @author ken
 * @see java.beans.PropertyEditorSupport
 * @see CustomBeanWrapper
 */
public class CustomDateEditor extends PropertyEditorSupport {

    @Override
    public String getAsText() {
        Object value = getValue();
        if (value instanceof Date date) {
            Long time = DateUtils.formatDateTime(date);
            return time == null ? "" : time.toString();
        } else {
            return super.getAsText();
        }
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().length() == 0){
            setValue(null);
            return;
        }
        setValue(DateUtils.parse(text));
    }
}
