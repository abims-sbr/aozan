/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan;

import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

/**
 * This class define a RunData object that contains all information generated by
 * collectors.
 * @author Laurent Jourdren
 * @since 0.8
 */
public class RunData {

  /** Splitter. */
  private static final Splitter COMMA_SPLITTER =
      Splitter.on(",").trimResults().omitEmptyStrings();

  /** The map. */
  private final Map<String, String> map = new LinkedHashMap<>();

  //
  // Methods to extract data
  //
  /**
   * Get all projects names, separated by comma.
   * @return all projects names
   */
  public String getProjectsName() {
    return this.get("design.projects.names");
  }

  /**
   * Get all samples names related to the lane, separated by comma.
   * @param lane the lane number
   * @return all samples related to the lane
   */
  public String getSamplesNameInLane(final int lane) {
    return this.get("design.lane" + lane + ".samples.names");
  }

  /**
   * Get the read count in run.
   * @return read count.
   */
  public int getReadCount() {
    return this.getInt("run.info.read.count");
  }

  /**
   * Get the lane count in run.
   * @return lane count
   */
  public int getLaneCount() {
    return this.getInt("run.info.flow.cell.lane.count");
  }

  /**
   * Check if the read is indexed.
   * @param read the read number
   * @return true if the read is indexed,otherwise false
   */
  public boolean isReadIndexed(final int read) {
    return this.getBoolean("run.info.read" + read + ".indexed");
  }

  /**
   * Checks if is lane indexed.
   * @param lane the lane
   * @return true, if is lane indexed
   */
  public boolean isLaneIndexed(final int lane) {
    final String res =
        this.get("demux.lane" + lane + ".sample.lane" + lane + ".barcode");

    return (res == null
        ? false : res.trim().toLowerCase(Globals.DEFAULT_LOCALE)
            .equals("undetermined"));
  }

  /**
   * Get the tiles count.
   * @return tiles count
   */
  public int getTilesCount() {
    return this.getInt("read1.lane1.tile.count");
  }

  /**
   * Get the sample names related to a lane, in a list.
   * @param lane the number
   * @return a list with the sample names related to the lane
   */
  public List<String> getSamplesNameListInLane(final int lane) {
    return COMMA_SPLITTER
        .splitToList(this.get("design.lane" + lane + ".samples.names"));
  }

  /**
   * Get all projects names, in a list.
   * @return a list projects names
   */
  public List<String> getProjectsNameList() {
    return COMMA_SPLITTER.splitToList(this.get("design.projects.names"));
  }

  /**
   * Get the sequence index related to the lane number and the sample name.
   * @param lane the lane number
   * @param sampleName the sample name
   * @return sequence index related to the lane and sample name
   */
  public String getIndexSample(final int lane, final String sampleName) {
    return this.get("design.lane" + lane + "." + sampleName + ".index");
  }

  /**
   * Get the project related to a sample of the lane.
   * @param lane the lane number
   * @param sampleName the sample name
   * @return the project related to the sample
   */
  public String getProjectSample(final int lane, final String sampleName) {
    return this
        .get("design.lane" + lane + "." + sampleName + ".sample.project");
  }

  /**
   * Get the description of a sample.
   * @param lane the lane
   * @param sampleName the sample name
   * @return the description of the sample
   */
  public String getSampleDescription(final int lane, final String sampleName) {
    return this.get("design.lane" + lane + "." + sampleName + ".description");
  }

  /**
   * Gets the sample genome.
   * @param lane the lane
   * @param sampleName the sample name
   * @return the sample genome
   */
  public String getSampleGenome(final int lane, final String sampleName) {
    return this.get("design.lane" + lane + "." + sampleName + ".sample.ref");
  }

  /**
   * Get the raw cluster count for a sample.
   * @param lane the lane
   * @param read the read
   * @param sampleName sample name
   * @return the raw cluster count of the sample
   */
  public int getSampleRawClusterCount(final int lane, final int read,
      final String sampleName) {

    return this.getInt("demux.lane"
        + lane + ".sample." + sampleName + ".read" + read
        + ".raw.cluster.count");
  }

