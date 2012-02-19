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

package fr.ens.transcriptome.aozan.tests;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;
import fr.ens.transcriptome.aozan.util.DoubleInterval;
import fr.ens.transcriptome.aozan.util.Interval;

/**
 * This class define a lane test on phasing / prephasing.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PhasingPrePhasingLaneTest extends AbstractLaneTest {

  private Interval phasingInterval;
  private Interval prephasingInterval;

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    final String keyPrefix = "read" + read + ".lane" + lane;
    final double phasing = data.getDouble(keyPrefix + ".phasing");
    final double prephasing = data.getDouble(keyPrefix + ".prephasing");

    final String message =
        String.format(
            AozanTest.DOUBLE_FORMAT + " / " + AozanTest.DOUBLE_FORMAT, phasing,
            prephasing);

    final boolean result =
        phasingInterval.isInInterval(phasing)
            || prephasingInterval.isInInterval(prephasing);

    return new TestResult(result ? 9 : 0, message);
  }

  @Override
  public String[] getCollectorsNamesRequiered() {

    return new String[] {ReadCollector.COLLECTOR_NAME};
  }

  //
  // Constructor
  //

  public PhasingPrePhasingLaneTest() {

    super("phasingprephasing", "", "Phasing / Prephasing");
    this.phasingInterval = new DoubleInterval(0, 0.4);
    this.prephasingInterval = new DoubleInterval(0, 0.5);
  }

}
