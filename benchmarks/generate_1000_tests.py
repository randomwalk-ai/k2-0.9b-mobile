"""
Generates 1,000 deep, real-world stress test cases for the K2 Horizon vs Llama 3.2 Notification Analyzer.
Breakdown:
- 500 Deep AI On-Demand Semantic/Tone & Emotional Nuance Test Cases (Rules 1-6)
- 500 AOT Conditional, App Filters, Fast Contact, Topic, and Suppression Test Cases (Rules 7-15)
Includes ground truth decisions (ALERT vs MUTE), ground truth reasons, difficulty ratings, and edge cases.
"""

import json
import random

# Ensure deterministic generation
random.seed(42)

test_cases = []
current_id = 1

def add_case(group, rule_id, rule_name, app, sender, text, is_call, ground_truth_alert, reason, difficulty="Medium"):
    global current_id
    test_cases.append({
        "id": current_id,
        "group": group,
        "rule_id": rule_id,
        "rule_name": rule_name,
        "app": app,
        "sender": sender,
        "text": text,
        "is_call": is_call,
        "ground_truth_alert": ground_truth_alert,
        "ground_truth_action": "ALERT" if ground_truth_alert else "MUTE",
        "ground_truth_reason": reason,
        "difficulty": difficulty
    })
    current_id += 1

# ==============================================================================
# GROUP 1: DEEP AI ON-DEMAND (500 TEST CASES)
# ==============================================================================

# ------------------------------------------------------------------------------
# Rule 1: Rahul Frustration / Passive Aggression -> MUTE (85 cases)
# Target: 45 MUTE (passive-aggressive / frustrated), 40 ALERT (normal / friendly / productive)
# ------------------------------------------------------------------------------
rahul_pa_samples = [
    ("Sure, do whatever you want, clearly my time is not valuable.", "Classic passive-aggressive dismissiveness"),
    ("Fine. I guess I'll just redo everything myself as usual.", "Frustrated martyr tone"),
    ("Must be nice to have the entire afternoon off while the rest of us fix your bugs.", "Sarcastic resentment"),
    ("Don't bother apologizing now, the client already noticed.", "Blaming / passive-aggressive snark"),
    ("No worries, I didn't actually need that report by the deadline anyway.", "Heavily sarcastic resentment"),
    ("Sigh... yet another meeting that could have been an email.", "Exasperated frustration"),
    ("I love how you made this decision without asking anyone who actually writes the code.", "Passive-aggressive corporate sarcasm"),
    ("Whatever you say, boss. You always know best.", "Disguised insubordination / passive aggression"),
    ("Thanks for finally replying after 6 hours. Really helpful.", "Sarcastic irritation regarding response delay"),
    ("Cool, glad to see my suggestions were completely ignored in the design deck.", "Bitter frustration over ignored input"),
    ("I guess my 4 years on this project don't mean anything to this new team.", "Disappointed passive-aggressive resentment"),
    ("Great job team, especially to those who contributed literally zero commits.", "Hostile sarcasm masked as praise"),
    ("Don't worry about helping, I'm used to doing the heavy lifting alone.", "Guilt-tripping passive aggression"),
    ("Interesting how you always find time for coffee breaks but never code reviews.", "Snarky personal attack"),
    ("It's fine. I'll just cancel my weekend plans to finish your slides.", "Guilt-inducing corporate passive-aggression"),
    ("Apparently asking for basic documentation is too much to ask around here.", "Frustrated snark about teamwork"),
    ("Funny how you take credit in the all-hands for work I finished at 2 AM.", "Bitter resentment over credit"),
    ("I won't bother sharing my thoughts next time since they clearly don't matter.", "Passive-aggressive withdrawal"),
    ("Right, because my opinion has ever stopped you before.", "Cynical frustration"),
    ("As per my previous email... which you obviously didn't read.", "Cold corporate passive-aggression"),
    ("Just let me know when you're done breaking the build so I can actually work.", "Biting technical frustration"),
    ("Oh wow, you actually showed up to the standup today. Groundbreaking.", "Mocking sarcasm"),
    ("No, really, take your time. It's not like the production server is waiting.", "Urgent-sounding sarcasm hiding irritation"),
    ("I'll just assume my calendar invite was invisible to your inbox.", "Passive-aggressive meeting complaint"),
    ("Whatever. Do what you want. I wash my hands of this release.", "Defeatist frustrated dismissal"),
    ("I'm so thrilled to be assigned the legacy codebase cleanup yet again.", "Heavy irony / frustration"),
    ("Sure, let's change the architecture the day before deployment. Genius idea.", "Sarcastic technical frustration"),
    ("Glad I stayed up till 3 AM just for you to cancel the feature.", "Bitter wasted effort complaint"),
    ("I suppose replying to high-priority Slack threads is optional now.", "Passive-aggressive reprimand"),
    ("Don't ask me for help when this breaks in front of the VP.", "Spiteful frustration"),
    ("Love waiting 45 minutes for a code review on a one-line change.", "Irritated venting"),
    ("It's fine, I didn't want to leave the office before 9 PM anyway.", "Martyr sarcasm"),
    ("Sure, go ahead and merge untested code. What could possibly go wrong?", "Mocking passive disapproval"),
    ("Fascinating how the deadline is strict for me but flexible for everyone else.", "Perceived unfairness venting"),
    ("Thanks for nothing. I'll figure it out myself.", "Overt dismissive frustration"),
    ("I'm not angry, just disappointed that basic requirements were missed.", "Classic passive-aggressive guilt"),
    ("Clearly someone didn't bother checking the test logs before pushing.", "Snide condescension"),
    ("You know what? Never mind. It's pointless explaining this to you.", "Frustrated cutoff"),
    ("Must be great being immune to code review feedback.", "Jealous passive-aggressive snark"),
    ("I'll add this to the long list of things that are suddenly my responsibility.", "Overwhelmed martyr complaint"),
    ("Yeah sure, let's ignore all standard protocols because you're in a rush.", "Cynical process complaint"),
    ("Don't apologize, just don't do it again next time.", "Stiff condescending dismissal"),
    ("Glad you had time to post memes in random while the API was failing.", "Shaming passive-aggression"),
    ("It's totally fine. I love re-architecting systems on 2 hours of notice.", "Sarcastic overtime complaint"),
    ("I'm sure you had a very good reason to ignore all my Slack pings today.", "Passive-aggressive interrogation")
]

rahul_normal_samples = [
    ("Hey Rahul, let's sync at 3 PM for the project update.", "Neutral calendar/sync request"),
    ("Can you review PR #412 when you have a moment? Thanks!", "Standard polite code review request"),
    ("Morning! Here are the updated API documentation links.", "Helpful work update"),
    ("Great work on the caching layer optimization, latency dropped 40%!", "Genuine praise / positive collaboration"),
    ("Are we still on for the customer demo at 4:30 PM?", "Standard scheduling check"),
    ("Merged your branch into staging. Everything looks clean.", "Informational technical update"),
    ("Let's grab coffee in the cafeteria after the standup.", "Friendly casual invite"),
    ("Attached the updated database schema diagram for review.", "Work attachment share"),
    ("Happy birthday Rahul! Have a great day ahead!", "Friendly personal celebration"),
    ("Did you catch the new release notes for PyTorch 2.4?", "Casual tech conversation"),
    ("Just submitted the pull request for the authentication service.", "Direct work progress update"),
    ("Thanks for the pointer yesterday, the build passed cleanly.", "Polite gratitude"),
    ("Let me know if you need any help with the Docker configuration.", "Cooperative offer of support"),
    ("The client approved the design proposal! Great job everyone.", "Positive business milestone"),
    ("Can you share the Figma link for the mobile layout?", "Standard asset inquiry"),
    ("Updated the Jira ticket status to In Progress.", "Routine workflow tracking"),
    ("Heading to lunch with the frontend team if you want to join.", "Friendly lunch invitation"),
    ("The latency benchmark script finished running, results look solid.", "Productive test update"),
    ("Let's do a quick 5-min huddle on Slack to align on the schema.", "Productive collaboration request"),
    ("Added unit tests covering the edge cases we discussed.", "Quality engineering update"),
    ("Here is the meeting summary from the leadership sync.", "Informational minutes share"),
    ("Can you approve the release build for v1.8.0?", "Standard release authorization"),
    ("I will be working from home tomorrow due to internet maintenance.", "Routine administrative notice"),
    ("The backend migration to Kubernetes is scheduled for 10 PM.", "Operational deployment schedule"),
    ("Check out this article on KleidiAI acceleration on ARM CPUs.", "Educational technical article"),
    ("Everything is looking good for the quarterly review next week.", "Positive status update"),
    ("Do you have the contact details for the billing department?", "Factual operational query"),
    ("Created a new branch `feature/auth-v2` for the upcoming sprint.", "Standard git workflow notice"),
    ("Let me know what time works best for you to walk through the slides.", "Polite meeting coordination"),
    ("We reached 99.99% uptime this month across all production services!", "Positive milestone announcement"),
    ("Shared the Google Doc with comments on the architecture draft.", "Collaborative document review"),
    ("The new hire starts on Monday, let's prepare the onboarding checklist.", "Team operational planning"),
    ("Updated the README with setup instructions for Windows.", "Documentation update"),
    ("Can you push your latest commit so I can test locally?", "Direct engineering request"),
    ("The conference tickets got approved by management!", "Positive team news"),
    ("Let's review sprint velocity in tomorrow's retrospective.", "Standard agile process"),
    ("Sent you the invite for the quarterly planning workshop.", "Calendar event notification"),
    ("The load test sustained 10,000 requests/sec without errors.", "Successful performance benchmark"),
    ("Just sent over the revised vendor agreement for review.", "Business operational document"),
    ("Looking forward to the hackathon this Friday!", "Enthusiastic team event message")
]

for text, rsn in rahul_pa_samples:
    add_case("Group 1: K2 Deep AI", "rule_1", "Rahul Frustration Filter", "WhatsApp", "Rahul", text, False, False, rsn, "Hard")

for text, rsn in rahul_normal_samples:
    add_case("Group 1: K2 Deep AI", "rule_1", "Rahul Frustration Filter", "WhatsApp", "Rahul", text, False, True, rsn, "Medium")

# ------------------------------------------------------------------------------
# Rule 2: Boss Panic / Urgent Crisis -> ALERT (85 cases)
# Target: 45 ALERT (genuine panic / crisis), 40 MUTE (casual / humor / non-crisis)
# ------------------------------------------------------------------------------
boss_panic_samples = [
    ("Production database is corrupt and customers cannot log in, call me immediately!", "Critical data corruption / sev-0 emergency"),
    ("SEV-1 OUTAGE: Payment gateway is failing 100% of transactions. All hands on deck now!", "Total revenue-halting outage"),
    ("Our AWS root keys were leaked on GitHub! Revoke them instantly before we get compromised!", "Severe security breach emergency"),
    ("The VP is on the phone screaming. The main dashboard is completely white-screened in prod!", "Executive escalation crisis"),
    ("EMERGENCY: Ransomware detected on backup server 04. Disconnect all network bridges immediately!", "Cybersecurity ransomware crisis"),
    ("Why is customer data showing up in public search results?? Call me right this second!", "Catastrophic privacy / data breach"),
    ("We are losing $50k every minute the checkout API is down. Get the entire backend team on bridge!", "Extreme financial loss outage"),
    ("The mobile app was pulled from Google Play Store for policy violation! We need an emergency build!", "Critical store takedown emergency"),
    ("All Kubernetes clusters in US-East are in CrashLoopBackOff. Nothing is routing!", "Infrastructure catastrophe"),
    ("I need you on Zoom immediately. The client threatened to terminate our contract within the hour!", "Severe existential business crisis"),
    ("CRITICAL: SSL certificates expired across all public domains. Users getting security warnings!", "Global access shutdown crisis"),
    ("Our main database replica broke sync and primary is out of disk space at 99.9%!", "Imminent database crash panic"),
    ("The live demo to the venture capital board is in 10 minutes and staging is failing auth!", "High-stakes investor demo failure"),
    ("Someone deleted the production users table! Restore the WAL backups right now!", "Catastrophic database loss panic"),
    ("Security team flagged unauthorized SSH access from an unknown IP to our bastion host!", "Active intrusion emergency"),
    ("All background workers crashed and message queue has 5 million unprocessed jobs piling up!", "System overload breakdown"),
    ("URGENT: Legal team called. We must take down the staging environment immediately due to subpoena!", "Urgent legal compliance crisis"),
    ("The API rate limiter failed and we are getting hammered by a massive DDoS attack right now!", "Active DDoS infrastructure crisis"),
    ("Cloud bill just jumped to $80,000 in 2 hours due to an infinite recursion bug. Kill all workers!", "Financial runaway bug crisis"),
    ("Customer support is receiving 500 calls a minute about lost orders. Fix the queue now!", "Customer support meltdown crisis"),
    ("Why is production returning HTTP 500 on every single endpoint? Call me right now!", "Total service outage"),
    ("The automated deployment script just wiped the staging storage bucket! Stop the prod pipeline!", "Destructive pipeline bug panic"),
    ("Emergency bridge is active. Major data center power failure affecting our primary zone.", "Infrastructure physical disaster"),
    ("Our domain DNS records disappeared! The entire website is unreachable worldwide!", "Global DNS blackout crisis"),
    ("CRITICAL: Hacker posted our API keys on Twitter. Rotate all credentials instantly!", "Public security exposure emergency"),
    ("The mobile app crashes on launch for all Android 14 users after today's update. Roll back NOW!", "Mass customer app crash emergency"),
    ("I need every engineer online in 2 minutes. The government compliance audit failed!", "Critical regulatory crisis"),
    ("All webhooks to Stripe are timing out and subscriptions are cancelling automatically!", "Revenue loss panic"),
    ("The main cache cluster ran out of memory and collapsed the master database under load!", "Cascading infrastructure failure"),
    ("Emergency: Email server is blacklisted globally as a spam relay. Zero emails going through!", "Global communication collapse"),
    ("Why did the deployment push unencrypted passwords to the logs?? Sanitize everything now!", "Critical compliance vulnerability"),
    ("The load balancer certificates are revoked. Emergency deployment required within 15 minutes!", "Urgent certificate revocation"),
    ("Database connection pool exhausted. Every user query is hanging indefinitely!", "Database deadlocking emergency"),
    ("Our storage disk filled to 100% and SQLite databases are throwing I/O write errors!", "Storage corruption panic"),
    ("URGENT: Production Redis instance dropped all user sessions. Everyone got logged out!", "Mass user disconnection panic"),
    ("The fraud detection model went rogue and is auto-banning paying enterprise accounts!", "Business damaging bug crisis"),
    ("Drop whatever you are doing and join the emergency bridge. Production is on fire.", "Direct emergency summon"),
    ("We have an active data leak on the analytics webhook. Pull the plug immediately!", "Active data exfiltration panic"),
    ("The CEO is demanding a resolution in 5 minutes for the broken enterprise portal.", "Executive pressure emergency"),
    ("Critical zero-day vulnerability announced for our web framework. Patch it this hour!", "Urgent zero-day exploit patch"),
    ("Why are transactions double-charging customer credit cards?? Kill the billing service NOW!", "Catastrophic double-billing panic"),
    ("The production SSL proxy crashed and traffic is completely unencrypted. Stop routing!", "Severe compliance violation emergency"),
    ("We just lost all telemetry and monitoring metrics across every cluster. We are flying blind!", "Total monitoring blackout panic"),
    ("Emergency: The database failover didn't trigger and primary is dead. Manual failover needed NOW!", "Manual disaster recovery crisis"),
    ("Customer data export dumped wrong tenant data! Major multi-tenant breach! Call me!", "Severe multi-tenant isolation breach")
]

