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
import org.codehaus.groovy.runtime.InvokerHelper

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

        binderConfig.global.converters.each {
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

        def targetInstance
        if (target instanceof Class) {
            //create new instance through default factory
            targetInstance = target.newInstance()
        } else
            targetInstance = target

        def sourcePropsList = []
        //find properties that don't contain 'class'
        //todo add the prefix check as filter
        def targetPropsList = target.metaClass.properties.findAll { !(it.name =~ /.*(C|c)lass.*/) }
        //todo def targetStaticProps = target.metaClass.properties.findAll{ it.getter.static }

        def excludeList = configureBinderParams?.exclude
        def includeList = configureBinderParams?.include

        //if explicit include white list of target properties to bind
        //only build targetProp from exact matches
        if (includeList) {
            def reduced = []

            for (prop in targetPropsList) {
                for (name in includeList) {
                    if (prop.name == name)
                        reduced << prop
                }
            }
            targetPropsList = reduced
        }

        switch (source) {
            case Map :
            case Expando:
                //binding source from list of attributes held in a map, sourceProps is ArrayList of [prop string names, value, type]
                sourcePropsList = source.collect { [it.key, it.value, it.value.getClass()] }

                //if exclusion list remove from source before we process
                if (excludeList) {
                    def reduced = []

                    //only build targetProp from exact matches
                    for (prop in sourcePropsList) {
                        for (name in excludeList) {
                            if (prop[0] != name)
                                reduced << prop
                        }
                    }
                    sourcePropsList = reduced
                }

                def ans = this.processMapAttributes(targetInstance, sourcePropsList)
                break
            default:
                sourcePropsList = source.metaClass.properties.findAll { it.name != "class" }

                //if exclusion list remove from source before we process
                if (excludeList) {
                    def reduced = []

                    //only build targetProp from exact matches
                    for (prop in sourcePropsList) {
                        for (name in excludeList) {
                            if (prop.name != name)
                                reduced << prop
                        }
                    }
                    sourcePropsList = reduced
                }

                processSourceInstanceAttributes (targetInstance, source, sourcePropsList)
                break
        }

        targetInstance
    }

    /**
     * where source is map bind the leaf property values into the target
     *
     * @param targetInstance - instacne of class being bound from source param map
     * @param paramsList - list of leave properties in form [prop, value, type]
     * @return
     */
    private def bindLeafMapAttributes(targetInstance, paramsList) {
        assert targetInstance, paramsList

        boolean isPrimitive
        MetaProperty targetProperty

        //for each array entry of [property, value, type], try and bind it in targetInstance
        paramsList?.each { ArrayList sprop ->

            def converters
            //check if property exists in the target.
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
                    if (sprop[2] instanceof Date || sprop[2] instanceof LocalDateTime ) {
                        SimpleDateFormat df = new SimpleDateFormat(dateFormat)
                        sourceProp = df.format (sprop[1])
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

    /**
     * if the source is a map do the binding from the map attributes, sim0ple and compound
     * @param targetInstance
     * @param params
     * @param result
     * @return
     */
    private def processMapAttributes(targetInstance, params) {
        def leafParams = params.findAll { !(it[0].contains(".")) }
        def compoundParams = params.findAll { it[0].contains(".") }

        //if any leaf params then bind them
        if (leafParams) {
            bindLeafMapAttributes(targetInstance, leafParams)
        }

        Map attMap = new HashMap()
        List propList = new LinkedList()
        def subParamsBlock = []

        //for each compound property form like [xxx.yyy, value, type]
        //build map of prop name and attributes to bind to that property
        compoundParams.each {
            String tprop
            String[] parts = it[0].tokenize('.')
            if (parts.size() == 2) {
                //process leaf attributes
                tprop = parts[0]
                propList << [parts[1], it[1], it[2]]
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
    private def processSourceInstanceAttributes (targetInstance, sourceInstance, sourcePropsList) {
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


}
