-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 24, 2026 at 04:46 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `studentgig_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `applications`
--

CREATE TABLE `applications` (
  `id` int(11) NOT NULL,
  `job_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `status` varchar(50) DEFAULT NULL,
  `applied_at` datetime DEFAULT current_timestamp(),
  `accepted_at` datetime DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL,
  `employer_note` text DEFAULT NULL,
  `rating` int(11) DEFAULT NULL,
  `checked_in_at` datetime DEFAULT NULL,
  `work_done_at` datetime DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `applications`
--

INSERT INTO `applications` (`id`, `job_id`, `user_id`, `status`, `applied_at`, `accepted_at`, `started_at`, `completed_at`, `paid_at`, `employer_note`, `rating`, `checked_in_at`, `work_done_at`, `confirmed_at`) VALUES
(42, 86, 23, 'pending', '2026-03-23 12:29:14', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(43, 91, 23, 'accepted', '2026-03-23 12:42:08', '2026-03-23 07:12:40', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(44, 90, 56, 'pending', '2026-03-23 13:12:36', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `jobs`
--

CREATE TABLE `jobs` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `pay_amount` decimal(10,2) NOT NULL,
  `location` varchar(255) NOT NULL,
  `skills_required` text DEFAULT NULL,
  `is_urgent` tinyint(1) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `employer_id` int(11) DEFAULT NULL,
  `max_applicants` int(11) DEFAULT 1,
  `deadline` datetime DEFAULT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `category` varchar(100) DEFAULT NULL,
  `job_type` varchar(50) DEFAULT 'one-time',
  `duration` varchar(100) DEFAULT NULL,
  `status` varchar(50) DEFAULT 'open',
  `contact_info` varchar(255) DEFAULT NULL,
  `job_date` varchar(50) DEFAULT NULL,
  `start_time` varchar(20) DEFAULT NULL,
  `end_time` varchar(20) DEFAULT NULL,
  `address` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `jobs`
--

INSERT INTO `jobs` (`id`, `title`, `description`, `pay_amount`, `location`, `skills_required`, `is_urgent`, `created_at`, `employer_id`, `max_applicants`, `deadline`, `company_name`, `category`, `job_type`, `duration`, `status`, `contact_info`, `job_date`, `start_time`, `end_time`, `address`) VALUES
(86, 'Frontend Engineering Intern - Mobile App UI', 'Develop high-performance mobile interfaces for our global client base. You will translate design files into React Native code and optimize existing UX flows.\n\nKey Responsibilities:\n- Build reusable frontend components.\n- Map UI states using Redux/Context API.\n- Collaborate with product designers for pixel-perfection.', 25000.00, 'Indiranagar, Bangalore', '[\"React Native\", \"TypeScript\", \"Redux\", \"UI/UX Design\"]', 1, '2026-03-23 12:26:37', 45, 15, NULL, 'TechTonic Solutions Pvt Ltd.', 'Tech', 'part-time', '3 Months', 'open', NULL, 'March 26, 2026', '10:00 AM', '06:00 PM', 'Level 4, Zenith Tower, 100 Feet Road, Indiranagar, Bangalore, KA 560038'),
(87, 'High School Mathematics Tutor - Board Exam Prep', 'Patient and knowledgeable math tutor required for 10th-grade preparation. Focus on foundation for board exams twice a week.\n\nRequirements:\n- Strong command of Euclidean Geometry and Algebra.\n- Must have scored 95%+ in their own secondary school board exams.\n- Verifiable teaching background preferred.', 6000.00, 'Hauz Khas, New Delhi', '[\"Mathematics\", \"Teaching\", \"Calculus\", \"Algebra\"]', 1, '2026-03-23 12:26:37', 45, 2, NULL, 'Professional Tutors Enclave', 'Tutoring', 'recurring', '4 Months', 'open', NULL, 'April 2, 2026', '04:30 PM', '06:30 PM', 'H-Block Residence, Hauz Khas Enclave, New Delhi, DL 110016'),
(88, 'Event Photographer - Global Tech Summit 2026', 'Capturing keynote sessions and the innovation expo at the HICC Novotel summit.\n\nDeliverables:\n- 200+ high-res edited shots.\n- Highlight reel of top event moments.\n- Delivery required within 48 hours of the summit.', 12000.00, 'HITEC City, Hyderabad', '[\"Event Photography\", \"DSLR\", \"Lightroom\", \"Editing\"]', 0, '2026-03-23 12:26:37', 45, 1, NULL, 'Focus Media Agency', 'Photography', 'one-time', '1 Day', 'open', NULL, 'April 5, 2026', '09:00 AM', '07:00 PM', 'Hall 3, HICC Novotel Convention Center, HITEC City, Hyderabad, TG 500081'),
(89, 'Senior UI/UX Design Lead - Dashboard Revamp', 'Leading the design sprint for a major retail analytics platform. You will create interactive Figma prototypes and a design design system.\n\nKey Tasks:\n- User flow mapping for mobile/web.\n- Collaborative prototyping in Figma.\n- Stakeholder documentation.', 30000.00, 'Gachibowli, Hyderabad', '[\"Figma\", \"UI/UX Design\", \"User Testing\", \"Wireframing\"]', 0, '2026-03-23 12:26:37', 45, 4, NULL, 'Nexus Creative Studio', 'Design', 'part-time', '1 Month', 'open', NULL, 'April 10, 2026', '11:00 AM', '04:00 PM', 'Tower 1, Cyber Gateway, Gachibowli, Hyderabad, TG 500032'),
(90, 'Social Media Strategist & Content Creator', 'Manage handles for premium wellness brands. You will be responsible for viral reels, community interaction, and growth analysis.\n\nRequirements:\n- Creative visual storytelling skills.\n- Deep understanding of X (Twitter) and Instagram trends.', 10000.00, 'Andheri West, Mumbai', '[\"Social Media Marketing\", \"Content Creation\", \"Copywriting\", \"Trends\"]', 0, '2026-03-23 12:26:37', 45, 6, NULL, 'Apex Digital Agency', 'Marketing', 'part-time', '3 Months', 'open', NULL, 'April 15, 2026', '02:00 PM', '06:00 PM', '6th Floor, Platinum Towers, New Link Road, Andheri West, Mumbai, MH 400053'),
(91, 'social media manager', 'Are you a social media enthusiast with a passion for creating engaging content? We\'re looking for a social media manager to join our team in Mangalore. As a social media manager, you\'ll be responsible for managing our social media presence, creating and scheduling posts, responding to comments and messages, and analyzing engagement metrics. If you\'re a college student looking for a part-time gig that\'s flexible and fun, this could be the perfect opportunity for you. You\'ll work half-day, which means you\'ll have plenty of time to focus on your studies and other activities. We\'re looking for someone who is creative, proactive, and has a good understanding of social media platforms like Facebook, Instagram, and Twitter. Your duties will include creating and curating content, developing a social media strategy, and monitoring engagement metrics. You\'ll also need to stay up-to-date with the latest social media trends and best practices. If you\'re a self-motivated and organized individual who is passionate about social media, we\'d love to hear from you. This gig is a great way to gain experience in marketing and social media management, and you\'ll have the opportunity to work with a dynamic team. We\'re looking for someone who is available to work half-day, which is approximately 4-5 hours per day. You\'ll need to be able to work independently and as part of a team, and have excellent communication and writing skills. If you\'re interested in this opportunity, please apply with your resume and a cover letter. We can\'t wait to hear from you!', 500.00, 'Mangalore', '[\"social media\",\"communication\",\"content\",\"seo\",\"analytics\"]', 0, '2026-03-23 12:41:25', 54, 1, NULL, 'navayuga tech', 'Marketing', 'part-time', 'Full day', 'open', NULL, '2026-03-23', '09:00', '06:00', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `type` varchar(50) NOT NULL,
  `related_job_id` int(11) DEFAULT NULL,
  `related_application_id` int(11) DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`id`, `user_id`, `title`, `message`, `type`, `related_job_id`, `related_application_id`, `is_read`, `created_at`) VALUES
(27, 23, 'Application Accepted! 🎉', 'Your application for \'social media manager\' has been accepted.', 'application_accepted', 91, 43, 0, '2026-03-23 12:42:40');

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--

CREATE TABLE `payments` (
  `id` int(11) NOT NULL,
  `application_id` int(11) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `from_user_id` int(11) NOT NULL,
  `to_user_id` int(11) NOT NULL,
  `status` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `released_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `payments`
--

INSERT INTO `payments` (`id`, `application_id`, `amount`, `from_user_id`, `to_user_id`, `status`, `created_at`, `released_at`) VALUES
(4, 43, 500.00, 54, 23, 'pending', '2026-03-23 12:42:40', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `ratings`
--

CREATE TABLE `ratings` (
  `id` int(11) NOT NULL,
  `application_id` int(11) NOT NULL,
  `rater_id` int(11) NOT NULL,
  `rated_id` int(11) NOT NULL,
  `score` int(11) NOT NULL,
  `review` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `skills_json` text DEFAULT NULL,
  `role` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `total_earned` decimal(10,2) DEFAULT 0.00,
  `gigs_completed` int(11) DEFAULT 0,
  `rating` decimal(3,2) DEFAULT 0.00,
  `hashed_password` varchar(255) DEFAULT NULL,
  `security_question` varchar(255) DEFAULT NULL,
  `hashed_security_answer` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `phone`, `name`, `skills_json`, `role`, `created_at`, `total_earned`, `gigs_completed`, `rating`, `hashed_password`, `security_question`, `hashed_security_answer`) VALUES
(1, '9876543210', 'TestUser', '[\"python\",\"communication\"]', 'student', '2026-02-21 15:17:17', 0.00, 0, 0.00, NULL, NULL, NULL),
(2, '7674877379', 'pardhu ', NULL, 'student', '2026-02-21 15:18:59', 0.00, 0, 0.00, NULL, NULL, NULL),
(3, '8680889990', 'vvv', NULL, 'student', '2026-02-21 15:19:29', 0.00, 0, 0.00, NULL, NULL, NULL),
(4, '7842503845', 'nithin', NULL, 'student', '2026-02-21 15:40:09', 550.00, 1, 0.00, NULL, NULL, NULL),
(5, 'pardhub66@gmail', 'Pardhu B', NULL, 'student', '2026-02-25 15:13:10', 0.00, 0, 0.00, NULL, NULL, NULL),
(7, 'pardhub1816@gma', 'pardhu boddupalli', NULL, 'student', '2026-02-25 18:41:21', 2000.00, 2, 0.00, NULL, NULL, NULL),
(8, '9999999999', 'Test User', '[\"python\",\"testing\"]', 'student', '2026-02-26 21:24:57', 500.00, 1, 0.00, NULL, NULL, NULL),
(18, 'test@gmail.com', 'Test User', NULL, 'student', '2026-03-03 09:36:10', 0.00, 0, 0.00, NULL, NULL, NULL),
(23, 'nithin.v.kandula@gmail.com', 'Nithin Kandula', '[\"python\",\"design\"]', 'student', '2026-03-03 09:57:29', 500.00, 1, 5.00, NULL, NULL, NULL),
(24, '1234567890', 'TestUser', NULL, 'student', '2026-03-08 09:55:51', 0.00, 0, 0.00, NULL, NULL, NULL),
(25, '8888888888', 'John Seeker', '[\"python\", \"machine-learning\", \"data-analysis\", \"teaching\"]', 'student', '2026-03-09 10:20:48', 0.00, 0, 0.00, NULL, NULL, NULL),
(26, '7777777777', 'Test Employer', NULL, 'student', '2026-03-09 10:20:54', 0.00, 0, 0.00, NULL, NULL, NULL),
(27, 'pardhub1070.sse@saveetha.com', 'Pardhu B', NULL, 'student', '2026-03-09 10:46:02', 0.00, 0, 0.00, NULL, NULL, NULL),
(28, '9363965653', 'suresh sir', NULL, 'student', '2026-03-09 15:01:40', 0.00, 0, 0.00, NULL, NULL, NULL),
(30, 'fff@gmail.com', 'fff', NULL, 'student', '2026-03-09 23:26:39', 0.00, 0, 0.00, NULL, NULL, NULL),
(31, 'pardhub66@gmail.com', 'Pardhu B', NULL, 'student', '2026-03-16 09:36:03', 0.00, 0, 0.00, NULL, NULL, NULL),
(32, '9392311813', 'ajay', NULL, 'student', '2026-03-17 16:31:36', 0.00, 0, 0.00, NULL, NULL, NULL),
(33, '7842502845', 'rohit', NULL, 'student', '2026-03-17 19:37:06', 0.00, 0, 0.00, NULL, NULL, NULL),
(34, '9966038080', 'rahul', NULL, 'student', '2026-03-18 12:26:17', 0.00, 0, 5.00, NULL, NULL, NULL),
(35, '9032747678', 'virat', NULL, 'student', '2026-03-18 12:33:27', 0.00, 0, 0.00, NULL, NULL, NULL),
(36, 'bpardhu907@gmail.com', 'Pardhu B', NULL, 'student', '2026-03-18 14:35:17', 0.00, 0, 0.00, NULL, NULL, NULL),
(38, '9911223344', 'Success Final', NULL, 'student', '2026-03-18 20:34:25', 0.00, 0, 0.00, '$2b$12$ZnbzC8kudRCUBkzj6dC.je7XxLQGsDy7MQGuIXujlmrhk9Bja6D4q', 'What?', '$2b$12$fFKj2PIs9kinPZamR62/QuCmu/lRTx3fCNczH7DuV3xmYfENwmmPO'),
(39, '9000100200', 'Stable Tester', NULL, 'student', '2026-03-18 20:46:12', 0.00, 0, 0.00, '$2b$12$KG0tlWVwjIvTQO.VdxgUW.NbrqDHsU.k1yUGQ7e/II5/VaDSrbW.2', 'Why test?', '$2b$12$M2lho4l7ImfreU625gpDUOxXL1hz4eJsPzKN1lDIw0NN/VSMW/zYq'),
(40, '7842503844', 'puhpa', NULL, 'student', '2026-03-18 21:09:56', 0.00, 0, 0.00, '$2b$12$BgzsURfM0ivFn2vvJsQ5PeIxGe.IMqlbC/MBWdhxfjAlYxoRWlHgi', 'namep', '$2b$12$xH6nkrKs.C3CnL6S3lXmfeaITCAp0s6GCqfXs/l9zLpGmcxxiEFW2'),
(41, '7842503855', 'riya', NULL, 'student', '2026-03-18 22:41:50', 0.00, 0, 0.00, '$2b$12$YJ2LFgOEF0KVYLQG4w8Dhu0OwDHNmp6odOdlrb7KLbMPyFtcPDhTC', 'mother name', '$2b$12$bQWhJ9gyeylju1XXViy10uuNiXg.P56XLVGBD/ebs25yChaW0TBqi'),
(42, '7842503811', 'isha', NULL, 'student', '2026-03-19 08:32:13', 0.00, 0, 0.00, '$2b$12$/TSNVXhUzKFY5/9GMk1QPOL.AJ25DHinSlo2ri8WiBdIjWn5Ypxaa', 'What is your mother\'s maiden name?', '$2b$12$5LFYwVES52hHalfziiOHCesUhtysIWc6vYFzmw5o9pXlKZnxmULtu'),
(43, '7842503822', 'shiva', NULL, 'student', '2026-03-19 10:00:03', 0.00, 0, 0.00, '$2b$12$toKh9AvNhSwkO2u9XyPt6OegmNqkr5MGKqQDvTPgYg92LgvQEmvCO', 'In what city were you born?', '$2b$12$ewHE.1ClboeXrqEFPEz4p.z7iSG4MYiBiYmEJcVo7jO1PV/F9fdMq'),
(44, '7842503833', 'sunil', NULL, 'student', '2026-03-19 10:02:30', 0.00, 0, 0.00, '$2b$12$OKdkh.J/gZNgAI16.1rBfubQ0OrpwZnBA46jKR/REC9JuJgHFFYRG', 'name4', '$2b$12$L7D0Ub1sUwMPVY1ghgjFDOVJhH4hddBWF6miTxpIJDgZnx2xGEMN6'),
(45, '7458963210', 'tester', '[\"1\",\"2\",\"python\",\"teaching\",\"english\",\"communication\",\"data-analysis\",\"hindi\",\"teamwork\",\"mathematics\",\"photography\",\"punctuality\",\"editing\",\"content-writing\",\"patience\",\"bicycle\",\"instagram\",\"automated\",\"selenium\",\"canva\",\"testing\"]', 'employer', '2026-03-19 11:19:17', 0.00, 0, 0.00, '$2b$12$znhx5CrvKtQAM3tpDJOUA.mbZ67ZeT.gmKTFXYFnQCSQP9cGIFSmi', 'In what city were you born?', '$2b$12$rG/Xqb/iGt.3OBErpYAnCex67REJruK.FKv0/5YbRgBiI40UaXwmS'),
(46, '8203049103', 'Great Employer', NULL, 'employer', '2026-03-19 11:34:10', 0.00, 0, 0.00, '$2b$12$eLmwCnpEw3K0PtUGzHdJzu5TLuNbhh0D3mY191Ecz3gzv98HQs4r.', 'pet name', '$2b$12$sbFh1Lnlx0JCRHqUiHQba.briHhQuPvMYfKemoomnz7a.P0BepNbe'),
(47, '9640766695', 'gowri', NULL, 'student', '2026-03-23 08:30:40', 0.00, 0, 0.00, '$2b$12$43zoCr.Ds0P0qeDg8DmcbeCUC5Sh8jdAxq0LV/qQxctmrzMmyQTtG', 'mother', '$2b$12$QPPtN9AbXTDtBRFal7CR/u1E//YCX./C41GDwv3erCbYcMC9XYz2.'),
(48, '9253146875', 'priya', NULL, 'student', '2026-03-23 08:31:56', 0.00, 0, 0.00, '$2b$12$tCfdvowIL4H06E62sJrcluYS30a9xl.gkGSc3O9ZeIM6SXFrcxu7u', 'What was the make of your first car?', '$2b$12$zf3THFCmioo06/YOhQqy9uOLk.FtHo2Z6JtYuDzqWp4yXF9DDdM7a'),
(49, '9876543235', 'Employer Test', NULL, 'employer', '2026-03-23 10:33:58', 0.00, 0, 0.00, '$2b$12$QBUI9nTdJH4d3YeVP7GVtedzdyoGY7caeCiKEPyDXSLtBCSHgfbCO', 'My first school?', '$2b$12$LvYKXIjT3P..t01hYNXBYut2afnRb3a4MPHBFc.ELJ04uBaVKTx02'),
(50, '9876543249', 'Employer Three', NULL, 'employer', '2026-03-23 10:36:07', 0.00, 0, 0.00, '$2b$12$zAdUJBaPiKqIjhXGgVkGhuMfo369DLJr6G7U0umFR6Npp9iXli/Hu', 'My first school?', '$2b$12$LzMV5LoZkLrWsVP5F46GVOZn3e8eBGH/nBvjylD1KaY47arrzoiVq'),
(51, '9876500001', 'TestStudent', NULL, 'student', '2026-03-23 10:56:37', 0.00, 0, 0.00, '$2b$12$RJfwknGAi8OIiXaIyaAuJOs58lToeqFInrpW1pTo25Hg3cF2g1Qhq', 'My pet?', '$2b$12$b/zX3gLAhf6H56.0DCEBY.72URjBs/ilDww.3X6yF9mron2Icj3.S'),
(52, '9876500002', 'TestEmployer', NULL, 'employer', '2026-03-23 10:56:41', 0.00, 0, 0.00, '$2b$12$QwO5BMMb.5VZQm8oXPm3feaoOtzzXTd3iJr3yVgWxR56dmLmjz3Pa', 'My pet?', '$2b$12$7jJjj1Jpbrwsos34ayc8eOCY2J8Z1cdBtKQHbFpg49sW1qExSgswm'),
(53, '9876500099', 'TestStu', NULL, 'student', '2026-03-23 10:57:47', 0.00, 0, 0.00, '$2b$12$zlwuZHypwdbOs5Uk8PcfdO1vE68L4Ce4XrCUIK8SkuzRRwePp3ESa', 'My first school?', '$2b$12$V1rfXjjwg.sO9oB07MikTeREeOLF7XBU1Sv0eYMmGOAEJBc.pSCVC'),
(54, '7842503866', 'rohit', NULL, 'employer', '2026-03-23 12:32:02', 0.00, 0, 0.00, '$2b$12$xjFHGETQ.kMLaE81nlQhf./Wd3Av2n5Y8F889crbRHQnrARulRZOu', 'In what city were you born?', '$2b$12$iNiH/VryEvX1vtKMoI4NIuHOnHvbr4jaXwCQllH9TA9nBQrCX9oJi'),
(55, '7842503877', 'mahendra', NULL, 'employer', '2026-03-23 12:47:40', 0.00, 0, 0.00, '$2b$12$sScxSC5y82P5ou59wcgiveXLXENETZKUYvbh5mM7Ymrwm/XGO7Mn6', 'What is your mother\'s maiden name?', '$2b$12$5wdEPj2lV3S403tEKfogf.q2Zrubq8xg7th6N0v5o6rFraRuAOMHu'),
(56, 'pardhub1816@gmail.com', 'pardhu boddupalli', '[\"communication\",\"python\"]', 'student', '2026-03-23 13:12:12', 0.00, 0, 0.00, NULL, NULL, NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `applications`
--
ALTER TABLE `applications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `job_id` (`job_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `ix_applications_id` (`id`);

--
-- Indexes for table `jobs`
--
ALTER TABLE `jobs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `ix_jobs_id` (`id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `ix_notifications_id` (`id`);

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `application_id` (`application_id`),
  ADD KEY `from_user_id` (`from_user_id`),
  ADD KEY `to_user_id` (`to_user_id`),
  ADD KEY `ix_payments_id` (`id`);

--
-- Indexes for table `ratings`
--
ALTER TABLE `ratings`
  ADD PRIMARY KEY (`id`),
  ADD KEY `application_id` (`application_id`),
  ADD KEY `rater_id` (`rater_id`),
  ADD KEY `rated_id` (`rated_id`),
  ADD KEY `ix_ratings_id` (`id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `ix_users_phone` (`phone`),
  ADD KEY `ix_users_id` (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `applications`
--
ALTER TABLE `applications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=45;

--
-- AUTO_INCREMENT for table `jobs`
--
ALTER TABLE `jobs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=92;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=28;

--
-- AUTO_INCREMENT for table `payments`
--
ALTER TABLE `payments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `ratings`
--
ALTER TABLE `ratings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=57;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `applications`
--
ALTER TABLE `applications`
  ADD CONSTRAINT `applications_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`),
  ADD CONSTRAINT `applications_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `payments`
--
ALTER TABLE `payments`
  ADD CONSTRAINT `payments_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`),
  ADD CONSTRAINT `payments_ibfk_2` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `payments_ibfk_3` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `ratings`
--
ALTER TABLE `ratings`
  ADD CONSTRAINT `ratings_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`),
  ADD CONSTRAINT `ratings_ibfk_2` FOREIGN KEY (`rater_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `ratings_ibfk_3` FOREIGN KEY (`rated_id`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
