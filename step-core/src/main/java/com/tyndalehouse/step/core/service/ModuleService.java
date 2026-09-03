package com.tyndalehouse.step.core.service;

import com.tyndalehouse.step.core.models.BibleVersion;
import com.tyndalehouse.step.core.models.HomeDirectoryInfo;
import org.crosswire.jsword.book.BookCategory;

import java.util.List;

/**
 * Interface to the service that gives information about the books of the bible, the different types of bible,
 * etc. This service will mainly use JSword but may also rely on other data sources to display text.
 */
public interface ModuleService {
    /**
     * @return all installed modules
     */
    List<BibleVersion> getAvailableModules();

    /**
     * @param categories     the types of modules to include
     * @param installerIndex installer index to look up modules from
     * @return a list of all modules that could be installed
     */
    List<BibleVersion> getAllInstallableModules(int installerIndex, BookCategory... categories);

    /**
     * @return a list of home directories with their paths and modification times
     */
    List<HomeDirectoryInfo> listHomes();

    /**
     * @return byte[] of file contennts
     */
    byte[] getHomeFile(String relativePath);
}
