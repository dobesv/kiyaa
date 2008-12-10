package com.datejs.client;

import java.util.Date;

import com.datejs.client.DateJs.DateParseFailedException;
import com.habitsoft.kiyaa.util.ClientLocalizedParser;
import com.habitsoft.kiyaa.util.DateParseException;

public class LocalizedParserWithDateJs extends ClientLocalizedParser {

    public Date parseDate(String dateString) throws DateParseException {
        try {
            return DateJs.parse(dateString).toDate();
        } catch (DateParseFailedException e) {
            throw new DateParseException(e);
        }
    }
}