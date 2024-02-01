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

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 日期工具类
 *
 * @author ken
 */
@Slf4j
public class DateUtils {
    // 定义支持的日期格式
    private static final String[] SUPPORTED_FORMATS = new String[] {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss"
    };

    // // 创建并重用SimpleDateFormat实例
    // private static final List<SimpleDateFormat> dateFormats = Arrays.stream(SUPPORTED_FORMATS)
    //         .map(SimpleDateFormat::new)
    //         .toList();

    // 定义时间戳的格式长度
    private static final int TIMESTAMP_LENGTH_SECONDS = 10;
    private static final int TIMESTAMP_LENGTH_MILLISECONDS = 13;

    public static Long formatDateTime(Date date){
        return formatDateTime(date, null);
    }

    public static Long formatDateTime(Date date, Long defaultValue){
        if (date == null){
            return defaultValue;
        }

        return date.getTime();
    }

    public static Date parse(String text) throws IllegalArgumentException {
        if (text == null || text.trim().isEmpty()){
            return null;
        }

        // 尝试解析时间戳
        if ((text.length() == TIMESTAMP_LENGTH_SECONDS || text.length() == TIMESTAMP_LENGTH_MILLISECONDS)
            && StringUtil.isNumeric(text)) {
            try {
                long time = Long.parseLong(text);
                return new Date(text.length() == TIMESTAMP_LENGTH_SECONDS ? time * 1000 : time);
            } catch (NumberFormatException e) {
                // 忽略异常，尝试其他格式解析
            }
        }

        // // 创建并重用SimpleDateFormat实例
        List<SimpleDateFormat> dateFormats = Arrays.stream(SUPPORTED_FORMATS)
                .map(SimpleDateFormat::new)
                .toList();

        // 尝试解析日期字符串
        for (SimpleDateFormat dateFormat : dateFormats) {
            try {
                dateFormat.setLenient(false);
                return dateFormat.parse(text);
            } catch (ParseException e) {
                // 忽略异常，继续尝试其他格式
            }
        }
        if (log.isDebugEnabled()){
            log.error("Could not parse date: {}", text);
        }
        throw new IllegalArgumentException("Could not parse date: " + text);
    }
}
