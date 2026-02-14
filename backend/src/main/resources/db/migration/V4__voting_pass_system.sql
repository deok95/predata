-- V4: Voting Pass system - replace ticket-based voting with permanent voting pass
ALTER TABLE members ADD COLUMN has_voting_pass BOOLEAN NOT NULL DEFAULT FALSE;