boss_casual_samples = [
    ("OMG you have to check out this crazy funny video right now haha!", "Casual viral video share"),
    ("Good morning team, let's make this week productive and reach our sprint goals.", "Routine motivational greeting"),
    ("Who wants to grab tacos for team lunch today around 1 PM?", "Casual team lunch invitation"),
    ("Great presentation at the all-hands yesterday, really appreciated the clear diagrams.", "Positive non-crisis praise"),
    ("FYI: Office will be closed next Monday for the national holiday.", "Routine administrative notice"),
    ("Please remember to submit your monthly expense receipts by Friday.", "Standard administrative reminder"),
    ("Check out this interesting blog post about AI agents in mobile applications.", "Casual industry reading share"),
    ("Happy Friday everyone! Enjoy your weekend and recharge.", "Casual weekend greeting"),
    ("Here are the updated slides for next month's product roadmap preview.", "Routine non-urgent roadmap share"),
    ("Does anyone have a spare USB-C charger in the conference room?", "Mundane office equipment query"),
    ("The team dinner is confirmed for 7 PM at the Italian bistro.", "Casual social gathering notice"),
    ("Just sharing the latest quarterly company newsletter.", "Informational newsletter broadcast"),
    ("Welcome our new product manager joining the team today!", "Team welcome announcement"),
    ("Coffee machine on the 3rd floor is fixed finally!", "Humorous mundane office news"),
    ("Please take 5 minutes to fill out the annual workplace satisfaction survey.", "Routine HR survey request"),
    ("Check out the photos from yesterday's company offsite event!", "Casual photo sharing"),
    ("Let's do our 1-on-1 catchup tomorrow afternoon instead of today.", "Routine meeting reschedule"),
    ("Great job on wrapping up the sprint tasks ahead of schedule.", "Routine positive feedback"),
    ("Reminder: Flu vaccination drive in the office lobby tomorrow.", "Mundane wellness reminder"),
    ("Who is bringing snacks for the game night this Thursday?", "Social activity coordination"),
    ("Interesting podcast on startup scaling, sharing for those interested.", "Optional content share"),
    ("Please review the updated travel policy document when convenient.", "Low-priority policy notice"),
    ("Let me know if you want tickets to the upcoming tech conference.", "Casual ticket offer"),
    ("The office air conditioning is being serviced this afternoon.", "Facility maintenance notice"),
    ("Congrats to the design team on winning the mobile UX award!", "Positive company news"),
    ("Don't forget to lock the meeting rooms when leaving for the day.", "Routine office etiquette reminder"),
    ("Sharing the link to the recording of yesterday's town hall meeting.", "Recorded video link share"),
    ("Hope everyone had a relaxing long weekend with family.", "Casual friendly greeting"),
    ("We got some branded company hoodies in the mailroom, go grab yours!", "Fun company swag notice"),
    ("Let's review the draft design mockups sometime next week.", "Non-urgent design review proposal"),
    ("The parking lot will be repaved this weekend, please park on level 2.", "Mundane parking notice"),
    ("Found a pair of blue headphones in conference room B.", "Lost and found office message"),
    ("Reminder to update your Slack status when out of the office.", "Routine workplace communication tip"),
    ("Anyone interested in joining the company football league this season?", "Sports hobby invitation"),
    ("Great discussion in today's brainstorm session, lots of good ideas.", "Casual positive reflection"),
    ("Happy work anniversary to our lead architect on 5 years with the company!", "Anniversary celebration message"),
    ("Sharing the slide deck from the marketing presentation for reference.", "Informational reference material"),
    ("Please make sure to approve pending timesheets before the end of the month.", "Routine administrative check"),
    ("The pantry has fresh donuts today courtesy of the sales team!", "Office snack notice"),
    ("Let's celebrate our Q3 revenue numbers with pizza this Friday!", "Social celebration plan")
]

for text, rsn in boss_panic_samples:
    add_case("Group 1: K2 Deep AI", "rule_2", "Boss Panic / Urgent Crisis", "Slack", "Boss", text, False, True, rsn, "Hard")

for text, rsn in boss_casual_samples:
    add_case("Group 1: K2 Deep AI", "rule_2", "Boss Panic / Urgent Crisis", "Slack", "Boss", text, False, False, rsn, "Medium")

# ------------------------------------------------------------------------------
# Rule 3: Sarah Appreciation / Thanking -> ALERT (85 cases)
# Target: 45 ALERT (thanking / appreciation), 40 MUTE (mundane / routine)
# ------------------------------------------------------------------------------
sarah_thanks_samples = [
    ("Thank you so much for helping with the client presentation today, outstanding work!", "Direct heartfelt gratitude for presentation help"),
    ("I truly appreciate you staying late to help fix that tricky CSS bug for our demo.", "Appreciation for overtime assistance"),
    ("Huge thanks for your leadership on the sprint deliverable, you crushed it!", "Enthusiastic professional appreciation"),
    ("Thank you for stepping in when I was sick and covering the stakeholder meeting.", "Gratitude for crisis coverage"),
    ("I really appreciate how clearly you explained the API architecture to the client.", "Appreciation for client communication skill"),
    ("Thanks a million for the quick code review, it unblocked our whole frontend team!", "Enthusiastic gratitude for timely unblocking"),
    ("I just wanted to say thank you for always being so supportive of the junior devs.", "Appreciation for mentorship and support"),
    ("Your help on the database optimization was invaluable, thank you so much!", "High appreciation for technical contribution"),
    ("Thanks for catching that security vulnerability before the release went live!", "Gratitude for critical bug prevention"),
    ("I appreciate your quick thinking during the system demo today, saved the deal!", "High-value commercial appreciation"),
    ("Thank you for organizing the team workshop, it was informative and engaging.", "Appreciation for event leadership"),
    ("Big thanks for refactoring the legacy authentication service, so much cleaner now!", "Engineering quality appreciation"),
    ("I wanted to personally thank you for your patience while training our new intern.", "Appreciation for patient mentorship"),
    ("Thanks for sending over those benchmark numbers so quickly, super helpful!", "Gratitude for rapid turnaround"),
    ("I really appreciate your constructive feedback on my pull request today.", "Appreciation for thoughtful peer review"),
    ("Thank you for backing me up in the executive meeting regarding the roadmap.", "Gratitude for political/team support"),
    ("Kudos to you for the smooth Kubernetes migration, zero downtime achieved!", "Celebratory appreciation"),
    ("Thank you for drafting the post-mortem report so thoroughly and honestly.", "Appreciation for thorough documentation"),
    ("I appreciate you taking the time to review my design portfolio draft.", "Personal professional gratitude"),
    ("Thanks for fixing the CI pipeline on the weekend, we truly appreciate your dedication!", "High appreciation for out-of-hours fix"),
    ("A massive thank you for all your hard work on the Q2 product launch!", "Broad milestone appreciation"),
    ("I'm so grateful for your assistance with the compliance audit documentation.", "Heartfelt compliance support gratitude"),
    ("Thank you for simplifying the onboarding guide, it made a huge difference.", "Appreciation for impactful documentation"),
    ("Really appreciate your thoughtful suggestions in today's architecture brainstorm.", "Appreciation for creative ideation"),
    ("Thanks so much for resolving the customer escalation ticket within 10 minutes!", "Rapid customer support appreciation"),
    ("I appreciate how reliable you always are with sprint commitments. Great work!", "Appreciation for consistency and reliability"),
    ("Thank you for preparing the financial model spreadsheets ahead of schedule.", "Appreciation for early delivery"),
    ("Huge shoutout and thanks for troubleshooting the memory leak on our production node!", "High appreciation for critical debugging"),
    ("I want to express my sincere appreciation for your partnership on this project.", "Formal professional appreciation"),
    ("Thank you for the wonderful recommendation letter you wrote for me!", "Personal professional gratitude"),
    ("Thanks for setting up the automated testing framework, it saves us hours daily!", "Appreciation for toolchain productivity"),
    ("I appreciate your honesty regarding the project timeline limitations.", "Appreciation for candor and integrity"),
    ("Thank you for delivering the customer feature with such attention to detail.", "Appreciation for craftsmanship"),
    ("So thankful for your support during a very stressful launch week!", "Emotional resilience appreciation"),
    ("Thanks for sharing your knowledge on Rust concurrency with the rest of the team.", "Knowledge sharing appreciation"),
    ("I really appreciate your proactive approach to monitoring our cloud infrastructure.", "Proactivity appreciation"),
    ("Thank you for coordinating with the design agency so seamlessly.", "Cross-team collaboration gratitude"),
    ("Thanks for making the extra effort to document all the GraphQL endpoints!", "Thoroughness appreciation"),
    ("I truly appreciate you answering all my questions so patiently this morning.", "Helpfulness appreciation"),
    ("Thank you for always bringing positive energy and solutions to our team!", "Cultural positive impact appreciation"),
    ("Much appreciated for jumping on the customer call on short notice!", "Flexibility gratitude"),
    ("Thanks for finding the root cause of that intermittent network dropout!", "Diagnostic skill appreciation"),
    ("I appreciate the high standard of code quality you maintain across the repo.", "Code craftsmanship appreciation"),
    ("Thank you for being such a wonderful teammate and collaborator this past year!", "Long-term partnership gratitude"),
    ("A heartfelt thank you for helping me prepare for the technical interview!", "Mentorship gratitude")
]

sarah_mundane_samples = [
    ("Where did you leave the office keys?", "Routine lost item inquiry"),
    ("Did you submit your timesheet for this week yet?", "Routine administrative reminder"),
    ("Can you send me the link to the Figma file for project Horizon?", "Standard asset link request"),
    ("Are you coming to the team standup in room 3B?", "Meeting attendance query"),
    ("Do you know when the sprint planning meeting starts?", "Schedule inquiry"),
    ("Is the staging server currently running the latest build?", "Technical status check"),
    ("What was the password for the test Wi-Fi network?", "Credential query"),
    ("Can you reassign Jira ticket #204 to the frontend backlog?", "Routine task management"),
    ("Did the client respond to our email inquiry from yesterday?", "Business follow-up query"),
    ("Please add the meeting notes to the shared drive.", "Administrative task request"),
    ("What time are you leaving the office today?", "Casual commute inquiry"),
    ("Is the PDF export feature working on your local machine?", "Local environment check"),
    ("Did you see the announcement about the upcoming fire drill?", "Informational query"),
    ("Can you check if the webhook endpoint is receiving test pings?", "Routine debugging verification"),
    ("Who is the secondary on-call engineer for this weekend?", "On-call roster check"),
    ("Please review slide 4 on the pitch deck when you get a chance.", "Standard work review request"),
    ("Where can I find the brand guidelines logo vector files?", "Asset location inquiry"),
    ("Do you have the phone number for the IT support desk?", "Support contact inquiry"),
    ("Did you push the updated translation strings to master?", "Git status query"),
    ("Are we having the retrospective before or after lunch?", "Calendar coordination"),
    ("Can you print 5 copies of the agenda for the client visit?", "Office administrative request"),
    ("What version of Node.js are we supporting in the new SDK?", "Technical specification question"),
    ("Did the vendor send the updated quote for cloud licenses?", "Procurement status check"),
    ("Please update your profile photo on the internal directory.", "Administrative compliance request"),
    ("Is anyone using the large conference room at 2 PM?", "Room booking query"),
    ("Did you receive the calendar invite for the security training?", "Invite receipt check"),
    ("Can you share the spreadsheet from last month's user metrics?", "Data file request"),
    ("What is the zoom link for the quarterly all-hands?", "Meeting link query"),
    ("Did the QA team finish testing the checkout workflow?", "QA progress inquiry"),
    ("Please remember to turn off the test monitors before leaving.", "Office etiquette request"),
    ("Are you working remotely tomorrow or coming to the office?", "Workplace attendance query"),
    ("Did the Docker image build complete without errors?", "Build verification question"),
    ("Where is the documentation for the payment refund API?", "Documentation location query"),
    ("Can you forward me the email thread with the vendor?", "Email forwarding request"),
    ("What is the IP address for the internal staging database?", "Environment connection query"),
    ("Did you log your PTO days in the HR portal?", "HR tracking inquiry"),
    ("Are we deploying the patch before or after the customer demo?", "Release timing question"),
    ("Can you check the logs for any 404 errors on the blog page?", "Routine log inspection"),
    ("Who approved the latest dependency update in package.json?", "Code governance check"),
    ("Is the coffee machine on 2nd floor working right now?", "Casual office facility inquiry")
]

for text, rsn in sarah_thanks_samples:
    add_case("Group 1: K2 Deep AI", "rule_3", "Sarah Appreciation Rule", "Teams", "Sarah", text, False, True, rsn, "Medium")

for text, rsn in sarah_mundane_samples:
    add_case("Group 1: K2 Deep AI", "rule_3", "Sarah Appreciation Rule", "Teams", "Sarah", text, False, False, rsn, "Easy")

