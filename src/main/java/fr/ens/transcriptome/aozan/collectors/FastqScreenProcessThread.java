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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * The private class define a class for a thread that execute fastqScreen for a
 * sample. It receive results in rundata and create a report file.
 * @since 1.0
 * @author Sandrine Perrin
 */
class FastqScreenProcessThread extends AbstractFastqProcessThread {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private final File reportDir;
  private final FastqScreen fastqscreen;
  private final List<String> genomes;
  private final String genomeSample;
  private final boolean isPairedEndMode;
  private final boolean isRunPE;
  private FastqSample fastqSampleR2;
  private final RunData data;

  private FastqScreenResult resultsFastqscreen = null;
  private File fastqscreenXSLFile = null;

  @Override
  protected void notifyStartLogger() {
    LOGGER.fine(
        "FASTQSCREEN : start for " + getFastqSample().getKeyFastqSample());
  }

  @Override
  protected void process() throws AozanException {

    processResults();
  }

  @Override
  protected void notifyEndLogger(final String duration) {

    LOGGER.fine("FASTQSCREEN : end for "
        + getFastqSample().getKeyFastqSample() + " in mode "
        + (this.isPairedEndMode ? "paired" : "single")
        + (isSuccess()
            ? " on genome(s) " + this.genomes + " in " + duration
            : " with fail."));

  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

    final String report = this.reportDir.getAbsolutePath()
        + "/" + getFastqSample().getPrefixReport() + "-fastqscreen";

    writeCSV(report);
    // Report with a link in qc html page
    writeHtml(report);

    LOGGER.fine("FASTQSCREEN : save "
        + getFastqSample().getPrefixReport() + " report fastqscreen");

  }

  /**
   * Create a report fastqScreen for a sample in csv format.
   * @param fileName name of the report file in csv format
   * @throws IOException if an error occurs during writing file
   */
  private void writeCSV(final String fileName)
      throws AozanException, IOException {

    final File file = new File(fileName + ".csv");

    final BufferedWriter br = Files.newWriter(file, StandardCharsets.UTF_8);
    br.append(this.resultsFastqscreen.reportToCSV(getFastqSample(),
        this.genomeSample));

    br.close();

    // Run paired-end : copy file for read R2
    if (this.isRunPE) {
      final File fileR2 = new File(this.reportDir.getAbsolutePath()
          + "/" + getFastqSample().getPrefixReport(2) + "-fastqscreen.csv");

      if (fileR2.exists()) {
        if (!fileR2.delete()) {
          LOGGER.warning(
              "Fastqscreen : fail delete report " + fileR2.getAbsolutePath());
        }
      }

      FileUtils.copyFile(file, fileR2);
    }

  }

  /**
   * Create a report fastqScreen for a sample in html format.
   * @param fileName name of the report file in html format
   * @throws IOException if an error occurs during writing file
   */
  private void writeHtml(final String fileName)
      throws AozanException, IOException {

    final File outputReportR1 = new File(fileName + ".html");

    // BufferedWriter br = Files.newWriter(file, Charsets.UTF_8);
    this.resultsFastqscreen.reportToHtml(getFastqSample(), this.data,
        this.genomeSample, outputReportR1, this.fastqscreenXSLFile);

    // br.close();

    // Run paired-end : copy file for read R2
    if (this.isRunPE) {
      final File outputReportR2 = new File(this.reportDir.getAbsolutePath()
          + "/" + getFastqSample().getPrefixReport(2) + "-fastqscreen.html");

      if (outputReportR2.exists()) {
        if (!outputReportR2.delete()) {
          LOGGER.warning("Fastqscreen : fail delete report "
              + outputReportR2.getAbsolutePath());
        }
      }

      FileUtils.copyFile(outputReportR1, outputReportR2);

    }
  }

