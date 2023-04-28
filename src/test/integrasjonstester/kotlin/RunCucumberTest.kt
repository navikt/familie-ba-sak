import io.cucumber.core.options.Constants.PLUGIN_PROPERTY_NAME
import org.junit.jupiter.api.Disabled
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite(failIfNoTests = false)
@IncludeEngines("cucumber")
@SelectClasspathResource("no/nav/familie/ba/sak/cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@Disabled // TODO må fjernes når vi merger
class RunCucumberTest
