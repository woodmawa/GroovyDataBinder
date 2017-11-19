import com.softwood.databinder.converters.*

//Binder bootstrap configuration

import java.time.LocalDateTime

//global type converters
global {
    converters  =  [
        [Calendar, LocalDateTime, CalendarToLocalDateTimeConverter],
        [Date, LocalDateTime, DateToLocalDateTimeConverter],
        [String, File, StringToFileConverter],
        [String, LocalDateTime, StringToLocalDateTimeConverter],
        [URI, File, UriToFileConverter],
        [URL, File, UrlToFileConverter]
    ]

    //if true, source strings will by default be trimmed.
    trimStrings = true

    //default date format for binder date to string conversion like '2001.07.04 at 12:08:56 GMT'
    dateFormat = "yyyy.MM.dd 'at' HH:mm:ss z"
}

environments {
    dev {

    }
    test {

    }
    prod {

    }
}