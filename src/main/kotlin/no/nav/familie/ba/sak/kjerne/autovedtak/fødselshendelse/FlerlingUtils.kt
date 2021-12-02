package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import org.slf4j.Logger

fun finnBarnSomSkalBehandlesForMor(
    nyBehandlingHendelse: NyBehandlingHendelse,
    barnaTilMor: List<ForelderBarnRelasjon>,
    barnaSomHarBlittBehandlet: List<String>,
    secureLogger: Logger? = null
): Pair<List<String>, List<String>> {
    val barnaPåHendelse = barnaTilMor.filter { nyBehandlingHendelse.barnasIdenter.contains(it.personIdent.id) }
    val andreBarnFødtInnenEnDag = barnaTilMor.filter {
        barnaPåHendelse.any { barnPåHendelse ->
            barnPåHendelse.personIdent.id != it.personIdent.id &&
                (
                    barnPåHendelse.fødselsdato == it.fødselsdato ||
                        barnPåHendelse.fødselsdato?.plusDays(1) == it.fødselsdato ||
                        barnPåHendelse.fødselsdato?.minusDays(1) == it.fødselsdato
                    )
        }
    }

    secureLogger?.info(
        "Behandler fødselshendelse på ${nyBehandlingHendelse.morsIdent}. " +
            "Barn på hendelse: ${barnaPåHendelse.map { it.toSecureString() }}, barn med tilstøtende fødselsdato som også behandles: ${andreBarnFødtInnenEnDag.map { it.toSecureString() }}"
    )

    val alleBarnSomKanBehandles = (barnaPåHendelse + andreBarnFødtInnenEnDag).map { it.personIdent.id }
    val barnSomSkalBehandlesForMor = alleBarnSomKanBehandles
        .filter { !barnaSomHarBlittBehandlet.contains(it) }

    return Pair(barnSomSkalBehandlesForMor, alleBarnSomKanBehandles)
}

fun barnPåHendelseBlirAlleredeBehandletIÅpenBehandling(
    barnaPåHendelse: List<String>,
    barnaPåÅpenBehandling: List<String>
): Boolean {
    return barnaPåHendelse.all { barnaPåÅpenBehandling.contains(it) }
}
