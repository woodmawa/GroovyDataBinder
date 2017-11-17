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
}

environments {
    dev {

    }
    test {

    }
    prod {

    }
}