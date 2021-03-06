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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sonarsource.scanner.api.internal.cache.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class SimulatedLauncherTest {
  private static final String VERSION = "5.2";
  private SimulatedLauncher launcher;
  private Logger logger;
  private String filename;

  @Parameters(name = "use_deprecated_prop={0}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {{true}, {false}});
  }

  private final boolean useDeprecatedProp;

  public SimulatedLauncherTest(boolean useDeprecatedProp) {
    this.useDeprecatedProp = useDeprecatedProp;
  }

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() {
    logger = mock(Logger.class);
    launcher = new SimulatedLauncher(VERSION, logger);
    filename = new File(temp.getRoot(), "props").getAbsolutePath();
  }

  @Test
  public void testDump() throws IOException {
    Properties global = new Properties();
    global.putAll(createProperties(true));
    Properties analysis = new Properties();
    analysis.putAll(createProperties(false));

    launcher.start(global, null);
    launcher.execute(analysis);
    assertDump(global, analysis);
  }

  @Test(expected = IllegalStateException.class)
  public void error_if_no_dump_file() {
    launcher.execute(new Properties());
  }

  @Test(expected = IllegalStateException.class)
  public void error_dump() throws IOException {
    Properties p = mock(Properties.class);
    doThrow(IOException.class).when(p).store(any(OutputStream.class), anyString());
    launcher.execute(p);
  }

  @Test
  public void testOldExecute() {
    Properties global = new Properties();
    global.putAll(createProperties(true));
    Properties analysis = new Properties();
    analysis.putAll(createProperties(false));

    launcher.start(global, null);
    launcher.executeOldVersion(analysis, null);

  }

  private Properties createProperties(boolean global) {
    Properties prop = new Properties();
    prop.put("key1_" + global, "value1");
    prop.put("key2_" + global, "value2");
    prop.put(useDeprecatedProp ? InternalProperties.SCANNER_DUMP_TO_FILE_DEPRECATED : InternalProperties.SCANNER_DUMP_TO_FILE, filename);
    return prop;
  }

  @Test
  public void version() {
    assertThat(launcher.getVersion()).isEqualTo(VERSION);
  }

  private void assertDump(Properties global, Properties analysis) throws IOException {
    if (analysis != null) {
      Properties p = new Properties();
      p.load(new FileInputStream(new File(filename)));
      assertThat(p).isEqualTo(analysis);
    } else {
      assertThat(new File(filename)).doesNotExist();
    }

    if (global != null) {
      Properties p = new Properties();
      p.load(new FileInputStream(new File(filename + ".global")));
      assertThat(p).isEqualTo(global);
    } else {
      assertThat(new File(filename + ".global")).doesNotExist();
    }
  }

}
