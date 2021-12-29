package com.softwood.databinder

import com.softwood.utilities.BinderHelper
import groovy.json.JsonSlurper
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.junit.jupiter.api.Test
import spock.lang.Specification


class GbinderTestSpecification extends Specification {

    @Test
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

    @Test
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

    @Test
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

    @Test
    def "bind from source class inst to inst of target, than as class type and compare  " () {
        setup:
        def s = new Source (name:"will", number:"10", tOrF: true, address:[county:'uk',town:'ipswich'], col:["abc",1,true], spec:new SpecialType(name:"woodman", sal: 53.7))
        def t = new Target()
        Gbinder binder = Gbinder.newBinder ()
        binder.addTypeConverter(SpecialType,List, {[it.name, it.sal]} )
        println "user.dir >>> "+ System.getProperty("user.dir")

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

    @Test
    def "bind nested map source into target " () {
        setup:
        def paramsMap  = new HashMap  (tOrF: true, address:[county:'uk',town:'ipswich'] )
        Gbinder binder = Gbinder.newBinder ()

        when:
        Target res = binder.bind (Target, paramsMap)

        then :
        res.tOrF == "true"
        res.address.county == 'uk'

    }

    @Test
    def "bind date value in source to string in target " () {
        setup:
        def paramsMap  = new HashMap  (dateAsString : new Date())
        Gbinder binder = Gbinder.newBinder ()

        when:
        Target res = binder.bind (Target, paramsMap)
        println "in test: date as string : "+ res.dateAsString

        then :
        res.dateAsString.contains ("GMT")
    }

    @Test
    def "bind with prefix matcher and include list " () {
        setup:
        def paramsMap  = [tOrF: true, address:[county:'uk',town:'ipswich'] ]
        Gbinder binder = Gbinder.newBinder ()

        when:
        Target res = binder.bind (Target, paramsMap, [include: ["town", "county"]], "address")
        println "bound result : " + res

        then :
        res.address.town == "ipswich"
        res.address.county == "uk"

    }

    @Test
    def "bind with positive include list " () {
        setup:
        def paramsMap  = [tOrF: true, address:[county:'uk',town:'ipswich'] ]
        Gbinder binder = Gbinder.newBinder ()

        when:
        Target res = binder.bind (Target, paramsMap, [include: ["tOrF"]])
        println "bound result : " + res

        then :
        res.tOrF == "true"

        when:
        res = binder.bind (Target, paramsMap, [include: ["address"]])
        println "bound result : " + res

        then :
        res.address.town == "ipswich"
    }

    @Test
    def "bind Map, then Expando with param map  " () {
        setup:
        def paramsMap  = [tOrF: true, address:[county:'uk',town:'ipswich'] ]
        Gbinder binder = Gbinder.newBinder ()

        when:
        Map res = binder.bind (Map, paramsMap /*, [include: ["tOrF"]]*/)
        println "bound result : " + res

        then :
        res.tOrF

        when:
        Expando eres = binder.bind (Expando, paramsMap /*, [include: ["tOrF"]]*/)
        println "bound result : " + eres

        then :
        eres.tOrF

    }
}
