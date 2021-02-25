/*
 * Copyright (c) 2021-present, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN]
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.salesforce.androidsdk.smartstore.store;

import android.util.LruCache;
import com.salesforce.androidsdk.analytics.security.Encryptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Key value store that keeps recently accessed values in a in-memory lru cache for faster access
 */
public class MemCachedKeyValueStore implements KeyValueStore {

    private KeyValueStore keyValueStore;
    private LruCache<String, String> memCache;

    public MemCachedKeyValueStore(KeyValueStore keyValueStore, int cacheSize){
        this.keyValueStore = keyValueStore;
        memCache = new LruCache<>(cacheSize);
    }

    @Override
    public String getValue(String key) {
        String value = memCache.get(key);
        if (value == null) {
            value = keyValueStore.getValue(key);
            if (value != null) {
                keyValueStore.saveValue(key, value);
                memCache.put(key, value);
            }
        }
        return value;
    }

    @Override
    public InputStream getStream(String key) {
        String value = getValue(key);
        return value == null ?  null : new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean saveValue(String key, String value) {
        if (keyValueStore.saveValue(key, value)) {
            memCache.put(key, value);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean saveStream(String key, InputStream stream) throws IOException {
        String value = Encryptor.getStringFromStream(stream);
        return saveValue(key, value);
    }

    @Override
    public boolean deleteValue(String key) {
        if (keyValueStore.deleteValue(key)) {
            memCache.remove(key);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void deleteAll() {
        memCache.evictAll();
        keyValueStore.deleteAll();
    }

    @Override
    public int count() {
        return keyValueStore.count();
    }

    @Override
    public boolean isEmpty() {
        return keyValueStore.isEmpty();
    }

    @Override
    public String getStoreName() {
        return keyValueStore.getStoreName();
    }
}