# ------------------------------------------------------------------------------
# Rule 4: Manager Demanding / Angry Email -> ALERT (85 cases)
# Target: 45 ALERT (demanding / angry escalation), 40 MUTE (routine email / newsletter)
# ------------------------------------------------------------------------------
manager_angry_samples = [
    ("Why was this task not completed yesterday? I demand an explanation right now.", "Overt anger and aggressive accountability demand"),
    ("This level of negligence is completely unacceptable. Report to my office immediately!", "Severe angry reprimand and summons"),
    ("I am furious that our biggest client found this bug before our own QA team did.", "Explicit furious emotion over failure"),
    ("Explain why the sprint deliverable was missed without any prior notice to leadership.", "Demanding escalation on missed commitment"),
    ("Your refusal to follow engineering guidelines has caused a major compliance incident.", "Accusatory hostile management email"),
    ("I expect a detailed incident root-cause analysis on my desk by 8 AM tomorrow without fail.", "Demanding strict deadline with stern tone"),
    ("Why did you push code directly to main without review?? This is a gross violation of protocol.", "Angry interrogation on process violation"),
    ("How many times do I have to remind you to update the project timeline before the board meeting?", "Exasperated aggressive management tone"),
    ("The client is threatening legal action over the downtime you caused. Call me this minute.", "Severe high-stakes anger and urgency"),
    ("I will not tolerate repeated missed deadlines. Consider this your final written warning.", "Disciplinary aggressive escalation"),
    ("Why is the staging environment still broken after 3 days of zero progress??", "Frustrated aggressive query on lack of progress"),
    ("Your communication on this project has been abysmal. I demand daily status reports henceforth.", "Demanding micromanagement driven by anger"),
    ("Who authorized this architecture overhaul? Explain yourself immediately.", "Authoritarian aggressive interrogation"),
    ("This demo was an absolute embarrassment in front of the executive team.", "Harsh direct reprimand"),
    ("I am appalled by the lack of testing that went into this production release.", "Indignant angry feedback on engineering quality"),
    ("You completely dropped the ball on the customer migration. Fix it today or face consequences.", "Threatening aggressive escalation"),
    ("Why was I left in the dark about the security incident that occurred over the weekend??", "Angry confrontation over lack of communication"),
    ("I demand to know who approved merging this broken branch into the release candidate.", "Hostile accountability demand"),
    ("The quality of this deliverable is disgraceful. Redo the entire document by 5 PM.", "Harsh degrading management tone"),
    ("Stop making excuses for missed milestones and start delivering results.", "Dismissive hostile rebuke"),
    ("Why are customer tickets sitting unassigned for 48 hours?? This is intolerable.", "Angry operational reprimand"),
    ("I expect you to be in the office at 8:00 AM sharp to explain this budget discrepancy.", "Strict authoritative demand"),
    ("Your sloppy code caused a 4-hour system outage. I want answers right now.", "Direct blame and angry inquiry"),
    ("How could you deploy on a Friday afternoon against my explicit instructions??", "Furious confrontation over insubordination"),
    ("I am deeply displeased with your performance and attitude in today's client sync.", "Formal authoritative displeasure"),
    ("This is the third time this week the deployment pipeline failed. Fix it permanently today.", "Demanding operational mandate"),
    ("I demand an immediate audit of all commits pushed by your team this sprint.", "Aggressive compliance demand"),
    ("Do not leave the office today until the data corruption issue is 100% resolved.", "Authoritative overtime demand"),
    ("Your failure to notify the stakeholder has jeopardized our $2M renewal contract.", "High-stakes accusatory escalation"),
    ("Why were the test cases skipped for the billing engine?? This is complete incompetence.", "Aggressive questioning with harsh insult"),
    ("I will not accept any more delays on the mobile release. Ship it tomorrow morning.", "Demanding non-negotiable ultimatum"),
    ("Explain why the cloud infrastructure costs doubled this month without authorization.", "Angry financial confrontation"),
    ("You have 1 hour to revert the breaking changes before I escalate this to the VP.", "Time-sensitive aggressive threat"),
    ("I am sick and tired of hearing promises with zero execution. Deliver the PR today.", "Exasperated hostile demand"),
    ("Why did you dismiss the customer's bug report as 'not reproducible' without checking logs??", "Angry reprimand on customer neglect"),
    ("I expect an immediate apology to the client for the unprofessional remarks in the ticket.", "Demanding interpersonal mandate"),
    ("This catastrophic data loss should have been prevented. Meeting in my office now.", "Urgent crisis confrontation"),
    ("Who gave permission to turn off the automated security scanners?? Answer immediately.", "Alarmed angry interrogation"),
    ("I demand a complete breakdown of every hour logged on this ticket.", "Aggressive time-tracking scrutiny"),
    ("You missed the regulatory filing deadline. This is a severe failure of duty.", "Severe legal compliance reprimand"),
    ("Why are you ignoring my urgent emails regarding the system outage?? Pick up the phone!", "Angry confrontation over responsiveness"),
    ("I am revoking your production deployment access until you complete re-training.", "Punitive managerial action"),
    ("Explain why the API response latency increased by 300% after your latest commit.", "Direct technical confrontation"),
    ("This report is filled with inaccuracies. Rewrite the entire thing before the meeting.", "Harsh uncompromising mandate"),
    ("I have zero confidence in this deployment plan. Present a corrected version in 30 minutes.", "Severe vote of no confidence and demand")
]

manager_routine_samples = [
    ("Please find attached the weekly corporate newsletter for your perusal.", "Routine company newsletter"),
    ("Reminder: Annual performance review self-assessments are due next Friday.", "Standard HR cycle reminder"),
    ("Sharing the agenda for our upcoming quarterly team planning session.", "Standard meeting agenda share"),
    ("FYI: Updated company travel and expense policies are now published on the intranet.", "Informational policy announcement"),
    ("Please complete the mandatory cybersecurity training module by end of month.", "Routine compliance training notice"),
    ("Here is the summary of key metrics and achievements from our Q2 all-hands.", "Informational business summary"),
    ("Reminder: The office will be closed on Monday in observance of the public holiday.", "Standard holiday schedule notice"),
    ("Please remember to submit your hardware refresh requests before the fiscal deadline.", "Routine equipment procurement"),
    ("Welcome to our new team members joining the engineering division this week!", "New hire welcome broadcast"),
    ("Sharing the approved vacation calendar for the upcoming winter holidays.", "Administrative schedule planning"),
    ("Please take a few moments to participate in the annual employee engagement survey.", "Standard HR survey request"),
    ("Here are the slides from yesterday's executive roadmap presentation for reference.", "Reference document sharing"),
    ("Reminder to submit your monthly health and wellness expense claims.", "Routine benefit claim notice"),
    ("The corporate office building will undergo routine power maintenance this Saturday.", "Facility maintenance broadcast"),
    ("Sharing the latest updates on our corporate sustainability and green initiatives.", "Company sustainability newsletter"),
    ("Please ensure all client confidentiality agreements are refreshed in the document vault.", "Routine compliance maintenance"),
    ("The quarterly town hall meeting is scheduled for next Thursday at 2 PM.", "Standard town hall notification"),
    ("Here is the updated engineering team directory and contact sheet.", "Informational directory update"),
    ("Reminder to log all outstanding paid time off in the HR system before month-end.", "Routine PTO logging reminder"),
    ("Sharing an interesting case study on our recent cloud migration published on Forbes.", "External PR / marketing share"),
    ("Please review the proposed sprint calendar for the upcoming quarter.", "Planning calendar proposal"),
    ("The cafeteria is introducing a new organic lunch menu starting next week.", "Mundane cafeteria update"),
    ("Reminder: Flu shots available in the medical room on the 1st floor today.", "Office health benefit notice"),
    ("Congratulations to the mobile team on achieving 1 million downloads this milestone!", "Positive milestone announcement"),
    ("Please find attached the minutes from yesterday's department sync.", "Meeting minutes distribution"),
    ("Sharing the list of upcoming internal technical webinars and workshops.", "Optional learning event schedule"),
    ("Reminder to ensure your emergency contact details are up to date in Workday.", "Routine HR profile check"),
    ("The parking garage security gates will be upgraded over the weekend.", "Facility security upgrade notice"),
    ("Here are the updated brand assets and PowerPoint templates for corporate decks.", "Corporate asset distribution"),
    ("Please submit any nominations for the quarterly peer recognition awards by Friday.", "Employee recognition reminder"),
    ("Reminder to review and sign the updated confidentiality agreement in DocuSign.", "Routine document signing request"),
    ("The annual benefits enrollment window is now open until the 15th of next month.", "Benefits enrollment announcement"),
    ("Sharing the quarterly financial results press release for team awareness.", "Public financial press release"),
    ("Please remember to mute your microphones when not speaking during large all-hands.", "Standard video call etiquette reminder"),
    ("Here are the instructions for accessing the new cloud sandbox environment.", "Technical setup documentation share"),
    ("The company store has added new branded merchandise for the summer season.", "Company merchandise notice"),
    ("Reminder: Clean desk policy inspection will occur at the end of the month.", "Office etiquette compliance reminder"),
    ("Sharing the recording of the guest speaker lecture on distributed systems.", "Educational video recording link"),
    ("Please confirm your attendance for the end-of-year team banquet.", "Social event RSVP request"),
    ("Wishing everyone a wonderful and productive week ahead!", "Warm routine weekly greeting")
]

for text, rsn in manager_angry_samples:
    add_case("Group 1: K2 Deep AI", "rule_4", "Manager Demanding / Anger Rule", "Gmail", "Manager", text, False, True, rsn, "Hard")

for text, rsn in manager_routine_samples:
    add_case("Group 1: K2 Deep AI", "rule_4", "Manager Demanding / Anger Rule", "Gmail", "Manager", text, False, False, rsn, "Easy")

# ------------------------------------------------------------------------------
# Rule 5: Vikram Disappointment / Regret -> MUTE (80 cases)
# Target: 40 MUTE (disappointment / regret), 40 ALERT (casual / positive / normal chat)
# ------------------------------------------------------------------------------
vikram_disappoint_samples = [
    ("I feel so let down by how the project turned out, really disappointed with the outcome.", "Direct expression of disappointment with project"),
    ("I deeply regret ever agreeing to take on this client contract. What a total disaster.", "Explicit regret over business decision"),
    ("It's so sad and frustrating that our hard work was completely dismissed by management.", "Disappointment and sadness over rejection"),
    ("I really regret not speaking up during the planning meeting, now we are stuck with this mess.", "Self-regret over silence"),
    ("So disappointed that the promotion went to someone who joined 3 months ago.", "Career disappointment and bitterness"),
    ("I feel let down by the whole team. Nobody helped when the deployment was failing.", "Disappointment with peer collaboration"),
    ("It's a shame we lost the customer account after spending 6 months pitching them.", "Regret over lost business"),
    ("I regret putting in all those 60-hour weeks just to have the product killed.", "Bitter regret over wasted overtime"),
    ("Really disappointed with my performance in today's technical interview, I blew it.", "Personal regret and failure reflection"),
    ("I feel so let down by our architect's decision to scrap our entire codebase.", "Disappointment with leadership decision"),
    ("It saddens me to see how toxic our team culture has become over the last sprint.", "Disappointment with workplace culture"),
    ("I regret trusting the vendor's promises. Their API is completely broken.", "Regret regarding external vendor"),
    ("So disappointed that our talk proposal was rejected by the Python conference committee.", "Conference submission disappointment"),
    ("I feel like all our effort was for nothing. Really regret taking this path.", "Existential work regret and despair"),
    ("It's disappointing that we couldn't even achieve 50% of our quarterly revenue targets.", "Financial target disappointment"),
    ("I regret not taking that other job offer when I had the chance last year.", "Career path regret"),
    ("Feeling let down that the hackathon judges didn't even test our live demo.", "Competition disappointment"),
    ("I am so disappointed with the buggy release we shipped to customers yesterday.", "Product quality disappointment"),
    ("I regret spending all weekend debugging code that got deleted on Monday morning.", "Wasted time regret"),
    ("It hurts to see how little credit our backend team received for the launch.", "Disappointment over lack of recognition"),
    ("I feel so let down by the company's decision to cancel all annual bonuses.", "Financial compensation disappointment"),
    ("Really regret not writing unit tests before refactoring the payment module.", "Engineering regret"),
    ("Disappointed that our feature was pushed back to Q4 roadmap again.", "Roadmap delay disappointment"),
    ("I feel let down by the lack of transparency from executive leadership.", "Disappointment in governance"),
    ("I regret recommending that library, it has caused endless memory leaks.", "Technical recommendation regret"),
    ("Sad to see our longest-tenured engineer resign today. Really disappointed.", "Loss of colleague disappointment"),
    ("I feel so let down by how the customer escalation was handled by management.", "Operational escalation disappointment"),
    ("Regretting my decision to volunteer for the on-call shift this holiday weekend.", "On-call duty regret"),
    ("Disappointed that the server migration caused so much unexpected customer downtime.", "Migration failure disappointment"),
    ("I regret not pushing back harder against the unrealistic product deadlines.", "Project management regret"),
    ("Feeling let down by the poor turnout at our technical community workshop.", "Community event disappointment"),
    ("So disappointed with the review ratings our mobile app received on the App Store.", "App Store rating disappointment"),
    ("I regret spending our entire innovation budget on a single unproven technology.", "Budget allocation regret"),
    ("It is deeply disappointing that our bug report was marked as 'won't fix'.", "Triage decision disappointment"),
    ("I feel let down that my pull request was closed without any constructive feedback.", "Peer review disappointment"),
    ("I regret not backing up the staging database before running the migration script.", "Data loss regret"),
    ("Disappointed that our team's proposal for the new API standard was rejected.", "Standards proposal disappointment"),
    ("I feel so let down by the lack of support from our cross-functional partners.", "Cross-functional disappointment"),
    ("I truly regret how I reacted during the heated sprint retrospective meeting.", "Interpersonal conduct regret"),
    ("So disappointed that our product launch date was postponed for the third time.", "Repeated delay disappointment")
]

vikram_normal_samples = [
    ("Hey, let's grab coffee in the cafeteria.", "Casual friendly social invite"),
    ("Are you coming to the football match tonight at 7 PM?", "Casual sports invitation"),
    ("Congrats on the successful deployment, great job on the caching layer!", "Positive congratulatory message"),
    ("Do you have the link to the sprint planning Zoom room?", "Routine meeting coordination"),
    ("Heading to the food truck for lunch if you want to join.", "Friendly lunch invite"),
    ("Check out this new open-source vector database on GitHub, looks awesome!", "Excited technology share"),
    ("Are you free for a quick 5-min sync about the API endpoints?", "Standard work collaboration query"),
    ("Just sent you the revised architecture diagram for your review.", "Work document share"),
    ("Happy Friday! Any plans for the weekend?", "Casual friendly weekend conversation"),
    ("The new monitor for your desk arrived in the IT department.", "Equipment arrival notice"),
    ("Let's test the new WebSocket streaming feature together this afternoon.", "Collaborative engineering work"),
    ("Did you see the score for yesterday's Champions League game??", "Casual sports chat"),
    ("Great presentation in the sprint demo today, very clear and crisp.", "Positive praise"),
    ("Can you share the Docker command you used to spin up the local replica?", "Technical help inquiry"),
    ("I booked the conference room for our brainstorming session at 3 PM.", "Meeting room confirmation"),
    ("Happy birthday Vikram! Hope you have a wonderful celebration today!", "Birthday greeting"),
    ("Let's grab a slice of pizza from the kitchen, they just delivered fresh pies.", "Casual snack invite"),
    ("The client approved our proposed milestone schedule without changes!", "Positive business news"),
    ("Are you going to the developer meetup downtown tomorrow evening?", "Community event query"),
    ("I pushed the bug fix for issue #304 to the testing branch.", "Standard engineering update"),
    ("Let me know when you're done with the staging environment so I can test.", "Environment sharing coordination"),
    ("Here are the flight details for our client visit next month.", "Travel coordination document"),
    ("The database benchmark results beat our previous record by 25%!", "Exciting performance milestone"),
    ("Do you want to pair program on the authentication service refactor?", "Collaborative coding offer"),
    ("Just updated the Wiki documentation with setup instructions for macOS.", "Documentation notice"),
    ("Let's order some bubble tea for the afternoon energy boost!", "Casual team treat proposal"),
    ("Did you try the new dark mode theme on our web dashboard? Looks sleek.", "Product feedback query"),
    ("The conference organizer accepted our workshop proposal for October!", "Excited event acceptance"),
    ("Can you approve my time-off request in the HR system when you have a sec?", "Standard administrative request"),
    ("Looking forward to the team dinner at the rooftop restaurant on Thursday.", "Positive anticipation of social event"),
    ("The load balancer is handling 50k requests with zero dropped packets!", "Technical success milestone"),
    ("Do you want to grab some fresh air and take a walk outside the office?", "Casual wellness invite"),
    ("Check out the awesome UI mockups the design team just posted.", "Design showcase share"),
    ("The customer sent a glowing testimonial about our customer service response!", "Positive customer feedback"),
    ("Let me know if you need a ride home after work today.", "Friendly personal offer"),
    ("We just hit 100,000 active users on the platform today! Huge milestone!", "Celebratory company milestone"),
    ("Can you review the pull request for the dark mode toggle when free?", "Routine code review request"),
    ("The automated CI tests passed in under 2 minutes with the new caching.", "Productivity win update"),
    ("Let's do a quick coffee run before the 4 PM product sync.", "Casual coffee invite"),
    ("Excited to start working on the new AI agent integration next sprint!", "Enthusiastic project message")
]

