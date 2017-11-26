package com.softwood.databinder

/**
 * Copyright (c) 2017 Softwood Consulting Ltd
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.softwood.databinder.converters.CalendarToLocalDateTimeConverter
import com.softwood.databinder.converters.DateToLocalDateTimeConverter
import com.softwood.databinder.converters.StringToFileConverter
import com.softwood.databinder.converters.StringToLocalDateTimeConverter
import com.softwood.databinder.converters.UriToFileConverter
import com.softwood.databinder.converters.UrlToFileConverter
import com.softwood.utilities.BinderHelper
import groovy.beans.Bindable

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.codehaus.groovy.reflection.CachedClass
import org.codehaus.groovy.reflection.GeneratedMetaMethod
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.callsite.CallSite
import org.codehaus.groovy.runtime.callsite.CallSiteAwareMetaMethod

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue

import static java.lang.reflect.Modifier.isStatic

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Slf4j
class Gbinder {

    private static ConcurrentLinkedQueue typeConverters = new ConcurrentLinkedQueue()
    private ConcurrentLinkedQueue localTypeConverters = new ConcurrentLinkedQueue()

    //Listen to changes when new global typeConverter added
    private static PropertyChangeSupport $propertyChangeSupport =
            new PropertyChangeSupport(Gbinder.class)

    static void addPropertyChangeListener(PropertyChangeListener listener) {
        $propertyChangeSupport.addPropertyChangeListener(listener)
    }

    //utility : read the Binder configuration from resources in the classpath
    static def processBinderConfiguration () {
        // class class loader will include 'resources' on classpath so file will be found
        def configFileName =  "config/BinderConfig.groovy"
        def resource = Gbinder.getClassLoader().getResource(configFileName)
        def binderConfig = new ConfigSlurper().parse (resource)
        binderConfig
    }

    private boolean trimStrings
    private String dateFormat = ""

    //configure standard converters
    static {

        if (!System.getProperty("PID")) {
            System.setProperty("PID", (BinderHelper.PID).toString())
        }

        def binderConfig = processBinderConfiguration()

        binderConfig.binder.classDefault.converters.each {
            typeConverters << it
        }

        typeConverters
    }


    static def registerGlobalTypeConverter(sourceType, targetType, converter) {
        assert targetType instanceof Class
        assert sourceType instanceof Class
        def oldVal = new ConcurrentLinkedQueue<>(typeConverters)
        typeConverters << [sourceType, targetType, converter]

        $propertyChangeSupport.firePropertyChange("typeConverters", oldVal, typeConverters )
    }

    //tests if type is simple type classifier or not
    static private boolean isSimpleType(type) {
        ArrayList simpleTypes = [int, short, long, float, char, boolean, double, byte]
        simpleTypes.contains(type)
    }

    static Gbinder newBinder() {
        def binder = new Gbinder ()

        // class class loader will include 'resources' on classpath so file will be found
        def binderConfig = processBinderConfiguration()

        binder.trimStrings = binderConfig.binder.classDefault.trimStrings
        binder.dateFormat = binderConfig.binder.classDefault.dateFormat

        log.debug "newBinder(): default settings> trim : $binder.trimStrings, dateFormat : $binder.dateFormat"

        binder

    }

    //private constructor
    private Gbinder () {

        //setup new instance with ref copy of global type converters
        //if new class level converter is added - ensure its added to local registry
        def pcListener = {pce ->
            def newRecord
            if (pce.propertyName == "typeConverters"){
                if (pce.newValue.size() > pce.oldValue.size() )
                    newRecord = pce.newValue - pce.oldValue
            }
            //add new global converter entry to local registry
            addTypeConverter(*newRecord)
        }
        addPropertyChangeListener(pcListener)

        //build local type converter registry as copy of global master
        typeConverters.each {
            addTypeConverter (*it) }
    }

    Queue getConverters () {
        localTypeConverters
    }

    def addTypeConverter(sourceType, targetType, converter) {
        ArrayList entry = []
        entry << sourceType
        entry << targetType
        entry << converter
        localTypeConverters << entry
    }

    def removeTypeConverter(sourceType, targetType, converter) {
        if (lookupTypeConverters(sourceType, targetType)) {
            ArrayList entry = [sourceType, targetType, converter]
            def res = localTypeConverters.remove(entry)
            true
        } else
            false
    }

    //see if standard converter is in registry listing, return as List in case there are more than 1, and return it
    def lookupTypeConverters(sourceType, targetType) {
        def converters =[]
        localTypeConverters.each {
            def entry = it
            if (it[0] == sourceType && it[1] == targetType)
                converters << it[2]
        }
        converters
    }

    //if more than one type converter just return the first match - expected norm
    def lookupFirstTypeConverter(sourceType, targetType) {
        def converters =[]
        localTypeConverters.each {
            def entry = it
            if (it[0] == sourceType && it[1] == targetType)
                converters << it[2]
        }
        if (converters)
            converters[0]
        else
            null
    }

    //get new instance of target if class or interface, else just return as
    private def getInstanceOfClass (target) {
        def targetInstance
        if (target instanceof Class) {
            if (target.isInterface()) {
                if (target == Map)
                    targetInstance = [:]
                else if (target == Expando)
                    targetInstance = new Expando()
                else
                    targetInstance = new Object()
            } else {
                //create new instance through default factory
                targetInstance = target.newInstance()
            }
        } else {
            if (target?.getClass().isInstance(target)) {
                //just return the instance
                targetInstance = target.getClass().newInstance()
            } else
                targetInstance = target
            //todo else if (target.getClass().isEnum()){}
        }

        targetInstance
    }

    //find properties that don't contain 'class'
    private List<MetaProperty> getPropertyList (target) {
        //todo def targetStaticProps = target.metaClass.properties.findAll{ it.getter.static }
        if (target instanceof Map) {
            MetaMethod getter = InvokerHelper.getMetaClass(target.getClass()).getMetaMethod("get", String)
            MetaMethod setter = InvokerHelper.getMetaClass(target.getClass()).getMetaMethod ("put", String, Object)
            target.collect {
                //generate a simulated property for each key entry in the map
                new MetaBeanProperty (it.key, it.value.getClass(), getter, setter )
            }

        } else
            target.metaClass.properties.findAll { !(it.name =~ /.*(C|c)lass.*/ )}
    }

    private def buildSubTargetPropertiesList (target, prefix){
        def targetPropsList = target.metaClass.properties.findAll { !(it.name =~ /.*(C|c)lass.*/) }
        def prefixMatched
        def subTargetPropsList
        def newInst
        if (prefix) {
            prefixMatched = targetPropsList.find {it.name == "$prefix"}
            MetaProperty meta = prefixMatched
            switch (meta?.type) {
                case Map :
                    newInst = [:]
                    subTargetPropsList = []
                    break
                case Collection:
                case List :
                    newInst = []
                    subTargetPropsList = []
                    break
                default :  //if either interface or some class type
                    if (meta.type.interface) {
                        newInst = new Object()
                        subTargetPropsList = []
                    }
                    else {
                        newInst = meta.type.newInstance()
                        subTargetPropsList = meta.type.metaClass.properties.findAll { !(it.name =~ /.*(C|c)lass.*/) }
                    }
                   break
            }
        }
        //multi return
        [newInst, subTargetPropsList]

    }

    //gets the right source property depending on whether source is a map, or  a class
    private def getSourcePropertyValue (source, MetaProperty metaProperty) {
        if (source instanceof Map) {
            metaProperty.getter.invoke (source, metaProperty.name)
        } else {
            metaProperty.getProperty(source)
        }

    }
    /**
     *
     * @param target - either instance of or the class type of the target
     * @param source - source of data to bind
     * @param params
     * @param prefix
     * @return
     */
    def bind(target, source, Map configureBinderParams = null, prefix = null) {
        boolean isPrimitive

        def targetInstance = getInstanceOfClass (target)

        def sourcePropsList = []

        //todo add the prefix check as filter
        def targetPropsList = getPropertyList (target)

        def excludeList = configureBinderParams?.exclude
        def includeList = configureBinderParams?.include

        //if explicit [include:] white list of target properties to bind
        //only build targetProp list from exact matches
        def reduced = []
        def newTarget
        def targetSubPropsList
        if (includeList) {
            def prefixMatched = targetPropsList.find{it.name == prefix}
            if (prefixMatched) {
                //bind property matched with the prefix
                (newTarget, targetSubPropsList) =  buildSubTargetPropertiesList (targetInstance, prefix)
                targetSubPropsList.each {reduced << it}
                if (targetInstance."$prefix" == null) {
                    (prefixMatched as MetaProperty).setProperty(targetInstance, newTarget)
                }
                //bind the prefix matched property, and source entry
                bind (newTarget, source["$prefix"], configureBinderParams) //targetInstance = newTarget
                configureBinderParams.remove (prefix)   //having done bind remove from 'include' configureBinderParams

            }
            for (tprop in targetPropsList) {
                for (sname in includeList) {
                    if (tprop.name == sname)
                        reduced << tprop
                }

            }
            targetPropsList = reduced
        }

        //if exclusion list remove from source before we process
        sourcePropsList = getPropertyList(source)
        if (excludeList) {
            reduced = []
            //only build targetProp from exact matches
            for (sprop in sourcePropsList) {
                for (name in excludeList) {
                    if (sprop.name != name)
                        reduced << sprop
                }
            }
            sourcePropsList = reduced
        }

        def ans = this.processSourceAttributes(targetInstance, targetPropsList, source, sourcePropsList, prefix)

        /*switch (source) {
            case Map :
            case Expando:
                //binding source from list of attributes held in a map, sourceProps is ArrayList of [prop string names, value, type]
                sourcePropsList = source.collect { [it.key, it.value, it.value.getClass()] }

                //if exclusion list remove from source before we process
                reduced = []
                if (excludeList) {

                    //only build targetProp from exact matches
                    for (prop in sourcePropsList) {
                        for (name in excludeList) {
                            if (prop[0] != name)
                                reduced << prop
                        }
                    }
                    sourcePropsList = reduced
                }

                def ans = this.processMapAttributes(targetInstance, targetPropsList, source, sourcePropsList, prefix)
                break
            default:
                sourcePropsList = getPropertyList(source)

                //if exclusion list remove from source before we process
                reduced = []
                if (excludeList) {

                    //only build targetProp from exact matches
                    for (prop in sourcePropsList) {
                        for (name in excludeList) {
                            if (prop.name != name)
                                reduced << prop
                        }
                    }
                    sourcePropsList = reduced
                }

                processSourceInstanceAttributes (targetInstance, targetPropsList, source, sourcePropsList)
                break
        }*/

        targetInstance
    }

    /**
     * where source is map bind the leaf property values into the target
     *
     * @param targetInstance - instacne of class being bound from source param map
     * @param paramsList - list of leave properties in form [prop, value, type]
     * @return
     */
    private def bindLeafMapAttributes(targetInstance, targetPropsList, source, sourceParamsList, prefix=null) {

        boolean isPrimitive

        MetaProperty targetProperty

        def sPrefixMatchValue
        if (prefix) {
            //if source props have key that matches prefix, set the target to be the value
            sourceParamsList.each {if (it.name == "${prefix}")
                sPrefixMatchValue = getSourcePropertyValue(source, it)}
            //if (targetInstance.isAssignable (sPrefixMatchValue))
            targetInstance = sPrefixMatchValue
            return targetInstance
        }

        //if target is map or expando then targetPropsList will be []
        if (targetInstance instanceof Map || targetInstance instanceof Expando) {
            sourceParamsList.each {MetaProperty sprop ->
                //add source prop to map
                def value = getSourcePropertyValue (source, sprop)
                targetInstance."${sprop.name}" =  value
            }
            return targetInstance
        }

        //only process target properties in the targetPropsList
        for (tprop in targetPropsList) {
            //for each array entry of [property, value, type], try and bind it in targetInstance
            sourceParamsList?.each { MetaProperty sprop ->
                //match each sprop name with the tprop
                if (tprop.name != sprop.name) {
                    return
                }

                def converters
                //confirm property exists in the target.
                targetProperty = targetInstance.hasProperty(sprop.name)

                //if target doesn't have source match skip to next source property
                if (targetProperty == null)
                    return

                if (isSimpleType(targetProperty.type))
                    isPrimitive = true
                else
                    isPrimitive = false

                if (targetProperty) {
                    if (targetProperty.type == String) {
                        //invoke the reflected getter on the 'source' for the prop.'name' key in the source map
                        def sourceval = getSourcePropertyValue (source, sprop)
                        log.debug "bindLeafMapAttributes: target prop :'${targetProperty.name}',  casting source to target, set prop value : ${sourceval} "
                        def sourceProp
                        //if source is date or localDateTime then format the instance to default formatted string
                        if (sourceval instanceof Date || sourceval instanceof LocalDateTime) {
                            SimpleDateFormat df = new SimpleDateFormat(dateFormat)
                            sourceProp = df.format(sourceval)
                        } else {
                            sourceProp = trimStrings ? sourceval.toString().trim() : sourceval.toString()
                        }
                        targetProperty.setProperty(targetInstance, sourceProp)
                    } else if (targetProperty.type.isAssignableFrom(sprop.type)) {
                        //get value of the source property using closure param metaMethod 'prop as ${prop.getProperty(source)}"
                        //and set this on the target instance
                        def sourceval = getSourcePropertyValue (source, sprop)
                        log.debug "bindLeafMapAttributes: target prop :'${targetProperty.name}' is assignable from source, set prop value : ${sourceval}"
                        targetProperty.setProperty(targetInstance, sourceval)
                        //todo only bind as target [includes:] ?
                        //bind (targetInstance."${targetProperty.name}", sourceval, binderConfigMap, prefix)
                    } else if (isPrimitive) {
                        def sourceval = getSourcePropertyValue (source, sprop)
                        log.debug "bindLeafMapAttributes: parse '$sourceval' to primitive "
                        switch (targetProperty.type) {
                            case int:
                                targetProperty.setProperty(targetInstance, Integer.parseInt(sourceval.toString()))
                                break
                            case char:
                                targetProperty.setProperty(targetInstance, sourceval as char)
                                break
                            case float:
                                targetProperty.setProperty(targetInstance, Float.parseFloat(sourceval.toString()))
                                break
                            case double:
                                targetProperty.setProperty(targetInstance, Double.parseDouble(sourceval.toString()))
                                break
                            case boolean:
                                targetProperty.setProperty(targetInstance, Boolean.parseBoolean(sourceval.toString()))
                                break
                            default:
                                break

                        }

                    } else {
                        converters = localTypeConverters.collect {
                            if (it[0] == sprop.type && it[1] == targetProperty.type)
                                it[2] as ValueConverter
                            else
                                null
                        }
                        if (converters) {
                            def sourceValue = getSourcePropertyValue (source, sprop)

                            if (sourceValue) {
                                def newValue = converters[0].convert(sourceValue)
                                targetProperty.setProperty(targetInstance, newValue)
                            }
                        }
                    }

                }
            }
        }

    }

    /**
     * where source is map bind the leaf property values into the target
     *
     * @param targetInstance - instacne of class being bound from source param map
     * @param paramsList - list of leave properties in form [prop, value, type]
     * @return
     */
    private def origBindLeafMapAttributes(targetInstance, targetPropsList, sourceParamsList, prefix=null) {

        boolean isPrimitive

        MetaProperty targetProperty

        def sPrefixMatchValue
        if (prefix) {
            //if source props have key that matches prefix, set the target to be the value
            sourceParamsList.each {if (it[0] == "${prefix}") sPrefixMatchValue = it[1]}
            //if (targetInstance.isAssignable (sPrefixMatchValue))
                targetInstance = sPrefixMatchValue
            return targetInstance
        }

        //if target is map or expando then targetPropsList will be []
        if (targetInstance instanceof Map || targetInstance instanceof Expando) {
            sourceParamsList.each {ArrayList sprop ->
                //add source prop to map
                targetInstance."${sprop[0]}" = sprop[1]
            }
            return targetInstance
        }

        //only process target properties in the targetPropsList
        for (tprop in targetPropsList) {
            //for each array entry of [property, value, type], try and bind it in targetInstance
            sourceParamsList?.each { ArrayList sprop ->
                //match each sprop name with the tprop
                if (tprop.name != sprop[0]) {
                    return
                }

                def converters
                //confirm property exists in the target.
                targetProperty = targetInstance.hasProperty(sprop[0])

                //if target doesn't have source match skip to next source property
                if (targetProperty == null)
                    return

                if (isSimpleType(targetProperty.type))
                    isPrimitive = true
                else
                    isPrimitive = false

                if (targetProperty) {
                    if (targetProperty.type == String) {
                        log.debug "bindLeafMapAttributes: target prop :'${targetProperty.name}',  casting source to target, set prop value : ${sprop[1]} "
                        def sourceProp
                        //if source is date or localDateTime then format the instance to default formatted string
                        if (sprop[2] instanceof Date || sprop[2] instanceof LocalDateTime) {
                            SimpleDateFormat df = new SimpleDateFormat(dateFormat)
                            sourceProp = df.format(sprop[1])
                        } else {
                            sourceProp = trimStrings ? sprop[1].toString().trim() : sprop[1].toString()
                        }
                        targetProperty.setProperty(targetInstance, sourceProp)
                    } else if (targetProperty.type.isAssignableFrom(sprop[2])) {
                        //get value of the source property using closure param metaMethod 'prop as ${prop.getProperty(source)}"
                        //and set this on the target instance
                        log.debug "bindLeafMapAttributes: target prop :'${targetProperty.name}' is assignable from source, set prop value : ${sprop[1]}"
                        def value = sprop[1]
                        targetProperty.setProperty(targetInstance, value)
                    } else if (isPrimitive) {
                        log.debug "bindLeafMapAttributes: parse sprop[1].toString() to primitive "
                        switch (targetProperty.type) {
                            case int:
                                targetProperty.setProperty(targetInstance, Integer.parseInt(sprop[1].toString()))
                                break
                            case char:
                                targetProperty.setProperty(targetInstance, sprop[1] as char)
                                break
                            case float:
                                targetProperty.setProperty(targetInstance, Float.parseFloat(sprop[1].toString()))
                                break
                            case double:
                                targetProperty.setProperty(targetInstance, Double.parseDouble(sprop[1].toString()))
                                break
                            case boolean:
                                targetProperty.setProperty(targetInstance, Boolean.parseBoolean(sprop[1].toString()))
                                break
                            default:
                                break

                        }

                    } else {
                        converters = typeConverters.collect {
                            if (it[0] == sprop[2] && it[1] == targetProperty.type)
                                it[2] as ValueConverter
                            else
                                null
                        }
                        if (converters) {
                            def sourceValue = sprop[1]
                            if (sourceValue) {
                                def newValue = converters[0].convert(sourceValue)
                                targetProperty.setProperty(targetInstance, newValue)
                            }
                        }
                    }

                }
            }
        }

    }

    /**
     * if the source is a map do the binding from the map attributes, sim0ple and compound
     * @param targetInstance
     * @param params
     * @param result
     * @return
     */
    private def origProcessMapAttributes(targetInstance, targetPropsList, source, sourcePropsList, prefix=null) {
        def leafParams = sourcePropsList.findAll { !(it.getProperty (source).contains(".")) }
        def compoundParams = sourcePropsList.findAll { it.getProperty (source).contains(".") }

        //if any leaf params then bind them
        if (leafParams) {
            bindLeafMapAttributes(targetInstance, targetPropsList, source, leafParams, prefix)
        }

        Map attMap = new HashMap()
        List propList = new LinkedList()
        def subParamsBlock = []

        //for each compound property form like [xxx.yyy, value, type]
        //build map of prop name and attributes to bind to that property
        compoundParams.each {MetaProperty sprop ->
            String tprop
            String[] parts = sprop.name.tokenize('.')
            if (parts.size() == 2) {
                //process leaf attributes
                tprop = parts[0]
                propList << [parts[1], sprop.getProperty(source), sprop.type]
                attMap << ["${tprop}": propList]
            }

            if (parts.size() > 2) {
                //if nested attributes set up next param block to process, tack orig target property onto end of list
                tprop = parts[0]
                parts = parts.drop(1)
                subParamsBlock << [parts.join('.'), it[1], it[2], tprop]
            }
        }
        if (attMap) {
            attMap.each { prop, propValue ->
                MetaProperty mprop = targetInstance.hasProperty(prop)
                def subTarget = targetInstance."${prop}"
                if (subTarget == null) {
                    //todo what to do parent child pointer to parent if any ?
                    subTarget = mprop.type.newInstance()
                    mprop.setProperty(targetInstance, subTarget)
                }
                bindLeafMapAttributes(subTarget, propValue)
            }
        }

        //if remaining compound attributes, then recurse
        if (subParamsBlock) {
            subParamsBlock.each { subParams ->
                def newSubTargetInstance = targetInstance."${subParams[3]}"
                //process subtarget and reduced subparams block till complete
                def map = processMapAttributes(newSubTargetInstance, subParamsBlock)
            }
        }

        targetInstance
    }

    /**
     * if the source is a map do the binding from the map attributes, sim0ple and compound
     * @param targetInstance
     * @param params
     * @param result
     * @return
     */
    private def origProcessSourceInstanceAttributes (targetInstance, targetPropertiesList, sourceInstance, sourcePropsList, prefix=null) {
        sourcePropsList?.each { MetaProperty sprop ->
            def converters
            MetaProperty targetProperty
            boolean isPrimitive

            //if property exists in the target.
            targetProperty = targetInstance.hasProperty("${sprop.name}")

            if (targetProperty == null)
                return //return from closure

            if (isSimpleType(targetProperty.type))
                isPrimitive = true
            else
                isPrimitive = false

            if (targetProperty) {
                def sourcePropClassType = sprop.type
                def value = sprop.getProperty(sourceInstance)
                log.debug "processSourceInstanceAttributes: sourceClassType ${sourcePropClassType.canonicalName}"
                assert sourcePropClassType instanceof Class
                log.debug "processSourceInstanceAttributes: source property ${sprop.name}, with targetPropclass : ${targetProperty.type}, and sourcePropClass : $sourcePropClassType"

                if (targetProperty.type == String) {
                    log.debug "processSourceInstanceAttributes: casting source to target string type"
                    def sourceProp
                    if (sprop.type instanceof Date || sprop.type instanceof LocalDateTime ) {
                        SimpleDateFormat df = new SimpleDateFormat(dateFormat)
                        sourceProp = df.format (sprop.getProperty(sourceInstance))
                    } else {
                        sourceProp = trimStrings ? sprop.getProperty(sourceInstance).toString().trim() : sprop.getProperty(sourceInstance).toString()
                    }
                    targetProperty.setProperty(targetInstance, sourceProp )
                } else if (targetProperty.type.isAssignableFrom(sourcePropClassType)) {
                    //get value of the source property using closure param metaMethod 'prop as ${prop.getProperty(source)}"
                    //and set this on the target instance
                    log.debug "processSourceInstanceAttributes: target prop is assignable from source, set prop value : ${sprop.getProperty(sourceInstance)}"
                    targetProperty.setProperty(targetInstance, sprop.getProperty(sourceInstance))
                } else if (isPrimitive) {
                    log.debug "processSourceInstanceAttributes: parse ${sprop.getProperty(sourceInstance).toString()} to primitive "
                    switch (targetProperty.type) {
                        case int:
                            targetProperty.setProperty(targetInstance, Integer.parseInt(sprop.getProperty(sourceInstance).toString()))
                            break
                        case char:
                            targetProperty.setProperty(targetInstance, sprop.getProperty(sourceInstance) as char)
                            break
                        case float:
                            targetProperty.setProperty(targetInstance, Float.parseFloat(sprop.getProperty(sourceInstance).toString()))
                            break
                        case double:
                            targetProperty.setProperty(targetInstance, Double.parseDouble(sprop.getProperty(sourceInstance).toString()))
                            break
                        case boolean:
                            targetProperty.setProperty(targetInstance, Boolean.parseBoolean(sprop.getProperty(sourceInstance).toString()))
                            break
                        default:
                            break

                    }

                } else {
                    //check if any registered custom type convertors
                    converters = lookupTypeConverters(sourcePropClassType, targetProperty.type)
                    if (converters) {
                        def sourceValue = sprop.getProperty(sourceInstance)
                        if (sourceValue) {
                            def newValue = converters[0].asType(ValueConverter).convert(sourceValue)
                            targetProperty.setProperty(targetInstance, newValue)
                        }
                    }
                }


            }

            targetInstance
        }

    }

    /**
     * if the source is a map do the binding from the map attributes, sim0ple and compound
     * @param targetInstance
     * @param params
     * @param result
     * @return
     */
    private def processSourceAttributes(targetInstance, targetPropsList, source, sourcePropsList, prefix=null) {
        def leafParams = sourcePropsList.findAll { MetaProperty prop -> !prop.name.contains(".") }
        def compoundParams = sourcePropsList.findAll {  MetaProperty prop -> prop.name.contains(".") }

        //if any leaf params then bind them
        if (leafParams) {
            bindLeafMapAttributes(targetInstance, targetPropsList, source, leafParams, prefix)
        }

        Map attMap = new HashMap()
        List propList = new LinkedList()
        def subParamsBlock = []

        //for each compound property form like [xxx.yyy, value, type]
        //build map of prop name and attributes to bind to that property
        compoundParams.each {MetaMethod cprop ->
            String tprop
            String[] parts = cprop.name.tokenize('.')
            if (parts.size() == 2) {
                //process leaf attributes
                tprop = parts[0]
                propList << [parts[1], cprop[1], cprop[2]]
                attMap << ["${tprop}": propList]
            }

            if (parts.size() > 2) {
                //if nested attributes set up next param block to process, tack orig target property onto end of list
                tprop = parts[0]
                parts = parts.drop(1)
                subParamsBlock << [parts.join('.'), cprop[1], cprop[2], tprop]
            }
        }
        if (attMap) {
            attMap.each { prop, propValue ->
                MetaProperty mprop = targetInstance.hasProperty(prop)
                def subTarget = targetInstance."${prop}"
                if (subTarget == null) {
                    //todo what to do parent child pointer to parent if any ?
                    subTarget = mprop.type.newInstance()
                    mprop.setProperty(targetInstance, subTarget)
                }
                bindLeafMapAttributes(subTarget, getPropertyList(subTarget), source, propValue)  //will break
            }
        }

        //if remaining compound attributes, then recurse
        if (subParamsBlock) {
            subParamsBlock.each { subParams ->
                def newSubTargetInstance = targetInstance."${subParams[3]}"
                //process subtarget and reduced subparams block till complete
                def map = processSourceAttributes(newSubTargetInstance, subParamsBlock)
            }
        }

        targetInstance
    }


}
