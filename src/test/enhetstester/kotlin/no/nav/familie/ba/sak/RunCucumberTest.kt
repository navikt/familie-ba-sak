package no.nav.familie.ba.sak

import io.cucumber.core.options.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite(failIfNoTests = false)
@IncludeEngines("cucumber")
@SelectClasspathResource("no/nav/familie/ba/sak/cucumber")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @Disabled")
class RunCucumberTest
