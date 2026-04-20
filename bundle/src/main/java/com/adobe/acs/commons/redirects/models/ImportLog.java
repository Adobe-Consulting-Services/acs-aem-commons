/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.redirects.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A class to collect messages when importing redirect rules from a spreadsheet.
 */
public class ImportLog {
    private final List<Entry> log = new ArrayList<>();
    private String path;

    public void setPath(String path){
        this.path = path;
    }

    public String getPath(){
        return path;
    }

    public List<Entry> getLog(){
        // WARNs first, followed by INFOs, then by cell ascending
        log.sort(Comparator.comparing(Entry::getLevel).thenComparing(Entry::getCell));
        return log;
    }

    public void warn(String cell, String msg){
        log.add(new Entry(Level.WARN, cell , msg));
    }

    public void info(String cell, String msg){
        log.add(new Entry(Level.INFO, cell , msg));
    }

    public static class Entry {
        Level level;
        String cell;
        String msg;

        Entry(){
            // default constructor needed to de-serialize from json
        }

        Entry(Level level, String cell, String msg) {
            this.level = level;
            this.cell = cell;
            this.msg = msg;
        }

        public Level getLevel() {
            return level;
        }

        public String getCell() {
            return cell;
        }

        public String getMsg() {
            return msg;
        }
    }

    public enum Level {
        WARN,
        INFO
    }
}
