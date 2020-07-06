package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.toRestFagsak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personinfo.Ident
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class StatsborgerskapServiceTest

val integrasjonClient = mockk<IntegrasjonClient>()

lateinit var statsborgerskapService: StatsborgerskapService

@BeforeEach
fun setUp() {
    statsborgerskapService = StatsborgerskapService(integrasjonClient)
}

@Test
fun `e`() {
    statsborgerskapService.hentStatsborgerskap(Ident("0011"))
}
