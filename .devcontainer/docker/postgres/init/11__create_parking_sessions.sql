-- DDL: 駐車セッションテーブルの作成
CREATE TABLE parking_sessions (
    id            UUID         PRIMARY KEY,
    space_number  CHAR(2)      NOT NULL,
    entry_time    TIMESTAMP    NOT NULL,
    exit_time     TIMESTAMP,
    fee           INTEGER,
    status        VARCHAR(16)  NOT NULL
);