for text, rsn in vikram_disappoint_samples:
    add_case("Group 1: K2 Deep AI", "rule_5", "Vikram Disappointment Filter", "WhatsApp", "Vikram", text, False, False, rsn, "Hard")

for text, rsn in vikram_normal_samples:
    add_case("Group 1: K2 Deep AI", "rule_5", "Vikram Disappointment Filter", "WhatsApp", "Vikram", text, False, True, rsn, "Medium")

# ------------------------------------------------------------------------------
# Rule 6: Universal Emergency vs Casual Help -> ALERT (80 cases)
# Target: 45 ALERT (life/safety/physical emergency), 35 MUTE (casual help / leisure)
# ------------------------------------------------------------------------------
emergency_samples = [
    ("I met with an accident on highway 4, please help call an ambulance!", "Severe car accident with emergency medical need"),
    ("My father collapsed and is unconscious, please call emergency services right away!", "Life-threatening medical collapse emergency"),
    ("There is a fire in our apartment building, we are trapped on the 3rd floor! Help!", "Active building fire entrapment emergency"),
    ("I am bleeding severely after a cut, need emergency first aid and a hospital ride NOW!", "Severe trauma / arterial bleeding emergency"),
    ("My child is choking and cannot breathe! Please send help immediately!", "Acute pediatric airway emergency"),
    ("Someone broke into my house and is downstairs right now! Call 911 please!", "Active violent home invasion emergency"),
    ("My car broke down in the middle of a blizzard on the mountain pass, freezing cold, help!", "Life-threatening hypothermia / stranded emergency"),
    ("I got bitten by a venomous snake in the hiking trail, rushing to hospital, need antivenom!", "Venomous snakebite medical emergency"),
    ("Severe chest pain and numbness in my left arm, I think I am having a heart attack!", "Acute myocardial infarction cardiac emergency"),
    ("Our boat engine failed and taking on water 5 miles off the coast! Mayday!", "Maritime sinking distress emergency"),
    ("Rooftop collapsed during the earthquake, neighbors are buried under rubble! Send rescue!", "Natural disaster building collapse emergency"),
    ("I am stranded in a flash flood with water rising up to the car windows! Help!", "Immediate flash flood drowning peril"),
    ("Severe allergic reaction, throat is swelling shut and cannot find my EpiPen! HELP!", "Severe anaphylactic shock emergency"),
    ("Gas leak explosion in the neighborhood! Multiple casualties, send ambulances!", "Mass casualty gas explosion emergency"),
    ("I am being followed by an aggressive stranger in a dark alley, please stay on phone / call police!", "Active criminal stalker threat emergency"),
    ("My grandmother fell down the stairs and has a broken hip, cannot move! Help!", "Elderly fall with severe fracture emergency"),
    ("Chemical spill in the factory lab, toxic fumes spreading, need hazardous team and medic!", "Industrial toxic chemical disaster"),
    ("We were attacked by wild dogs on the trail, bleeding heavily, need immediate rescue!", "Severe animal attack trauma"),
    ("Carbon monoxide alarm is blaring and whole family is dizzy and vomiting! Call emergency!", "Lethal carbon monoxide poisoning emergency"),
    ("The elevator snapped and dropped 2 floors, 4 people injured and trapped inside!", "Elevator mechanical failure with injury"),
    ("I am having an extreme diabetic hypoglycemic crash, shaking and losing consciousness!", "Diabetic coma medical emergency"),
    ("Struck by lightning on the golf course! Breathing is irregular, start CPR and send paramedics!", "Lightning strike electrical trauma"),
    ("Armed robbery in progress at the grocery store next door, shots fired! Call police!", "Active shooter / armed robbery emergency"),
    ("My baby has a 105F fever and having severe febrile seizures! Call pediatric emergency!", "Pediatric critical seizure emergency"),
    ("Scuba diving accident, partner surfaced too fast and has decompression sickness! Need chamber!", "Decompression sickness diving emergency"),
    ("Car rolled over into the ditch on icy road, door is jammed and smoke coming from hood!", "Vehicle rollover with fire hazard"),
    ("Electrocution in the basement due to flooded wiring! Power is live, send emergency utility & medic!", "High-voltage electrocution danger"),
    ("I was mugged and stabbed in the park, bleeding from abdomen! Need ambulance immediately!", "Penetrating abdominal trauma emergency"),
    ("Severe heat stroke in the desert hike, unconscious and not sweating! Need helicopter evacuation!", "Critical heat stroke hyperthermia"),
    ("Bridge collapsed during heavy rain, several vehicles submerged! Send water rescue team!", "Infrastructure collapse water rescue"),
    ("Asthma inhaler is empty and having severe hypoxia, turning blue! Need oxygen NOW!", "Severe hypoxic asthma attack"),
    ("Our tent was buried under a snow avalanche on the north ridge! 2 members missing!", "Avalanche burial search & rescue"),
    ("Grandpa is showing facial drooping and slurred speech! Suspected acute stroke! Call 911!", "Acute ischemic stroke emergency"),
    ("Industrial press crushed worker's arm in the workshop! Need tourniquet and emergency squad!", "Severe industrial crushing trauma"),
    ("Severe burns across face and arms from kitchen grease fire! Need burn unit ambulance!", "Major thermal burn emergency"),
    ("Lost in the forest with zero water for 24 hours and severe dehydration! Send search party!", "Wilderness dehydration survival emergency"),
    ("Tornado touchdown just destroyed the neighborhood! Power lines down, people trapped!", "Direct tornado destruction emergency"),
    ("Accidental poison ingestion! Toddler swallowed liquid bleach, need poison control & ER!", "Pediatric toxic poison ingestion"),
    ("Explosion in the chemistry building, glass shards everywhere, multiple severe lacerations!", "Laboratory explosion trauma"),
    ("I was caught in a rip current and pulled far out to sea, getting exhausted! Help!", "Ocean rip current drowning peril"),
    ("Severe concussion and bleeding from ears after motorcycle wipeout! Unconscious!", "Severe traumatic brain injury"),
    ("High-voltage power line fell onto my car! Trapped inside with sparks everywhere!", "Downed power line electrical hazard"),
    ("Massive sinkhole opened on the road and swallowed 2 cars! Emergency rescue needed!", "Sinkhole structural collapse"),
    ("Severe post-operative internal bleeding, fainting and blood pressure dropping fast!", "Post-surgical hemorrhage emergency"),
    ("House is surrounded by advancing wildfire! Evacuation route is blocked, need air rescue!", "Wildfire encirclement emergency")
]

casual_help_samples = [
    ("Can anyone help me find a good movie to watch on Netflix tonight?", "Casual leisure movie recommendation query"),
    ("Could you help me solve today's New York Times crossword puzzle clue?", "Word game puzzle assistance"),
    ("Can you help me choose between the blue sneakers and the white ones?", "Casual shopping opinion inquiry"),
    ("I need help picking a nice Italian restaurant for my date this Saturday.", "Dining recommendation request"),
    ("Can someone help me beat level 45 on Candy Crush? It's so tricky!", "Casual video game help request"),
    ("Could you help me brainstorm some cool Instagram captions for beach photos?", "Social media caption help"),
    ("Can you help me proofread my 200-word review of the new Marvel movie?", "Casual pop culture review feedback"),
    ("Need some help deciding which Spotify playlist to put on for our road trip.", "Music playlist selection query"),
    ("Can you help me figure out how to fold this origami crane paper figure?", "Craft / hobby instruction help"),
    ("Could anyone help me find a recipe for gluten-free chocolate chip cookies?", "Baking recipe inquiry"),
    ("Can you help me choose a fun board game for our Saturday game night?", "Board game leisure query"),
    ("I need help deciding whether to buy a PlayStation 5 or Nintendo Switch.", "Gaming console shopping query"),
    ("Can you help me pick out a funny birthday card for my younger brother?", "Greeting card selection help"),
    ("Could you help me assemble this IKEA coffee table sometime this weekend?", "Casual furniture assembly request"),
    ("Can anyone help me remember the name of that 90s rock band with the yellow album?", "Music trivia inquiry"),
    ("Need help finding a cute dog breed that doesn't shed too much fur.", "Pet selection inquiry"),
    ("Can you help me calculate how much pizza we need to order for 8 people?", "Casual food ordering math"),
    ("Could you help me choose a paint color for the guest bedroom accent wall?", "Home decor leisure question"),
    ("Can you help me find a funny meme about working from home on Mondays?", "Meme search request"),
    ("I need help picking a coffee shop with good Wi-Fi to read a book this afternoon.", "Casual cafe recommendation"),
    ("Can someone help me water my balcony plants while I am away for the weekend?", "Casual house-sitting favor"),
    ("Could you help me choose between the iPhone 15 and Samsung Galaxy S24?", "Smartphone shopping query"),
    ("Can you help me figure out the chords to that acoustic folk song on guitar?", "Music hobby assistance"),
    ("Need help finding a lightweight backpack for my day hike in the park.", "Gear shopping recommendation"),
    ("Can you help me pick a good fantasy book series to read on vacation?", "Book recommendation request"),
    ("Could anyone help me choose a Halloween costume idea for a couples theme?", "Costume party brainstorm"),
    ("Can you help me find a YouTube tutorial on how to make sourdough bread?", "Cooking tutorial search"),
    ("I need help deciding whether to adopt a cat or a golden retriever puppy.", "Pet adoption choice query"),
    ("Can someone help me translate this 3-word French phrase from a menu?", "Casual menu translation"),
    ("Could you help me choose a nice thank-you gift for our yoga instructor?", "Gift idea consultation"),
    ("Can you help me identify this weird bird I saw sitting on my porch fence?", "Casual bird identification"),
    ("Need some help picking out a funny movie to watch with the kids tonight.", "Family movie selection"),
    ("Can you help me practice basic Spanish greetings for my trip to Cancun?", "Language practice hobby query"),
    ("Could anyone help me recommend a durable suitcase for international travel?", "Luggage recommendation query"),
    ("Can you help me pick the best looking photo for my new LinkedIn profile avatar?", "Profile picture selection")
]

for text, rsn in emergency_samples:
    add_case("Group 1: K2 Deep AI", "rule_6", "Universal Emergency & Help Rule", "SMS", "Stranger / Neighbor", text, False, True, rsn, "Hard")

for text, rsn in casual_help_samples:
    add_case("Group 1: K2 Deep AI", "rule_6", "Universal Emergency & Help Rule", "WhatsApp", "Friend", text, False, False, rsn, "Medium")

# ==============================================================================
# GROUP 2: AOT CONDITIONAL FILTER RULES (255 TEST CASES)
# ==============================================================================

# ------------------------------------------------------------------------------
# Rule 7: Priya (Mute Reels / Memes, Alert on Normal) (85 cases)
# Target: 45 ALERT (normal text/plans), 40 MUTE (reels, memes, tiktok, funny video)
# ------------------------------------------------------------------------------
priya_alert_samples = [
    ("Hi, are you free this evening for coffee?", "Personal chat with Priya, zero reels/memes keywords"),
    ("Can you send me the class notes from yesterday's lecture?", "Direct academic request from Priya"),
    ("My flight lands at 6:30 PM at terminal 2, see you soon!", "Flight arrival coordination with Priya"),
    ("Are we still studying together for the machine learning midterm?", "Study group plan with Priya"),
    ("Let me know when you get home so I know you're safe.", "Caring personal check-in from Priya"),
    ("Did you finish reading the research paper on neural attention?", "Academic discussion with Priya"),
    ("I left my notebook at your apartment, can you bring it tomorrow?", "Personal item retrieval request"),
    ("Heading to the library now, let me know if you want me to save a seat.", "Library study coordination"),
    ("Happy birthday! Wishing you an amazing year ahead filled with joy!", "Heartfelt birthday greeting from Priya"),
    ("Can you review my resume draft before I submit it to the career portal?", "Career advice request from Priya"),
    ("Are you coming to the department seminar on robotics this Friday?", "Academic event attendance check"),
    ("Let's grab lunch at the campus food court after our 11 AM class.", "Lunch coordination with Priya"),
    ("I got the internship offer at Google! So excited to tell you!", "Exciting personal career news from Priya"),
    ("Can you explain how backpropagation works on recurrent networks?", "Technical study question from Priya"),
    ("Do you have the contact number for our academic advisor?", "Administrative academic query"),
    ("Let me know if you need a ride to the airport tomorrow morning.", "Helpful travel offer from Priya"),
    ("What time does the bookstore close on campus today?", "Mundane campus query from Priya"),
    ("I found the textbook PDF online, sending you the drive link now.", "Study material share from Priya"),
    ("Are we presenting first or second in tomorrow's project review?", "Presentation schedule check"),
    ("Thanks for helping me prepare for the GRE exam, feeling confident!", "Warm gratitude message from Priya"),
    ("Did the professor post the grades for assignment 3 on the portal?", "Grading inquiry from Priya"),
    ("Let's meet at the coffee shop near campus to finalize our slides.", "Meeting coordination from Priya"),
    ("Can you check if my name is on the graduation ceremony roster?", "Graduation inquiry from Priya"),
    ("I booked the conference room for our capstone project demo.", "Project facility confirmation"),
    ("What was the due date for the semester fee payment?", "University administrative query"),
    ("Heading to the gym in 20 minutes if you want to work out together.", "Fitness activity invite from Priya"),
    ("Did you receive the email from the scholarship committee?", "Scholarship status query"),
    ("Can you proofread my cover letter for the research assistant role?", "Document review request"),
    ("Let's celebrate finishing our finals with dinner downtown this weekend!", "Celebration plan with Priya"),
    ("Do you want to split an Uber to the concert on Saturday?", "Transportation coordination"),
    ("I have an extra ticket for the university basketball game tonight.", "Event ticket offer from Priya"),
    ("What chapters are included in next week's quiz?", "Course curriculum question"),
    ("Can you share your notes on the distributed systems consensus protocols?", "Study notes request"),
    ("Let me know when you are free to do a quick mock interview.", "Career preparation request"),
    ("I found your water bottle in the study hall, I'll bring it to class.", "Item recovery message"),
    ("Are you attending the guest lecture on quantum computing tomorrow?", "Lecture attendance query"),
    ("Let's grab dinner at that new Thai place near the metro station.", "Dinner invitation from Priya"),
    ("Did the lab instructor reply about the project extension request?", "Academic extension check"),
    ("Can you help me install Linux on my old laptop this weekend?", "Technical favor request from Priya"),
    ("So happy we finished our team thesis defense today! We did great!", "Academic milestone celebration"),
    ("What time are we meeting the advisor for our weekly progress update?", "Advisor meeting check"),
    ("Can you forward me the syllabus for the computer vision elective?", "Course syllabus request"),
    ("Are you going to the campus career fair on Thursday morning?", "Career fair attendance check"),
    ("Let's do a quick grocery run to the supermarket after classes.", "Errand coordination with Priya"),
    ("Sending you the final draft of our project report for one last look.", "Project deliverable review")
]