  /**
   * Get the passing filter cluster count for a sample.
   * @param lane the lane
   * @param read the read
   * @param sampleName sample name
   * @return the passing filter cluster count of the sample
   */
  public int getSamplePFClusterCount(final int lane, final int read,
      final String sampleName) {

    return this.getInt("demux.lane"
        + lane + ".sample." + sampleName + ".read" + read
        + ".pf.cluster.count");
  }

  /**
   * Get the raw cluster recovery count for a sample.
   * @param lane the lane
   * @param sampleName sample name
   * @return the raw cluster recovery count of the sample
   */
  public int getSampleRawClusterRecoveryCount(final int lane,
      final String sampleName) {

    if (sampleName == null)
      return this.getInt(
          "undeterminedindices.lane" + lane + ".recoverable.raw.cluster.count");

    return this.getInt("undeterminedindices.lane"
        + lane + ".sample." + sampleName + ".recoverable.raw.cluster.count");
  }

  /**
   * Get the passing filter cluster recovery count for a sample.
   * @param lane the lane
   * @param sampleName sample name
   * @return the passing filter cluster recovery count of the sample
   */
  public int getSamplePFClusterRecoveryCount(final int lane,
      final String sampleName) {

    if (sampleName == null)
      return this.getInt(
          "undeterminedindices.lane" + lane + ".recoverable.pf.cluster.count");

    return this.getInt("undeterminedindices.lane"
        + lane + ".sample." + sampleName + ".recoverable.pf.cluster.count");
  }

  /**
   * Get the percent mapped read on dataset contaminants.
   * @param lane the lane
   * @param sampleName sample name
   * @param read the read number
   * @return the percent mapped read on dataset contaminants.
   */
  public double getPercentMappedReadOnContaminationSample(final int lane,
      final String sampleName, final int read) {

    if (sampleName == null)
      return this.getDouble("fastqscreen.lane"
          + lane + ".undetermined.read" + read + ".mappedexceptgenomesample");

    return this.getDouble("fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read" + read + "." + sampleName
        + ".mappedexceptgenomesample");
  }

  /**
   * Check if the lane related to the sample name is a control lane.
   * @param lane the lane number
   * @param sampleName the sample name
   * @return true if the lane is a control otherwise false
   */
  public boolean isLaneControl(final int lane, final String sampleName) {
    return this
        .getBoolean("design.lane" + lane + "." + sampleName + ".control");
  }

  /**
   * Get the percent align Phix related to the lane and the read.
   * @param lane the lane number
   * @param read the read number
   * @return percent align Phix related to the lane and the read
   */
  public double getReadPrcAlign(final int lane, final int read) {
    return this.getDouble("read" + read + ".lane" + lane + ".prc.align");
  }

  /**
   * Get the raw cluster count related to a lane and a read.
   * @param lane the lane number
   * @param read the read number
   * @return the raw cluster count related to a lane and a read
   */
  public long getReadRawClusterCount(final int lane, final int read) {
    return this.getLong("read" + read + ".lane" + lane + ".clusters.raw");
  }

  /**
   * Get the passing filter cluster count related to a lane and a read.
   * @param lane the lane number
   * @param read the read number
   * @return the passing filter cluster count related to a lane and a read
   */
  public long getReadPFClusterCount(final int lane, final int read) {
    return this.getLong("read" + read + ".lane" + lane + ".clusters.pf");
  }

  /**
   * Gets the run tiles per lane.
   * @return the run tiles per lane
   */
  public int getRunTilesPerLane() {

    return this.getInt("run.info.tiles.per.lane.count");
  }

  /**
   * Get the run mode.
   * @return run mode
   */
  public String getRunMode() {
    return this.get("run.info.run.mode");
  }

