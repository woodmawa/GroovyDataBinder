package com.softwood.logging.logback

import ch.qos.logback.classic.pattern.ThrowableProxyConverter
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.CoreConstants

/**
 * {@link ThrowableProxyConverter} that adds some additional whitespace around the stack
 * trace.
 *
 * @author Will Woodman
 */

public class WhitespaceThrowableProxyConverter extends ThrowableProxyConverter {

    @Override
    protected String throwableProxyToString(IThrowableProxy tp) {
        return CoreConstants.LINE_SEPARATOR + super.throwableProxyToString(tp)
        + CoreConstants.LINE_SEPARATOR
    }

}