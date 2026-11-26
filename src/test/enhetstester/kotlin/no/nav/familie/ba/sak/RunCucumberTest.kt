package no.nav.familie.ba.sak

import io.cucumber.core.options.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite(failIfNoTests = false)
@IncludeEngines("cucumber")
@SelectClasspathResource("/cucumber")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "no.nav.familie.ba.sak.cucumber")
// Glue-hintet i Cucumber 8 kaster IncompatibleClassChangeError for Kotlin-klasser fra inline-funksjoner (cucumber-jvm#3255)
@ConfigurationParameter(key = Constants.GLUE_HINT_ENABLED_PROPERTY_NAME, value = "false")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "summary")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @Disabled")
class RunCucumberTest
