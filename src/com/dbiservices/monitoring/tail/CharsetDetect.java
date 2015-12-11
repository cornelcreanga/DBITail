/*
 * Copyright 2015 ph002551.
 *
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
 */
package com.dbiservices.monitoring.tail;

import com.dbiservices.tools.Logger;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *
 * @author ph002551
 */
public class CharsetDetect extends Thread {

    private static final Logger logger = Logger.getLogger(CharsetDetect.class);

    private InputStream in = null;
    private OutputStream out = null;
    private InformationObject informationObject = null;
    private long maxChanlangedBytes = 4096;

    public Charset charset = StandardCharsets.US_ASCII;

    public CharsetDetect(InputStream in, OutputStream out, InformationObject informationObject, long maxChanlangedBytes) {

        this.in = in;
        this.out = out;
        this.informationObject = informationObject;
        this.maxChanlangedBytes = maxChanlangedBytes;
    }

    public void run() {
        byte[] buf = new byte[4096];

        SortedMap<String, Charset> charsetMap = Charset.availableCharsets();

        if (informationObject.getCharset() == null) {
            try {

                UniversalDetector detector = new UniversalDetector(null);
                String encoding = null;
                int nread;
                long nreadtotal = 0;
                while ((nread = in.read(buf)) > 0 && !detector.isDone() && nreadtotal < maxChanlangedBytes) {
                    detector.handleData(buf, 0, nread);
                    nreadtotal += nread;
                    if (out != null) {
                        out.write(buf, 0, nread);
                        out.flush();
                    }
                }

                detector.dataEnd();
                
                encoding = detector.getDetectedCharset();
                if (encoding != null) {
                    logger.info("Detected charset = " + encoding);

                    if (charsetMap.containsKey(encoding.toUpperCase())) {

                        logger.info("Detected charset is in supported list !");
                        charset = charsetMap.get(encoding.toUpperCase());
                        this.informationObject.setCharset(charset);
                        DbiTail.saveTreeToFile();
                    }
                } else {
                    logger.warning("No charset detected. Using default: " + charset.name());
                }

                detector.reset();
                in.close();

            } catch (Exception e) {
            } finally {

                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception ex) {
                    }
                }

                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
        }
    }
}