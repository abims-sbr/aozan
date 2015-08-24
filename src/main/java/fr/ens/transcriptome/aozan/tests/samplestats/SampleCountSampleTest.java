package fr.ens.transcriptome.aozan.tests.samplestats;

import static fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics.COLLECTOR_PREFIX;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics;

public class SampleCountSampleTest extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatistics.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final String sampleName) {

    return COLLECTOR_PREFIX + sampleName + ".samples.count";
  }

  @Override
  protected Class<?> getValueType() {

    return Integer.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public SampleCountSampleTest() {
    super("samplecountsample", "", "Samples count");
  }

}