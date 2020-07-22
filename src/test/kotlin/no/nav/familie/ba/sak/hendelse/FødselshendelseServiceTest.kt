package no.nav.familie.ba.sak.hendelse

import io.mockk.*
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.IdentInformasjon
import no.nav.familie.kontrakter.felles.personinfo.Ident
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FødselshendelseServiceTest {
    val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    val personopplysningerServiceMock= mockk<PersonopplysningerService>()
    val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()

    val søkerFnr = "12345678910"
    val barn1Fnr = "12345678911"
    val barn2Fnr = "12345678912"

    val fødselshendelseService = FødselshendelseService(infotrygdFeedServiceMock, infotrygdBarnetrygdClientMock, personopplysningerServiceMock)

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal returne true dersom klienten returnerer false`() {
        every { personopplysningerServiceMock.hentIdenter(any()) } returns listOf(IdentInformasjon(søkerFnr, false, "FOLKEREGISTERIDENT"))
        every { infotrygdBarnetrygdClientMock.finnesIkkeHosInfotrygd(any(), any()) } returns false

        val skalBehandlesHosInfotrygd = fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr))

        Assertions.assertTrue(skalBehandlesHosInfotrygd)
    }

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal filtrere bort aktørId`() {
        every { personopplysningerServiceMock.hentIdenter(Ident(søkerFnr)) } returns listOf(IdentInformasjon(søkerFnr, false, "FOLKEREGISTERIDENT"),
                                                                                         IdentInformasjon("1234567890123", false, "AKTORID"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn1Fnr)) } returns listOf(IdentInformasjon(barn1Fnr, false, "FOLKEREGISTERIDENT"))

        val slot = slot<List<String>>()
        every { infotrygdBarnetrygdClientMock.finnesIkkeHosInfotrygd(capture(slot), any()) } returns false

        fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr))

        Assertions.assertEquals(1, slot.captured.size)
        Assertions.assertEquals(søkerFnr, slot.captured[0])
    }

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal kollapse listen av barn til en samlet list av barn mot klienten`() {
        every { personopplysningerServiceMock.hentIdenter(Ident(søkerFnr)) } returns listOf(IdentInformasjon(søkerFnr, false, "FOLKEREGISTERIDENT"),
                IdentInformasjon("1234567890123", false, "AKTORID"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn1Fnr)) } returns listOf(IdentInformasjon(barn1Fnr, false, "FOLKEREGISTERIDENT"),
                IdentInformasjon("98765432101", false, "FOLKEREGISTERIDENT"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn2Fnr)) } returns listOf(IdentInformasjon(barn2Fnr, false, "FOLKEREGISTERIDENT"))

        val slot = slot<List<String>>()
        every { infotrygdBarnetrygdClientMock.finnesIkkeHosInfotrygd(any(), capture(slot)) } returns false

        fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr, barn2Fnr))

        Assertions.assertEquals(3, slot.captured.size)
    }
}