  @Override
  protected void processResults() throws AozanException {

    final File read1 = getFastqSample().getPartialFile();

    if (!read1.exists()) {
      return;
    }

    File read2 = null;
    // mode paired
    if (this.isPairedEndMode) {
      read2 = this.fastqSampleR2.getPartialFile();

      if (!read2.exists()) {
        return;
      }
    }

    // Add read2 in command line
    this.resultsFastqscreen =
        this.fastqscreen.execute(read1, read2, getFastqSample(), this.genomes,
            this.genomeSample, this.isPairedEndMode);

    if (this.resultsFastqscreen == null) {
      throw new AozanException("Fastqscreen returns no result for sample "
          + String.format("/Project_%s/Sample_%s",
              getFastqSample().getProjectName(),
              getFastqSample().getSampleName()));
    }

    // Create rundata for the sample
    final String prefixR1 = "fastqscreen" + getFastqSample().getPrefixRundata();
    getResults().put(this.resultsFastqscreen.createRundata(prefixR1));

    // run paired : same values for fastqSample R2
    if (this.isRunPE) {

      final String prefixR2 =
          getFastqSample().getPrefixRundata().replace(".read1.", ".read2.");

      getResults()
          .put(this.resultsFastqscreen.createRundata("fastqscreen" + prefixR2));
    }

    try {
      createReportFile();
    } catch (final IOException e) {
      throw new AozanException(e);
    }

  }// end method collect

  //
  // Constructor
  //

  /**
   * Public constructor for a thread object collector for FastqScreen in
   * pair-end mode.
   * @param fastqSampleR1 fastqSample corresponding to the read 1
   * @param fastqSampleR2 fastqSample corresponding to the read 2
   * @param fastqscreen instance of fastqscreen
   * @param data object rundata on the run
   * @param genomes list of references genomes for FastqScreen
   * @param genomeSample genome reference corresponding to sample
   * @param reportDir path for the directory who save the FastqScreen report
   * @param isPairedEndMode true if a pair-end run and option paired mode equals
   *          true else false
   * @param isRunPE true if the run is PE else false
   * @param fastqscreenXSLFile xsl file needed to create report html
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */
  public FastqScreenProcessThread(final FastqSample fastqSampleR1,
      final FastqSample fastqSampleR2, final FastqScreen fastqscreen,
      final RunData data, final Set<String> genomesToMapping,
      final String genomeSample, final File reportDir,
      final boolean isPairedEndMode, final boolean isRunPE,
      final File fastqscreenXSLFile) throws AozanException {

    this(fastqSampleR1, fastqscreen, data, genomesToMapping, genomeSample,
        reportDir, isPairedEndMode, isRunPE, fastqscreenXSLFile);

    if (isPairedEndMode) {
      checkNotNull(fastqSampleR2, "fastqSampleR2 argument cannot be null");
    }

    this.fastqSampleR2 = fastqSampleR2;
  }

  /**
   * Public constructor for a thread object collector for FastqScreen in
   * single-end mode.
   * @param fastqSample fastqSample corresponding to the read 1
   * @param fastqscreen instance of fastqscreen
   * @param data object rundata on the run
   * @param genomesToMapping list of references genomes for FastqScreen
   * @param genomeSample genome reference corresponding to sample
   * @param reportDir path for the directory who save the FastqScreen report
   * @param isPairedEndMode true if a paired-end run and option paired mode
   *          equals true else false
   * @param isRunPE true if the run is PE else false
   * @param fastqscreenXSLFile xsl file needed to create report html
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */
  public FastqScreenProcessThread(final FastqSample fastqSample,
      final FastqScreen fastqscreen, final RunData data,
      final Set<String> genomesToMapping, final String genomeSample,
      final File reportDir, final boolean isPairedEndMode,
      final boolean isRunPE, final File fastqscreenXSLFile)
          throws AozanException {

    super(fastqSample);

    checkNotNull(fastqscreen, "fastqscreen argument cannot be null");
    checkNotNull(data, "data argument cannot be null");
    checkNotNull(genomesToMapping, "genomesToMapping argument cannot be null");
    checkNotNull(reportDir, "reportDir argument cannot be null");

    this.fastqSampleR2 = null;
    this.fastqscreen = fastqscreen;
    this.genomeSample = genomeSample;
    this.reportDir = reportDir;
    this.isPairedEndMode = isPairedEndMode;
    this.isRunPE = isRunPE;
    this.data = data;

    if (fastqscreenXSLFile == null || !fastqscreenXSLFile.exists()) {
      this.fastqscreenXSLFile = null;
    } else {
      this.fastqscreenXSLFile = fastqscreenXSLFile;
    }

    // Copy list genomes names for fastqscreen
    this.genomes = new ArrayList<>(genomesToMapping);

  }

}
