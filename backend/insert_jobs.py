# -*- coding: utf-8 -*-
from database import SessionLocal
from models import User, Job, Application
import json

def insert_perfect_jobs():
    db = SessionLocal()
    
    # Reset for perfection
    db.query(Application).delete()
    db.query(Job).delete()
    db.commit()

    emp = db.query(User).filter(User.role == 'employer').first()
    if not emp:
        from auth import get_password_hash
        emp = User(
            phone='9990001112', 
            name='TechCorp India', 
            hashed_password=get_password_hash('password123'), 
            role='employer', 
            security_question='What is your company?', 
            security_answer='TechCorp'
        )
        db.add(emp)
        db.commit()
        db.refresh(emp)

    jobs = [
        {
            'title': 'Frontend Engineering Intern - Mobile App UI',
            'company_name': 'TechTonic Solutions Pvt Ltd.',
            'category': 'Tech',
            'job_type': 'part-time',
            'pay_amount': 25000.0,
            'location': 'Indiranagar, Bangalore',
            'description': 'Develop high-performance mobile interfaces for our global client base. You will translate design files into React Native code and optimize existing UX flows.\n\nKey Responsibilities:\n- Build reusable frontend components.\n- Map UI states using Redux/Context API.\n- Collaborate with product designers for pixel-perfection.',
            'skills_required': json.dumps(['React Native', 'TypeScript', 'Redux', 'UI/UX Design']),
            'duration': '3 Months',
            'job_date': 'March 26, 2026',
            'start_time': '10:00 AM',
            'end_time': '06:00 PM',
            'address': 'Level 4, Zenith Tower, 100 Feet Road, Indiranagar, Bangalore, KA 560038',
            'is_urgent': True,
            'max_applicants': 15
        },
        {
            'title': 'High School Mathematics Tutor - Board Exam Prep',
            'company_name': 'Professional Tutors Enclave',
            'category': 'Tutoring',
            'job_type': 'recurring',
            'pay_amount': 6000.0,
            'location': 'Hauz Khas, New Delhi',
            'description': 'Patient and knowledgeable math tutor required for 10th-grade preparation. Focus on foundation for board exams twice a week.\n\nRequirements:\n- Strong command of Euclidean Geometry and Algebra.\n- Must have scored 95%+ in their own secondary school board exams.\n- Verifiable teaching background preferred.',
            'skills_required': json.dumps(['Mathematics', 'Teaching', 'Calculus', 'Algebra']),
            'duration': '4 Months',
            'job_date': 'April 2, 2026',
            'start_time': '04:30 PM',
            'end_time': '06:30 PM',
            'address': 'H-Block Residence, Hauz Khas Enclave, New Delhi, DL 110016',
            'is_urgent': True,
            'max_applicants': 2
        },
        {
            'title': 'Event Photographer - Global Tech Summit 2026',
            'company_name': 'Focus Media Agency',
            'category': 'Photography',
            'job_type': 'one-time',
            'pay_amount': 12000.0,
            'location': 'HITEC City, Hyderabad',
            'description': 'Capturing keynote sessions and the innovation expo at the HICC Novotel summit.\n\nDeliverables:\n- 200+ high-res edited shots.\n- Highlight reel of top event moments.\n- Delivery required within 48 hours of the summit.',
            'skills_required': json.dumps(['Event Photography', 'DSLR', 'Lightroom', 'Editing']),
            'duration': '1 Day',
            'job_date': 'April 5, 2026',
            'start_time': '09:00 AM',
            'end_time': '07:00 PM',
            'address': 'Hall 3, HICC Novotel Convention Center, HITEC City, Hyderabad, TG 500081',
            'is_urgent': False,
            'max_applicants': 1
        },
        {
            'title': 'Senior UI/UX Design Lead - Dashboard Revamp',
            'company_name': 'Nexus Creative Studio',
            'category': 'Design',
            'job_type': 'part-time',
            'pay_amount': 30000.0,
            'location': 'Gachibowli, Hyderabad',
            'description': 'Leading the design sprint for a major retail analytics platform. You will create interactive Figma prototypes and a design design system.\n\nKey Tasks:\n- User flow mapping for mobile/web.\n- Collaborative prototyping in Figma.\n- Stakeholder documentation.',
            'skills_required': json.dumps(['Figma', 'UI/UX Design', 'User Testing', 'Wireframing']),
            'duration': '1 Month',
            'job_date': 'April 10, 2026',
            'start_time': '11:00 AM',
            'end_time': '04:00 PM',
            'address': 'Tower 1, Cyber Gateway, Gachibowli, Hyderabad, TG 500032',
            'is_urgent': False,
            'max_applicants': 4
        },
        {
            'title': 'Social Media Strategist & Content Creator',
            'company_name': 'Apex Digital Agency',
            'category': 'Marketing',
            'job_type': 'part-time',
            'pay_amount': 10000.0,
            'location': 'Andheri West, Mumbai',
            'description': 'Manage handles for premium wellness brands. You will be responsible for viral reels, community interaction, and growth analysis.\n\nRequirements:\n- Creative visual storytelling skills.\n- Deep understanding of X (Twitter) and Instagram trends.',
            'skills_required': json.dumps(['Social Media Marketing', 'Content Creation', 'Copywriting', 'Trends']),
            'duration': '3 Months',
            'job_date': 'April 15, 2026',
            'start_time': '02:00 PM',
            'end_time': '06:00 PM',
            'address': '6th Floor, Platinum Towers, New Link Road, Andheri West, Mumbai, MH 400053',
            'is_urgent': False,
            'max_applicants': 6
        }
    ]

    for d in jobs:
        job = Job(**d, employer_id=emp.id, status='open')
        db.add(job)
    
    db.commit()
    print('Completed: 5 truly professional jobs inserted.')
    db.close()

if __name__ == '__main__':
    insert_perfect_jobs()
