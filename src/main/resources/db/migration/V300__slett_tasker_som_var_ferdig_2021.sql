delete from task
where status = 'FERDIG'
  and opprettet_tid < '2021-02-01'
  and trigger_tid is null;