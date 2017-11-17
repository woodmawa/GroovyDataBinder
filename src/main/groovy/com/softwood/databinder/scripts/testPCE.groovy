package com.softwood.databinder.scripts

import com.softwood.databinder.Gbinder

//will register a listener
Gbinder binder = Gbinder.newBinder()


Gbinder.registerGlobalTypeConverter(Object,Object,{})

def pcs = binder.$propertyChangeSupport

println "direct run of script user.dir " + System.getProperty("user.dir")
