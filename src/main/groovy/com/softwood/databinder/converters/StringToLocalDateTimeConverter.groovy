package com.softwood.databinder.converters

import java.time.LocalDateTime

class StringToLocalDateTimeConverter extends AbstractConverterImpl {
    StringToLocalDateTimeConverter() {
        super (LocalDateTime)
    }

    @Override
    LocalDateTime convert (value) {
        assert targetClazz == LocalDateTime
        assert value instanceof Date

        LocalDateTime.parse (value.toString())
    }

    @Override
    boolean canConvert (value) {
        assert targetClazz == LocalDateTime

        if (value instanceof Date)
            true
        else
            false

    }

}
