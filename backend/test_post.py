import requests

# 1. Register
r1 = requests.post('http://localhost:8000/api/register', json={
    'phone': '9876543235',
    'name': 'Employer Test',
    'password': 'password123',
    'security_question': 'My first school?',
    'security_answer': 'abc',
    'role': 'employer'
})
print('reg:', r1.text)
token = r1.json().get('access_token')

if not token:
    print("Failed to get token!")
    exit(1)

# 2. Post Job with same payload as mobile app
r2 = requests.post('http://localhost:8000/api/jobs', headers={'Authorization': f'Bearer {token}'}, json={
    'title': 'Test Job 2',
    'pay_amount': 500,
    'location': 'Pune',
    'company_name': 'My Company',
    'category': 'Tech',
    'job_type': 'one-time',
    'max_applicants': 1,
    'description': '',
    'skills_required': None,
    'duration': ''
})
print('job:', r2.text)