priya_mute_samples = [
    ("Check out this funny instagram reels video! https://instagram.com/reel/C8x9", "Instagram reel link shared by Priya -> Mute"),
    ("OMG this cat meme is literally you every Monday morning haha: https://meme.com/cat", "Meme link shared by Priya -> Mute"),
    ("Look at this hilarious tiktok video about programming bugs: https://tiktok.com/@dev/video/123", "TikTok funny video shared by Priya -> Mute"),
    ("Sending you 5 funny reels in a row, watch the third one first!", "Explicit reels batch mention -> Mute"),
    ("Check out this viral meme going around on Twitter today lol", "Viral meme keyword -> Mute"),
    ("Watch this funny video of a dog failing to catch a frisbee: https://video.link/dog", "Funny video link -> Mute"),
    ("This instagram reels clip had me dying of laughter: https://instagram.com/reel/D9y1", "Instagram reels clip -> Mute"),
    ("Look at this hilarious meme about sprint deadlines vs reality", "Meme keyword in text -> Mute"),
    ("Check out this funny reel about working in tech in 2026", "Reel keyword in text -> Mute"),
    ("You have to watch this crazy funny video on TikTok right now haha", "TikTok video mention -> Mute"),
    ("Sending you an Instagram reel: https://www.instagram.com/reel/Cz1992/", "Direct Instagram reel URL -> Mute"),
    ("Here is a funny meme template for our group chat", "Meme template share -> Mute"),
    ("Look at this video of a baby panda sneezing, so cute: https://reel.link", "Video keyword and reel link -> Mute"),
    ("Another hilarious reel about engineering standups: https://instagram.com/reel/A8b2", "Engineering reel -> Mute"),
    ("This meme perfectly describes our machine learning training loss graph", "Meme keyword in chat -> Mute"),
    ("Watch this TikTok dance video that went viral overnight: https://tiktok.com/v/991", "TikTok viral video -> Mute"),
    ("Can't stop laughing at this funny reel: https://instagram.com/reel/Kk90", "Funny reel link -> Mute"),
    ("Sending a fresh batch of programmer memes for your afternoon break", "Programmer memes share -> Mute"),
    ("Check out this compilation of funny video fails on YouTube", "Funny video compilation -> Mute"),
    ("This Instagram reel about coffee addiction is 100% accurate: https://instagram.com/reel/F4j1", "Instagram reel link -> Mute"),
    ("Look at this relatable meme about debugging in production", "Relatable meme share -> Mute"),
    ("Watch this short video clip of a parrot singing opera music", "Video clip keyword -> Mute"),
    ("Here's that viral reel everyone was talking about at lunch: https://instagram.com/reel/Xx88", "Viral reel link -> Mute"),
    ("This funny meme about Git merge conflicts is pure gold", "Git meme share -> Mute"),
    ("Check out this TikTok video tutorial on making 3-minute mug cakes: https://tiktok.com/v/441", "TikTok video -> Mute"),
    ("Sending you an awesome travel reel from Bali: https://instagram.com/reel/P0q1", "Travel reel URL -> Mute"),
    ("Look at this hilarious video of cats being scared by cucumbers", "Hilarious video keyword -> Mute"),
    ("This meme format never gets old haha", "Meme format share -> Mute"),
    ("Watch this funny reel about software engineers trying to explain their job: https://instagram.com/reel/M3n9", "Instagram reel -> Mute"),
    ("Check out this viral meme about remote work vs office work", "Viral meme keyword -> Mute"),
    ("Sending this funny video link from Reddit: https://reddit.com/r/funny/video", "Funny video URL -> Mute"),
    ("Look at this dog meme: https://instagram.com/p/meme123", "Dog meme link -> Mute"),
    ("This TikTok video had 10 million views in 24 hours: https://tiktok.com/v/8812", "TikTok video mention -> Mute"),
    ("Another funny reel about Monday morning meetings: https://instagram.com/reel/L8k1", "Monday reel link -> Mute"),
    ("Here's a hilarious meme about ChatGPT writing our code", "ChatGPT meme keyword -> Mute"),
    ("Watch this video of a golden retriever swimming in a ball pit", "Video keyword in message -> Mute"),
    ("Check out this Instagram reel on quick pasta recipes: https://instagram.com/reel/Q7w2", "Recipe reel link -> Mute"),
    ("This funny meme about JavaScript type coercion is too real", "JavaScript meme share -> Mute"),
    ("Look at this TikTok video of an incredible drone light show: https://tiktok.com/v/5512", "TikTok video -> Mute"),
    ("Sending you a funny reel to cheer you up: https://instagram.com/reel/Z9x1", "Cheer-up reel link -> Mute")
]

for text, rsn in priya_alert_samples:
    add_case("Group 2: AOT Conditional", "rule_7", "Priya (Mute Reels/Memes)", "Instagram", "Priya", text, False, True, rsn, "Easy")

for text, rsn in priya_mute_samples:
    add_case("Group 2: AOT Conditional", "rule_7", "Priya (Mute Reels/Memes)", "Instagram", "Priya", text, False, False, rsn, "Easy")

# ------------------------------------------------------------------------------
# Rule 8: Rohit Interview / Salary -> ALERT (85 cases)
# Target: 45 ALERT (interview, salary, offer, compensation, CTC), 40 MUTE (Goa trip, movies, casual)
# ------------------------------------------------------------------------------
rohit_career_samples = [
    ("HR scheduled your final interview round for tomorrow at 10 AM.", "Direct final round interview scheduling notice"),
    ("The company revised the salary offer to $145,000 base plus equity!", "Direct salary compensation revision update"),
    ("Attached is the official offer letter with your full CTC breakdown.", "Official job offer letter with CTC"),
    ("The hiring manager wants to conduct a 45-minute technical interview on system design.", "Technical interview invitation"),
    ("Did HR confirm your annual salary appraisal percentage for this cycle?", "Salary appraisal inquiry"),
    ("Your interview with the VP of Engineering is confirmed for Thursday afternoon.", "Executive interview confirmation"),
    ("They agreed to our compensation expectations including the signing bonus!", "Compensation package agreement"),
    ("The recruitment team sent over the interview preparation guide and coding syllabus.", "Interview prep material from HR"),
    ("Here is the revised employment contract showing your base salary and stock options.", "Employment contract with base salary"),
    ("HR reached out to schedule the behavioral interview round with the director.", "Behavioral interview scheduling"),
    ("What was your expected CTC range when you spoke with the talent acquisition lead?", "CTC compensation inquiry"),
    ("The technical interview feedback was overwhelmingly positive! Offer coming soon.", "Positive interview feedback leading to offer"),
    ("They offered a $20,000 sign-on bonus in addition to the base salary package!", "Sign-on bonus compensation detail"),
    ("Please confirm your availability for the panel interview this Friday at 2 PM.", "Panel interview availability check"),
    ("The compensation committee approved the promotion and 18% salary hike!", "Promotion with salary hike confirmation"),
    ("Here is the breakdown of your annual CTC, provident fund, and performance bonus.", "Annual CTC component breakdown"),
    ("The HR manager wants to have a quick call to negotiate your salary expectations.", "Salary negotiation call request"),
    ("Your screening interview with the recruiting coordinator is set for Monday at 11 AM.", "Screening interview setup"),
    ("Did you receive the written offer letter from the enterprise team?", "Written offer letter confirmation"),
    ("The company offers comprehensive health insurance and competitive 401k salary matching.", "Salary benefits and matching"),
    ("Can you share your updated resume for the principal engineer interview loop?", "Resume submission for interview loop"),
    ("HR confirmed that the salary will be disbursed on the last working day of the month.", "Salary disbursement notice"),
    ("The recruiter confirmed you passed all 4 technical interview rounds!", "Successful interview rounds completion"),
    ("Here is the formal job offer detailing your salary, bonus targets, and start date.", "Formal job offer with salary details"),
    ("We need to discuss the compensation package before you sign the acceptance letter.", "Compensation package discussion"),
    ("The interview panel will focus on distributed systems, concurrency, and DB design.", "Interview topic focus notice"),
    ("Did they match your current CTC plus the standard 30% hike in the offer?", "CTC hike percentage inquiry"),
    ("The hiring committee meeting is tomorrow to decide on your offer package.", "Hiring committee offer decision"),
    ("HR sent the link for your virtual onsite interview with the engineering team.", "Virtual onsite interview link"),
    ("The starting salary for this senior role exceeds our initial budget expectations!", "Starting salary celebration"),
    ("Please complete the pre-interview coding assessment on HackerRank before Tuesday.", "Pre-interview coding assessment"),
    ("The compensation team included relocation bonus and annual equity grants in the offer.", "Equity and compensation grant in offer"),
    ("Are you ready for your mock system design interview tonight at 8 PM?", "Mock interview practice session"),
    ("HR sent an update: your salary band is classified in the senior tier.", "Salary band classification update"),
    ("The recruiter called to give a verbal offer with a very competitive salary!", "Verbal offer with competitive salary"),
    ("Make sure to prepare questions about team culture for the manager interview round.", "Manager interview preparation"),
    ("What is the variable pay component in your total CTC offer?", "Variable pay CTC component question"),
    ("Your coding interview with the staff engineer is scheduled for 3:30 PM.", "Staff engineer coding interview"),
    ("The company announced a company-wide 10% salary adjustment for inflation.", "Company-wide salary adjustment"),
    ("Did the recruiter email you the schedule for the 5 interview stages?", "Multi-stage interview schedule check"),
    ("Here is a breakdown of base salary vs stock units vesting over 4 years.", "Base salary vs stock vesting breakdown"),
    ("The hiring manager wants to conduct a culture fit interview over Zoom.", "Culture fit interview invitation"),
    ("Make sure to negotiate the base salary before accepting the equity grant.", "Base salary negotiation strategy"),
    ("The offer letter arrived in your inbox with all compensation terms attached!", "Offer letter arrival with compensation"),
    ("Your final interview score was approved by the director of engineering!", "Final interview score approval")
]

rohit_casual_samples = [
    ("Let's go to Goa this weekend for a road trip.", "Casual holiday road trip invitation from Rohit"),
    ("Are you coming to play FIFA at my place tonight?", "Casual video gaming plan with Rohit"),
    ("Did you watch the latest Marvel movie in IMAX yesterday?", "Movie review query from Rohit"),
    ("Heading to the gym in 15 minutes if you want to join.", "Workout invitation from Rohit"),
    ("Who is winning the cricket match between India and Australia today?", "Cricket match score discussion"),
    ("Let's grab a burger from the diner around the corner.", "Casual lunch plan with Rohit"),
    ("Did you buy tickets for the upcoming music festival in December?", "Music concert query from Rohit"),
    ("Look at this cool photo of the sunset from my balcony.", "Personal scenic photo share"),
    ("Happy Diwali! Wishing you and your family a joyous festival!", "Holiday greeting from Rohit"),
    ("Do you want to play tennis this Sunday morning at 8 AM?", "Casual tennis match query"),
    ("What time are we meeting the group for dinner tonight?", "Group dinner coordination"),
    ("Did you hear about the new electric car launched by Tesla?", "Automotive news chat"),
    ("Can you recommend a good gaming monitor under $300?", "Monitor shopping query from Rohit"),
    ("Let's plan a trekking trip to the Western Ghats next month.", "Outdoor trekking plan"),
    ("Are you watching the Champions League football final tonight?", "Football match viewing query"),
    ("Did you finish watching season 3 of Stranger Things?", "TV show discussion with Rohit"),
    ("My flight was delayed by 2 hours due to bad weather at the airport.", "Travel delay update"),
    ("Let's order some pizza and watch standup comedy on Netflix.", "Casual weekend movie plan"),
    ("Do you know a good mechanic to service my motorcycle?", "Vehicle service recommendation"),
    ("Heading to the beach this afternoon for some fresh air.", "Casual weekend outing update"),
    ("Happy new year! Hope 2026 brings you great success and health!", "New Year greeting from Rohit"),
    ("Did you catch the new trailer for the upcoming Batman movie?", "Movie trailer chat"),
    ("Let's grab some craft beer at the brewery downtown this Friday.", "Casual social drinks invite"),
    ("Are you free to help me move some furniture this Saturday morning?", "Personal moving favor request"),
    ("What is your favorite coffee shop to work from on weekends?", "Coffee shop preference query"),
    ("Look at this funny picture of my golden retriever wearing sunglasses.", "Pet photo share from Rohit"),
    ("Did you try the new spicy ramen restaurant in the mall?", "Dining experience query"),
    ("Let's go bowling with the college gang this Sunday evening.", "Bowling leisure activity invite"),
    ("Can you share the Spotify playlist from our road trip last summer?", "Music playlist request"),
    ("Are you going to the electronics expo at the convention center?", "Expo attendance query"),
    ("Let's do a barbecue party in the backyard this holiday weekend.", "Barbecue social party invite"),
    ("Did you see the northern lights photos posted on Reddit today?", "Nature photography chat"),
    ("What time does the shopping mall close on weekdays?", "Shopping hours inquiry"),
    ("Let's book tickets for the comedy club show this Friday night.", "Standup comedy ticket booking"),
    ("Do you want to go cycling around the lake tomorrow at sunrise?", "Cycling hobby invitation"),
    ("Heading to the bookstore to browse some sci-fi novels if you want to come.", "Bookstore visit invite"),
    ("Did you watch the live stream of the rocket launch this morning?", "Space rocket launch chat"),
    ("Let's try that new escape room puzzle game downtown this weekend.", "Escape room leisure invite"),
    ("Are you coming to the watch party for the World Cup final?", "Sports watch party coordination"),
    ("Hope you are having a relaxing Sunday with family!", "Casual weekend greeting from Rohit")
]

for text, rsn in rohit_career_samples:
    add_case("Group 2: AOT Conditional", "rule_8", "Rohit Interview / Salary Rule", "WhatsApp", "Rohit", text, False, True, rsn, "Easy")

for text, rsn in rohit_casual_samples:
    add_case("Group 2: AOT Conditional", "rule_8", "Rohit Interview / Salary Rule", "WhatsApp", "Rohit", text, False, False, rsn, "Easy")

