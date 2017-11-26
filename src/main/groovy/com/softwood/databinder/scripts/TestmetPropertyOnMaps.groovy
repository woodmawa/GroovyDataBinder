package com.softwood.databinder.scripts

import org.codehaus.groovy.runtime.InvokerHelper

Map myMap = [:]

myMap << [house:10] << [street: "South close"]

println myMap

MetaClass mc = myMap.getMetaClass()
assert mc

print "myMap metaclass props : " + mc.properties.each {println it.name}
print "metaprop values props : " + mc.getMetaPropertyValues().each {println it.name}
print "myMap properties  values props : " + myMap.properties.each {println it.name}
def house = myMap.getAt('house')
println house
assert myMap.house == myMap.getAt('house')

MetaMethod getter = InvokerHelper.getMetaClass(Map).getMetaMethod('get', Object)
MetaProperty mp = new MetaBeanProperty('house', myMap.getClass(), getter, null)
println "mp : " + mp

def houseNum = mp.getter.invoke(myMap, 'house')
println "getter 'house' : "+ getter.invoke(myMap, 'house')
println "invoke get with 'house' : "+ InvokerHelper.getMetaClass(Map).invokeMethod(myMap, 'get', 'house')
//def es = mp.getProperty(myMap)

//println "get hse from map " + es