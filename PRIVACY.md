# AI Brief Privacy Policy

**Effective Date:** August 17, 2026

AI Brief ("we", "our", or "the app") is a native Android reader designed for quiet, distraction-free reading of artificial intelligence research and engineering developments.

---

## 1. Information We Do Not Collect

- **Personal Information**: We do not collect names, email addresses, phone numbers, postal addresses, or government identifiers.
- **Account Data**: AI Brief does not require an account, login, or profile to access all features.
- **Device & Location Data**: We do not access GPS location, contacts, microphones, cameras, or local file systems beyond private app cache storage.
- **Advertising Identifiers**: We do not collect Google Advertising IDs (AAID) or use tracking SDKs.

---

## 2. Network Activity & External Feeds

- **RSS Feed Ingestion**: The app requests public RSS and Atom XML feeds directly from publisher endpoints (including MIT News, Google AI Research, Hugging Face, and VentureBeat). Standard network requests are handled via HTTPS encryption.
- **Article Images**: When an RSS item includes a cover image URL, the app may request and cache that image directly from the hosting domain.
- **External Links**: Tapping "Read original source" opens the article's canonical URL in your device's default web browser. External websites operate under their own independent privacy policies.

---

## 3. Local On-Device Storage

- **Saved Articles**: Bookmarked articles and reading states are stored exclusively in private on-device preferences (`SharedPreferences`). This data never leaves your device and is not synchronized to external cloud servers.
- **Image Cache**: Cached thumbnails are saved in the app's private cache directory and can be cleared at any time via Android system settings.

---

## 4. Third-Party Services & Analytics

AI Brief contains **no third-party advertising frameworks, no analytics trackers, and no user profiling libraries**. We do not sell, rent, or monetize user data.

---

## 5. Contact & Inquiries

For questions regarding this Privacy Policy, editorial corrections, or publisher requests, please contact:

**Editor & Developer:** Riki Caceras  
**Email:** [rikicaceras@gmail.com](mailto:rikicaceras@gmail.com)  
**Project Repository:** [https://github.com/Caceras/ai-news-kotlin](https://github.com/Caceras/ai-news-kotlin)

---

## 6. Updates to this Policy

Any future revisions will be reflected in this document with an updated effective date prior to the release of corresponding app updates.
