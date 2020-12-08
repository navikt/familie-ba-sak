update behandling b
set resultat = vilkaarsvurdering.samlet_resultat
from b inner join vilkaarsvurdering on b.id = vilkaarsvurdering.fk_behandling_id and vilkaarsvurdering.aktiv = true;