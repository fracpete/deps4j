/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * MinDeps.java
 * Copyright (C) 2017 FracPete
 */

package com.github.fracpete.deps4j;

import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MinDeps {

  /** the java home directory to use. */
  protected File m_JavaHome;

  /** the jdeps binary. */
  protected File m_Jdeps;

  /** the classpath to use. */
  protected String m_ClassPath;

  /** the file with classes to determine the minimum dependencies for. */
  protected File m_ClassesFile;

  /** the file with additional resources to include (optional). */
  protected File m_ResourceFiles;

  /** the classes to inspect. */
  protected List<String> m_Classes;

  /** the optional resource files. */
  protected List<String> m_Resources;

  /** the dependent classes. */
  protected Set<String> m_DependentClasses;

  /** the full dependencies. */
  protected List<String> m_Dependencies;

  public MinDeps() {
    super();

    m_JavaHome         = (System.getenv("JAVA_HOME") != null ? new File(System.getenv("JAVA_HOME")) : null);
    m_ClassesFile      = null;
    m_ClassPath        = null;
    m_ResourceFiles    = null;
    m_Classes          = new ArrayList<>();
    m_Resources        = new ArrayList<>();
    m_DependentClasses = new HashSet<>();
    m_Dependencies     = new ArrayList<>();

  }

  /**
   * Sets the java home directory.
   *
   * @param value	the directory
   */
  public void setJavaHome(File value) {
    m_JavaHome = value;
  }

  /**
   * Returns the java home directory.
   *
   * @return		the directory
   */
  public File getJavaHome() {
    return m_JavaHome;
  }

  /**
   * Sets the classpath to use.
   *
   * @param value	the classpath
   */
  public void setClassPath(String value) {
    m_ClassPath = value;
  }

  /**
   * Returns the classpath to use.
   *
   * @return		the classpath, null if not set
   */
  public String getClassPath() {
    return m_ClassPath;
  }

  /**
   * Sets the file with the class files to inspect.
   *
   * @param value	the file
   */
  public void setClassesFile(File value) {
    m_ClassesFile = value;
  }

  /**
   * Returns the file with the class files to inspect.
   *
   * @return		the file, null if not set
   */
  public File getClassesFile() {
    return m_ClassesFile;
  }

  /**
   * Sets the file with the additional resource files to include (optional).
   *
   * @param value	the file
   */
  public void setResourceFiles(File value) {
    m_ResourceFiles = value;
  }

  /**
   * Returns the file with the additional resource files to include (optional).
   *
   * @return		the file, null if not set
   */
  public File getResourceFiles() {
    return m_ResourceFiles;
  }

  /**
   * Sets the commandline options.
   *
   * @param options	the options to use
   * @return		true if successful
   * @throws Exception	in case of an invalid option
   */
  public boolean setOptions(String[] options) throws Exception {
    ArgumentParser parser;
    Namespace ns;

    parser = ArgumentParsers.newArgumentParser(MinDeps.class.getName());
    parser.addArgument("--java-home")
      .type(Arguments.fileType().verifyExists().verifyIsDirectory())
      .dest("javahome")
      .required(true)
      .help("The java home directory of the JDK that includes the jdeps binary, default is taken from JAVA_HOME environment variable.");
    parser.addArgument("--class-path")
      .dest("classpath")
      .required(true)
      .help("The CLASSPATH to use for jdeps.");
    parser.addArgument("--classes")
      .type(Arguments.fileType().verifyExists().verifyIsFile().verifyCanRead())
      .dest("classes")
      .required(true)
      .help("The file containing the classes to determine the dependencies for. Empty lines and lines starting with # get ignored.");
    parser.addArgument("--resources")
      .type(Arguments.fileType())
      .setDefault(new File("."))
      .required(false)
      .dest("resources")
      .help("The file with resources to include (eg .props files).");

    try {
      ns = parser.parseArgs(options);
    }
    catch (ArgumentParserException e) {
      parser.handleError(e);
      return false;
    }

    setJavaHome(ns.get("javahome"));
    setClassPath(ns.getString("classpath"));
    setClassesFile(ns.get("classes"));
    setResourceFiles(ns.get("resources"));

    return true;
  }

  /**
   * Initializes the execution.
   */
  protected void initialize() {
    m_DependentClasses.clear();
    m_Resources.clear();
    m_Classes.clear();
    m_Dependencies.clear();
  }

  /**
   * Reads the file into the the provided list.
   * Skips empty lines and lines starting with #.
   *
   * @param file	the file to read
   * @param lines 	the lines to add the content to
   * @return 		null if successful, otherwise error message
   */
  protected String readFile(File file, List<String> lines) {
    int		i;

    try {
      lines.addAll(Files.readAllLines(file.toPath()));
      i = 0;
      while (i < lines.size()) {
        if (lines.get(i).trim().isEmpty()) {
          lines.remove(i);
          continue;
	}
	if (lines.get(i).startsWith("#")) {
          lines.remove(i);
          continue;
	}
	i++;
      }
    }
    catch (Exception e) {
      return "Failed to read file: " + file + "\n" + e;
    }

    return null;
  }

  /**
   * Performs some checks.
   *
   * @return		null if successful, otherwise error message
   */
  protected String check() {
    String		error;

    if (!m_JavaHome.exists())
      return "Java home directory does not exist: " + m_JavaHome;
    if (!m_JavaHome.isDirectory())
      return "Java home does not point to a directory: " + m_JavaHome;
    if (System.getProperty("os.name").toLowerCase().contains("windows"))
      m_Jdeps = new File(m_JavaHome.getAbsolutePath() + File.separator + "bin" + File.separator + "jdeps.exe");
    else
      m_Jdeps = new File(m_JavaHome.getAbsolutePath() + File.separator + "bin" + File.separator + "jdeps");
    if (!m_Jdeps.exists())
      return "jdeps binary does not exist: " + m_Jdeps;

    if (!m_ClassesFile.exists())
      return "File with class names does not exist: " + m_ClassesFile;
    if (m_ClassesFile.isDirectory())
      return "File with class names points to directory: " + m_ClassesFile;

    // read classes
    error = readFile(m_ClassesFile, m_Classes);
    if (error != null)
      return error;

    // read resources
    if ((m_ResourceFiles != null) && (!m_ResourceFiles.exists()) && (!m_ResourceFiles.isDirectory())) {
      error = readFile(m_ResourceFiles, m_Resources);
      if (error != null)
	return error;
    }

    return null;
  }

  /**
   * Determines the dependencies.
   *
   * @return		null if successful, otherwise error message
   */
  protected String determine() {
    String[] 			cmd;
    ProcessBuilder 		builder;
    CollectingProcessOutput 	output;

    for (String cls: m_Classes) {
      // progress
      System.err.println(cls);

      cmd = new String[]{
        m_Jdeps.getAbsolutePath(),
	"-cp",
	m_ClassPath,
	"-recursive",
	"-verbose:class",
	cls
      };
      builder = new ProcessBuilder();
      builder.command(cmd);
      output = new CollectingProcessOutput();
      try {
	output.monitor(builder);
      }
      catch (Exception e) {
        return "Failed to execute: " + builder.toString() + "\n" + e;
      }

      // TODO parse output
    }

    return null;
  }

  /**
   * Determines the dependencies.
   *
   * @return		null if successful, otherwise error message
   */
  public String execute() {
    String		result;

    initialize();

    result = check();

    if (result == null)
      result = determine();

    if (result == null) {
      m_Dependencies = new ArrayList<>();
      m_Dependencies.addAll(m_DependentClasses);
      m_Dependencies.addAll(m_Resources);
      Collections.sort(m_Dependencies);
    }

    return result;
  }

  /**
   * Outputs the dependencies on stdout.
   */
  public void output() {
    for (String dep: m_Dependencies)
      System.out.println(dep);
  }

  public static void main(String[] args) throws Exception {
    MinDeps	mindeps;
    String	error;

    mindeps = new MinDeps();
    if (mindeps.setOptions(args)) {
      error = mindeps.execute();
      if (error != null) {
        System.err.println(error);
	System.exit(2);
      }
      else {
        mindeps.output();
      }
    }
    else {
      System.exit(1);
    }
  }
}
