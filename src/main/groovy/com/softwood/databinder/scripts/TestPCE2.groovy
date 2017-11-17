package com.softwood.databinder.scripts

import com.softwood.databinder.Gbinder

class TestPCE2 {
    static void main (args) {
        println System.getProperty("user.dir")

        Gbinder binder = Gbinder.newBinder()

        println binder.toString()
    }
}
