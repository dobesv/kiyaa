package com.datejs.client;

import java.util.Date;

import com.datejs.client.DateJs.DateParseFailedException;
import com.habitsoft.kiyaa.util.ClientLocalizedParser;
import com.habitsoft.kiyaa.util.DateParseException;

public class LocalizedParserWithDateJs extends ClientLocalizedParser {

    @Override
    public Date parseDate(String dateString) throws DateParseException {
        try {
            // Try the "standard" date format first
            return super.parseDate(dateString);
        } catch(DateParseException e) {
        }
        try {
            // Fall back on DAteJs to parse everything else
            return DateJs.parse(dateString).toDate();
        } catch (DateParseFailedException e) {
            throw new DateParseException(e);
        }
    }
}
