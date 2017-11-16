package com.softwood.databinder

import groovy.json.JsonSlurper
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import spock.lang.Specification

@ToString
class SpecialType {
    String name
    BigDecimal sal
}

@ToString()
class Source {
    String name
    String number
    boolean tOrF
    //static myStat = 1
    double dub
    Map address
    List col
    SpecialType spec
    Building building
}

@ToString
@EqualsAndHashCode
class Target {
    List myList
    String name
    int  number
    String bar
    String tOrF
    Map address
    Collection col
    Date bday
    List spec
    Building building

}

@ToString
class Building {
    String Name
    BigDecimal height
    Address address
}

@ToString
class Address {
    String number
    String street
    String town
}


class GbinderTestSpecification extends Specification {

    def "add local convertor and ensure its add to local regsitry  " () {
        when:
        Gbinder binder = Gbinder.newInstance ()
        Queue conv =  binder.getConverters()
        def startNum = conv.size()
        binder.addTypeConverter(Object,Object, {})
        def revised = binder.getConverters().size()
        def c = binder.lookupFirstTypeConverter(Object, Object)
        binder.removeTypeConverter(Object,Object, c)

        then:
        revised == startNum + 1
        binder.getConverters().size() == startNum
    }

    def "add global convertor and ensure it gets added to local registry, then remove   " () {
        when:
        Gbinder binder = Gbinder.newInstance ()
        def initNumConv =  binder.getConverters().size()

        //when we add a new global type converter then the local regsitry is updated
        def doNothingConverter = {}
        Gbinder.registerGlobalTypeConverter(Object, Object, doNothingConverter)

        then:
        binder.getConverters().size() - initNumConv == 1

        and  :
        def conv = binder.lookupTypeConverters(Object, Object)

        then:
        conv[0] == doNothingConverter

        and :
        binder.removeTypeConverter(Object, Object, doNothingConverter)

        then :
        binder.getConverters().size() == initNumConv
        binder.lookupTypeConverters(Object, Object) == []

    }

    def "bind with slurped json input "() {
        setup :
        def slurper = new JsonSlurper()
        def obj = slurper.parseText( '{"myList": [1,2,3]}')
        Gbinder binder = Gbinder.newInstance ()

        when:
        Target res = binder.bind (Target, obj)

        then:
        res.myList == [1,2,3]

    }

    def "bind from source class inst to inst of target, than as class type and compare  " () {
        setup:
        def s = new Source (name:"will", number:"10", tOrF: true, address:[county:'uk',town:'ipswich'], col:["abc",1,true], spec:new SpecialType(name:"woodman", sal: 53.7))
        def t = new Target()
        Gbinder binder = Gbinder.newInstance ()
        binder.addTypeConverter(SpecialType,List, {[it.name, it.sal]} )

        when:

        Target res1 = binder.bind (t, s)
        Target res2 = binder.bind (Target, s)
        println res1

        then:
        res1 == res2
        res1.name == "will"
        res1.number == 10
        res1.tOrF == "true"
        res1.address == [county:'uk',town:'ipswich']
        res1.col == ["abc",1,true]
        res1.spec == ["woodman", 53.7]
    }
}
