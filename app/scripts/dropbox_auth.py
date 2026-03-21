"""One-time helper to obtain a Dropbox refresh token.

Usage::

    python -m app.scripts.dropbox_auth

Prerequisites:
  1. Create an app at https://www.dropbox.com/developers/apps
  2. On the Permissions tab enable: files.metadata.read, files.content.read, sharing.read
  3. Click Submit, then copy App key and App secret from the Settings tab

The script will:
  - Ask for your App key and App secret
  - Open a browser for you to authorize the app
  - Exchange the authorization code for a **refresh token**
  - Print the three env vars to paste into your .env file
"""

from __future__ import annotations

import sys
import webbrowser

import requests


def main() -> None:
    print("=== Dropbox Refresh Token Setup ===\n")

    app_key = input("App key: ").strip()
    if not app_key:
        sys.exit("App key is required.")

    app_secret = input("App secret: ").strip()
    if not app_secret:
        sys.exit("App secret is required.")

    auth_url = (
        f"https://www.dropbox.com/oauth2/authorize"
        f"?client_id={app_key}"
        f"&token_access_type=offline"
        f"&response_type=code"
    )

    print(f"\nOpening browser for authorization…\n{auth_url}\n")
    webbrowser.open(auth_url)

    auth_code = input("Paste the authorization code here: ").strip()
    if not auth_code:
        sys.exit("Authorization code is required.")

    print("\nExchanging code for refresh token…")
    resp = requests.post(
        "https://api.dropboxapi.com/oauth2/token",
        data={
            "code": auth_code,
            "grant_type": "authorization_code",
            "client_id": app_key,
            "client_secret": app_secret,
        },
    )

    if resp.status_code != 200:
        print(f"Error {resp.status_code}: {resp.text}", file=sys.stderr)
        sys.exit(1)

    data = resp.json()
    refresh_token = data.get("refresh_token")
    if not refresh_token:
        print(f"No refresh_token in response: {data}", file=sys.stderr)
        sys.exit(1)

    print("\n✓ Success! Add these to your .env file:\n")
    print(f"DROPBOX_APP_KEY={app_key}")
    print(f"DROPBOX_APP_SECRET={app_secret}")
    print(f"DROPBOX_REFRESH_TOKEN={refresh_token}")
    print()


if __name__ == "__main__":
    main()
