INSERT INTO kunde (kundeid, vorname, nachname, email, strasse, strassenzusatz, ort, land, plz, firmenname, created_at, updated_at)
VALUES
    ('1', 'Anna', 'Schmidt', 'anna.schmidt@example.com', 'Hauptstrasse 1', '', 'Berlin', 'DE', '10115', 'Schmidt GmbH', now() - interval '30 days', now() - interval '3 days'),
    ('2', 'Bernd', 'Meyer', 'bernd.meyer@example.com', 'Musterweg 22', '', 'Hamburg', 'DE', '20095', 'Meyer Consulting', now() - interval '25 days', now() - interval '2 days'),
    ('3', 'Claire', 'Dubois', 'claire.dubois@example.com', 'Rue de Paris 5', '', 'Paris', 'FR', '75001', 'Dubois SARL', now() - interval '20 days', now() - interval '5 days'),
    ('4', 'Daniel', 'Rossi', 'daniel.rossi@example.com', 'Via Roma 9', '', 'Milano', 'IT', '20121', 'Rossi Logistics', now() - interval '18 days', now() - interval '1 days');

INSERT INTO auftraege (auftragid, artikelnummer, created, lastchange, kundeid)
VALUES
    ('A-100', 'ART-001', now() - interval '10 days', now() - interval '3 days', '1'),
    ('A-200', 'ART-002', now() - interval '8 days', now() - interval '2 days', '2'),
    ('A-300', 'ART-003', now() - interval '7 days', now() - interval '5 days', '3'),
    ('A-400', 'ART-004', now() - interval '5 days', now() - interval '1 days', '4');

