# Video Archive

Spring Boot + Maven + MongoDB + Google Authenticator (TOTP).

## Requirements

- Java 25
- Maven 3.9+
- Docker Desktop

## 1. Start MongoDB

```bash
docker compose up -d
```

MongoDB will be available on `localhost:27017`.

## 2. Configure video disk

Edit `src/main/resources/application.properties`:

```properties
video.storage.path=D:/VideoArchive
```

The directory may be on a different physical disk, for example `E:/VideoArchive`.

The application creates the directory if it does not exist.

## 3. Run

```bash
mvn spring-boot:run
```

Open:

http://localhost:8080

## First login

The initial user is:

- username: `admin`
- password: `admin123`

On the first login the application shows a QR code. Scan it with Google
Authenticator. Then enter the 6-digit TOTP code.

IMPORTANT: change the initial password before using the application in a
real environment.

## Functionality

- login + password + Google Authenticator TOTP
- create archive records
- arbitrary number of records (10 is only the initial amount)
- fields: who supplied files, date, company, title, description
- attach one or more video files to a record
- files are physically copied to the configured video disk
- list/search records
- search by company, title and date
- download video
- stream/play video in browser
- delete a video
- delete a record

MongoDB stores metadata and the physical file names/paths, not the video bytes.


## Change password

After login, use **Сменить пароль** in the navigation.

The application requires:
- current password;
- new password of at least 8 characters;
- confirmation of the new password.

After changing the password, the current session is invalidated and a new
login is required.


## 2FA brute-force protection

The TOTP verification is rate-limited:
- maximum 5 failed codes;
- after the 5th failure, verification is locked for 15 minutes;
- malformed/non-numeric codes count as failed attempts;
- a successful code resets the failed-attempt counter.

This in-memory limiter is suitable for a single application instance. If the
application is later deployed as multiple instances, the limiter should be
moved to a shared store such as Redis.

## Edit records

Existing records can be edited using **Редактировать запись**.

Editing changes only the metadata:
- who supplied the files;
- date;
- company;
- title;
- description.

Attached video files are not changed during metadata editing.


## TOTP brute-force protection

The 6-digit Google Authenticator code is protected by a simple rate limit:
- maximum 5 failed codes within 10 minutes;
- after the fifth failure, verification is blocked for 15 minutes;
- a successful code resets the failed-attempt counter.

## Editing records

Open any record and click **Редактировать запись**. The metadata can be changed:
- who supplied the files;
- date;
- company;
- title;
- description.

Attached videos are preserved when the record is edited.

## Upload and HTTP Range playback

A record page lets you select one or more videos from a USB flash drive, internal disk, or external disk. The server copies them to `video.storage.path` and stores metadata in MongoDB. Playback uses HTTP byte ranges (`206 Partial Content`, `Accept-Ranges`, `Content-Range`) so large files can be sought by the browser without downloading the whole file. Playback opens in a separate tab.


## Security forms

CSRF protection is enabled. All HTML POST forms include the Spring Security
CSRF token. The JavaScript chunk uploader sends the token in the CSRF header.
The login flow is: username/password -> Google Authenticator -> application.
