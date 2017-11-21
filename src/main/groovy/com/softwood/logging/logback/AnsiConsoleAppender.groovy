package com.softwood.logging.logback

import java.io.OutputStream
import org.fusesource.jansi.AnsiConsole
import ch.qos.logback.core.ConsoleAppender

public class AnsiConsoleAppender<E> extends ConsoleAppender<E> {

    @Override
    public void setOutputStream(OutputStream outputStream) {
        super.setOutputStream(AnsiConsole.wrapOutputStream(outputStream))
    }
}