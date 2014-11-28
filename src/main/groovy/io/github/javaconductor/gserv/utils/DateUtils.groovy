/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Lee Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.javaconductor.gserv.utils

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Created by javaConductor on 3/30/14.
 */
class DateUtils {

    private DateFormat m_ISO8601Local =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public String formatDateTime() {
        return formatDateTime(new Date());
    }

    public String formatUTC(Date date) {
        if (date == null) {
            return formatDateTime(new Date());
        }

        // format in (almost) ISO8601 format
        String dateStr = m_ISO8601Local.format(date)

        // remap the timezone from 0000 to 00:00 (starts at char 22)
        return dateStr
    }

    public String parseUTC(String utcDate) {
        // format in (almost) ISO8601 format
        return m_ISO8601Local.parse(utcDate);
    }

}
