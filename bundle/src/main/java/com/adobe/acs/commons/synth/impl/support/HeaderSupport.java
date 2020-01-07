package com.adobe.acs.commons.synth.impl.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**

 * @since 2018-10-09
 */
public class HeaderSupport {
    
    private static final DateFormat RFC1123_DATE_FORMAT;
    private static final TimeZone TIMEZONE_GMT;
    
    static {
        RFC1123_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        TIMEZONE_GMT = TimeZone.getTimeZone("GMT");
        RFC1123_DATE_FORMAT.setTimeZone(TIMEZONE_GMT);
    }
    
    private List<HeaderValue> headers = new ArrayList();

    public HeaderSupport() {
    }

    public void addHeader(String name, String value) {
        this.headers.add(new HeaderSupport.HeaderValue(name, value));
    }

    public void addIntHeader(String name, int value) {
        this.headers.add(new HeaderSupport.HeaderValue(name, Integer.toString(value)));
    }

    public void addDateHeader(String name, long date) {
        Calendar calendar = Calendar.getInstance(TIMEZONE_GMT, Locale.US);
        calendar.setTimeInMillis(date);
        this.headers.add(new HeaderSupport.HeaderValue(name, formatDate(calendar)));
    }

    public void setHeader(String name, String value) {
        this.removeHeaders(name);
        this.addHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        this.removeHeaders(name);
        this.addIntHeader(name, value);
    }

    public void setDateHeader(String name, long date) {
        this.removeHeaders(name);
        this.addDateHeader(name, date);
    }

    private void removeHeaders(String name) {
        for(int i = this.headers.size() - 1; i >= 0; --i) {
            if (StringUtils.equalsIgnoreCase(this.headers.get(i).getKey(), name)) {
                this.headers.remove(i);
            }
        }
    }

    public boolean containsHeader(String name) {
        return !this.getHeaders(name).isEmpty();
    }

    public String getHeader(String name) {
        Collection<String> values = this.getHeaders(name);
        return !values.isEmpty() ? values.iterator().next() : null;
    }

    public int getIntHeader(String name) {
        String value = this.getHeader(name);
        return NumberUtils.toInt(value);
    }

    public long getDateHeader(String name) {
        String value = this.getHeader(name);
        if (StringUtils.isEmpty(value)) {
            return 0L;
        } else {
            try {
                return parseDate(value).getTimeInMillis();
            } catch (ParseException var4) {
                return 0L;
            }
        }
    }

    public Collection<String> getHeaders(String name) {
        List<String> values = new ArrayList();
        Iterator var3 = this.headers.iterator();

        while(var3.hasNext()) {
            HeaderSupport.HeaderValue entry = (HeaderSupport.HeaderValue)var3.next();
            if (StringUtils.equalsIgnoreCase(entry.getKey(), name)) {
                values.add(entry.getValue());
            }
        }

        return values;
    }

    public Collection<String> getHeaderNames() {
        Set<String> values = new HashSet();
        Iterator var2 = this.headers.iterator();

        while(var2.hasNext()) {
            HeaderSupport.HeaderValue entry = (HeaderSupport.HeaderValue)var2.next();
            values.add(entry.getKey());
        }

        return values;
    }

    public void reset() {
        this.headers.clear();
    }

    public static Enumeration<String> toEnumeration(Collection<String> collection) {
        return (new Vector(collection)).elements();
    }

    private static synchronized String formatDate(Calendar date) {
        return RFC1123_DATE_FORMAT.format(date.getTime());
    }

    private static synchronized Calendar parseDate(String dateString) throws ParseException {
        RFC1123_DATE_FORMAT.parse(dateString);
        return RFC1123_DATE_FORMAT.getCalendar();
    }

    private static class HeaderValue {
        private String key;
        private String value;

        public HeaderValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return this.value;
        }
    }
}
