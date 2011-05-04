package com.habitsoft.kiyaa.util;

import java.util.Date;

import com.datejs.client.DateJs;
import com.datejs.client.DateJs.DateParseFailedException;

public class LocalizedParserWithDateJs extends ClientLocalizedParser {

    @Override
    public Date parseDate(String dateString) throws DateParseException {
    	dateString = validateDateString(dateString);
        try {
            // Try the "standard" date format first
            return super.parseDate(dateString);
        } catch(DateParseException e) {
        }
        try {
            // Fall back on DateJs to parse everything else
            return DateJs.parse(dateString).toDate();
        } catch (DateParseFailedException e) {
            throw new DateParseException(e);
        }
    }
}
