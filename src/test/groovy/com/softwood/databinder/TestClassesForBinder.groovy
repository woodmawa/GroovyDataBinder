package com.softwood.databinder

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString


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
    String dateAsString

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
    Address secondaryAddress
}
