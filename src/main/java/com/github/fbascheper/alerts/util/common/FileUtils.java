package com.github.fbascheper.alerts.util.common;

import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Basic File utilities.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public final class FileUtils {

    private static final Logger LOGGER = getLogger(FileUtils.class);

    private FileUtils() {
        // prevent instantiation.
    }

    /**
     * Read a file on the classpath.
     *
     * @param filename name of the file to read
     * @return an array of bytes.
     */
    public static byte[] readFile(String filename) {

        try {
            InputStream stream = FileUtils.class.getClassLoader().getResourceAsStream(filename);
            Objects.requireNonNull(stream);
            return IOUtils.toByteArray(stream);

        } catch (IOException | NullPointerException ex) {
            LOGGER.error("Could not read file " + filename + " from classpath", ex);
            throw new IllegalStateException("Could not read file " + filename + " from classpath", ex);
        }

    }

    /**
     * Read a list of lines from a file on the classpath.
     *
     * @param filename name of the file to read
     * @return an array of bytes.
     */
    public static List<String> readLines(String filename) {

        try {
            InputStream stream = FileUtils.class.getClassLoader().getResourceAsStream(filename);
            Objects.requireNonNull(stream);
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return buffer.lines().collect(Collectors.toList());
            }
        } catch (IOException | NullPointerException ex) {
            LOGGER.error("Could not read file " + filename + " from classpath", ex);
            throw new IllegalStateException("Could not read file " + filename + " from classpath", ex);
        }
    }
}
