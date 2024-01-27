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
import java.util.Date;

@Slf4j
public class DateUtils {

    public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("yyyy-MM");

    public static String formatDateTime(Date date){
        return formatDateTime(date, null);
    }

    public static String formatDateTime(Date date, String defaultValue){
        if (date == null){
            return defaultValue;
        }

        return DATE_TIME_FORMAT.format(date);
    }

    public static Date parse(String text){
        if (text == null || text.trim().length() == 0){
            return null;
        }

        if (text.length() <= DATE_TIME_FORMAT.toPattern().length() && text.length() >= DATE_TIME_FORMAT.toPattern().length() - 5){
            try {
                return DATE_TIME_FORMAT.parse(text);
            } catch (ParseException e) {
                if (log.isDebugEnabled()){
                    log.error("parse text to date error",e);
                }
            }
        }


        return null;
        // if (text.length() <= DATE_FORMAT)
    }
}
