// EventFlow demo data, MongoDB Compass Shell / mongosh.
// 40 events, zero seeded registrations, zero seeded feedback, zero new accounts.
// This RESET deletes previous seed data and any bookings/reviews on these 40 demo events.
// Manual records for OTHER events are not touched.
(function () {
  const d = db.getSiblingDB("eventflow");
  const tag = "EVENTFLOW_DEMO_SEED_V1";
  const organizer = d.users.findOne({ email: "organizer@eventflow.com" });
  const admin = d.users.findOne({ email: "admin@eventflow.com" });
  const participant = d.users.findOne({ email: "participant@eventflow.com" });
  if (!organizer || !admin || !participant) {
    throw new Error("Start EventFlow once to create the 3 existing demo accounts, then try again.");
  }
  const organizerId = String(organizer._id);
  const adminId = String(admin._id);
  const participantId = String(participant._id);
  const key = i => String(i + 1).padStart(2, "0");
  const eventId = i => "demo40_event_" + key(i);
  const allEventIds = Array.from({ length: 40 }, (_, i) => eventId(i));

  // Detect LocalTime offset BEFORE removing old seed data.
  let timeShift = 0;
  const sample = d.events.findOne({ title: "Tech Innovation Summit", time: { $type: "date" } });
  if (sample && sample.time instanceof Date) {
    timeShift = sample.time.getUTCHours() * 60 + sample.time.getUTCMinutes() - 600;
    if (timeShift > 720) timeShift -= 1440;
    if (timeShift < -720) timeShift += 1440;
  }
  function timeOf(hour, minute) {
    const x = new Date(Date.UTC(2026, 8, 23));
    x.setUTCMinutes(hour * 60 + (minute || 0) + timeShift);
    return x;
  }
  const today = new Date();
  function dateIn(days) {
    return new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate() + days, 12));
  }
  const decimal = x => NumberDecimal(String(x));

  // Reset ONLY the previous seed and the fixed demo40 IDs.
  const resetCollections = [
    "notifications", "contact_requests", "feedback", "registrations",
    "event_gallery_images", "event_schedule", "event_speakers",
    "event_sponsors", "event_ticket_types", "event_coupons", "events"
  ];
  for (const name of resetCollections) {
    d.getCollection(name).deleteMany({
      $or: [{ seedTag: tag }, { _id: /^demo40_/ }]
    });
  }

  // Also reset bookings/reviews manually created on these 40 demo events.
  // This is intentional: user wants to register every attendee manually from zero.
  d.registrations.deleteMany({ eventId: { $in: allEventIds } });
  d.feedback.deleteMany({ eventId: { $in: allEventIds } });
  // Clear live booking confirmations for these demo events, if any.
  d.notifications.deleteMany({ link: { $regex: "^(/organizer/events/demo40_event_|/participant/registrations#registration-demo40_event_)" } });

  // Older script added extra accounts; remove ONLY those marked as seed-created.
  // The existing Admin, Organizer and Participant are explicitly protected.
  d.users.deleteMany({
    seedTag: tag,
    email: { $nin: [admin.email, organizer.email, participant.email] }
  });

  // If the VERY FIRST seed replaced one of six stock images, restore only
  // the exact photo URL that script wrote (never overwrite a different image).
  const oldPhotos = [
    ["Tech Innovation Summit", "photo-1505373877841-8d25f7d46678"],
    ["Creative Design Meetup", "photo-1558655146-d09347e92766"],
    ["Campus Music Night", "photo-1492684223066-81342ee5ff30"],
    ["Startup Founders Circle", "photo-1511578314322-379afb476865"],
    ["Photography Walk", "photo-1500530855697-b586d89ba3ee"],
    ["Career & Skills Expo", "photo-1521737711867-e3b97375f902"]
  ];
  const oldSuffix = "?auto=format&fit=crop&w=1200&q=82";
  for (const [title, photo] of oldPhotos) {
    d.events.updateMany(
      { title, imageUrl: { $in: ["https://images.unsplash.com/" + photo + oldSuffix, "/images/event-visual.png?demo=40"] } },
      { $set: { imageUrl: "/images/event-visual.png" } }
    );
  }

  // 24 UPCOMING and 16 COMPLETED. Format:
  // [title, category, venue, days from today, hour, minute, capacity, price in BDT]
  const rows = [
    ["AI & Cloud Innovation Summit", "Technology", "BICC, Dhaka", 5, 9, 30, 350, 800],
    ["Full Stack Web Bootcamp", "Technology", "SEU Computer Lab", 8, 10, 0, 100, 500],
    ["Campus Music Fiesta", "Music", "Southeast University Auditorium", 12, 18, 0, 500, 300],
    ["Entrepreneur Networking Night", "Business", "Gulshan Innovation Hub", 15, 17, 30, 150, 600],
    ["UX Product Design Workshop", "Design", "Banani Creative Studio", 18, 11, 0, 90, 450],
    ["Career & Internship Expo", "Career", "Southeast University, Dhaka", 21, 10, 0, 650, 0],
    ["Robotics & IoT Showcase", "Technology", "Dhaka Innovation Center", 24, 14, 0, 240, 0],
    ["Health & Wellness Awareness Camp", "Health", "SEU Community Hall", 26, 9, 0, 180, 0],
    ["Inter-University Football Tournament", "Sports", "Dhaka University Sports Ground", 29, 15, 0, 800, 150],
    ["Data Science for Beginners", "Education", "SEU CSE Lab 2", 32, 10, 30, 100, 300],
    ["Startup Pitch Day", "Business", "Bangladesh Startup Hub", 35, 11, 0, 200, 500],
    ["Photography & Storytelling Meetup", "Community", "Hatirjheel, Dhaka", 38, 8, 0, 100, 0],
    ["Cybersecurity Conference", "Technology", "International Convention City", 42, 9, 30, 450, 1000],
    ["Digital Marketing Masterclass", "Business", "Dhanmondi Training Centre", 45, 11, 0, 120, 350],
    ["UI Design Portfolio Review", "Design", "Banani Creative Studio", 48, 14, 0, 80, 400],
    ["University Job Fair", "Career", "SEU Central Auditorium", 52, 10, 0, 750, 0],
    ["Marathon for a Cause", "Sports", "Hatirjheel Running Track", 55, 7, 0, 600, 250],
    ["Blockchain Technology Forum", "Technology", "BICC, Dhaka", 58, 10, 0, 250, 700],
    ["Music & Cultural Festival", "Music", "SEU Open Stage", 61, 17, 0, 900, 200],
    ["Women in Technology Panel", "Education", "Southeast University Auditorium", 64, 15, 0, 230, 0],
    ["Mobile App Hackathon", "Technology", "SEU Innovation Lab", 68, 9, 0, 140, 0],
    ["Public Speaking Workshop", "Education", "SEU Seminar Hall", 72, 11, 0, 90, 200],
    ["Creative Entrepreneurship Fest", "Business", "Gulshan Business Centre", 75, 12, 0, 320, 500],
    ["Community Volunteer Meetup", "Community", "Dhanmondi Lake", 80, 9, 0, 120, 0],
    ["Spring Boot API Workshop", "Technology", "SEU CSE Lab 3", -60, 10, 30, 80, 0],
    ["Open Source Contribution Day", "Education", "SEU Computer Lab", -57, 10, 0, 95, 0],
    ["Campus Cultural Night", "Music", "University Auditorium", -53, 18, 0, 550, 200],
    ["Career Planning Seminar", "Career", "SEU Seminar Hall", -49, 11, 0, 200, 0],
    ["Design Thinking Bootcamp", "Design", "Banani Creative Studio", -45, 10, 0, 85, 350],
    ["Startup Founders Mixer", "Business", "Gulshan Innovation Hub", -42, 18, 0, 130, 300],
    ["AI Research Poster Session", "Technology", "Dhaka Innovation Center", -38, 13, 0, 170, 0],
    ["University Debate Competition", "Education", "SEU Auditorium", -34, 14, 0, 350, 0],
    ["Mental Health Awareness Talk", "Health", "SEU Community Hall", -30, 10, 0, 180, 0],
    ["Beginner Photography Walk", "Community", "Lalbagh Fort, Dhaka", -27, 8, 0, 65, 0],
    ["Green Tech Innovation Day", "Technology", "SEU Innovation Lab", -23, 11, 0, 180, 0],
    ["E-commerce Fundamentals Class", "Business", "Dhanmondi Training Centre", -20, 10, 0, 90, 250],
    ["Football Friendly Cup", "Sports", "SEU Sports Ground", -17, 15, 0, 600, 100],
    ["Product Management Bootcamp", "Design", "Banani Creative Studio", -14, 10, 0, 95, 500],
    ["Internship Interview Clinic", "Career", "SEU CSE Lab 2", -10, 11, 0, 85, 0],
    ["Coding Contest & Awards", "Technology", "SEU Auditorium", -6, 9, 30, 270, 0]
  ];
  if (rows.length !== 40) throw new Error("Expected 40 events.");

  // Use the project's exact original bundled default image, with no external URL.
  const DEFAULT_IMAGE = "/images/event-visual.png";
  d.events.insertMany(rows.map((r, i) => ({
    _id: eventId(i), title: r[0], category: r[1], location: r[2],
    date: dateIn(r[3]), time: timeOf(r[4], r[5]),
    description: r[0] + " is a demonstration event featuring sessions and community activities at " + r[2] + ".",
    capacity: r[6], status: r[3] < 0 ? "COMPLETED" : "UPCOMING",
    featured: i < 6 || i === 12,
    organizerId, organizerName: organizer.fullName,
    imageUrl: DEFAULT_IMAGE,
    pricingType: r[7] > 0 ? "PAID" : "FREE", basePrice: decimal(r[7]),
    seedTag: tag, _class: "com.eventflow.model.Event"
  })));

  // Free admission and paid Standard/VIP options. ALL tickets start unsold.
  const tickets = [];
  rows.forEach((r, i) => {
    const price = r[7];
    tickets.push({
      _id: "demo40_ticket_" + key(i) + "_standard",
      eventId: eventId(i),
      name: price > 0 ? "Standard" : "General Admission",
      description: price > 0 ? "Regular event entry" : "Free admission",
      price: decimal(price),
      capacity: price > 0 ? Math.floor(r[6] * 0.8) : r[6],
      active: true, sortOrder: 1, seedTag: tag,
      _class: "com.eventflow.model.EventTicketType"
    });
    if (price > 0) tickets.push({
      _id: "demo40_ticket_" + key(i) + "_vip",
      eventId: eventId(i), name: "VIP",
      description: "Priority seating and enhanced access",
      price: decimal(price * 2),
      capacity: r[6] - Math.floor(r[6] * 0.8),
      active: true, sortOrder: 2, seedTag: tag,
      _class: "com.eventflow.model.EventTicketType"
    });
  });
  d.event_ticket_types.insertMany(tickets);

  // Coupons to TEST manually when registering. No coupon usage is pre-seeded.
  const coupons = [];
  rows.forEach((r, i) => {
    if (r[7] === 0 || (i < 24 && i % 3 === 2) || (i >= 24 && i % 3 !== 0)) return;
    const pct = i % 2 === 0;
    coupons.push({
      _id: "demo40_coupon_" + key(i), eventId: eventId(i), organizerId,
      code: "EVENT" + key(i) + "OFF",
      discountType: pct ? "PERCENT" : "FIXED",
      discountValue: decimal(pct ? 15 : 100),
      expiryDate: dateIn(r[3] - 1), usageLimit: 25, active: i !== 9,
      createdAt: new Date(), updatedAt: new Date(),
      seedTag: tag, _class: "com.eventflow.model.Coupon"
    });
  });
  if (coupons.length) d.event_coupons.insertMany(coupons);

  // 3 sessions per event, 20 speakers and 14 sponsors; no external images.
  const sessions = [], speakers = [], sponsors = [];
  const names = ["Arif Rahman", "Nabila Chowdhury", "Tanvir Mahmud", "Farhana Islam", "Mahin Kabir", "Sabrina Noor", "Raihan Ahmed", "Nusrat Karim"];
  rows.forEach((r, i) => {
    const start = r[4] * 60 + r[5];
    const parts = [
      ["Registration & Welcome", 0, 25, "Check-in and introductions."],
      ["Main Session: " + r[0], 30, 100, "Talks, examples and interactive activities."],
      ["Questions & Networking", 110, 155, "Discussion and networking."]
    ];
    parts.forEach((p, j) => {
      const from = start + p[1], to = start + p[2];
      sessions.push({
        _id: "demo40_schedule_" + key(i) + "_" + (j + 1),
        eventId: eventId(i), title: p[0],
        startTime: timeOf(Math.floor(from / 60) % 24, from % 60),
        endTime: timeOf(Math.floor(to / 60) % 24, to % 60),
        location: r[2], description: p[3],
        speakerId: j === 1 && i % 2 === 0 ? "demo40_speaker_" + key(i) : null,
        seedTag: tag, _class: "com.eventflow.model.EventScheduleItem"
      });
    });
    if (i % 2 === 0) speakers.push({
      _id: "demo40_speaker_" + key(i), eventId: eventId(i),
      name: names[i % names.length], designation: "Guest Speaker",
      organization: "EventFlow University Demo",
      bio: "Demo speaker for the " + r[1] + " event.",
      photoUrl: "/images/default-avatar.png",
      seedTag: tag, _class: "com.eventflow.model.EventSpeaker"
    });
    if (i % 3 === 0) sponsors.push({
      _id: "demo40_sponsor_" + key(i), eventId: eventId(i),
      name: "Campus Innovation Partner", level: "GOLD", website: null,
      description: "Fictional demo sponsor.", logoUrl: null, sortOrder: 1,
      seedTag: tag, _class: "com.eventflow.model.EventSponsor"
    });
  });
  d.event_schedule.insertMany(sessions);
  d.event_speakers.insertMany(speakers);
  d.event_sponsors.insertMany(sponsors);

  // Realistic SUPPORT examples, none claiming to have registered.
  const support = [
    ["GENERAL_INQUIRY", "How do I join an event?", "Please explain how event registration works.", "NEW"],
    ["COUPON_PRICING_ISSUE", "Using a discount coupon", "How can I apply a coupon before registration?", "IN_PROGRESS"],
    ["EVENT_SUPPORT", "Event venue information", "Where can I find event location and schedule details?", "RESOLVED"],
    ["TECHNICAL_PROBLEM", "Profile settings question", "How can I update my profile information?", "NEW"],
    ["ORGANIZER_SUPPORT", "Managing event speakers", "How do I edit speakers and schedules?", "IN_PROGRESS"]
  ].map((s, i) => ({
    _id: "demo40_contact_" + (i + 1),
    fullName: i === 4 ? organizer.fullName : participant.fullName,
    email: i === 4 ? organizer.email : participant.email,
    phone: "", requestType: s[0], subject: s[1], message: s[2],
    status: s[3], adminNote: s[3] === "RESOLVED" ? "Demo guidance provided." : null,
    createdAt: dateIn(-5 + i), updatedAt: dateIn(-4 + i),
    seedTag: tag, _class: "com.eventflow.model.ContactRequest"
  }));
  d.contact_requests.insertMany(support);

  const notices = [
    [adminId, "Five demo support requests are ready for review.", "/admin/contact-requests", false],
    [adminId, "The demo has 40 events ready for testing.", "/admin/events", true],
    [organizerId, "Your 40 demo events are ready to manage.", "/organizer/events", false],
    [organizerId, "You can edit tickets, coupons, speakers, and schedules.", "/organizer/events", true],
    [participantId, "Explore events and register for one to test booking.", "/events", false],
    [participantId, "Your demo account has no registrations yet.", "/participant/registrations", true]
  ].map((m, i) => ({
    _id: "demo40_notification_" + (i + 1),
    userId: m[0], message: m[1], link: m[2], read: m[3],
    createdAt: new Date(Date.now() - i * 3600000),
    seedTag: tag, _class: "com.eventflow.model.Notification"
  }));
  d.notifications.insertMany(notices);

  // Zero pre-registrations and reviews: you create these yourself via EventFlow.
  const registrationCount = d.registrations.countDocuments({ eventId: { $in: allEventIds } });
  const feedbackCount = d.feedback.countDocuments({ eventId: { $in: allEventIds } });
  print("=== EVENTFLOW DEMO READY ===");
  print("40 demo events: " + d.events.countDocuments({ seedTag: tag }));
  print("Upcoming: " + d.events.countDocuments({ seedTag: tag, status: "UPCOMING" }));
  print("Completed: " + d.events.countDocuments({ seedTag: tag, status: "COMPLETED" }));
  print("Tickets: " + tickets.length + ", coupons: " + coupons.length);
  print("Sessions: " + sessions.length + ", speakers: " + speakers.length + ", sponsors: " + sponsors.length);
  print("Support requests: " + support.length + ", notifications: " + notices.length);
  print("Registrations for the 40 demo events: " + registrationCount);
  print("Reviews for the 40 demo events: " + feedbackCount);
  print("New accounts: 0. External image URLs: 0.");
  if (registrationCount !== 0 || feedbackCount !== 0) throw new Error("Expected zero registrations and reviews on the 40 demo events.");
})();
