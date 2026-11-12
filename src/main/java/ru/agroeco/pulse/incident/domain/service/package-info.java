/**
 * Domain services — операции, не принадлежащие одной entity,
 * но являющиеся частью доменной логики.
 *
 * <p>Компромисс: используют Spring Data репозитории и {@code EventDispatcher}
 * напрямую. В строгом DDD зависели бы от портов в {@code domain.port};
 * при текущем размере домена это не окупается.</p>
 */
package ru.agroeco.pulse.incident.domain.service;