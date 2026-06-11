-- CREATE DATABASE tripbudgetplanner;

-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE Locations (
                           id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           city VARCHAR(150) NOT NULL,
                           country VARCHAR(100) NOT NULL
);

CREATE TABLE Images (
                        id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        url TEXT NOT NULL,
                        object_type TEXT NOT NULL CHECK (object_type IN ('users', 'voyages')),
                        object_id INT NOT NULL
);

CREATE TABLE Users (
                       id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       last_name VARCHAR(250),
                       first_name VARCHAR(250),
                       username VARCHAR(250) UNIQUE NOT NULL,
                       password VARCHAR(250) NOT NULL,
                       mail VARCHAR(250) UNIQUE NOT NULL,
                       phone_numb VARCHAR(20),
                       birthday DATE,
                       profile_image_id INT REFERENCES Images(id) ON DELETE SET NULL,
                       location_id INT REFERENCES Locations(id) ON DELETE SET NULL
);

CREATE TABLE Friends (
                         user_id INT REFERENCES Users(id) ON DELETE CASCADE,
                         friend_id INT REFERENCES Users(id) ON DELETE CASCADE,
                         PRIMARY KEY (user_id, friend_id)
);

CREATE TABLE Voyages (
                         id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         object_type TEXT NOT NULL CHECK (object_type IN ('users', 'travel_groups')),
                         object_id INT NOT NULL,
                         destination TEXT NOT NULL,
                         budget_total NUMERIC(10, 2) NOT NULL,
                         duration_days INT NOT NULL CHECK (duration_days > 0),
                         start_date DATE,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         cover_image_id INT REFERENCES Images(id) ON DELETE SET NULL
);


CREATE TABLE Travel_groups (
                               id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               voyage_id INT REFERENCES voyages(id) ON DELETE SET NULL,
                               name VARCHAR(100) NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE Group_memberships (
                                   group_id INT REFERENCES travel_groups(id) ON DELETE CASCADE,
                                   user_id INT REFERENCES users(id) ON DELETE CASCADE,
                                   PRIMARY KEY (group_id, user_id)
);

CREATE TABLE Expenses (
                          id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          voyage_id         INT NOT NULL REFERENCES voyages(id) ON DELETE CASCADE,
                          transport_amount  NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (transport_amount >= 0),
                          hotel_amount      NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (hotel_amount >= 0),
                          restaurant_amount NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (restaurant_amount >= 0),
                          activities_amount NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (activities_amount >= 0),
                          total_amount      NUMERIC(12,2)
                              GENERATED ALWAYS AS
                                  (transport_amount + hotel_amount + restaurant_amount + activities_amount) STORED,

                          currency          CHAR(3) NOT NULL DEFAULT 'EUR',
                          created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          UNIQUE (voyage_id)
);

CREATE TABLE Itinerary (
                           id           SERIAL PRIMARY KEY,
                           voyage_id    INT NOT NULL REFERENCES voyages(id) ON DELETE CASCADE,
                           day_number   INT NOT NULL CHECK (day_number > 0),
                           activity     TEXT NOT NULL,

                           created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Empêche deux jours identiques pour le même voyage
ALTER TABLE Itinerary
    ADD CONSTRAINT unique_voyage_day UNIQUE (voyage_id, day_number);
