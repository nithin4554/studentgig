from database import engine
from sqlalchemy import text
with engine.connect() as conn:
    res = conn.execute(text('SELECT phone, name, role FROM users ORDER BY id DESC LIMIT 5'))
    for row in res:
        print(row)
