package com.softwood.logging.logback

/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter

/**
 * Logback {@link CompositeConverter} colors output using the {@link AnsiOutput} class. A
 * single 'color' option can be provided to the converter, or if not specified color will
 * be picked based on the logging level.
 *
 *
 *
 * @author Will Woodman
 */
class ColourConverter extends CompositeConverter<ILoggingEvent> {

    private static final Map<String, AnsiElement> elements

    static {
        Map<String, AnsiElement> ansiElements = new HashMap<>()
        ansiElements.put("faint", AnsiStyle.FAINT)
        ansiElements.put("red", AnsiColor.RED)
        ansiElements.put("green", AnsiColor.GREEN)
        ansiElements.put("yellow", AnsiColor.YELLOW)
        ansiElements.put("blue", AnsiColor.BLUE)
        ansiElements.put("magenta", AnsiColor.MAGENTA)
        ansiElements.put("cyan", AnsiColor.CYAN)
        elements = Collections.unmodifiableMap(ansiElements)
    }

    private static final Map<Integer, AnsiElement> levels

    static {
        Map<Integer, AnsiElement> ansiLevels = new HashMap<>()
        ansiLevels.put(Level.ERROR_INTEGER, AnsiColor.RED)
        ansiLevels.put(Level.WARN_INTEGER, AnsiColor.YELLOW)
        levels = Collections.unmodifiableMap(ansiLevels)
    }

    @Override
    protected String transform(ILoggingEvent event, String inp) {
        AnsiElement element = elements.get(getFirstOption());
        if (element == null) {
            // Assume highlighting
            element = levels.get(event.getLevel().toInteger());
            element = (element == null ? AnsiColor.GREEN : element);
        }
        return toAnsiString(inp, element)
    }

    protected String toAnsiString(String inp, AnsiElement element) {
        return AnsiOutput.toString(element, inp)
    }

}