# ------------------------------------------------------------------------------
# Rule 9: Slack Deployment / Bug Fix -> ALERT (85 cases)
# Target: 45 ALERT (deployment, deploy, bug, hotfix, prod, release, fix), 40 MUTE (pizza, casual, watercooler)
# ------------------------------------------------------------------------------
slack_deploy_samples = [
    ("Hotfix deployment completed successfully for v2.4.", "Direct hotfix deployment notice on Slack"),
    ("Production bug fix for payment gateway timeout merged into main branch.", "Production bug fix notification"),
    ("CI/CD pipeline: Deployment to production cluster completed with zero errors.", "Automated production deployment status"),
    ("Critical bug detected in user authentication module. Releasing emergency patch.", "Critical bug fix patch announcement"),
    ("Release v1.8.0 is live in production across all regional servers.", "Live production release announcement"),
    ("Bug #402 (memory leak in streaming cache) has been verified and fixed.", "Bug resolution verification notice"),
    ("Deployment started for billing service update v3.1. Expected duration: 5 mins.", "Deployment start broadcast"),
    ("Hotfix v2.4.1 deployed to resolve database connection pooling bug.", "Hotfix addressing connection bug"),
    ("Prod deployment pipeline passed all automated integration tests.", "Production pipeline success"),
    ("The critical bug causing shopping cart reset on checkout has been fixed and deployed.", "Customer bug fix in production"),
    ("Deployment alert: Canary release v1.9.0 promoted to 100% of production traffic.", "Canary deployment promotion alert"),
    ("Bug fix for mobile push notification delivery failure is now live on staging.", "Bug fix deployment on staging"),
    ("Emergency hotfix deployed to mitigate zero-day API vulnerability.", "Emergency security hotfix deployment"),
    ("Production release 2.5 rollout completed successfully without downtime.", "Production release completion"),
    ("The memory corruption bug in the WebSocket handler has been resolved.", "Critical backend bug resolution"),
    ("Automated deployment rollback initiated due to elevated HTTP 500 error rates.", "Deployment rollback alert"),
    ("Bug #819 (deadlock in transaction processor) fixed in commit 7a8f9c2.", "Database deadlock bug fix commit"),
    ("Scheduled deployment of backend microservices starting tonight at 11 PM UTC.", "Scheduled deployment notice"),
    ("Hotfix applied to production load balancer to fix SSL handshake dropouts.", "Load balancer hotfix deployment"),
    ("The critical bug causing duplicate charges has been permanently fixed in prod.", "Billing bug fix in production"),
    ("Release candidate v3.0.0-rc2 deployed to pre-production testing environment.", "Release candidate deployment"),
    ("Bug fix for Android background notification crash merged and verified.", "Mobile crash bug fix"),
    ("Deployment status: Blue-Green switch completed successfully for API gateway.", "Blue-Green deployment switch"),
    ("Hotfix deployed to update expired third-party API certificates in production.", "Certificate hotfix deployment"),
    ("The memory leak bug in our PyTorch inference server has been fixed.", "ML inference server bug fix"),
    ("Release notes for production v2.6.0 published: 14 bug fixes and performance tuning.", "Production release with bug fixes"),
    ("Emergency bug fix deployed to stop infinite retry storm on auth endpoints.", "API retry bug fix"),
    ("Deployment finished: Kubernetes ingress controllers updated to v1.10.", "Infrastructure deployment completion"),
    ("Bug #552 (race condition in order processing) successfully patched in master.", "Race condition bug patch"),
    ("Hotfix v2.4.3 released to fix iOS crash on biometric login.", "Biometric login crash hotfix"),
    ("Prod deployment verification tests all green: latency p99 down to 42ms.", "Production deployment verification"),
    ("The critical bug preventing password reset emails from delivering is fixed.", "User auth bug fix in prod"),
    ("Nightly deployment pipeline finished building docker image `prod-service:v2.7`.", "Docker deployment build notice"),
    ("Hotfix deployed to resolve Redis cluster failover split-brain bug.", "Redis cluster hotfix deployment"),
    ("Bug fix for SQLite database lock contention merged into staging branch.", "Database lock bug fix"),
    ("Production release v3.1 is 100% rolled out across US, EU, and APAC regions.", "Global production release"),
    ("Emergency hotfix applied to patch XSS vulnerability on comment input box.", "Security vulnerability hotfix"),
    ("The bug causing incorrect currency conversion on checkout is now fixed.", "Financial calculation bug fix"),
    ("Deployment notification: Kafka consumer cluster scaled up and updated to v3.6.", "Kafka cluster deployment"),
    ("Hotfix deployed: S3 file upload timeout bug resolved for large video attachments.", "S3 upload bug fix in prod"),
    ("The critical deadlock bug in our background queue processor has been fixed.", "Queue processor bug fix"),
    ("Release v2.8.0 deployed to production. Monitoring error budgets for 30 mins.", "Production release monitoring"),
    ("Bug fix for timezone offset calculation error deployed to live servers.", "Timezone logic bug fix"),
    ("Deployment pipeline status: Helm charts deployed to production namespace.", "Helm chart deployment alert"),
    ("Hotfix v2.4.4 deployed to resolve API rate limiting bypass bug.", "Rate limiting hotfix deployment")
]

slack_casual_samples = [
    ("Who wants pizza for lunch today? Ordering in 15 minutes!", "Casual office lunch coordination on Slack"),
    ("Happy birthday Alice! Hope you have a wonderful celebration today! 🎂", "Birthday celebration greeting in channel"),
    ("Does anyone know if the 4th floor coffee machine is working?", "Office coffee machine query"),
    ("Sharing some cute photos of my new kitten on the #pets channel!", "Casual pet photo sharing"),
    ("Anyone up for a quick 15-minute table tennis game in the breakroom?", "Breakroom table tennis query"),
    ("Don't forget to submit your weekly timesheets in the HR portal by 5 PM.", "Routine administrative reminder"),
    ("Who left their blue hydroflask water bottle in conference room B?", "Lost and found office item"),
    ("Great all-hands meeting today, loved the demo of our new features!", "General positive meeting reaction"),
    ("Anyone want to grab bubble tea from the food truck outside?", "Snack / drink run query"),
    ("The office Wi-Fi password has been updated to `Summer2026!Pass`.", "Routine IT password announcement"),
    ("Who is bringing snacks for board game night this Thursday?", "Social activity coordination"),
    ("FYI: The elevators in tower A will be undergoing maintenance at 3 PM.", "Building facility maintenance notice"),
    ("Happy Friday everyone! Have a restful and energizing weekend!", "Casual weekend greeting"),
    ("Check out this funny gif of a cat typing furiously on a keyboard haha", "Casual humorous gif share"),
    ("Does anyone have a spare HDMI adapter I can borrow for a client call?", "Equipment borrowing request"),
    ("The company softball team won our match last night 8-5! Go team!", "Recreational sports team update"),
    ("Reminder: Annual flu vaccination drive in the main lobby on Wednesday.", "Wellness clinic reminder"),
    ("Please remember to clean out the office refrigerator before Friday evening.", "Pantry etiquette reminder"),
    ("Found a pair of black sunglasses on the sofa in the reception area.", "Lost and found inquiry"),
    ("Anyone interested in joining the weekly book club discussion on Dune?", "Book club social query"),
    ("The cafeteria is serving tacos and burritos for lunch today!", "Cafeteria daily menu announcement"),
    ("Let's do a quick coffee sync in the lounge area around 3:30 PM.", "Casual coffee catchup"),
    ("Who wants to join the fantasy football league for this upcoming season?", "Fantasy sports league invite"),
    ("Reminder to fill out the workplace ergonomics survey sent by HR.", "HR administrative survey"),
    ("Looking for recommendations for a good bicycle repair shop downtown.", "Personal errand recommendation"),
    ("Check out the team photos from yesterday's volunteer charity event!", "Community service photo share"),
    ("Anyone driving past the train station after work today? Need a quick lift.", "Carpool ride request"),
    ("The office thermostat has been adjusted to 72F in the main hall.", "Temperature adjustment notice"),
    ("Happy work anniversary to Mark on celebrating 3 years with the company!", "Work anniversary congratulations"),
    ("Please don't leave dirty dishes in the kitchen sink overnight.", "Pantry etiquette notice"),
    ("Anyone watching the season finale of House of the Dragon tonight?", "TV show discussion"),
    ("Sharing the link to the company swag store for free branded t-shirts.", "Company swag order link"),
    ("Who wants to split an order of fresh cookies from the bakery across the street?", "Afternoon bakery treat share"),
    ("The mailroom received a batch of package deliveries for the marketing team.", "Mailroom package arrival"),
    ("Looking for someone to play badminton with at the gym after 6 PM.", "Casual sports recreation query"),
    ("Reminder: Guest speaker lecture on blockchain in the auditorium at 4 PM.", "Optional lecture reminder"),
    ("Has anyone seen the green laser pointer for the main projector?", "Meeting room equipment query"),
    ("Have a great long weekend with your friends and family everyone!", "Holiday weekend farewell"),
    ("The ice cream freezer in the kitchen is fully restocked for the summer!", "Office snack notice"),
    ("Anyone interested in ordering custom team hoodies this winter?", "Team merchandise inquiry")
]

for text, rsn in slack_deploy_samples:
    add_case("Group 2: AOT Conditional", "rule_9", "Slack Deployment / Bug Fix", "Slack", "DevOps Bot / Engineer", text, False, True, rsn, "Easy")

for text, rsn in slack_casual_samples:
    add_case("Group 2: AOT Conditional", "rule_9", "Slack Deployment / Bug Fix", "Slack", "Colleague", text, False, False, rsn, "Easy")

# ==============================================================================
# GROUP 3: APP FILTER RULES (120 TEST CASES)
# ==============================================================================

# ------------------------------------------------------------------------------
# Rule 10: Microsoft Teams (All Messages Important) (60 cases)
# Target: 60 ALERT across various workplace communications
# ------------------------------------------------------------------------------
teams_messages = [
    ("Sync meeting starting in 5 minutes in General channel.", "Teams General channel sync alert"),
    ("Can you join the audio call with the architecture lead on Teams?", "Teams audio call request"),
    ("Project Horizon sprint review presentation starts at 10 AM.", "Teams sprint review event"),
    ("Your manager sent you a direct chat message on Teams.", "Teams direct message notification"),
    ("New comment on your shared design document in Teams channel.", "Teams document comment"),
    ("Daily standup huddle is starting in the Engineering team space.", "Teams daily standup"),
    ("Client meeting link: Microsoft Teams call with Enterprise Partners.", "Teams enterprise client meeting"),
    ("You were mentioned in the DevOps Incident Response Teams channel.", "Teams channel mention"),
    ("Security team posted a mandatory policy update in General channel.", "Teams security announcement"),
    ("Quarterly planning meeting calendar invite received via Teams.", "Teams meeting invite"),
    ("Product manager shared the Q3 roadmap slides on Teams.", "Teams file share"),
    ("The leadership all-hands broadcast is live on Microsoft Teams.", "Teams live event stream"),
    ("Incoming 1-on-1 video call from Product Lead on Teams.", "Teams direct video call"),
    ("Code review discussion thread opened on Teams backend channel.", "Teams engineering discussion"),
    ("Customer support escalated ticket #1029 in the Teams escalation room.", "Teams customer escalation"),
    ("Compliance team requested your sign-off in the Teams audit group.", "Teams compliance signoff"),
    ("Engineering director tagged you in the Q4 architecture proposal.", "Teams executive tag"),
    ("Team retrospective meeting is starting now in the Agile Teams room.", "Teams retrospective meeting"),
    ("New announcement posted in the Company-Wide Teams channel.", "Teams company broadcast"),
    ("HR coordinator shared the training schedule in the Teams onboarding channel.", "Teams HR schedule"),
    ("Database migration status update posted in the Cloud Infrastructure Teams room.", "Teams infrastructure update"),
    ("Billing system outage discussion active in the Sev-1 Teams room.", "Teams incident room discussion"),
    ("Your presence status on Teams changed to In a Call.", "Teams status change notification"),
    ("Sprint backlog grooming session scheduled on Teams for 2 PM.", "Teams backlog grooming"),
    ("Feedback on your pull request posted in the Frontend Teams channel.", "Teams code review feedback"),
    ("VP of Engineering joined the Quarterly Business Review Teams call.", "Teams leadership call"),
    ("New thread created in the Mobile Development Teams workspace.", "Teams workspace thread"),
    ("Customer success lead shared client meeting notes on Teams.", "Teams customer notes"),
    ("Emergency patch coordination happening in the Operations Teams room.", "Teams emergency patch room"),
    ("HR benefits consultation webinar link shared on Teams.", "Teams webinar link"),
    ("Design team posted the new mobile UI design system on Teams.", "Teams design system share"),
    ("Marketing team coordinated the press release on Teams channel.", "Teams marketing coordination"),
    ("QA lead posted the test execution report in the Releases Teams room.", "Teams test report"),
    ("You were added as a member to the Machine Learning Core Teams group.", "Teams group addition"),
    ("Legal department shared the non-disclosure agreement draft on Teams.", "Teams legal document share"),
    ("Weekly engineering office hours session is live on Teams.", "Teams office hours stream"),
    ("IT helpdesk updated your hardware support ticket on Teams.", "Teams IT helpdesk update"),
    ("Product analytics dashboard review starting in Teams room 4.", "Teams dashboard review"),
    ("Customer escalation bridge is active on Microsoft Teams.", "Teams customer bridge"),
    ("Team lunch coordination message in the Social Teams channel.", "Teams social channel message"),
    ("New wiki page published to the Cloud Architecture Teams tab.", "Teams wiki tab update"),
    ("Security compliance checklist submitted to the Auditing Teams channel.", "Teams compliance checklist"),
    ("Frontend performance tuning huddle starting on Teams.", "Teams performance huddle"),
    ("API documentation review session live on Microsoft Teams.", "Teams documentation review"),
    ("New ticket assignment notification from Jira bot on Teams.", "Teams bot notification"),
    ("Vendor negotiation call starting in the Procurement Teams space.", "Teams procurement call"),
    ("Sprint demo recording uploaded to the Engineering Teams stream.", "Teams recording upload"),
    ("All-hands Q&A session questions being voted on in Teams chat.", "Teams Q&A chat"),
    ("Cloud security assessment results shared in the InfoSec Teams group.", "Teams InfoSec assessment"),
    ("Database administrator posted index optimization plan on Teams.", "Teams DB optimization plan"),
    ("Emergency bridge activated for network routing issue on Teams.", "Teams network bridge"),
    ("New hire introduction message posted in the Welcome Teams room.", "Teams new hire welcome"),
    ("Customer feedback survey results presented on Teams meeting.", "Teams customer survey meeting"),
    ("Sprint retrospective action items assigned in Teams planner.", "Teams planner assignment"),
    ("Executive summary of quarterly earnings posted in Teams leadership channel.", "Teams earnings summary"),
    ("Build server maintenance schedule posted in DevOps Teams channel.", "Teams build server maintenance"),
    ("Contract approval notification received from DocuSign bot on Teams.", "Teams contract notification"),
    ("Engineering mentorship workshop starting on Microsoft Teams.", "Teams workshop call"),
    ("Incident post-mortem document shared in Reliability Teams room.", "Teams post-mortem share"),
    ("Final wrap-up meeting for Project Horizon starting on Teams.", "Teams project wrapup call")
]

for text, rsn in teams_messages:
    add_case("Group 3: App Filter", "rule_10", "Microsoft Teams App Filter", "Teams", "Colleague / Lead", text, False, True, rsn, "Easy")

