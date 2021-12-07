package no.nav.familie.ba.sak.kjerne.dokument

class BrevbegrunnelseTest {

    /*@Test
    fun test(testReporter: TestReporter) {
        val testmappe = File("./src/test/resources/brevbegrunnelseCaser")

        val antallFeil = testmappe.list()?.fold(0) { acc, it ->
            val fil = File("./src/test/resources/brevbegrunnelseCaser/$it")
            val behandlingsresultatPersonTestConfig =
                objectMapper.readValue<BrevBegrunnelseTestConfig>(fil.readText())

            val restPersoner =
                behandlingsresultatPersonTestConfig.begrunnelsepersoner.map { it.tilRestPersonTilTester() }

            val utvidetVedtaksperiodeMedBegrunnelser =
                UtvidetVedtaksperiodeMedBegrunnelser(
                    id = 1L,
                    fom = behandlingsresultatPersonTestConfig.fom,
                    tom = behandlingsresultatPersonTestConfig.tom,
                    type = behandlingsresultatPersonTestConfig.vedtaksperiodetype,
                    begrunnelser = behandlingsresultatPersonTestConfig.standardbegrunnelser,
                    fritekster = behandlingsresultatPersonTestConfig.fritekster,
                    gyldigeBegrunnelser = emptyList(),
                    utbetalingsperiodeDetaljer = behandlingsresultatPersonTestConfig.utbetalingsperiodeDetaljer.map {
                        it.tilUtbetalingsperiodeDetalj(
                            restPersoner.find { restPerson -> restPerson.personIdent == it.personIdent }!!
                        )
                    }
                )

            val brevperiode: BrevPeriode? =
                utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                    personerIPersongrunnlag = behandlingsresultatPersonTestConfig.begrunnelsepersoner,
                    målform = behandlingsresultatPersonTestConfig.målform,
                    uregistrerteBarn = behandlingsresultatPersonTestConfig.uregistrerteBarn,
                    utvidetScenario = UtvidetScenario.IKKE_UTVIDET_YTELSE
                )

            val feil = erLike(
                forventetOutput = behandlingsresultatPersonTestConfig.forventetOutput,
                output = brevperiode
            )

            if (feil.isNotEmpty()) {
                testReporter.publishEntry(
                    it,
                    "${behandlingsresultatPersonTestConfig.beskrivelse}\n" +
                        feil.joinToString("\n")
                )
                acc + 1
            } else {
                acc
            }
        }

        assert(antallFeil == 0)
    }

    private fun erLike(
        forventetOutput: BrevPeriodeTestConfig?,
        output: BrevPeriode?
    ): List<String> {

        val feil = mutableListOf<String>()

        fun validerFelt(forventet: String?, faktisk: String?, variabelNavn: String) {
            if (forventet != faktisk) {
                feil.add(
                    "Forventet $variabelNavn var: '$forventet', men fikk '$faktisk'"
                )
            }
        }

        if (forventetOutput == null || output == null) {
            if (forventetOutput != null && output == null)
                feil.add("Output er null, men forventet output er $forventetOutput.")
            if (forventetOutput == null && output != null)
                feil.add("Forventet output er null, men output er $output.")
        } else {
            validerFelt(forventetOutput.fom, output.fom?.single(), "fom")
            validerFelt(forventetOutput.tom, output.tom?.single(), "tom")
            validerFelt(forventetOutput.type, output.type?.single(), "type")
            validerFelt(forventetOutput.barnasFodselsdager, output.barnasFodselsdager?.single(), "barnasFodselsdager")
            validerFelt(forventetOutput.antallBarn, output.antallBarn?.single(), "antallBarn")
            validerFelt(
                if (forventetOutput.belop != null)
                    formaterBeløp(forventetOutput.belop)
                else null,
                output.belop?.single(), "belop"
            )

            val forventedeBegrunnelser = forventetOutput.begrunnelser.map {
                when (it) {
                    is BegrunnelseDataTestConfig -> it.tilBegrunnelseData()
                    is FritekstBegrunnelseTestConfig -> it.fritekst
                    else -> throw IllegalArgumentException("Ugyldig testconfig")
                }
            }

            forventedeBegrunnelser.filter { !output.begrunnelser.contains(it) }.forEach {
                feil.add(
                    "Fant ingen begrunnelser i output-begrunnelsene som matcher forventet begrunnelse $it"
                )
            }
            output.begrunnelser.filter { !forventedeBegrunnelser.contains(it) }.forEach {
                feil.add(
                    "Fant ingen begrunnelser i de forventede begrunnelser som matcher begrunnelse $it"
                )
            }
        }

        return feil
    }*/
}
