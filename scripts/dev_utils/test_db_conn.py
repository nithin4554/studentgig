
import pymysql
try:
    conn = pymysql.connect(host='localhost', user='root', password='', port=3306)
    print("Connected to MySQL successfully")
    cur = conn.cursor()
    cur.execute("SHOW DATABASES")
    databases = [db[0] for db in cur.fetchall()]
    print("Databases:", databases)
    if 'studentgig_db' in databases:
        print("Database 'studentgig_db' found")
    else:
        print("Database 'studentgig_db' NOT found")
    conn.close()
except Exception as e:
    print("Error:", e)
