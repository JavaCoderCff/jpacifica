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

package com.trs.pacifica.fs;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface FileReader {


    public static final int EOF = -1;

    /**
     * Read file into buffer starts from offset at most length.
     *
     * @param buffer   buffer
     * @param filename filename
     * @param offset   offset
     * @param length   max read length
     * @return -1 If the end of the file is reached, otherwise, the length read is returned.
     * @throws IOException io error
     */
    public int read(ByteBuffer buffer, String filename, int offset, int length) throws IOException;

}
