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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This internal class create a thread for each array of file to uncompress and
 * compile in temporary file.
 * @author Sandrine Perrin
 */
public class UncompressFastqThread extends AbstractFastqProcessThread {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /** Timer **/
  private Stopwatch timer = new Stopwatch();

  private long sizeFile = 0L;

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
    timer.start();

    try {
      processResults();
      this.success = true;

    } catch (AozanException e) {
      this.exception = e;

    } finally {

      synchronized (this) {
        LOGGER.fine("UNCOMPRESS fastq : for "
            + fastqSample.getKeyFastqSample()
            + " "
            + fastqSample.getFastqFiles().size()
            + " fastq file(s) in type compression "
            + fastqSample.getCompressionType()
            + " in "
            + toTimeHumanReadable(timer.elapsedMillis())
            + " : temporary fastq file size "
            + Globals.FORMATTER_MILLIER.format(sizeFile)
            + " (size estimated "
            + Globals.FORMATTER_MILLIER.format(fastqSample
                .getUncompressedSize()) + ")");
      }
      timer.stop();
    }

  }

  /**
   * Uncompresses and compiles files of array.
   * @return file compile all files
   * @throws AozanException if an error occurs while creating file
   */
  @Override
  protected void processResults() throws AozanException {

    File tmpFastqFile =
        new File(fastqStorage.getTemporaryFile(fastqSample) + ".tmp");

    if (!tmpFastqFile.exists()) {
      // Uncompresses and compiles files of array in new temporary files
      try {

        OutputStream out = new FileOutputStream(tmpFastqFile);

        for (File fastqFile : fastqSample.getFastqFiles()) {

          if (!fastqFile.exists()) {
            throw new IOException("Fastq file "
                + fastqFile.getName() + " doesn't exist");
          }

          // Get compression type
          CompressionType zType = fastqSample.getCompressionType();

          // Append compressed fastq file to uncompressed file
          final InputStream in = new FileInputStream(fastqFile);
          FileUtils.append(zType.createInputStream(in), out);
        }

        out.close();

      } catch (IOException io) {
        throw new AozanException(io.getMessage());
      }
    }

    sizeFile = tmpFastqFile.length();

    // Rename file for remove '.tmp' final
    tmpFastqFile.renameTo(new File(fastqStorage.getTemporaryFile(fastqSample)));

  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

  }

  //
  // Constructor
  //

  /**
   * Thread constructor.
   * @param FastqSample
   * @throws AozanException if an error occurs while creating sequence file for
   *           FastQC
   */
  public UncompressFastqThread(final FastqSample fastqSample)
      throws AozanException {

    super(fastqSample);
  }

}
