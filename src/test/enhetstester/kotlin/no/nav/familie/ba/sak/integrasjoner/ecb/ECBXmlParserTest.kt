package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.integrasjoner.ecb.domene.exchangeRatesForCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ECBXmlParserTest {
    val ecbXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <message:GenericData xmlns:message="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message" xmlns:common="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/common" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:generic="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/data/generic" xsi:schemaLocation="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message https://sdw-wsrest.ecb.europa.eu:443/vocabulary/sdmx/2_1/SDMXMessage.xsd http://www.sdmx.org/resources/sdmxml/schemas/v2_1/common https://sdw-wsrest.ecb.europa.eu:443/vocabulary/sdmx/2_1/SDMXCommon.xsd http://www.sdmx.org/resources/sdmxml/schemas/v2_1/data/generic https://sdw-wsrest.ecb.europa.eu:443/vocabulary/sdmx/2_1/SDMXDataGeneric.xsd">
            <message:Header>
                <message:ID>b155910b-633b-4e04-8556-5f279ff01dc7</message:ID>
                <message:Test>false</message:Test>
                <message:Prepared>2022-08-15T13:33:02.354+02:00</message:Prepared>
                <message:Sender id="ECB"/>
                <message:Structure structureID="ECB_EXR1" dimensionAtObservation="TIME_PERIOD">
                    <common:Structure>
                        <URN>urn:sdmx:org.sdmx.infomodel.datastructure.DataStructure=ECB:ECB_EXR1(1.0)</URN>
                    </common:Structure>
                </message:Structure>
            </message:Header>
            <message:DataSet action="Replace" validFromDate="2022-08-15T13:33:02.354+02:00" structureRef="ECB_EXR1">
                <generic:Series>
                    <generic:SeriesKey>
                        <generic:Value id="FREQ" value="D"/>
                        <generic:Value id="CURRENCY" value="NOK"/>
                        <generic:Value id="CURRENCY_DENOM" value="EUR"/>
                        <generic:Value id="EXR_TYPE" value="SP00"/>
                        <generic:Value id="EXR_SUFFIX" value="A"/>
                    </generic:SeriesKey>
                    <generic:Attributes>
                        <generic:Value id="COLLECTION" value="A"/>
                        <generic:Value id="UNIT" value="NOK"/>
                        <generic:Value id="DECIMALS" value="4"/>
                        <generic:Value id="TITLE" value="Norwegian krone/Euro"/>
                        <generic:Value id="TITLE_COMPL" value="ECB reference exchange rate, Norwegian krone/Euro, 2:15 pm (C.E.T.)"/>
                        <generic:Value id="UNIT_MULT" value="0"/>
                        <generic:Value id="TIME_FORMAT" value="P1D"/>
                        <generic:Value id="SOURCE_AGENCY" value="4F0"/>
                    </generic:Attributes>
                    <generic:Obs>
                        <generic:ObsDimension value="2022-06-28"/>
                        <generic:ObsValue value="10.337"/>
                        <generic:Attributes>
                            <generic:Value id="OBS_STATUS" value="A"/>
                            <generic:Value id="OBS_CONF" value="F"/>
                        </generic:Attributes>
                    </generic:Obs>
                    <generic:Obs>
                        <generic:ObsDimension value="2022-06-29"/>
                        <generic:ObsValue value="10.3065"/>
                        <generic:Attributes>
                            <generic:Value id="OBS_STATUS" value="A"/>
                            <generic:Value id="OBS_CONF" value="F"/>
                        </generic:Attributes>
                    </generic:Obs>
                </generic:Series>
                <generic:Series>
                    <generic:SeriesKey>
                        <generic:Value id="FREQ" value="D"/>
                        <generic:Value id="CURRENCY" value="SEK"/>
                        <generic:Value id="CURRENCY_DENOM" value="EUR"/>
                        <generic:Value id="EXR_TYPE" value="SP00"/>
                        <generic:Value id="EXR_SUFFIX" value="A"/>
                    </generic:SeriesKey>
                    <generic:Attributes>
                        <generic:Value id="TITLE" value="Swedish krona/Euro"/>
                        <generic:Value id="COLLECTION" value="A"/>
                        <generic:Value id="UNIT" value="SEK"/>
                        <generic:Value id="DECIMALS" value="4"/>
                        <generic:Value id="TITLE_COMPL" value="ECB reference exchange rate, Swedish krona/Euro, 2:15 pm (C.E.T.)"/>
                        <generic:Value id="UNIT_MULT" value="0"/>
                        <generic:Value id="TIME_FORMAT" value="P1D"/>
                        <generic:Value id="SOURCE_AGENCY" value="4F0"/>
                    </generic:Attributes>
                    <generic:Obs>
                        <generic:ObsDimension value="2022-06-28"/>
                        <generic:ObsValue value="10.6543"/>
                        <generic:Attributes>
                            <generic:Value id="OBS_STATUS" value="A"/>
                            <generic:Value id="OBS_CONF" value="F"/>
                        </generic:Attributes>
                    </generic:Obs>
                    <generic:Obs>
                        <generic:ObsDimension value="2022-06-29"/>
                        <generic:ObsValue value="10.6848"/>
                        <generic:Attributes>
                            <generic:Value id="OBS_STATUS" value="A"/>
                            <generic:Value id="OBS_CONF" value="F"/>
                        </generic:Attributes>
                    </generic:Obs>
                </generic:Series>
            </message:DataSet>
        </message:GenericData>
    """.trimIndent()

    @Test
    fun `Test at ECBXmlParser parser xml string som forventet`() {
        val ecbExchangeRatesData = ECBXmlParser.parse(ecbXml)
        assertEquals(ecbExchangeRatesData.ecbExchangeRatesDataSet.ecbExchangeRatesForCurrencies.size, 2)
        val nokExchangeRates = ecbExchangeRatesData.exchangeRatesForCurrency("NOK")
        val sekExchangeRates = ecbExchangeRatesData.exchangeRatesForCurrency("SEK")
        assertEquals(2, nokExchangeRates.size)
        assertEquals(2, sekExchangeRates.size)

        assertEquals(BigDecimal.valueOf(10.337), nokExchangeRates.filter { it.date.value == "2022-06-28" }[0].ecbExchangeRateValue.value)
        assertEquals(BigDecimal.valueOf(10.3065), nokExchangeRates.filter { it.date.value == "2022-06-29" }[0].ecbExchangeRateValue.value)

        assertEquals(BigDecimal.valueOf(10.6543), sekExchangeRates.filter { it.date.value == "2022-06-28" }[0].ecbExchangeRateValue.value)
        assertEquals(BigDecimal.valueOf(10.6848), sekExchangeRates.filter { it.date.value == "2022-06-29" }[0].ecbExchangeRateValue.value)
    }
}
