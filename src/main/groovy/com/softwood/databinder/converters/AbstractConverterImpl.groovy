package com.softwood.databinder.converters

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

import com.softwood.databinder.ValueConverter

/**
 * Abstract default converter that can be extended and
 * overriden by concrete implementations
 *
 * alternatively groovy SAM type conversion can be used to morph a closure
 * or a class into the interface  form
 *
 */

abstract class AbstractConverterImpl implements ValueConverter {
    final Class<?> targetClazz

    def convert(value) {
        return value.asType (targetClazz)
    }

    boolean canConvert(value) {
        Class<?> sourceClazz = (value instanceof Class) ? value: value.getClass()
        boolean test = targetClazz.isAssignableFrom (sourceClazz)
        return test
    }

    Class<?> getTargetType() {
        return targetClazz
    }

    AbstractConverterImpl () {
        targetClazz = new Class()
    }
    AbstractConverterImpl (Class clazz) {
        targetClazz = clazz
    }
}
