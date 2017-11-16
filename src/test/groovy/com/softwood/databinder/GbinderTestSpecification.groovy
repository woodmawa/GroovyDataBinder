package com.softwood.databinder

import spock.lang.Specification

class GbinderTestSpecification extends Specification {

    def "add local convertor and ensure its add to local regsitry  " () {
        when:
        Gbinder binder = Gbinder.newInstance ()
        Queue conv =  binder.getConverters()
        def startNum = conv.size()
        binder.addTypeConverter(Object,Object, {})
        def revised = binder.getConverters().size()
        def c = binder.lookupTypeConverters(Object, Object)
        binder.removeTypeConverter(Object,Object, c)

        then:
        revised == startNum + 1
        binder.getConverters().size() == startNum
    }
}
