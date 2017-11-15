package com.softwood.databinder.converters

import java.time.LocalDateTime

class CalendarToLocalDateTimeConverter extends AbstractConverterImpl {
    CalendarToLocalDateTimeConverter() {
        super (LocalDateTime)
    }

    @Override
    LocalDateTime convert (value) {
        assert targetClazz == LocalDateTime
        assert value instanceof String || value instanceof GString

        LocalDateTime.parse (value)
    }

    @Override
    boolean canConvert (value) {
        assert targetClazz == LocalDateTime

        if (value instanceof String || value instanceof GString)
            true
        else
            false

    }

}
