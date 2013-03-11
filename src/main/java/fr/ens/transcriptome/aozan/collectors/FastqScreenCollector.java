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

package fr.ens.transcriptome.aozan.collectors;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.FastqScreenDemo;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.RunDataGenerator;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqStorage;

/**
 * This class manages the execution of Fastq Screen for a full run according to
 * the properties defined in the configuration file Aozan, which define the list
 * of references genomes.
 * @author Sandrine Perrin
 */
public class FastqScreenCollector implements Collector {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String COLLECTOR_NAME = "fastqscreen";

  public static final String KEY_GENOMES = "qc.conf.fastqscreen.genomes";

  public static final String KEY_READ_COUNT = "run.info.read.count";
  public static final String KEY_READ_X_INDEXED = "run.info.read";
  // private static final String COMPRESSION_EXTENSION = "fastq.bz2";

  private FastqScreen fastqscreen;
  private FastqStorage fastqStorage;
  private List<String> listGenomes = new ArrayList<String>();

  private String casavaOutputPath;
  private String qcOutputDir;
  private boolean paired = false;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector
   * @return list of names collector
   */
  @Override
  public String[] getCollectorsNamesRequiered() {
    return new String[] {RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME};
  }

  /**
   * Configure fastqScreen with properties from file aozan.conf
   * @param properties
   */
  @Override
  public void configure(Properties propertiesRDG) {

    // TODO fix call property for constructor FastqScreen
    Properties properties = FastqScreenDemo.getPropertiesDemo();

    this.fastqStorage =
        FastqStorage.getInstance(properties
            .getProperty(RunDataGenerator.TMP_DIR));

    this.fastqStorage.setThreads(Integer.parseInt(properties.getProperty(
        "qc.conf.fastqc.threads").trim()));

    this.casavaOutputPath =
        properties.getProperty(RunDataGenerator.CASAVA_OUTPUT_DIR);

    this.qcOutputDir = properties.getProperty(RunDataGenerator.QC_OUTPUT_DIR);

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();

    for (String genome : s.split(properties.getProperty(KEY_GENOMES))) {
      this.listGenomes.add(genome);
    }

    this.fastqscreen = new FastqScreen(properties, listGenomes);
  }

  /**
   * Collect all data generated by fastqScreen for a run and update data
   * @param data RunData contain data from required collectors
   * @throws AozanException if one result collected with FastqScreen
   */
  @Override
  public void collect(RunData data) throws AozanException {

    // TODO to remove
    data = FastqScreenDemo.getRunData();

    FastqScreenResult resultsFastqscreen = null;

    File read1 = null;
    File read2 = null;

    final int laneCount = data.getInt("run.info.flow.cell.lane.count");

    // mode paired or single-end present in Rundata
    final int readCount = data.getInt(KEY_READ_COUNT);
    final boolean lastReadIndexed =
        data.getBoolean(KEY_READ_X_INDEXED + readCount + ".indexed");

    paired = readCount > 1 && !lastReadIndexed;

    // Uncompress all fastq files of the current run
    fastqStorage.uncompressFastqFiles(data, casavaOutputPath);

    for (int read = 1; read <= readCount - 1; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        for (String sampleName : sampleNames) {

          final long startTime = System.currentTimeMillis();

          // Get the sample index
          final String index =
              data.get("design.lane" + lane + "." + sampleName + ".index");

          // Get project name
          final String projectName =
              data.get("design.lane"
                  + lane + "." + sampleName + ".sample.project");

          System.out.println("lane current "
              + lane + "\tsample current " + sampleName + "\tproject name "
              + projectName);

          LOGGER.fine("Start test in collector with project "
              + projectName + " sample " + sampleName);

          read1 =
              fastqStorage.getFastqFile(this.casavaOutputPath, read, lane,
                  projectName, sampleName, index);

          if (read1 == null || !read1.exists())
            continue;

          if (paired) {
            // mode paired
            // concatenate fastq files of one sample
            read2 =
                fastqStorage.getFastqFile(this.casavaOutputPath, (read + 1),
                    lane, projectName, sampleName, index);

            if (read2 == null || !read2.exists())
              continue;
          }

          // TODO new
          // receive genome name for sample
          String genomeSample =
              data.get("design.lane" + lane + "." + sampleName + ".sample.ref");

          // add read2 in command line
          resultsFastqscreen =
              fastqscreen.execute(read1, read2, this.listGenomes, projectName,
                  sampleName, genomeSample);

          if (resultsFastqscreen == null)
            throw new AozanException("Fastqscreen return no result for sample "
                + String.format("/Project_%s/Sample_%s", projectName,
                    sampleName));

          // update rundata
          processResults(data, resultsFastqscreen, read, lane, projectName,
              sampleName, index);

          fastqStorage.removeTemporaryFastq(read1, read2);

          LOGGER.fine("End test in collector with project "
              + projectName + " sample " + sampleName + " : "
              + toTimeHumanReadable(System.currentTimeMillis() - startTime));

        } // sample
      } // lane
    } // read

    clean();

  }// end method collect

  /**
   * Process results and update rundata.
   * @param data rundata
   * @param result object fastqScreenResult which contains all values from
   *          fastqscreen of one sample
   * @param read read number
   * @param lane lane number
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @param fastqFileName fastq file
   * @throws AozanException if an error occurs while generating FastqScreen
   *           results
   */
  private void processResults(final RunData data,
      final FastqScreenResult result, final int read, final int lane,
      final String projectName, final String sampleName, final String index)
      throws AozanException {

    // Set the prefix for the run data entries
    String prefix =
        "fastqscreen.lane"
            + lane + ".sample." + sampleName + ".read" + read + "."
            + sampleName;

    result.updateRundata(data, prefix);

    createFileReportFastqScreen(result, read, lane, projectName, sampleName,
        index);
  }

  /**
   * Save in file result from fastqscreen for one sample.
   * @param result object fastqScreenResult which contains all values from
   *          fastqscreen of one sample
   * @param read read number
   * @param lane lane number
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @throws AozanException if an error occurs while creating file
   */
  private void createFileReportFastqScreen(final FastqScreenResult result,
      final int read, final int lane, final String projectName,
      final String sampleName, final String index) throws AozanException {

    // create a file to save result fastq screen
    System.out.println(result.statisticalTableToString());

    File[] fastqFiles =
        fastqStorage.createListFastqFiles(casavaOutputPath, read, lane,
            projectName, sampleName, index);

    String firstFile = fastqFiles[0].getName();

    final String filename = firstFile.substring(0, firstFile.lastIndexOf("."));

    final File pathFile =
        new File(this.qcOutputDir
            + String.format("/Project_%s/%s-fastqc/", projectName, filename));

    if (!pathFile.exists())
      if (!pathFile.mkdirs())
        throw new AozanException("Cannot create report directory: "
            + pathFile.getAbsolutePath());

    result.createFileResultFastqScreen(pathFile.getAbsolutePath());

  }

  /**
   * Remove temporary files created in temporary directory which is defined in
   * properties of Aozan
   */
  public void clean() {

    LOGGER.fine("Delete fastq file uncompress");

    fastqStorage.clear();

  }

  //
  // Constructor
  //

  /**
   * Public constructor for FastqScreenCollector
   */
  public FastqScreenCollector() {

  }

}
