/*
 * SonarQube Scanner API
 * Copyright (C) 2011-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.api.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.sonarsource.scanner.api.internal.cache.FileCache;
import org.sonarsource.scanner.api.internal.cache.FileCacheBuilder;
import org.sonarsource.scanner.api.internal.cache.Logger;

import static java.lang.String.format;

class Jars {

  private final FileCache fileCache;
  private final ServerConnection connection;
  private final JarExtractor jarExtractor;
  private final Logger logger;

  Jars(ServerConnection conn, JarExtractor jarExtractor, Logger logger, Properties props) {
    this.logger = logger;
    this.fileCache = new FileCacheBuilder(logger)
      .setUserHome(props.getProperty("sonar.userHome"))
      .build();
    this.connection = conn;
    this.jarExtractor = jarExtractor;
  }

  /**
   * For unit tests
   */
  Jars(FileCache fileCache, ServerConnection conn, JarExtractor jarExtractor, Logger logger) {
    this.logger = logger;
    this.fileCache = fileCache;
    this.connection = conn;
    this.jarExtractor = jarExtractor;
  }

  /**
   * For unit tests
   */
  FileCache getFileCache() {
    return fileCache;
  }

  List<File> download() {
    List<File> files = new ArrayList<>();
    logger.debug("Extract sonar-scanner-api-batch in temp...");
    files.add(jarExtractor.extractToTemp("sonar-scanner-api-batch").toFile());
    files.addAll(getScannerEngineFiles());
    return files;
  }

  private List<File> getScannerEngineFiles() {
    List<File> files = new ArrayList<>();
    String bootstrapIndex = getBootstrapIndex();
    try {
      String[] lines = bootstrapIndex.split("[\r\n]+");
      ScannerFileDownloader scannerFileDownloader = new ScannerFileDownloader(connection);
      for (String line : lines) {
        line = line.trim();
        String[] libAndHash = line.split("\\|");
        String filename = libAndHash[0];
        String hash = libAndHash[1];
        files.add(fileCache.get(filename, hash, scannerFileDownloader));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to bootstrap from server. Bootstrap index was:\n" + bootstrapIndex, e);
    }
    return files;
  }

  private String getBootstrapIndex() {
    try {
      logger.debug("Get bootstrap index...");
      String libs = connection.downloadString("/batch/index");
      logger.debug("Get bootstrap completed");
      return libs;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get bootstrap index from server", e);
    }
  }

  static class ScannerFileDownloader implements FileCache.Downloader {
    private final ServerConnection connection;

    ScannerFileDownloader(ServerConnection conn) {
      this.connection = conn;
    }

    @Override
    public void download(String filename, File toFile) throws IOException {
      connection.downloadFile(format("/batch/file?name=%s", filename), toFile.toPath());
    }
  }
}