  /**
   * Gets the sequencer type.
   * @return the sequencer type
   */
  public String getSequencerType() {
    return this.get("run.info.sequencer.type");
  }

  /**
   * Get the cycles count in the read.
   * @param read the read number
   * @return cycles count in the read
   */
  public int getReadCyclesCount(final int read) {
    return this.getInt("run.info.read" + read + ".cycles");
  }

  /**
   * Gets the flowcell id.
   * @return the flowcell id
   */
  public String getFlowcellId() {

    return this.get("run.info.flow.cell.id");
  }

  //
  // Getters
  //

  /**
   * Get a key.
   * @param key key name
   * @return the value of the data for the key
   */
  public String get(final String key) {

    if (key == null) {
      return null;
    }

    return this.map.get(key.toLowerCase().trim());
  }

  /**
   * Get a boolean key.
   * @param key key name
   * @return the value of the data for the key
   */
  public boolean getBoolean(final String key) {

    return Boolean.parseBoolean(get(key));
  }

  /**
   * Get an integer key.
   * @param key key name
   * @return the value of the data for the key
   */
  public int getInt(final String key) {

    final String value = get(key);

    if (value == null) {
      throw new AozanRuntimeException(
          "DataRun getInt throw NullPointerException on this key " + key);
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new AozanRuntimeException(
          "DataRun getInt throw NumberFormatException on this key "
              + key + " (value is " + value + ")");
    }
  }

