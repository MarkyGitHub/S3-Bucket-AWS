CREATE TABLE kunde (
    kundeid VARCHAR(255) PRIMARY KEY,
    vorname VARCHAR(255) NOT NULL,
    nachname VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    strasse VARCHAR(255),
    strassenzusatz VARCHAR(255),
    ort VARCHAR(255),
    land VARCHAR(10) NOT NULL,
    plz VARCHAR(20),
    firmenname VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auftraege (
    auftragid VARCHAR(255) PRIMARY KEY,
    artikelnummer VARCHAR(255) NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL,
    lastchange TIMESTAMP WITH TIME ZONE NOT NULL,
    kundeid VARCHAR(255) NOT NULL REFERENCES kunde (kundeid)
);

CREATE INDEX idx_kunde_land ON kunde (land);
CREATE INDEX idx_auftraege_lastchange ON auftraege (lastchange);

