package org.whitesource.agent.utils;

/**
 * Copyright (C) 2014 WhiteSource Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.slf4j.Logger;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.LoggerFactory;

/**
 * Utility class for various zip operations.
 *
 * @author tom.shapira
 * @author eugen.horovitz
 */
public class ZipUtils {

    /* --- Static members --- */

    private static final Logger logger = LoggerFactory.getLogger(ZipUtils.class);

    public static final String JAVA_TEMP_DIR = System.getProperty("java.io.tmpdir");
    public static final String ZIP_UTILS = System.getProperty("WSZipUtils");
    public static final String UTF_8 = "UTF-8";
    public static final int BYTES_BUFFER_SIZE = 32 * 1024;
    public static final int STRING_MAX_SIZE = BYTES_BUFFER_SIZE;
    public static final String TMP_IN_ = "tmp_in_";
    public static final String TMP_OUT_ = "tmp_out_";
    public static final String ZIP_UTILS_SUFFIX = ".json";

    public static final int N_THREADS = 2;

    /* --- Static methods --- */

    /**
     * The method compresses the string using gzip.
     *
     * @param text The string to compress.
     * @return The compressed string.
     * @throws java.io.IOException Does some thing in old style.
     * @deprecated use {@link #compressString(String)} instead.
     */
    @Deprecated
    public static String compress(String text) throws IOException {
        String result;
        if (text != null && text.length() > 0) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(text.length());
            GZIPOutputStream gzipos = null;
            try {
                gzipos = new GZIPOutputStream(baos);
                gzipos.write(text.getBytes(UTF_8));
                gzipos.close();
                baos.close();
                result = (new BASE64Encoder()).encode(baos.toByteArray());
                /* TODO
                Replace result raw to this one : result = Base64.encodeBase64String(baos.toByteArray());
                See :
                Should not be using classes that are in sun.* packages - those classes are not part of the public API
                Java and can change in any new Java version
                http://stackoverflow.com/questions/29692146/java-lang-noclassdeffounderror-sun-misc-base64encoder
                http://www.oracle.com/technetwork/java/faq-sun-packages-142232.html
                */
            } catch (IOException e) {
                result = text;
            } finally {
                baos.close();
                if (gzipos != null) {
                    gzipos.close();
                }
            }
        } else {
            result = text;
        }
        return result;
    }

    /**
     * The method decompresses the string using gzip.
     *
     * @param text The string to decompress.
     * @return The decompressed string.
     * @throws java.io.IOException Does some thing in old style.
     * @deprecated use {@link #decompressString(String)} instead.
     */
    public static String decompress(String text) throws IOException {
        if (text == null || text.length() == 0) {
            return text;
        }

        byte[] bytes = new BASE64Decoder().decodeBuffer(text);
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, UTF_8));
        String outStr = "";
        String line;
        while ((line = bf.readLine()) != null) {
            outStr += line;
        }
        return outStr;
    }

    /**
     * The method compresses the big strings using gzip - low memory via the File system
     *
     * @param text The string to decompress.
     * @return The decompressed temp file path that should be deleted on a later stage.
     * @throws java.io.IOException
     */
    public static Path decompressChunks(String text) throws IOException {
        File tempFileOut = File.createTempFile(TMP_OUT_, ZIP_UTILS_SUFFIX, new File(JAVA_TEMP_DIR));
        if (text == null || text.length() == 0) {
            return null;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(tempFileOut.toPath())) {
            byte[] bytes = new BASE64Decoder().decodeBuffer(text);
            try (GZIPInputStream chunkZipper = new GZIPInputStream(new ByteArrayInputStream(bytes));
                 InputStream in = new BufferedInputStream(chunkZipper);) {

                byte[] buffer = new byte[BYTES_BUFFER_SIZE];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    String val = new String(buffer, StandardCharsets.UTF_8);
                    writer.write(val);
                }
            }
        }

        return tempFileOut.toPath();
    }

    public static String compressString(String text) throws IOException {
        try (ByteArrayOutputStream exportByteArrayOutputStream = new ByteArrayOutputStream()) {
            fillExportStreamCompress(text, exportByteArrayOutputStream);
            return getStringFromEncode(exportByteArrayOutputStream.toByteArray());
        }
    }

    public static String decompressString(String text) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        fillExportStreamDecompress(text, stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * The method compresses the big strings using gzip - low memory via the File system
     *
     * @param text The string to compress.
     * @return The compressed string.
     * @throws java.io.IOException
     */
    public static String compressChunks(String text) throws IOException {
        Path tempFolder = Paths.get(JAVA_TEMP_DIR, ZIP_UTILS);
        File tempFileIn = File.createTempFile(TMP_IN_, ZIP_UTILS_SUFFIX, tempFolder.toFile());
        File tempFileOut = File.createTempFile(TMP_OUT_, ZIP_UTILS_SUFFIX, tempFolder.toFile());

        writeChunkBytes(text, tempFileIn);
        String result;
        if (text != null && text.length() > 0) {
            try (InputStream in = new FileInputStream(tempFileIn);
                 FileOutputStream fileOutputStream = new FileOutputStream(tempFileOut);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                 OutputStream out = new GZIPOutputStream(bufferedOutputStream);) {

                byte[] bytes = new byte[BYTES_BUFFER_SIZE];
                int len;
                while ((len = in.read(bytes)) > 0) {
                    out.write(bytes, 0, len);
                }

                in.close();
                out.flush();
                out.close();

                result = new BASE64Encoder().encode(Files.readAllBytes(tempFileOut.toPath()));
            }
        } else {
            result = text;
        }

        Files.deleteIfExists(tempFileIn.toPath());
        Files.deleteIfExists(tempFileOut.toPath());

        return result;
    }

    /* --- Compress Helpers --- */

    private static String getStringFromEncode(byte[] bytes) {
        return new BASE64Encoder().encode(bytes);
    }

    private static void fillExportStreamCompress(String text, OutputStream exportByteArrayOutputStream) {
        try {
            try (PipedInputStream pipedInputStream = new PipedInputStream()) {
                try (PipedOutputStream pipedOutputStream = new PipedOutputStream()) {
                    pipedInputStream.connect(pipedOutputStream);

                    Runnable producer = () -> producedDataCompress(text, pipedOutputStream);
                    Runnable consumer = () -> consumeDataCompress(pipedInputStream, exportByteArrayOutputStream);

                    transferData(producer, consumer);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to produce data :", e);
        }
    }

    private static void transferData(Runnable producer, Runnable consumer) {
        ExecutorService threadPool = Executors.newFixedThreadPool(N_THREADS);
        threadPool.submit(producer);
        try {
            threadPool.submit(consumer).get();
        } catch (InterruptedException e) {
            logger.error("Task failed : ", e);
        } catch (ExecutionException e) {
            logger.error("Task failed : ", e);
        } finally {
            threadPool.shutdown();
        }
    }

    private static void consumeDataCompress(PipedInputStream pipedInputStream, OutputStream exportByteArrayOutputStream) {
        try (OutputStream out = new GZIPOutputStream(new BufferedOutputStream(exportByteArrayOutputStream))) {
            try {
                byte[] bytes = new byte[BYTES_BUFFER_SIZE];
                int len;
                while ((len = pipedInputStream.read(bytes)) > 0) {
                    out.write(bytes, 0, len);
                }
                out.flush();
            } catch (IOException e) {
                logger.error("Failed to consume data to compress:", e);
            }
        } catch (IOException e) {
            logger.error("Failed to consume data to compress:", e);
        }
    }

    private static void producedDataCompress(String text, PipedOutputStream pipedOutputStream) {
        int start_String = 0;
        int chunk = text.length();
        if (text.length() > STRING_MAX_SIZE) {
            chunk = text.length() / STRING_MAX_SIZE;
        }
        try {
            writeStringChunks(text, pipedOutputStream, start_String, chunk);
            pipedOutputStream.close();
        }
        catch (IOException e) {
            logger.error("Failed to produce data to compress : ", e);
        }
    }

    private static void writeStringChunks(String text, PipedOutputStream pipedOutputStream, int start_String, int chunk) throws IOException {
        while (start_String < text.length()) {
            int end = start_String + chunk;
            if (end > text.length()) {
                end = text.length();
            }
            byte[] bytes = text.substring(start_String, end).getBytes(StandardCharsets.UTF_8);

            pipedOutputStream.write(bytes);
            start_String = end;
        }
    }

    private static void fillExportStreamDecompress(String text, StringBuilder stringBuilder) {
        try {
            try (PipedInputStream pipedInputStream = new PipedInputStream()) {
                try (PipedOutputStream pipedOutputStream = new PipedOutputStream()) {
                    pipedInputStream.connect(pipedOutputStream);

                    Runnable producer = () -> producedDataDecompress(text, pipedOutputStream);
                    Runnable consumer = () -> consumeDataDecompress(pipedInputStream, stringBuilder);

                    transferData(producer, consumer);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to decompress : ", e);
        }
    }

    private static void consumeDataDecompress(PipedInputStream pipedInputStream, StringBuilder stringBuilder) {
        try (GZIPInputStream chunkZipper = new GZIPInputStream(pipedInputStream);
             InputStream in = new BufferedInputStream(chunkZipper);) {

            byte[] buffer = new byte[BYTES_BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) > 0) {
                if (len < buffer.length) {
                    byte[] writtenBytes = new byte[len];
                    getBytes(buffer, 0, len, writtenBytes, 0);
                    stringBuilder.append(new String(writtenBytes, StandardCharsets.UTF_8));
                } else {
                    stringBuilder.append(new String(buffer, StandardCharsets.UTF_8));
                }
            }
            pipedInputStream.close();
        } catch (IOException e) {
            logger.error("Failed to decompress : ", e);
        }
    }

    public static void getBytes(byte[] source, int srcBegin, int srcEnd, byte[] destination,
                                int dstBegin) {
        System.arraycopy(source, srcBegin, destination, dstBegin, srcEnd - srcBegin);
    }

    private static void producedDataDecompress(String text, PipedOutputStream pipedOutputStream) {
        try {
            byte[] bytes = getStringFromDecode(text);
            pipedOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getStringFromDecode(String text) throws IOException {
        return new BASE64Decoder().decodeBuffer(text);
    }

    /**
     * Writes a string piece by piece to file
     *
     * @param text
     * @param tempFileIn
     * @throws IOException
     */
    private static void writeChunkBytes(String text, File tempFileIn) throws IOException {
        try (FileOutputStream writer = new FileOutputStream(tempFileIn)) {
            int chunk = text.length();
            if (text.length() > STRING_MAX_SIZE) {
                chunk = text.length() / STRING_MAX_SIZE;
            }

            int startString = 0;
            while (startString < text.length()) {
                int endString = startString + chunk;
                if (endString > text.length()) {
                    endString = text.length();
                }
                byte[] bytes = text.substring(startString, endString).getBytes(StandardCharsets.UTF_8);
                writer.write(bytes);
                startString = endString;
            }
        }
    }

    /* --- Constructors --- */

    /**
     * Private default constructor
     */
    private ZipUtils() {
        // avoid instantiation
    }
}