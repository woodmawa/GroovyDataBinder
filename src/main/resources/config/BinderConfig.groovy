import com.softwood.databinder.converters.*

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

/**
 * Binder bootstrap configuration
 */

import java.time.LocalDateTime

//global type converters
binder {
    classDefault {
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
}

environments {
    dev {

    }
    test {

    }
    prod {

    }
}