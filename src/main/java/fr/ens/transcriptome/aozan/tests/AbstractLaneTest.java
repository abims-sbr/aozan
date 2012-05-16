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

/**
 * This class define a abstract Lane test.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractLaneTest implements LaneTest {

  private final String name;
  private final String description;
  private final String colunmnName;
  private final String unit;

  //
  // Getters
  //

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public String getDescription() {

    return this.description;
  }

  @Override
  public String getColumnName() {

    return this.colunmnName;
  }

  @Override
  public String getUnit() {

    return this.unit;
  }

  //
  // Other methods
  //

  @Override
  public void init() {
  }

  //
  // Constructor
  //

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   */
  protected AbstractLaneTest(final String name, final String description,
      final String columnName) {

    this(name, description, columnName, "");
  }

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   * @param unit unit of the test
   */
  protected AbstractLaneTest(final String name, final String description,
      final String columnName, final String unit) {

    this.name = name;
    this.description = description;
    this.colunmnName = columnName;
    this.unit = unit;
  }

}
