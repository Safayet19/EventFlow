# EventFlow

EventFlow is a Spring Boot + Thymeleaf + MongoDB event management system with three roles:

- **ADMIN** - manages users, roles, account status, events and registrations.
- **ORGANIZER** - creates and manages events and sees registered participants.
- **PARTICIPANT** - discovers events, registers, cancels registrations and receives event updates.

## Stack

- Java 26
- Spring Boot 4.1.1
- Maven
- Spring MVC + Thymeleaf
- Spring Security
- MongoDB
- Caffeine cache
- HTML/CSS/JavaScript

## Backend structure

```text
Thymeleaf / Browser
        ↓
Controller
        ↓
Service
        ↓
Repository
        ↓
MongoDB
```

Packages:

- `controller` - routes, model data, redirects and the event-image endpoint.
- `dto` - form input objects.
- `service` - validation and business logic.
- `repository` - MongoDB data access.
- `model` - MongoDB documents.
- `config` - security and startup data.

The project intentionally avoids unnecessary `ServiceImpl` classes, mapper frameworks and duplicate interfaces.

## Main features

- Login, logout, remember-me and role-based access.
- Participant and Organizer self-registration with live JavaScript validation and backend validation.
- BCrypt password storage.
- Two-step demo password recovery: check a registered email, then set and confirm a new strong password. Unknown emails show a clear error, while protected demo accounts keep their published passwords.
- Admin can change another user's role to `PARTICIPANT`, `ORGANIZER` or `ADMIN`.
- Admin can enable, disable and delete normal users with self-protection. The three public demo accounts are visibly marked **Protected** and cannot be deleted, disabled or have their roles changed.
- Organizer event CRUD with event-image upload, participant list, browser date restrictions and backend rejection of past event date/time values.
- Participant event registration and cancellation.
- Professional event ticketing: events can be **Free** or **Paid**, with optional ticket types such as General, VIP or Student, separate capacities, prices, active/paused sales, and a booking-time ticket/price snapshot.
- Coupon management for paid events with percentage/fixed discounts, expiry dates, usage limits, active/paused states, live participant price preview and booking-time discount snapshots.
- Public Contact Support page with EventFlow support information and a request form. Admins receive notifications and manage requests through New, In Progress and Resolved states with internal notes.
- Event sponsors: Organizer/Admin can add Title, Gold, Silver, Bronze or Partner sponsors with logo, website, display order and description. Sponsor logos are stored separately in MongoDB and shown on the public event page.
- Duplicate-registration, capacity and past-event checks.
- In-app notifications with unread count and recent notification dropdown.
- Profile editing with profile-picture upload/remove. Users without a photo use the supplied default avatar. Password change is available for normal accounts; demo-account passwords are fixed so the shared credentials remain available.
- Responsive dark/gold UI with the supplied EventFlow logo, favicon, validation animation, toasts, loading states and confirmation modals for logout/destructive actions.
- Desktop login is compacted to fit the normal viewport without requiring page scrolling; smaller screens remain responsive.

## Performance improvements in this final build

- MongoDB indexes on frequently searched fields such as email, role, event date, category, organizer, registration and notification fields.
- Unique database protection for user email and `eventId + participantId` registration pairs.
- Caffeine cache for event lists, featured events, organizer event lists and repeated registration counts.
- Cache is cleared automatically when event or registration data changes.
- Event images are stored in a separate `event_images` MongoDB collection instead of inside every event document. This keeps event-list queries much smaller and faster.
- Event images use browser caching and event cards use lazy image loading.
- Spring response compression is enabled for HTML, CSS, JavaScript and JSON.
- Browser caching is enabled for static resources.
- Repositories use ordered database queries instead of loading everything and sorting it in Java where possible.
- Notification pages load only the latest 50 notifications and the top bar loads only the latest 8.
- Validation and UI animations remain enabled; performance work is mainly in the database, caching and resource loading.

## Public demo accounts

These three accounts are seeded automatically and are shown directly on the login page. The role buttons fill the login form instantly.

