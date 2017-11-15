package com.softwood.databinder.converters

import spock.lang.Specification

class UrlToFileConverterUnitTestSpecification extends Specification{

    def setup () {

    }

    def "create url to file converter" () {
        setup : "we create a new converter "
        UrlToFileConverter fc = new UrlToFileConverter(File)

        expect : "target to be set to File type "

        fc.targetClazz == File

    }

    def "check getTargetType method returns File "() {
        setup : "we create a new converter "
        UrlToFileConverter fc = new UrlToFileConverter(File)

        expect : "targetType to return File "
        fc.getTargetClazz() == File
    }

    def "check canConvert method returns true "() {
        setup : "we create a new converter "
        UrlToFileConverter fc = new UrlToFileConverter(File)

        def result = fc.canConvert(URL)

        expect : "canConvert to return true for Url input "
        result == true
    }

    def "call convert() method returns File instance "() {
        setup : "we create a new converter "
        UrlToFileConverter fc = new UrlToFileConverter(File)

        def result = fc.convert(new URL ("file:/temp"))

        expect : "canConvert to return true for Url input "
        result.getClass() == File

        result.canonicalPath == "D:\\temp"

    }
}
