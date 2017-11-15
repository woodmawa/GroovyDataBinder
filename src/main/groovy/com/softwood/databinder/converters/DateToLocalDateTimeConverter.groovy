package com.softwood.databinder.converters

import java.time.LocalDateTime

class DateToLocalDateTimeConverter extends AbstractConverterImpl {
    DateToLocalDateTimeConverter() {
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
