package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.tilBrev
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøker
import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsakService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class DokumentGenereringServiceTest {
    private val brevService = mockk<BrevService>()
    private val brevKlient = mockk<BrevKlient>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val saksbehandlerContext = mockk<SaksbehandlerContext>()
    private val sammensattKontrollsakService = mockk<SammensattKontrollsakService>()
    private val testVerktøyService = mockk<TestVerktøyService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val organisasjonService = mockk<OrganisasjonService>()

    private val dokumentGenereringService =
        DokumentGenereringService(
            persongrunnlagService = mockk(),
            brevService = brevService,
            brevKlient = brevKlient,
            kodeverkService = KodeverkService(integrasjonKlient = integrasjonKlient),
            saksbehandlerContext = saksbehandlerContext,
            sammensattKontrollsakService = sammensattKontrollsakService,
            testVerktøyService = testVerktøyService,
            personopplysningerService = personopplysningerService,
            organisasjonService = organisasjonService,
        )

    @Nested
    inner class GenererManueltBrevTest {
        @Test
        fun `skal kaste feil dersom fagsak type er institusjon men det ikke eksisterer en institusjon lagret på fagsak`() {
            // Arrange
            val søker = randomAktør()
            val fagsak = lagFagsakUtenId(type = FagsakType.INSTITUSJON)
            val request = mockk<ManueltBrevRequest>(relaxed = true)
            val brev = mockk<Brev>(relaxed = true)
            val mottakerIdent = slot<String>()

            every { personopplysningerService.hentPersoninfoEnkel(søker).navn } returns "Institusjon"
            every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Z000000"
            mockkStatic("no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequestKt")

            every {
                request.tilBrev(capture(mottakerIdent), any(), any(), any())
            } returns brev

            every { brevKlient.genererBrev(any(), eq(brev)) } returns byteArrayOf()
            every { personopplysningerService.hentPersoninfoEnkel(any()) } returns PersonInfo(fødselsdato = LocalDate.now(), navn = "navn")

            // Act && Assert

            val feilmelding =
                assertThrows<Feil> {
                    dokumentGenereringService.genererManueltBrev(request, fagsak)
                }.message

            assertThat(feilmelding).isEqualTo("Fant ikke institusjon på fagsak id ${fagsak.id}")
        }

        @Test
        fun `skal bruke søkerens fødselsnummer som mottakerident for SKJERMET_BARN`() {
            // Arrange
            val søker = randomAktør()
            val fagsak = lagFagsakUtenId(skjermetBarnSøker = SkjermetBarnSøker(aktør = søker), type = FagsakType.SKJERMET_BARN)
            val request = mockk<ManueltBrevRequest>(relaxed = true)
            val brev = mockk<Brev>(relaxed = true)
            val mottakerIdent = slot<String>()

            every { personopplysningerService.hentPersoninfoEnkel(søker).navn } returns "Skjermet Søker"
            every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Z000000"
            mockkStatic("no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequestKt")

            every {
                request.tilBrev(capture(mottakerIdent), any(), any(), any())
            } returns brev

            every { brevKlient.genererBrev(any(), eq(brev)) } returns byteArrayOf()
            every { personopplysningerService.hentPersoninfoEnkel(any()) } returns PersonInfo(fødselsdato = LocalDate.now(), navn = "navn")

            // Act
            dokumentGenereringService.genererManueltBrev(request, fagsak)

            // Assert
            assertThat(mottakerIdent.isCaptured).isTrue
            assertThat(mottakerIdent.captured).isEqualTo(søker.aktivFødselsnummer())
        }

        @Test
        fun `skal kaste feil dersom fagsak type er skjermet barn men det ikke eksisterer en søker lagret`() {
            // Arrange
            val søker = randomAktør()
            val fagsak = lagFagsakUtenId(skjermetBarnSøker = null, type = FagsakType.SKJERMET_BARN)
            val request = mockk<ManueltBrevRequest>(relaxed = true)
            val brev = mockk<Brev>(relaxed = true)
            val mottakerIdent = slot<String>()

            every { personopplysningerService.hentPersoninfoEnkel(søker).navn } returns "Skjermet Søker"
            every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Z000000"
            mockkStatic("no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequestKt")

            every {
                request.tilBrev(capture(mottakerIdent), any(), any(), any())
            } returns brev

            every { brevKlient.genererBrev(any(), eq(brev)) } returns byteArrayOf()
            every { personopplysningerService.hentPersoninfoEnkel(any()) } returns PersonInfo(fødselsdato = LocalDate.now(), navn = "navn")

            // Act && Assert
            val feilmelding =
                assertThrows<Feil> {
                    dokumentGenereringService.genererManueltBrev(request, fagsak)
                }.message

            assertThat(feilmelding).isEqualTo("Fant ikke søker på fagsak id ${fagsak.id}")
        }
    }
}
