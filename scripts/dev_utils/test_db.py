import pymysql
import os

DB_USER = "root"
DB_PASS = ""
DB_HOST = "localhost"
DB_PORT = 3306
DB_NAME = "studentgig_db"

try:
    print(f"Connecting to {DB_NAME} on {DB_HOST}:{DB_PORT} as {DB_USER}...")
    conn = pymysql.connect(
        host=DB_HOST,
        user=DB_USER,
        password=DB_PASS,
        port=DB_PORT
    )
    print("Base MySQL connection successful.")
    
    with conn.cursor() as cursor:
        cursor.execute("SHOW DATABASES")
        databases = [db[0] for db in cursor.fetchall()]
        print(f"Available databases: {databases}")
        
        if DB_NAME in databases:
            print(f"Database '{DB_NAME}' exists.")
            conn.select_db(DB_NAME)
            cursor.execute("SHOW TABLES")
            tables = [t[0] for t in cursor.fetchall()]
            print(f"Tables in {DB_NAME}: {tables}")
        else:
            print(f"Database '{DB_NAME}' DOES NOT EXIST.")
            
    conn.close()
except Exception as e:
    print(f"ERROR: {e}")