- **ADMIN** - `admin@eventflow.com` / `Admin123!`
- **ORGANIZER** - `organizer@eventflow.com` / `Organizer123!`
- **PARTICIPANT** - `participant@eventflow.com` / `Participant123!`

The demo users stay enabled and their roles/passwords are protected. Normal registered users can still be managed by Admin.

## Run locally

1. Set your MongoDB Atlas connection in the `MONGODB_URI` environment variable.
2. Run `EventflowApplication` from IntelliJ.
3. Open `http://localhost:8080`.
4. Choose Admin, Organizer or Participant under **Quick Demo Access** and sign in.

## Environment variables

- `MONGODB_URI` - required MongoDB Atlas connection string.
- `MONGODB_DATABASE` - database name. Default: `eventflow`.
- `EVENTFLOW_REMEMBER_KEY` - secret key for remember-me cookies.
- `PORT` - supplied automatically by Render.
- `THYMELEAF_CACHE` - set to `true` on deployment for faster template rendering.



## Profile images and branding

- `src/main/resources/static/images/eventflow-logo.png` - dark-theme website wordmark based on the supplied EventFlow logo.
- `src/main/resources/static/images/eventflow-logo-original.png` - untouched supplied logo.
- `src/main/resources/static/images/favicon.png` and `src/main/resources/static/favicon.ico` - browser-tab icon derived from the EventFlow mark.
- `src/main/resources/static/images/default-avatar.png` - supplied fallback user picture.
- Uploaded profile pictures are limited to **2 MB**, may be JPG/PNG/WEBP, and are stored in the separate `profile_images` MongoDB collection.

## Event images

Uploaded images are limited to **2 MB** and may be JPG, PNG or WEBP. They are stored as binary data in the `event_images` MongoDB collection and served through `/event-images/{eventId}`. The Event document stores only the image URL, so browsing event lists does not pull image bytes from MongoDB until the browser actually requests an image.

## Sponsors and ticketing data

- Sponsor metadata is stored in `event_sponsors`; uploaded sponsor logos are stored in `sponsor_images`.
- Custom ticket types are stored in `event_ticket_types`. Registrations store the selected ticket type/name and the price at the time of booking.
- Coupons are stored in `event_coupons`; registrations keep the applied coupon code, discount amount and final booking price.
- Contact submissions are stored in `contact_requests` and are available only to administrators.
- Changing an event between Free and Paid keeps ticket configuration consistent; ticket capacities cannot exceed the event capacity.
- Paid tickets use an instant simulated checkout for this student project; free tickets register immediately.

## Render deployment

EventFlow includes a Dockerfile for deployment. In Render:

1. Create a **Web Service**.
2. Select **Docker**.
3. Connect the GitHub repository.
4. Add `MONGODB_URI` and `EVENTFLOW_REMEMBER_KEY`.
5. Add `THYMELEAF_CACHE=true` for production.

Render supplies `PORT` automatically. MongoDB Atlas stores persistent application data, so the app does not depend on Render's temporary filesystem.

## Simulated checkout (EventFlow student project)

Free tickets register directly. Paid tickets open `/participant/events/{eventId}/checkout` with the existing bKash, Nagad and Visa logos. Choose a method and optionally apply a valid promo code, then select **Proceed to Payment**. EventFlow records a simulated payment and confirms the ticket immediately. No merchant account, real charge, external checkout or callback setup is needed.

The backend recalculates prices and coupon discounts and checks event capacity, ticket availability, duplicate registrations and coupon usage. The confirmed registration and its simulated payment record are saved together in MongoDB, and the participant and organizer are notified. Demo booking cancellation also updates the stored payment status. The project uses the existing MongoDB transaction manager; deploying with MongoDB Atlas supports atomic confirmation.

Manual checks: register for a free event, book a paid event using each payment method, apply valid/invalid/expired coupons, verify the final amount and registration history, try booking an already-full event, and test cancellation. No real money is collected.
