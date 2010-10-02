/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.field.function.sort;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.elasticsearch.script.search.SearchScript;

import java.io.IOException;

/**
 * @author kimchy (shay.banon)
 */
public class StringFieldsFunctionDataComparator extends FieldComparator {

    public static FieldComparatorSource comparatorSource(SearchScript script) {
        return new InnerSource(script);
    }

    private static class InnerSource extends FieldComparatorSource {

        private final SearchScript script;

        private InnerSource(SearchScript script) {
            this.script = script;
        }

        @Override public FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
            return new StringFieldsFunctionDataComparator(numHits, script);
        }
    }

    private final SearchScript script;

    private String[] values;

    private String bottom;

    public StringFieldsFunctionDataComparator(int numHits, SearchScript script) {
        this.script = script;
        values = new String[numHits];
    }

    @Override public void setNextReader(IndexReader reader, int docBase) throws IOException {
        script.setNextReader(reader);
    }

    @Override public int compare(int slot1, int slot2) {
        final String val1 = values[slot1];
        final String val2 = values[slot2];
        if (val1 == null) {
            if (val2 == null) {
                return 0;
            }
            return -1;
        } else if (val2 == null) {
            return 1;
        }

        return val1.compareTo(val2);
    }

    @Override public int compareBottom(int doc) {
        final String val2 = script.execute(doc).toString();
        if (bottom == null) {
            if (val2 == null) {
                return 0;
            }
            return -1;
        } else if (val2 == null) {
            return 1;
        }
        return bottom.compareTo(val2);
    }

    @Override public void copy(int slot, int doc) {
        values[slot] = script.execute(doc).toString();
    }

    @Override public void setBottom(final int bottom) {
        this.bottom = values[bottom];
    }

    @Override public Comparable value(int slot) {
        return values[slot];
    }
}
