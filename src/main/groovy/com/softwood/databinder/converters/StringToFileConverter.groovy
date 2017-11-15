package com.softwood.databinder.converters

class StringToFileConverter extends AbstractConverterImpl {
    StringToFileConverter() {
        super (String)
    }

    @Override
    File convert (value) {
        assert targetClazz == File
        assert value instanceof String || value instanceof GString

        File file = new File (value)
        file
    }

    @Override
    boolean canConvert (value) {
        assert targetClazz == File

        if (value instanceof String || value instanceof GString)
            true
        else
            false

    }

}
