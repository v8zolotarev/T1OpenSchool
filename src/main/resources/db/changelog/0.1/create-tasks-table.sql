CREATE TABLE tasks (
id SERIAL PRIMARY KEY,
title VARCHAR(255),
description TEXT,
status VARCHAR(50),
user_id BIGINT
);