package com.tyndalehouse.step.rest.controllers;

import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Controller for listing directory contents as JSON
 */
@Singleton
public class HomesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomesController.class);
    private static final String HOMES_DIRECTORY = "/opt/step/homes";

    /**
     * Lists the contents of the homes directory
     *
     * @return a list of directory entries with metadata
     */
    @Timed(name = "list-homes", group = "homes", rateUnit = TimeUnit.SECONDS, durationUnit = TimeUnit.MILLISECONDS)
    public List<Map<String, Object>> listHomes() {
        List<Map<String, Object>> items = new ArrayList<>();

        File directory = new File(HOMES_DIRECTORY);

        if (!directory.exists()) {
            LOGGER.warn("Homes directory does not exist: {}", HOMES_DIRECTORY);
            return items;
        }

        if (!directory.isDirectory()) {
            LOGGER.warn("Homes path is not a directory: {}", HOMES_DIRECTORY);
            return items;
        }

        try {
            File[] files = directory.listFiles();

            if (files == null) {
                LOGGER.warn("Unable to list files in directory: {}", HOMES_DIRECTORY);
                return items;
            }

            for (File file : files) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", file.getName());
                item.put("isDirectory", file.isDirectory());
                item.put("size", file.length());
                item.put("modified", file.lastModified());
                item.put("path", file.getAbsolutePath());
                items.add(item);
            }

            LOGGER.info("Listed {} items from homes directory", items.size());
        } catch (Exception e) {
            LOGGER.error("Error listing homes directory", e);
        }

        return items;
    }

    /**
     * Lists the contents of a specific subdirectory within homes
     *
     * @param subdir the subdirectory name to list
     * @return a list of directory entries with metadata
     */
    @Timed(name = "list-homes-subdir", group = "homes", rateUnit = TimeUnit.SECONDS, durationUnit = TimeUnit.MILLISECONDS)
    public List<Map<String, Object>> listHomesSubdirectory(final String subdir) {
        List<Map<String, Object>> items = new ArrayList<>();

        // Security: prevent directory traversal attacks
        if (subdir == null || subdir.isEmpty() || subdir.contains("..") || subdir.startsWith("/")) {
            LOGGER.warn("Invalid subdirectory requested: {}", subdir);
            return items;
        }

        File directory = new File(HOMES_DIRECTORY + File.separator + subdir);

        if (!directory.exists()) {
            LOGGER.warn("Subdirectory does not exist: {}", directory.getPath());
            return items;
        }

        if (!directory.isDirectory()) {
            LOGGER.warn("Path is not a directory: {}", directory.getPath());
            return items;
        }

        try {
            File[] files = directory.listFiles();

            if (files == null) {
                LOGGER.warn("Unable to list files in directory: {}", directory.getPath());
                return items;
            }

            for (File file : files) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", file.getName());
                item.put("isDirectory", file.isDirectory());
                item.put("size", file.length());
                item.put("modified", file.lastModified());
                item.put("path", file.getAbsolutePath());
                items.add(item);
            }

            LOGGER.info("Listed {} items from subdirectory: {}", items.size(), subdir);
        } catch (Exception e) {
            LOGGER.error("Error listing subdirectory: {}", subdir, e);
        }

        return items;
    }
}
