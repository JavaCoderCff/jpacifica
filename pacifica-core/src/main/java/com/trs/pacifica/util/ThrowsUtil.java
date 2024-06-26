/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trs.pacifica.util;

import java.util.concurrent.ExecutionException;

public class ThrowsUtil {

    private ThrowsUtil() {

    }

    public static Throwable addSuppressed(Throwable exception, Throwable suppressed) {
        if (exception != null && suppressed != null) {
            exception.addSuppressed(suppressed);
        }
        return exception == null ? suppressed : exception;
    }


    public static Throwable getCause(final ExecutionException e) {
        if (e != null) {
            final Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof ExecutionException) {
                    return getCause((ExecutionException) cause);
                } else {
                    return cause;
                }
            }
        }
        return null;
    }


}
