# MySQL to PostgreSQL Migration

This directory contains local-only migration helpers for moving data from the
restored MariaDB/MySQL dump database into the new PostgreSQL schema.

## 1. Restore the old dump into MySQL

```bash
docker compose -f /tmp/linku-develop-compose.yml up -d mysql
docker exec -i linkU-mysql mysql -uroot -plinkU1234 -e "
DROP DATABASE IF EXISTS linkudb_old;
CREATE DATABASE linkudb_old CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
"
docker exec -i linkU-mysql mysql -uroot -plinkU1234 linkudb_old < /Users/ijiwon/linkudb_dump_mysql_local.sql
```

## 2. Prepare the new PostgreSQL schema

Run the application once against PostgreSQL, or create the schema with DDL/Flyway.
Keep automatic `data.sql` execution disabled unless you intentionally need seed
data:

```properties
SQL_INIT_MODE=never
```

## 3. Install local script dependencies

```bash
python3 -m pip install pymysql 'psycopg[binary]'
```

## 4. Dry run

```bash
python3 scripts/migration/migrate_mysql_to_postgres.py --dry-run
```

Default connection values:

```text
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=linkudb_old
MYSQL_USER=root
MYSQL_PASSWORD=linkU1234

PG_HOST=127.0.0.1
PG_PORT=5432
PG_DATABASE=linkUDB
PG_USER=linkU
PG_PASSWORD=linkU1234
```

Override them with environment variables if needed.

## 5. Copy data

```bash
python3 scripts/migration/migrate_mysql_to_postgres.py
```

By default, the script skips base seed tables such as `domains`, `categories`,
`emotions`, `jobs`, and `situations`. Use the PostgreSQL-compatible
`src/main/resources/data.sql` or Flyway for those.

To include seed tables too:

```bash
python3 scripts/migration/migrate_mysql_to_postgres.py --include-seed
```

To copy only specific target tables:

```bash
python3 scripts/migration/migrate_mysql_to_postgres.py --only users linkus folders
```

To clear target tables first:

```bash
python3 scripts/migration/migrate_mysql_to_postgres.py --truncate-target
```

Use `--truncate-target` only against a disposable local database.