  /**
   * Get a long key.
   * @param key key name
   * @return the value of the data for the key
   */
  public long getLong(final String key) {

    final String value = get(key);

    if (value == null) {
      throw new AozanRuntimeException(
          "DataRun getInt throw NullPointerException on this key " + key);
    }

    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new AozanRuntimeException(
          "DataRun getLong throw NumberFormatException on this key "
              + key + " (value is " + value + ")");
    }
  }

  /**
   * Get a float key.
   * @param key key name
   * @return the value of the data for the key
   */
  public float getFloat(final String key) {

    final String value = get(key);

    if (value == null) {
      throw new AozanRuntimeException(
          "DataRun getInt throw NullPointerException on this key " + key);
    }

    try {
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      throw new AozanRuntimeException(
          "DataRun getFloat throw NumberFormatException on this key "
              + key + " (value is " + value + ")");
    }
  }

  /**
   * Get a double key.
   * @param key key name
   * @return the value of the data for the key
   */
  public double getDouble(final String key) {

    final String value = get(key);

    if (value == null) {
      throw new AozanRuntimeException(
          "DataRun getInt throw NullPointerException on this key " + key);
    }

    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new AozanRuntimeException(
          "DataRun getDouble throw NumberFormatException on this key "
              + key + " (value is " + value + ")");
    }
  }

  //
  // Setters
  //

  /**
   * Set a key.
   * @param key key to set
   * @param value value of the key
   */
  public void put(final String key, final String value) {

    if (key == null) {
      return;
    }

    this.map.put(key.toLowerCase().trim(), value == null ? "" : value.trim());
  }

  /**
   * Set a key with a boolean value.
   * @param key key to set
   * @param boolValue value of the key
   */
  public void put(final String key, final boolean boolValue) {

    put(key, Boolean.toString(boolValue));
  }

  /**
   * Set a key with an integer value.
   * @param key key to set
   * @param intValue value of the key
   */
  public void put(final String key, final int intValue) {

    put(key, Integer.toString(intValue));
  }

  /**
   * Set a key with a long value.
   * @param key key to set
   * @param longValue value of the key
   */
  public void put(final String key, final long longValue) {

    put(key, Long.toString(longValue));
  }

  /**
   * Set a key with a float value.
   * @param key key to set
   * @param floatValue value of the key
   */
  public void put(final String key, final float floatValue) {

    put(key, Float.toString(floatValue));
  }

  /**
   * Set a key with a double value.
   * @param key key to set
   * @param doubleValue value of the key
   */
  public void put(final String key, final double doubleValue) {

    put(key, Double.toString(doubleValue));
  }

  /**
   * Set a key with an array of strings as value.
   * @param key key to set
   * @param strings list of the key
   */
  public void put(final String key, final String... strings) {

    if (strings == null) {
      put(key, (String) null);
    } else {
      put(key, Joiner.on(',').join(strings));
    }
  }

  /**
   * Set a key with a collection of strings as value.
   * @param key key to set
   * @param strings set of the key
   */
  public void put(final String key, final Collection<String> strings) {

    if (strings == null) {
      put(key, (String) null);
    } else {
      put(key, Joiner.on(',').join(strings));
    }
  }

  /**
   * Set the key and values of a RunData object in the current RunData.
   * @param data Data to add
   */
  public void put(final RunData data) {

    if (data == null) {
      return;
    }

    for (final Map.Entry<String, String> e : data.map.entrySet()) {
      this.map.put(e.getKey(), e.getValue());
    }
  }

  //
  // Other methods
  //

  /**
   * Get the number of entries in RunData.
   * @return the number of entries
   */
  public int size() {

    return this.map.size();
  }

  @Override
  public String toString() {

    final StringBuilder sb = new StringBuilder();
    for (final Map.Entry<String, String> e : this.map.entrySet()) {
      sb.append(e.getKey());
      sb.append('=');
      sb.append(e.getValue());
      sb.append('\n');
    }

    return sb.toString();
  }

  /**
   * Create the data file.
   * @param fileName path destination
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void createRunDataFile(final String fileName) throws IOException {
    createRunDataFile(new File(fileName));
  }

  /**
   * Create the data file.
   * @param fileName file destination
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void createRunDataFile(final File fileName) throws IOException {

    if (fileName == null) {
      throw new NullPointerException();
    }

    if (fileName.isDirectory()) {
      throw new IOException();
    }

    BufferedWriter bw;

    bw = Files.newWriter(fileName, Globals.DEFAULT_FILE_ENCODING);
    bw.write(this.toString());
    bw.close();

  }

  /**
   * Add the data file in the rundata.
   * @param fileName file source
   * @throws IOException if an error occurs while reading the data file
   */
  public void addDataFileInRundata(final String fileName) throws IOException {
    addDataFileInRundata(new File(fileName));
  }

  /**
   * Add the data file in the rundata.
   * @param file file source
   * @throws IOException if an error occurs while reading the data file
   */
  public void addDataFileInRundata(final File file) throws IOException {

    if (file == null) {
      throw new NullPointerException("The file parameter is null");
    }

    checkExistingFile(file, " rundata file ");

    final BufferedReader br =
        Files.newReader(file, Globals.DEFAULT_FILE_ENCODING);

    String line;

    while ((line = br.readLine()) != null) {

      final int pos = line.indexOf('=');
      if (pos == -1) {
        continue;
      }

      final String key = line.substring(0, pos);
      final String value = line.substring(pos + 1);

      put(key, value);
    }
    br.close();
  }

  /**
   * Print the content of the object on standard output.
   */
  public void print() {

    for (final Map.Entry<String, String> e : this.map.entrySet()) {
      System.out.println(e.getKey() + "=" + e.getValue());
    }
  }

  /**
   * Gets the map.
   * @return the map
   */
  public Map<String, String> getMap() {

    return Collections.unmodifiableMap(map);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public RunData() {
  }

  /**
   * Public constructor.
   * @param file file to read
   * @throws IOException if an error occurs while reading the data file
   */
  public RunData(final File file) throws IOException {

    if (file == null) {
      throw new NullPointerException("The file parameter is null");
    }

    final BufferedReader br =
        Files.newReader(file, Globals.DEFAULT_FILE_ENCODING);

    String line;

    while ((line = br.readLine()) != null) {

      final int pos = line.indexOf('=');
      if (pos == -1) {
        continue;
      }

      final String key = line.substring(0, pos);
      final String value = line.substring(pos + 1);

      put(key, value);
    }
    br.close();
  }

}