# ------------------------------------------------------------------------------
# Rule 11: Swiggy & Uber (All Notifications Important) (60 cases)
# Target: 60 ALERT across food delivery and rideshare updates
# ------------------------------------------------------------------------------
swiggy_uber_samples = [
    ("Swiggy", "Swiggy Delivery", "Your Swiggy order is out for delivery with driver Ravi.", "Food delivery out for delivery alert"),
    ("Uber", "Uber Driver", "Your Uber driver Ramesh (Honda City - KA05MH1234) is arriving in 2 mins.", "Rideshare driver arriving alert"),
    ("Swiggy", "Swiggy Support", "Your food from Domino's Pizza is arriving in 5 minutes. Please be ready at the gate.", "Food delivery arriving soon"),
    ("Uber", "Uber Ride", "Your Uber ride has started towards Bangalore International Airport.", "Ride started notification"),
    ("Swiggy", "Swiggy Instamart", "Your Instamart grocery order has been delivered at your doorstep.", "Grocery delivery confirmation"),
    ("Uber", "Uber Auto", "Your Uber Auto is waiting at your pickup location (5 mins remaining).", "Uber driver waiting at pickup"),
    ("Swiggy", "Swiggy Gourmet", "The restaurant has accepted your dinner order and is preparing your food.", "Food preparation update"),
    ("Uber", "Uber Go", "Driver has arrived at the pickup location. Please board vehicle KA01EJ9081.", "Driver arrived at pickup"),
    ("Swiggy", "Swiggy Delivery", "Delivery partner Arun is near your location. Contact: +91-9876543210.", "Driver proximity alert"),
    ("Uber", "Uber XL", "Your ride to Whitefield Tech Park is confirmed. Driver on the way.", "Ride confirmation alert"),
    ("Swiggy", "Swiggy Dineout", "Your table reservation at Olive Beach is confirmed for 8:00 PM tonight.", "Table reservation confirmation"),
    ("Uber", "Uber Moto", "Your bike taxi driver is 1 minute away at the main gate.", "Bike taxi arrival alert"),
    ("Swiggy", "Swiggy Delivery", "Order #99281 delivered successfully. Enjoy your meal!", "Order delivery success"),
    ("Uber", "Uber Premier", "Your luxury ride with driver Suresh is approaching the airport departure gate.", "Premier ride arrival"),
    ("Swiggy", "Swiggy Instamart", "Delivery partner is on the way with your milk and groceries.", "Instamart in-transit update"),
    ("Uber", "Uber Trip", "Trip completed! Total fare ₹420. Receipt sent to your email.", "Trip completion receipt"),
    ("Swiggy", "Swiggy Delivery", "Your order from Truffles has left the restaurant kitchen.", "Kitchen dispatch alert"),
    ("Uber", "Uber Driver", "Driver changed due to traffic delay. New driver: Vijay (Maruti Swift - DL01AB4432).", "Driver change update"),
    ("Swiggy", "Swiggy Genie", "Your Genie package has been picked up from the sender and is in transit.", "Package delivery in transit"),
    ("Uber", "Uber Reserve", "Your scheduled ride for tomorrow 6:00 AM has been assigned to driver Sunil.", "Scheduled ride confirmation"),
    ("Swiggy", "Swiggy Delivery", "Delivery partner reached your society security gate.", "Gate arrival alert"),
    ("Uber", "Uber Shuttle", "Your office shuttle is arriving at bus bay 4 in 3 minutes.", "Shuttle arrival alert"),
    ("Swiggy", "Swiggy Food", "Order delayed by 10 mins due to heavy rain. New delivery ETA: 8:45 PM.", "Weather delay notification"),
    ("Uber", "Uber Intercity", "Your outstation cab to Mysore has been booked with driver Rajesh.", "Intercity cab booking"),
    ("Swiggy", "Swiggy Delivery", "Delivery partner cannot find your flat number. Please call them via app.", "Driver navigation assistance needed"),
    ("Uber", "Uber Driver", "Your driver is waiting at the arrival pickup point pillar B3.", "Airport pillar pickup alert"),
    ("Swiggy", "Swiggy Instamart", "Order #44821 packed and assigned to delivery partner Deepak.", "Grocery packing alert"),
    ("Uber", "Uber Go", "Your Uber driver has accepted your trip request and is 4 mins away.", "Ride request accepted"),
    ("Swiggy", "Swiggy Food", "Special instructions: 'Leave package on doorstep' confirmed by driver.", "Delivery instructions confirmed"),
    ("Uber", "Uber Package", "Your courier package has been handed over to the receiver successfully.", "Package delivery confirmed"),
    ("Swiggy", "Swiggy Delivery", "Food is piping hot and on its way! Track live location on map.", "Live tracking alert"),
    ("Uber", "Uber Pool", "Co-rider pickup completed. Estimated arrival at your destination: 9:15 AM.", "Carpool arrival estimate"),
    ("Swiggy", "Swiggy Gourmet", "Order #81729 from Royal Orchid confirmed by chef.", "Gourmet order confirmation"),
    ("Uber", "Uber Driver", "Driver is calling regarding exact landmark for your pickup point.", "Driver contact alert"),
    ("Swiggy", "Swiggy Instamart", "Your morning coffee beans and bread will arrive in 8 minutes.", "Express grocery arrival"),
    ("Uber", "Uber Green", "Your electric vehicle ride is arriving at the lobby in 2 minutes.", "EV ride arrival alert"),
    ("Swiggy", "Swiggy Delivery", "Delivery partner is wearing a helmet and sanitized delivery bag.", "Safety protocol notification"),
    ("Uber", "Uber Black", "Your executive chauffeur has arrived at the hotel main porch.", "Executive chauffeur arrival"),
    ("Swiggy", "Swiggy Dineout", "Reminder: Table for 4 reserved at Farzi Cafe in 30 minutes.", "Dineout reservation reminder"),
    ("Uber", "Uber Moto", "Helmet provided for your safety. Enjoy your quick ride!", "Safety equipment notice"),
    ("Swiggy", "Swiggy Delivery", "Delivery partner completed order handover to security desk.", "Security desk handover"),
    ("Uber", "Uber Trip", "Toll charges of ₹80 added to your trip receipt.", "Toll charge update"),
    ("Swiggy", "Swiggy Instamart", "Substituted out-of-stock item: organic bananas approved.", "Item substitution approval"),
    ("Uber", "Uber Rental", "Your 4-hour city rental package has started with driver Mohan.", "City rental start alert"),
    ("Swiggy", "Swiggy Food", "Your birthday cake order from Glen's Bakehouse is out for delivery.", "Specialty bakery delivery"),
    ("Uber", "Uber Driver", "Driver has reached the pickup pin. Please board vehicle.", "Boarding prompt"),
    ("Swiggy", "Swiggy Delivery", "Delivery partner is ringing your doorbell.", "Doorbell arrival alert"),
    ("Uber", "Uber Assist", "Wheelchair accessible vehicle dispatched and on the way.", "Accessible vehicle dispatch"),
    ("Swiggy", "Swiggy Instamart", "Your emergency medicine and band-aid order is arriving in 5 mins.", "Express medicine delivery"),
    ("Uber", "Uber Driver", "Heavy traffic on route, ETA adjusted by 5 minutes.", "Traffic delay ETA update"),
    ("Swiggy", "Swiggy Gourmet", "Complimentary dessert added to your dinner order by restaurant!", "Complimentary item update"),
    ("Uber", "Uber Go", "Your driver is entering the gated community main entrance.", "Gated entrance entry"),
    ("Swiggy", "Swiggy Delivery", "Payment of ₹650 completed via UPI on delivery.", "Payment receipt alert"),
    ("Uber", "Uber Pet", "Pet friendly ride confirmed with driver Anand.", "Pet ride confirmation"),
    ("Swiggy", "Swiggy Instamart", "Order items verified by warehouse quality team.", "Quality verification check"),
    ("Uber", "Uber Trip", "Driver rated you 5 stars! Thanks for riding with Uber.", "Driver rating feedback"),
    ("Swiggy", "Swiggy Food", "Order dispatched with thermal insulated hot-bag.", "Thermal bag dispatch"),
    ("Uber", "Uber Airport", "Priority airport queue assigned to your booking.", "Airport priority queue"),
    ("Swiggy", "Swiggy Delivery", "Your weekend breakfast order from Filter Coffee is on the way.", "Breakfast delivery alert"),
    ("Uber", "Uber Driver", "Driver completed vehicle disinfection protocol before pickup.", "Cleanliness protocol alert")
]

for app, sender, text, rsn in swiggy_uber_samples:
    add_case("Group 3: App Filter", "rule_11", "Swiggy & Uber Delivery Alert", app, sender, text, False, True, rsn, "Easy")

# ==============================================================================
# GROUP 4: FAST CONTACT & CALL RULES (85 TEST CASES)
# ==============================================================================

# ------------------------------------------------------------------------------
# Rule 12: Sneha Calls (Alert on Calls, Mute on Texts) (45 cases)
# Target: 25 ALERT (Phone call from Sneha), 20 MUTE (Text messages from Sneha)
# ------------------------------------------------------------------------------
sneha_calls = [
    ("Phone", "Sneha", "Incoming Call from Sneha Sharma (Mobile)", True, "Incoming phone call from Sneha -> Alert"),
    ("Phone", "Sneha Sharma", "Incoming Video Call from Sneha Sharma", True, "Incoming video call from Sneha -> Alert"),
    ("Phone", "Sneha", "Missed Call from Sneha (2 calls in last 5 mins)", True, "Missed phone call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming WhatsApp Voice Call from Sneha", True, "WhatsApp voice call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from Sneha (Work Phone)", True, "Work phone call from Sneha -> Alert"),
    ("Phone", "Sneha Sharma", "Incoming Call from Sneha Sharma (Home)", True, "Home phone call from Sneha -> Alert"),
    ("Phone", "Sneha", "Missed Video Call from Sneha", True, "Missed video call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from +91-9845012345 (Sneha)", True, "Direct phone call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming FaceTime Audio Call from Sneha", True, "FaceTime audio call from Sneha -> Alert"),
    ("Phone", "Sneha Sharma", "Incoming Call: Sneha Sharma calling...", True, "Active phone call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from Sneha (Emergency Contact)", True, "Emergency contact call from Sneha -> Alert"),
    ("Phone", "Sneha", "Missed Call from Sneha at 10:45 AM", True, "Missed call notification from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from Sneha (Cell)", True, "Cell call from Sneha -> Alert"),
    ("Phone", "Sneha Sharma", "Incoming Google Meet Voice Call from Sneha Sharma", True, "Voice call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from Sneha on SIM 1", True, "SIM 1 call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from Sneha (Roaming)", True, "Roaming call from Sneha -> Alert"),
    ("Phone", "Sneha", "Missed Call from Sneha (Mobile +919876543210)", True, "Missed call alert from Sneha -> Alert"),
    ("Phone", "Sneha Sharma", "Incoming Call from Sneha Sharma (VIP Contact)", True, "VIP phone call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Telegram Voice Call from Sneha", True, "Telegram voice call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming WhatsApp Video Call from Sneha", True, "WhatsApp video call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from Sneha...", True, "Phone call in progress from Sneha -> Alert"),
    ("Phone", "Sneha Sharma", "Missed Audio Call from Sneha Sharma", True, "Missed audio call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from Sneha (Speed Dial 1)", True, "Speed dial call from Sneha -> Alert"),
    ("Phone", "Sneha", "Incoming Call from Sneha (International)", True, "International phone call from Sneha -> Alert"),
    ("Phone", "Sneha Sharma", "Incoming Signal Voice Call from Sneha Sharma", True, "Signal voice call from Sneha -> Alert")
]

sneha_texts = [
    ("WhatsApp", "Sneha", "Hey, when are you free to chat?", False, "Text message from Sneha (not a call) -> Mute per rule"),
    ("SMS", "Sneha", "Did you get the package I sent to your address?", False, "SMS text message from Sneha -> Mute"),
    ("WhatsApp", "Sneha", "Let me know when you reach home tonight.", False, "WhatsApp text message from Sneha -> Mute"),
    ("Instagram", "Sneha", "Sent you a photo on Instagram direct.", False, "Instagram message from Sneha -> Mute"),
    ("WhatsApp", "Sneha Sharma", "Are we still meeting for lunch tomorrow?", False, "WhatsApp chat from Sneha -> Mute"),
    ("SMS", "Sneha", "Please send me the recipe for that pasta dish.", False, "SMS text from Sneha -> Mute"),
    ("WhatsApp", "Sneha", "Can you check your email when you have a moment?", False, "WhatsApp text from Sneha -> Mute"),
    ("Telegram", "Sneha", "Shared a link to the article we discussed.", False, "Telegram text from Sneha -> Mute"),
    ("WhatsApp", "Sneha", "Happy birthday! Hope you have a wonderful day!", False, "Birthday text from Sneha -> Mute"),
    ("SMS", "Sneha", "Don't forget to buy milk on your way back.", False, "SMS reminder from Sneha -> Mute"),
    ("WhatsApp", "Sneha Sharma", "What time does the movie start tonight?", False, "Movie query text from Sneha -> Mute"),
    ("WhatsApp", "Sneha", "Look at the cute photos from yesterday's hike.", False, "Photo share text from Sneha -> Mute"),
    ("Signal", "Sneha", "Sent you the document in PDF format.", False, "Signal text from Sneha -> Mute"),
    ("WhatsApp", "Sneha", "Did you find your car keys?", False, "Casual text from Sneha -> Mute"),
    ("SMS", "Sneha", "Call me when you are free later.", False, "Text message asking to call (not an incoming call) -> Mute"),
    ("WhatsApp", "Sneha", "Thanks for helping with the groceries earlier!", False, "Gratitude text from Sneha -> Mute"),
    ("Instagram", "Sneha", "Liked your story on Instagram.", False, "Instagram reaction from Sneha -> Mute"),
    ("WhatsApp", "Sneha Sharma", "The weather is so pleasant outside today.", False, "Casual text from Sneha -> Mute"),
    ("SMS", "Sneha", "I'll be there in 10 minutes.", False, "Arrival text from Sneha -> Mute"),
    ("WhatsApp", "Sneha", "Good night! Sleep well.", False, "Goodnight text from Sneha -> Mute")
]

for app, sender, text, is_call, rsn in sneha_calls:
    add_case("Group 4: Fast Contact & Call", "rule_12", "Sneha Phone Call Rule", app, sender, text, is_call, True, rsn, "Easy")

for app, sender, text, is_call, rsn in sneha_texts:
    add_case("Group 4: Fast Contact & Call", "rule_12", "Sneha Phone Call Rule", app, sender, text, is_call, False, rsn, "Easy")

