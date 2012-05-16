/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import uk.ac.bbsrc.babraham.FastQC.Modules.BasicStats;
import uk.ac.bbsrc.babraham.FastQC.Modules.KmerContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.NContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.OverRepresentedSeqs;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerBaseGCContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerBaseQualityScores;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerBaseSequenceContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerSequenceGCContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerSequenceQualityScores;
import uk.ac.bbsrc.babraham.FastQC.Modules.QCModule;
import uk.ac.bbsrc.babraham.FastQC.Modules.SequenceLengthDistribution;
import uk.ac.bbsrc.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.bbsrc.babraham.FastQC.Sequence.Sequence;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFormatException;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.RunDataGenerator;
import fr.ens.transcriptome.aozan.fastqc.BadTiles;

/**
 * This class define a FastQC Collector
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastQCCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "fastqc";

  private static final int CHECKING_DELAY_MS = 5000;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  private String casavaOutputPath;
  private String qcReportOutputPath;
  private int threads = Runtime.getRuntime().availableProcessors();
  private String compressionExtension = ".bz2";
  private boolean ignoreFilteredSequences = false;

  /**
   * This private class define a class for a thread that read fastq file for
   * FastQC modules.
   * @author Laurent Jourdren
   */
  private static final class SeqFileThread implements Runnable {

    private final String projectName;
    private final String sampleName;
    private final int lane;
    private final int read;
    private final SequenceFile seqFile;
    private final String firstFastqFileName;
    private final boolean ignoreFilteredSequences;
    private final List<QCModule> moduleList;

    private AozanException exception;
    private boolean success;

    /**
     * Get the exception generated by the call to processSequences in the run()
     * method.
     * @return a exception object or null if no Exception has been thrown
     */
    public Exception getException() {

      return this.exception;
    }

    /**
     * Test if the call to run method was a success
     * @return true if the call to run method was a success
     */
    public boolean isSuccess() {

      return this.success;
    }

    @Override
    public void run() {

      try {
        processSequences(this.seqFile);
        this.success = true;
      } catch (AozanException e) {
        this.exception = e;
      }
    }

    /**
     * Read FASTQ file and process the data by FastQC modules
     * @param seqFile input file
     * @throws AozanException if an error occurs while processing file
     */
    private void processSequences(final SequenceFile seqFile)
        throws AozanException {

      final boolean ignoreFiltered = this.ignoreFilteredSequences;
      final List<QCModule> modules = this.moduleList;

      try {

        while (seqFile.hasNext()) {

          final Sequence seq = seqFile.next();

          for (final QCModule module : modules) {

            if (ignoreFiltered && module.ignoreFilteredSequences())
              continue;

            module.processSequence(seq);
          }

        }

      } catch (SequenceFormatException e) {
        throw new AozanException(e);
      }
    }

    /**
     * Process results after the end of the thread.
     * @param data RunData object
     * @param qcReportOutputPath the path for the qc report
     * @param compressionExtension the compression extension
     * @throws AozanException if an error occurs while generate FastQC reports
     */
    public void processResults(final RunData data,
        final String qcReportOutputPath, final String compressionExtension)
        throws AozanException {

      // Set the prefix for the run data entries
      final String prefix =
          "fastqc.lane"
              + lane + ".sample." + sampleName + ".read" + read + "."
              + sampleName;

      // Fill the run data object
      for (final QCModule module : this.moduleList) {

        final String keyPrefix = prefix + "." + module.name().replace(' ', '.');

        data.put(keyPrefix + ".error", module.raisesError());
        data.put(keyPrefix + ".warning", module.raisesWarning());
      }

      // Create report
      try {
        createReportFile(qcReportOutputPath, compressionExtension);
      } catch (IOException e) {
        throw new AozanException(e);
      }
    }

    /**
     * Create the report file.
     * @param seqFile processed FastQC sequences
     * @param fastqFilename name of the input file
     * @param projectName project name
     * @throws AozanException if an error occurs while processing data
     * @throws IOException if an error occurs while processing data
     */
    private void createReportFile(final String qcReportOutputPath,
        final String compressionExtension) throws AozanException, IOException {

      final File reportDir =
          new File(qcReportOutputPath + "/Project_" + projectName);

      if (!reportDir.exists())
        if (!reportDir.mkdirs())
          throw new AozanException("Cannot create report directory: "
              + reportDir.getAbsolutePath());

      // Set the name of the prefix of the report file
      final String filename =
          this.firstFastqFileName.substring(0, this.firstFastqFileName.length()
              - ".fastq".length() - compressionExtension.length());

      final File reportFile = new File(reportDir, filename + "-fastqc.zip");

      // Force unzip of the report
      System.setProperty("fastqc.unzip", "true");

      new HTMLReportArchive(seqFile,
          this.moduleList.toArray(new QCModule[] {}), reportFile);

      // Keep only the uncompressed data
      if (reportFile.exists())
        reportFile.delete();
    }

    //
    // Constructor
    //

    /**
     * Thread constructor.
     * @param projectName name of the project
     * @param sampleName name of the sample
     * @param lane lane of the sample
     * @param read read of the sample
     * @param fastqFiles fastq files for the sample
     * @param ignoreFilteredSequences if true filtered sequences will be ignored
     * @throws AozanException if an error occurs while creating sequence file
     *           for FastQC
     */
    public SeqFileThread(final String projectName, final String sampleName,
        final int lane, final int read, final File[] fastqFiles,
        final boolean ignoreFilteredSequences) throws AozanException {

      if (fastqFiles == null || fastqFiles.length == 0)
        throw new AozanException("No fastq file defined");

      try {

        this.projectName = projectName;
        this.sampleName = sampleName;
        this.lane = lane;
        this.read = read;
        this.seqFile = SequenceFactory.getSequenceFile(fastqFiles);
        this.firstFastqFileName = fastqFiles[0].getName();
        this.ignoreFilteredSequences = ignoreFilteredSequences;

        // Define modules list
        final OverRepresentedSeqs os = new OverRepresentedSeqs();
        this.moduleList =
            Lists.newArrayList(new BasicStats(), new PerBaseQualityScores(),
                new PerSequenceQualityScores(), new PerBaseSequenceContent(),
                new PerBaseGCContent(), new PerSequenceGCContent(),
                new NContent(), new SequenceLengthDistribution(),
                os.duplicationLevelModule(), os, new KmerContent(),
                new BadTiles());

      } catch (SequenceFormatException e) {
        throw new AozanException(e);
      } catch (IOException e) {
        throw new AozanException(e);
      }
    }

  }

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public String[] getCollectorsNamesRequiered() {

    return new String[] {RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME};
  }

  @Override
  public void configure(final Properties properties) {

    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

    if (properties == null)
      return;

    this.casavaOutputPath =
        properties.getProperty(RunDataGenerator.CASAVA_OUTPUT_DIR);

    this.qcReportOutputPath =
        properties.getProperty(RunDataGenerator.QC_OUTPUT_DIR);

    if (properties.containsKey("qc.conf.fastqc.threads")) {

      try {
        int confThreads =
            Integer.parseInt(properties.getProperty("qc.conf.fastqc.threads")
                .trim());
        if (confThreads > 0)
          this.threads = confThreads;

      } catch (NumberFormatException e) {
      }
    }
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Create the list for threads
    final List<SeqFileThread> threads = Lists.newArrayList();
    final List<Future<SeqFileThread>> futureThreads = Lists.newArrayList();

    // Create executor service
    final ExecutorService executor = Executors.newFixedThreadPool(this.threads);

    final int readCount = data.getInt("run.info.read.count");
    final int laneCount = data.getInt("run.info.flow.cell.lane.count");
    int readSample = 0;

    // Create the thread and add them to the list
    for (int read = 1; read <= readCount; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      readSample++;

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        for (String sampleName : sampleNames) {

          // Get project name
          final String projectName =
              data.get("design.lane"
                  + lane + "." + sampleName + ".sample.project");

          // Get the sample index
          final String index =
              data.get("design.lane" + lane + "." + sampleName + ".index");

          // Process sample FASTQ(s)
          final SeqFileThread sft =
              processFile(data, projectName, sampleName, index, lane,
                  readSample);
          threads.add(sft);
          futureThreads.add(executor.submit(sft, sft));
        }
      }
    }

    // Wait for threads
    waitThreads(futureThreads, executor);

    for (SeqFileThread sft : threads) {
      sft.processResults(data, qcReportOutputPath, compressionExtension);
    }

  }

  /**
   * Process a FASTQ file.
   * @param data Run data
   * @param projectName name fo the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @param lane lane number
   * @param read read number
   * @throws AozanException if an error occurs while processing a FASTQ file
   */
  public SeqFileThread processFile(final RunData data,
      final String projectName, final String sampleName, final String index,
      final int lane, final int read) throws AozanException {

    // Set the directory to the file
    final File dir =
        new File(this.casavaOutputPath
            + "/Project_" + projectName + "/Sample_" + sampleName);

    // Set the prefix of the file
    final String prefix =
        String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
            ? "NoIndex" : index, lane, read);

    // Set the list of the files for the FASTQ data
    final File[] fastqFiles = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {

        return pathname.getName().startsWith(prefix)
            && pathname.getName().endsWith(compressionExtension);
      }
    });

    // Create the thread object
    return new SeqFileThread(projectName, sampleName, lane, read, fastqFiles,
        this.ignoreFilteredSequences);
  }

  /**
   * Wait the end of the threads.
   * @param threads list with the threads
   * @param executor the executor
   * @throws AozanException if an error occurs while executing a thread
   */
  private void waitThreads(final List<Future<SeqFileThread>> threads,
      final ExecutorService executor) throws AozanException {

    int samplesNotProcessed = 0;

    // Wait until all samples are processed
    do {

      try {
        Thread.sleep(CHECKING_DELAY_MS);
      } catch (InterruptedException e) {
        // LOGGER.warning("InterruptedException: " + e.getMessage());
      }

      samplesNotProcessed = 0;

      for (Future<SeqFileThread> fst : threads) {

        if (fst.isDone()) {

          try {

            final SeqFileThread st = fst.get();

            if (!st.isSuccess()) {

              // Close the thread pool
              executor.shutdownNow();

              // Wait the termination of current running task
              executor
                  .awaitTermination(WAIT_SHUTDOWN_MINUTES, TimeUnit.MINUTES);

              // Return error Step Result
              throw new AozanException(st.getException());
            }

          } catch (InterruptedException e) {
            // LOGGER.warning("InterruptedException: " + e.getMessage());
          } catch (ExecutionException e) {
            // LOGGER.warning("ExecutionException: " + e.getMessage());
          }

        } else {
          samplesNotProcessed++;
        }

      }

    } while (samplesNotProcessed > 0);

    // Close the thread pool
    executor.shutdown();
  }

}
