# Privacy Policy for Prozorro Radar

**Effective Date:** November 11, 2026  
**Last Updated:** November 11, 2026  
**Application:** Prozorro Радар / Prozorro Radar  
**Developer Contact:** `lonelycattools@gmail.com`  

---

## 1. Introduction

This Privacy Policy describes how the **Prozorro Radar** ("we", "our", or "the App") handles information when you install and use our Android mobile application. 

We are committed to protecting your privacy and ensuring transparency. **Prozorro Radar is designed as an offline-first, privacy-focused application.** We do not sell, monetize, or track your personal information or business interests.

---

## 2. Information We Collect and Process

### 2.1 Information Stored Locally on Your Device
The App stores the following configuration parameters and data **exclusively on your device** in a local SQLite database:
* **Procurement Filter Criteria:** Selected CPV codes (ДК 021:2015), keywords, budget boundaries, and procurement types.
* **Cached Tender Information:** Public tender records, titles, descriptions, deadlines, and customer identifiers (EDRPOU) fetched from public APIs.
* **Favorite Tenders & Notes:** Tenders you bookmark for offline review and monitoring.
* **App Preferences:** Scan intervals, notification toggles, night mode ("Do Not Disturb") hours, and theme settings.

> **None of this data is ever transmitted to our servers or any third-party analytics platforms.**

### 2.2 Optional Email Notification Address
If you choose to enable the email digest feature, you provide a recipient email address:
* The email address is stored **locally on your device**.
* It is used **solely** for dispatching requested procurement notifications and tender digests directly to your inbox.
* We never share, rent, or sell your email address to advertisers, marketers, or data brokers.

### 2.3 Information We Do NOT Collect
* We do **NOT** require user registration, logins, or social accounts.
* We do **NOT** collect your name, phone number, physical address, financial details, or contacts.
* We do **NOT** collect precise GPS or location data.
* We do **NOT** embed advertising SDKs, behavioral tracking libraries, or third-party analytics trackers (such as Google Analytics for Firebase, Facebook SDK, AppsFlyer, etc.).

---

## 3. Public Procurement Data (OpenProcurement API)

Prozorro Radar retrieves public procurement information directly from the official **OpenProcurement Public API** (`prozorro.gov.ua`).

* All procurement records, contract amounts, tender specifications, and government buyer names are public records in accordance with the Law of Ukraine "On Public Procurement" and "On Access to Public Information".
* The App acts as an independent viewer, scanner, and analytical parser of this publicly available government open data.

---

## 4. Android System Permissions & Foreground Services

To provide reliable autonomous monitoring without missing tender submission deadlines, the App may request the following permissions:

| Permission | Purpose & Justification |
| :--- | :--- |
| `POST_NOTIFICATIONS` | To notify you immediately when new tenders matching your criteria are discovered or when bidding deadlines are expiring. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | To execute scheduled autonomous background scans of the Prozorro public API, ensuring up-to-date tender feeds even when the app is in the background. A clear, persistent notification is displayed during active scanning. |
| `RECEIVE_BOOT_COMPLETED` | To restore your configured background scanning schedule after your mobile device restarts. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` *(optional)* | To prevent aggressive Android OEM battery savers from killing scheduled procurement scans while the device is sleeping. |
| `INTERNET` & `ACCESS_NETWORK_STATE` | To query the public Prozorro API and transmit user-requested email digests. |

---

## 5. Data Security and Retention

* **Local Storage:** All app data is held within the private internal application sandbox on your Android device (using Android's SQLite storage). No other application on your device can access this sandbox without root permissions.
* **Data Retention:** Data remains on your device for as long as you keep the app installed. You can delete cached records, reset search checkpoints, or clear all data at any time from within the App Settings or via Android App Info > Storage > Clear Data.
* **Data Deletion:** Uninstalling the App permanently removes all locally stored databases, preferences, and cached tenders.

---

## 6. Third-Party Services and External Links

* **Official Prozorro Links:** The App includes external links allowing you to open specific tenders directly on official platforms (such as `prozorro.gov.ua`). When you click these external links, you are directed to the respective third-party website, which is governed by its own privacy terms.
* **No Third-Party Advertising:** Prozorro Radar does not host third-party advertisements or ad exchange networks.

---

## 7. Children's Privacy

Prozorro Radar is a professional business intelligence tool intended for commercial entities, procurement specialists, entrepreneurs, and researchers. The App does not target and is not designed for children under 16 years of age. We do not knowingly collect information from children.

---

## 8. Compliance with Google Play Developer Policies

This Privacy Policy complies with Google Play's User Data Policy:
1. Clear disclosure of all permissions utilized by the application.
2. Prominent explanation of Foreground Service usage for timely data synchronization.
3. Strict prohibition of user data selling or unauthorized third-party transfers.
4. User capability to delete and reset all collected application data.

---

## 9. Changes to this Privacy Policy

We may update this Privacy Policy from time to time to reflect improvements in the application or changes in legal regulations. When changes are made, the "Last Updated" date at the top of this document will be updated. We encourage you to review this policy periodically.

---

## 10. Contact Us

If you have questions, feedback, or concerns regarding this Privacy Policy or the operation of Prozorro Radar, please contact us:

* **Email:** `lonelycattools@gmail.com`
* **Developer:** Lonely Cat Tools (Independent Developer)
* **Application:** Prozorro Радар / Prozorro Radar
