package com.softwood.databinder.converters

class UriToFileConverter extends AbstractConverterImpl {
    UriToFileConverter(Class fileClazz) {
        super (fileClazz)
    }

    @Override
    File convert (value) {
        assert targetClazz == File
        assert value instanceof URI

        File file = new File (value)
        file
    }

    @Override
    boolean canConvert (value) {
        assert targetClazz == File
        assert value.is(URI)

        true

    }

}
