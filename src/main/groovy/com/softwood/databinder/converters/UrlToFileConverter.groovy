package com.softwood.databinder.converters

class UrlToFileConverter extends AbstractConverterImpl {
    UrlToFileConverter () {
        super (File)
    }

    @Override
    File convert (value) {
        assert targetClazz == File
        assert value instanceof URL
        URI uri = value.toURI()
        assert uri

        File file = new File (uri)
        file
    }

    @Override
    boolean canConvert (value) {
        assert targetClazz == File

        if (value.is(URL))
            true
        else
            false

    }

}
