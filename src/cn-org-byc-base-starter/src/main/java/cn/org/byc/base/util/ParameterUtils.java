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

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 参数有效性检查工具类
 *
 * @author ken
 */
public class ParameterUtils {

    // 定义逗号字符作为分隔符
    private static final char SEPARATOR = ',';
    // 将分隔符转换为字符串格式
    private static final String SEPARATOR_STRING = String.valueOf(SEPARATOR);

    /**
     * 参数有效性检查
     * @param value
     * @return
     */
    public static boolean isParamValid(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String strValue) {
            if (strValue.trim().length() == 0) {
                return false;
            }
        }
        if (value.getClass().isArray()) {
            if (ArrayUtils.getLength(value) == 0 || Array.get(value, 0) == null) {
                return false;
            }
        }
        if (value instanceof Collection) {
            if (((Collection<?>) value).isEmpty() || ((Collection<?>) value).iterator().next() == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 连接字符串数组的元素
     * @param values
     * @return
     */
    public static String joinValues(String[] values) {
        if (values == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().length() == 0) {
                continue;
            }
            // 添加分隔符，并处理字符串中的分隔符
            sb.append(SEPARATOR);
            sb.append(value.replace(SEPARATOR_STRING, SEPARATOR_STRING + SEPARATOR_STRING));
        }
        return sb.toString();
    }

    /**
     * 分割字符串数组
     * @param values
     * @return
     */
    public static String[] splitValues(String values) {
        if (values == null) {
            return null;
        }
        List<String> valueList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length(); i++) {
            char c = values.charAt(i);
            // 检测分隔符
            if (c == SEPARATOR) {
                // 如果连续两个分隔符，视为转义，添加一个分隔符到结果
                if (i + 1 < values.length() && values.charAt(i + 1) == SEPARATOR) {
                    sb.append(SEPARATOR);
                    i++;
                } else {
                    // 否则视为字符串分割点
                    if (sb.length() > 0) {
                        valueList.add(sb.toString());
                        sb.setLength(0);
                    }
                }
            } else {
                // 添加非分隔符字符到当前段
                sb.append(c);
            }
            // 如果是字符串末尾，添加剩余部分到列表
            if (i == values.length() - 1) {
                if (sb.length() > 0) {
                    valueList.add(sb.toString());
                }
            }
        }
        return valueList.toArray(new String[valueList.size()]);
    }
}
