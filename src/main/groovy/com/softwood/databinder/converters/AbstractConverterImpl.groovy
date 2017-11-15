package com.softwood.databinder.converters

import com.softwood.databinder.ValueConverter

abstract class AbstractConverterImpl implements ValueConverter {
    Class<?> targetClazz

    def convert(value) {
        return value as Class<?>
    }

    boolean canConvert(value) {
        Class<?> clazz = (value instanceof Class) ? value: value.getClass()
        boolean test = targetClazz.isAssignableFrom (clazz)
        return test
    }

    Class<?> getTargetType() {
        targetClazz  = new Class()
        return targetClazz
    }

    AbstractConverterImpl () {
        targetClazz = new Class()
    }
    AbstractConverterImpl (Class clazz) {
        targetClazz = clazz
    }
}
