package com.softwood.databinder

import com.softwood.databinder.converters.CalendarToLocalDateTimeConverter
import com.softwood.databinder.converters.DateToLocalDateTimeConverter
import com.softwood.databinder.converters.StringToFileConverter
import com.softwood.databinder.converters.StringToLocalDateTimeConverter
import com.softwood.databinder.converters.UriToFileConverter
import com.softwood.databinder.converters.UrlToFileConverter

/**
 * Copyright (c) 2017 Softwood Consulting Ltd
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import groovy.transform.ToString
import org.codehaus.groovy.runtime.InvokerHelper

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue

import static java.lang.reflect.Modifier.isStatic

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class Gbinder {

    private static ConcurrentLinkedQueue typeConverters = new ConcurrentLinkedQueue()

    //configure standard converters
    static {
        typeConverters << [Calendar, LocalDateTime, CalendarToLocalDateTimeConverter]
        typeConverters << [Date, LocalDateTime, DateToLocalDateTimeConverter]
        typeConverters << [String, File, StringToFileConverter]
        typeConverters << [String, LocalDateTime, StringToLocalDateTimeConverter]
        typeConverters << [URI, File, UriToFileConverter]
        typeConverters << [URL, File, UrlToFileConverter]
    }


    static def registerTypeConverter(sourceType, targetType, converter) {
        assert targetType instanceof Class
        assert sourceType instanceof Class
        typeConverters << [sourceType, targetType, converter]
    }

    //see if standard converter is in registry listing and return it
    static def lookupTypeConvertors(sourceType, targetType) {
        def converters = typeConverters.collect {
            if (it[0] == sourceType && it[1] == targetType)
                it[2]
            else
                null
        }
    }


    Gbinder () {

    }

}
