# PayTrack

PayTrack is an Android personal finance app built with Jetpack Compose. It helps users track income and expenses, manage savings goals, organize spending with folders and limits, review analytics, and scan UPI merchant QR codes to launch payments and log them in the app.

The project is designed as a modern Kotlin Android app with local persistence, lightweight user preferences, a dashboard-focused experience, and a QR payment flow tailored for everyday expense tracking.

## What the app does

- Shows a home dashboard with current balance, total income, total expenses, savings progress, folder usage, and weekly expense trends.
- Lets users add, edit, delete, search, and filter transactions.
- Organizes spending into folders, including folder-specific limits and deadlines.
- Tracks savings goals with progress and target-date summaries.
- Displays insights such as top spending categories, weekly comparisons, and monthly trends.
- Includes a profile screen with user details and dark mode support.
- Supports scanning UPI merchant QR codes and recording those payments as expense entries.

## QR Payment Feature

PayTrack includes a QR-based UPI payment flow that keeps payment tracking simple:

1. Open the `QR` tab or tap `Scan QR` from the home screen.
2. Grant camera permission when prompted.
3. Scan a merchant UPI QR code.
4. PayTrack reads the merchant details from the QR and shows the payee name, UPI ID, note, and amount when available.
5. If the QR already contains a fixed amount, the amount field stays locked.
6. If the QR does not include an amount, the user can enter one manually.
7. The user selects a spending category before continuing.
8. PayTrack lists detected UPI apps installed on the device.
9. After the user chooses a UPI app, PayTrack opens it and records the payment in transaction history as an expense entry.

Important behavior:

- Only supported `upi://pay` merchant QR codes are accepted.
- Invalid or unsupported QR codes show a scanner error instead of opening the payment flow.
- Camera permission is required for scanning.
- Installed UPI apps are refreshed and detected inside the QR screen.
- QR-based payments are stored as expense transactions with QR/UPI context.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room
- DataStore Preferences
- CameraX
- Google ML Kit barcode scanning
- Vico charts
- Coil

## Project Structure

The app is currently centered around these areas:

- `Home`: dashboard, balance summary, savings progress, folders, and weekly expense chart
- `Transactions`: transaction list, search, filters, and manual add/edit flow
- `QR`: QR scanning, payment review, UPI app selection, and payment logging
- `Insights`: monthly and category-based spending analysis
- `Profile`: user details and dark mode preference

## Run Locally

### Requirements

- Android Studio
- Android SDK with minimum API level 24 support
- A device or emulator for running the app

### Steps

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync and download dependencies.
4. Run the `app` configuration on an emulator or Android device.

If you want to use the QR feature, test on a device or emulator setup that supports camera access and has a UPI app installed for the full payment handoff flow.

## Build From the Command Line

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Testing

The project includes both local unit tests and Android instrumentation/UI tests.

- Unit tests cover logic such as analytics, folder behavior, and UPI QR parsing.
- Android tests cover UI behavior including the weekly expense chart.

Run tests with:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

On Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

## Current Highlights

- Clean Compose-based navigation with dedicated screens for finance workflows
- Local-first data handling for transactions, folders, and profile settings
- Budget-focused folder system with optional spending limits
- Insights view for spending patterns and trend tracking
- UPI QR flow that bridges payment launch and expense recording





## Notes

- The repository currently does not include showcase screenshots, so this README stays text-first.
- The QR payment flow opens supported UPI apps and records the action from PayTrack's side, but final payment confirmation still happens inside the selected UPI app.



![WhatsApp Image 2026-04-05 at 4 03 24 PM](https://github.com/user-attachments/assets/aeb4c28b-331b-4cac-9890-80691605c88b)
![WhatsApp Image 2026-04-05 at 4 03 25 PM](https://github.com/user-attachments/assets/2ffbf671-9061-4ef3-880e-10d8339a70a9)
![WhatsApp Image 2026-04-05 at 4 03 25 PM](https://github.com/user-attachments/assets/af7cd701-a920-4554-b26e-c1ca1f801cd4)

![WhatsApp Image 2026-04-05 at 4 03 25 PM,](https://github.com/user-attachments/assets/efc16dd1-4aed-4c88-b725-ee4982606de5)
![WhatsApp Image 2026-04-05 at 4 03 25 PMm](https://github.com/user-attachments/assets/813fc5d2-f2dd-42b9-9928-bf0149ee64db)
![WhatsApp Image 2026-04-05 at 4 03 26 PM](https://github.com/user-attachments/assets/1293f0aa-e27f-4e07-9222-f06aeacde24b)![WhatsApp Image 2026-04-05 at 4 03 26 PMj](https://github.com/user-attachments/assets/349a96e0-3b45-496c-b5ed-ac898fea215c)

![WhatsApp Image 2026-04-05 at 4 03 27 PMk](https://github.com/user-attachments/assets/0cde2acd-0a06-4419-9f8e-a86ee720f298)
