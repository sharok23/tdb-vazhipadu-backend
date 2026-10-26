ALTER TABLE offering ADD COLUMN daily_limit INT CHECK (daily_limit > 0);