# ------------------------------------------------------------------------------
# Rule 13: Ananya Contact Rule (All messages from Ananya important) (40 cases)
# Target: 40 ALERT across WhatsApp, SMS, Instagram, Telegram
# ------------------------------------------------------------------------------
ananya_messages = [
    ("WhatsApp", "Ananya", "Hey! Can you send me the doc link?", "Direct WhatsApp message from Ananya -> Alert"),
    ("SMS", "Ananya", "My train arrives at 4:30 PM on platform 1.", "SMS message from Ananya -> Alert"),
    ("Instagram", "Ananya", "Are you free to jump on a quick video call?", "Instagram DM from Ananya -> Alert"),
    ("WhatsApp", "Ananya Roy", "Did you finish the slides for our presentation?", "WhatsApp message from Ananya Roy -> Alert"),
    ("Telegram", "Ananya", "Here is the dataset file for our machine learning project.", "Telegram message from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Can you pick me up from the metro station?", "Transportation request from Ananya -> Alert"),
    ("SMS", "Ananya", "Please call me back as soon as you see this.", "Urgent callback request from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Happy birthday! Hope you have the best year ahead!", "Birthday greeting from Ananya -> Alert"),
    ("Instagram", "Ananya", "I left my jacket at your house, is it there?", "Item inquiry from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Let's grab breakfast before class tomorrow morning.", "Breakfast invite from Ananya -> Alert"),
    ("SMS", "Ananya", "Your parcel was delivered to my apartment by mistake.", "Parcel delivery note from Ananya -> Alert"),
    ("WhatsApp", "Ananya Roy", "I got selected for the campus placement round!", "Exciting placement news from Ananya -> Alert"),
    ("Telegram", "Ananya", "Sending you the shared notes for physics exam.", "Study material from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Are we still going to the library today at 3 PM?", "Library plan with Ananya -> Alert"),
    ("Instagram", "Ananya", "Check out the photos from our weekend trip.", "Trip photo message from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Can you review my essay draft when you have time?", "Essay review request from Ananya -> Alert"),
    ("SMS", "Ananya", "I'm outside your building gate right now.", "Arrival notification from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Do you have the contact for the dance club lead?", "Club contact query from Ananya -> Alert"),
    ("Telegram", "Ananya", "Here are the flight tickets for our vacation trip.", "Flight ticket share from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Thank you so much for the graduation gift!", "Gratitude message from Ananya -> Alert"),
    ("Instagram", "Ananya", "Are you coming to the music concert on Friday?", "Concert attendance check from Ananya -> Alert"),
    ("WhatsApp", "Ananya Roy", "My laptop charger stopped working, can I borrow yours?", "Charger borrowing request from Ananya -> Alert"),
    ("SMS", "Ananya", "Meeting with the professor moved to 2:30 PM.", "Schedule update from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Let me know when you finish work today.", "Evening sync check from Ananya -> Alert"),
    ("Telegram", "Ananya", "Shared the code repository link with you on GitHub.", "Repository share from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "What are you ordering for dinner tonight?", "Dinner coordination with Ananya -> Alert"),
    ("Instagram", "Ananya", "Happy anniversary to our friendship! 🎉", "Friendship celebration from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "I found your glasses in the study room.", "Item recovery from Ananya -> Alert"),
    ("SMS", "Ananya", "Did you submit the scholarship application form?", "Application check from Ananya -> Alert"),
    ("WhatsApp", "Ananya Roy", "Can you explain question 4 from the assignment?", "Homework question from Ananya -> Alert"),
    ("Telegram", "Ananya", "Sending you the PDF textbook for semester 6.", "Textbook share from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Let's go shopping for the festival this evening.", "Shopping invite from Ananya -> Alert"),
    ("Instagram", "Ananya", "Are you free to hang out at the cafe after 5?", "Cafe hangout plan with Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Did the doctor give you the test reports?", "Health check-in from Ananya -> Alert"),
    ("SMS", "Ananya", "Call me when you wake up tomorrow morning.", "Morning sync request from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "So proud of your achievement in the competition!", "Praise and congratulations from Ananya -> Alert"),
    ("Telegram", "Ananya", "Here is the Spotify playlist we created together.", "Music playlist share from Ananya -> Alert"),
    ("WhatsApp", "Ananya Roy", "What time should we leave for the airport tomorrow?", "Airport departure planning with Ananya -> Alert"),
    ("Instagram", "Ananya", "Sent you a message request regarding our group project.", "Project DM from Ananya -> Alert"),
    ("WhatsApp", "Ananya", "Good morning! Wishing you a productive and happy day ahead!", "Morning greeting from Ananya -> Alert")
]

for app, sender, text, rsn in ananya_messages:
    add_case("Group 4: Fast Contact & Call", "rule_13", "Ananya Contact Rule", app, sender, text, False, True, rsn, "Easy")

# ==============================================================================
# GROUP 5: AOT TOPIC FILTER RULES (40 TEST CASES)
# ==============================================================================

# ------------------------------------------------------------------------------
# Rule 14: Badminton & Football Sports Rule (40 cases)
# Target: 25 ALERT (badminton, football, turf, court), 15 MUTE (cricket, tennis, movies)
# ------------------------------------------------------------------------------
sports_alert_samples = [
    ("Who is coming to play badminton at 6 PM today?", "Badminton sports invitation -> Alert"),
    ("Booked the football turf at Decathlon for 8 PM tonight, need 4 more players!", "Football turf booking -> Alert"),
    ("Badminton court 3 reserved at the sports complex from 7 to 8 PM.", "Badminton court reservation -> Alert"),
    ("Anyone up for a 7-a-side football match this Sunday morning?", "Football match invitation -> Alert"),
    ("We have a badminton doubles tournament this weekend, registration open!", "Badminton tournament -> Alert"),
    ("Football practice session scheduled at the community ground at 5 PM.", "Football practice session -> Alert"),
    ("Need 2 more players for badminton singles at the YMCA indoor court.", "Badminton singles player search -> Alert"),
    ("Who is bringing the football to the stadium today?", "Football equipment query -> Alert"),
    ("Badminton rackets and shuttlecocks are available at the club desk.", "Badminton gear notice -> Alert"),
    ("Football league registration deadline is tomorrow at noon.", "Football league signup -> Alert"),
    ("Playing badminton at the gym courts after work today if anyone wants to join.", "Badminton gym invite -> Alert"),
    ("Sunday football match against the alumni team is confirmed at 7 AM.", "Alumni football match -> Alert"),
    ("Badminton coaching camp for advanced players starts on Monday.", "Badminton coaching camp -> Alert"),
    ("The football turf lights have been repaired for night matches.", "Football turf facility update -> Alert"),
    ("Who has spare Yonex badminton shuttlecocks for our match tonight?", "Badminton shuttlecock search -> Alert"),
    ("Inter-college football championship quarterfinals live this afternoon at main stadium.", "College football championship -> Alert"),
    ("Indoor wooden badminton court booked for 2 hours this Saturday.", "Indoor badminton court booking -> Alert"),
    ("Football friendly match against the IT department confirmed for 6 PM.", "Company football friendly -> Alert"),
    ("Badminton training drills on footwork and smashes scheduled tomorrow.", "Badminton training drills -> Alert"),
    ("Need a goalkeeper for our 5-a-side football match in 30 minutes!", "Football goalkeeper search -> Alert"),
    ("Badminton club annual membership renewals are now open.", "Badminton club renewal -> Alert"),
    ("The university football team won the regional trophy today 3-1!", "Football victory celebration -> Alert"),
    ("Booked court 1 for mixed doubles badminton from 6 PM to 7 PM.", "Badminton mixed doubles booking -> Alert"),
    ("Who is playing football at the sports academy turf this evening?", "Football academy turf query -> Alert"),
    ("Badminton string tension restringing service available at the pro shop.", "Badminton racket restringing -> Alert")
]

sports_mute_samples = [
    ("Who wants to watch the cricket match between India and Australia tonight?", "Cricket match viewing (not badminton/football) -> Mute"),
    ("Anyone up for a game of tennis at the outdoor clay court tomorrow?", "Tennis game (not badminton/football) -> Mute"),
    ("Let's go swimming at the Olympic pool this afternoon.", "Swimming recreation (not badminton/football) -> Mute"),
    ("Basketball pickup game at the community center court at 5 PM.", "Basketball match -> Mute"),
    ("Who wants to play table tennis in the office games room after lunch?", "Table tennis / ping pong -> Mute"),
    ("Did you watch the Formula 1 Grand Prix race in Monaco yesterday?", "F1 motorsport racing -> Mute"),
    ("Anyone interested in joining the morning yoga class in the park?", "Yoga wellness session -> Mute"),
    ("Who is coming for the 10k marathon running training this Sunday?", "Marathon running -> Mute"),
    ("Let's play chess online on Lichess tonight at 9 PM.", "Chess board game -> Mute"),
    ("Anyone up for bouldering and rock climbing at the adventure gym?", "Rock climbing -> Mute"),
    ("Who wants to watch the IPL cricket playoff match on the big screen?", "IPL cricket match -> Mute"),
    ("Squash court reserved for 45 minutes at the club if anyone wants to play.", "Squash sport -> Mute"),
    ("Volleyball match on the beach court this Saturday at 4 PM.", "Volleyball match -> Mute"),
    ("Did you see the golf tournament highlights on TV this morning?", "Golf tournament -> Mute"),
    ("Anyone interested in learning archery at the sports complex?", "Archery sport -> Mute")
]

for text, rsn in sports_alert_samples:
    add_case("Group 5: AOT Topic Filter", "rule_14", "Badminton & Football Rule", "WhatsApp", "Sports Club / Group", text, False, True, rsn, "Easy")

for text, rsn in sports_mute_samples:
    add_case("Group 5: AOT Topic Filter", "rule_14", "Badminton & Football Rule", "WhatsApp", "Sports Club / Group", text, False, False, rsn, "Easy")

# ==============================================================================
# GROUP 6: SIMPLE BLOCK / SUPPRESSION RULES (40 TEST CASES)
# ==============================================================================

# ------------------------------------------------------------------------------
# Rule 15: Marketing & Spam Suppression (40 cases)
# Target: 25 MUTE (marketing, spam, discounts, promos), 15 MUTE (ambient non-matching fallthrough)
# ------------------------------------------------------------------------------
spam_mute_samples = [
    ("SMS", "Promo Alert", "Special promotional marketing offer: Get 50% discount today! Claim cashback now.", "Promotional discount marketing spam -> Mute"),
    ("SMS", "Flash Sale", "MEGA SALE! Flat 70% off on all electronics this weekend. Click link to buy now: http://spam.deal", "Flash sale spam with link -> Mute"),
    ("SMS", "Loan Offer", "Pre-approved instant personal loan of ₹5,00,000 at 0% processing fee. Apply today!", "Unsolicited loan marketing spam -> Mute"),
    ("SMS", "Marketing Blast", "Congratulations! You won a ₹10,000 gift voucher. Claim reward coupon code: WIN10K", "Fake prize voucher spam -> Mute"),
    ("SMS", "Casino Bet", "Deposit ₹500 and get ₹2000 free bonus on your first online casino game!", "Gambling promotional spam -> Mute"),
    ("SMS", "Credit Card", "Exclusive lifetime free credit card with 5X reward points waiting for you. Click here.", "Credit card sales marketing -> Mute"),
    ("SMS", "Clearance Deal", "Final hours! End of season clearance sale with extra 20% discount on cart value.", "Clearance sale promo -> Mute"),
    ("SMS", "Spam Rewards", "Your loyalty reward points are expiring today. Redeem coupon voucher immediately.", "Expiring points marketing lure -> Mute"),
    ("SMS", "Marketing Bot", "Exclusive VIP discount promo code: SUMMER50. Valid on all fashion apparel.", "Fashion promo code marketing -> Mute"),
    ("SMS", "Real Estate", "Luxury 3BHK apartments starting at ₹85 Lakhs in prime tech corridor. Book site visit.", "Real estate marketing spam -> Mute"),
    ("SMS", "Crypto Scam", "Earn 500% guaranteed returns on crypto automated trading bot. Join VIP group now.", "Crypto investment spam -> Mute"),
    ("SMS", "Super Saver", "Super saver weekend sale! Buy 1 Get 2 Free on all footwear and accessories.", "Buy 1 Get 2 retail promo -> Mute"),
    ("SMS", "Marketing Team", "Special discount for our valued customers. Use promo code VIP100 at checkout.", "Customer promo code marketing -> Mute"),
    ("SMS", "Health Insurance", "Protect your family with comprehensive health insurance for just ₹15/day. Call now.", "Cold insurance marketing -> Mute"),
    ("SMS", "Spam Alert", "Urgent claim: Your cashback of ₹2,450 is waiting in your wallet. Click to withdraw.", "Fake cashback claim spam -> Mute"),
    ("SMS", "Promo Hub", "Exclusive 40% discount on salon and spa services this Friday. Book appointment.", "Spa discount promo -> Mute"),
    ("SMS", "Flash Deal", "Midnight flash sale starts in 1 hour. Up to 80% off on premium smartphones!", "Midnight flash sale promo -> Mute"),
    ("SMS", "Auto Loan", "Zero down payment car loan pre-approved for your profile. Drive your dream car home.", "Car loan marketing spam -> Mute"),
    ("SMS", "Spam Winner", "Lucky draw winner! Your mobile number was selected for a free holiday trip to Dubai.", "Fake lottery winner spam -> Mute"),
    ("SMS", "Marketing Express", "Flat ₹500 cashback on your next electricity bill payment on our platform.", "Cashback promotional push -> Mute"),
    ("SMS", "Brand Sale", "Mega brand days: Top designer clothing at minimum 60% discount. Shop now.", "Designer clothes sale promo -> Mute"),
    ("SMS", "Spam Bot", "Claim your free gold coin on jewelry purchases above ₹25,000 this festive week.", "Jewelry promotional spam -> Mute"),
    ("SMS", "Quick Cash", "Instant cash credit up to ₹1,00,000 transferred in 5 minutes with zero paperwork.", "High-risk loan marketing spam -> Mute"),
    ("SMS", "Travel Deal", "Special discounted flight tickets to international destinations starting at ₹9,999.", "Flight discount marketing -> Mute"),
    ("SMS", "Spam Marketing", "Subscribe to our daily promotional newsletter and get 15% discount on first order.", "Newsletter subscription promo -> Mute")
]

ambient_fallthrough_samples = [
    ("Weather", "Weather App", "Sunny today with a high of 78°F and light winds from the west.", "Ambient weather update (no matching rule) -> Mute"),
    ("Calendar", "Calendar Bot", "Reminder: Recycling bin pickup scheduled for tomorrow morning.", "Ambient calendar reminder (no matching rule) -> Mute"),
    ("Settings", "System", "Software update available: Android Security Patch Level October 2026.", "System OS update (no matching rule) -> Mute"),
    ("Fitness", "Fitness Tracker", "Daily step goal achieved: 10,240 steps walked today!", "Fitness step tracker (no matching rule) -> Mute"),
    ("News", "News Digest", "Scientists discover new exoplanet 120 light years away with water vapor.", "General science news broadcast (no matching rule) -> Mute"),
    ("Clock", "Timer", "Timer completed: 15 minutes elapsed.", "Ambient timer chime (no matching rule) -> Mute"),
    ("Battery", "System", "Battery charged to 100%. Unplug charger.", "Battery status update (no matching rule) -> Mute"),
    ("Music", "Music Player", "Now Playing: 'Bohemian Rhapsody' by Queen.", "Music track player notification (no matching rule) -> Mute"),
    ("Photos", "Photos App", "Rediscover this day: 3 years ago in Tokyo, Japan.", "Photo memory reminder (no matching rule) -> Mute"),
    ("Drive", "Cloud Storage", "Automated camera backup completed for 14 new photos.", "Cloud backup confirmation (no matching rule) -> Mute"),
    ("Clock", "World Clock", "London is currently 5 hours behind your local time.", "World clock informational widget (no matching rule) -> Mute"),
    ("Health", "Sleep Monitor", "Sleep score for last night: 85 (Deep sleep: 2h 15m).", "Sleep tracking score (no matching rule) -> Mute"),
    ("Browser", "Browser", "Downloaded file `invoice_october.pdf` (1.2 MB).", "File download notification (no matching rule) -> Mute"),
    ("Notes", "Notes App", "Note updated 5 minutes ago on desktop device.", "Note sync update (no matching rule) -> Mute"),
    ("App Store", "App Store", "5 apps were updated in the background overnight.", "App store auto-update summary (no matching rule) -> Mute")
]

for app, sender, text, rsn in spam_mute_samples:
    add_case("Group 6: Simple Block & Spam", "rule_15", "Marketing & Spam Suppression", app, sender, text, False, False, rsn, "Easy")

for app, sender, text, rsn in ambient_fallthrough_samples:
    add_case("Group 6: Simple Block & Spam", "rule_15", "Marketing & Spam Suppression", app, sender, text, False, False, rsn, "Easy")

# Output dataset summary
print(f"Total test cases generated: {len(test_cases)}")
deep_ai_count = sum(1 for c in test_cases if "Group 1" in c["group"])
aot_count = sum(1 for c in test_cases if "Group 1" not in c["group"])
alerts_count = sum(1 for c in test_cases if c["ground_truth_alert"])
mutes_count = sum(1 for c in test_cases if not c["ground_truth_alert"])
print(f"Deep AI Cases: {deep_ai_count}")
print(f"AOT / Fast Filter Cases: {aot_count}")
print(f"Total Expected Alerts: {alerts_count} | Total Expected Mutes: {mutes_count}")

with open("test_cases_1000.json", "w", encoding="utf-8") as f:
    json.dump(test_cases, f, indent=2, ensure_ascii=False)

print("Saved to test_cases_1000.json successfully!")
