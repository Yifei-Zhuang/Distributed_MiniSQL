#!/bin/bash
mysql -u root -pmysql -Bse "SET GLOBAL binlog_format = 'MIXED';"