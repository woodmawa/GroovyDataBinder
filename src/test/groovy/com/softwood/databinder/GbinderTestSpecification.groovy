package com.softwood.databinder

import spock.lang.Specification

class GbinderTestSpecification extends Specification {

    def "basic type checker " () {
        setup:
        Gbinder binder = Gbinder.newInstance ()
        Queue conv =  binder.getConvertors()
        println "convertors : $conv"

        expect:
        conv.size () > 0
    }
}
