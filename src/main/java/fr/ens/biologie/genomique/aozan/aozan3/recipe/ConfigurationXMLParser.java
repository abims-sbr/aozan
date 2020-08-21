package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedChildTags;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.evaluateExpressions;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;

/**
 * This class define a XML parser for configuration.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class ConfigurationXMLParser extends AbstractXMLParser<Configuration> {

  private final String sectionName;
  private final Configuration parentConf;

  static final String PARAMETER_TAG_NAME = "parameter";
  static final String PARAMETERNAME_TAG_NAME = "name";
  static final String PARAMETERVALUE_TAG_NAME = "value";

  /**
   * Parse a configuration tag.
   * @param tagName name of the tag to parse
   * @param rootElement root element of the tag
   * @return a new Configuration object with the content of the parent
   *         configuration and the parameters defined inside the XML tag
   * @throws Aozan3Exception if an error occurs while parsing the tag
   */
  protected Configuration parse(NodeList nList, String source)
      throws Aozan3Exception {

    Configuration result = new Configuration(this.parentConf);

    final Set<String> parameterNames = new HashSet<>();

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed tags for the "parameter" tag
        checkAllowedChildTags(element, PARAMETER_TAG_NAME);

        // TODO configuration can be defined in a another key/value file
        // Load steps in the include file
        Path includePath = getIncludePath(element);
        if (includePath != null) {
          ConfigurationXMLParser parser = new ConfigurationXMLParser(getRootTagName(),
              this.sectionName, this.parentConf, getLogger());

          result.set(parser.parse(includePath));
        }

        final NodeList nParameterList =
            element.getElementsByTagName(PARAMETER_TAG_NAME);

        for (int j = 0; j < nParameterList.getLength(); j++) {

          final Node nParameterNode = nParameterList.item(j);

          if (nParameterNode.getNodeType() == Node.ELEMENT_NODE) {

            Element eParameterElement = (Element) nParameterNode;

            checkAllowedChildTags(eParameterElement, PARAMETERNAME_TAG_NAME,
                PARAMETERVALUE_TAG_NAME);

            final String paramName =
                getTagValue(PARAMETERNAME_TAG_NAME, eParameterElement);
            final String paramValue =
                getTagValue(PARAMETERVALUE_TAG_NAME, eParameterElement);

            if (paramName == null) {
              throw new Aozan3Exception(
                  "<name> Tag not found in parameter section of "
                      + this.sectionName + " in recipe file.");
            }
            if (paramValue == null) {
              throw new Aozan3Exception(
                  "<value> Tag not found in parameter section of "
                      + this.sectionName + " in recipe file.");
            }

            if (parameterNames.contains(paramName)) {
              throw new Aozan3Exception("The parameter \""
                  + paramName + "\" has been already defined for "
                  + this.sectionName + " in workflow file.");
            }
            parameterNames.add(paramName);

            result.set(paramName, evaluateExpressions(paramValue, result));
          }
        }

      }
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param sectionName name of the section in the recipe file
   * @param parentConf parent configuration
   * @param logger logger to use
   */
  public ConfigurationXMLParser(String rootTagName, String sectionName,
      Configuration parentConfiguration, AozanLogger logger) {

    super(rootTagName, logger);

    requireNonNull(sectionName);
    requireNonNull(parentConfiguration);

    this.sectionName = sectionName;
    this.parentConf = parentConfiguration;
  }

}
