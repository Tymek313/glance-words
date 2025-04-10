# Glance words (a Jetpack Glance widget)

![add_widget_from_clipboard_auto](https://bitbucket.org/tymek313/glance-words/raw/master/previews/add_widget_from_clipboard_auto.gif)
![add_widget_from_keyboard.gif](https://bitbucket.org/tymek313/glance-words/raw/master/previews/add_widget_from_keyboard.gif)
![sync.gif](https://bitbucket.org/tymek313/glance-words/raw/master/previews/sync.gif)


## Project goal
A home screen widget utilizing __Jetpack Glance__ library and __Google Sheets API__ that enables a user to easily train foreign vocabulary without needing to open a fullscreen app. The user is able to update vocabulary on any platform by using Google Sheets mobile or web apps and synchronize the widget at any time.

__Sheets before importing to the app need to have general access enabled with at least viewer role.__

## Features
* Display 50 random word pairs from the associated sheet,
* Reshuffle word pairs by clicking on any word pair on the widget,
* Multiple widgets can be added for the already imported or new sheets, 
* User can choose which spreadsheet and its associated sheet to use for a newly created widget,
* Material 3 dynamic colors support,
* Reuse already imported sheets to save storage space and delete them when the last associated widget is removed,
* Ability to synchronize words at any time by clicking on the last sync date on the widget if device is connected to the internet,
* Automatic spreadsheet link paste from the system clipboard if such has been detected. 

## Code quality
To keep up high code quality, CI pipeline runs:

✅ __unit tests__ and generates a __JaCoCo report__ for all relevant modules and files,

✅ __Ktlint__ code check,

✅ __Android lint__ check. 

## Authorization
For simplicity the app uses a Google __service account__ to authorize access to Google API. It's not considered the best method for its risk of credential leakage but it's sufficient for personal use. 

This method has been chosen because the app doesn't access user's personal data, it's simple to integrate and doesn't require the user to sign in in order to use the app.

### Set up Google Sheets API key
For the app to work correctly it needs Google API key file. To obtain it [create a new project in Google Cloud Platform](https://console.cloud.google.com/projectcreate) or use an existing one and [enable Google Sheets API](https://console.cloud.google.com/apis/enableflow?apiid=sheets.googleapis.com) for it. Then [create a service account](https://console.cloud.google.com/iam-admin/serviceaccounts), generate a new key under "keys" tab within the service account details, rename it and copy into the resources directory of the data module (`data/src/main/resources/google_sheets_credentials.json`).

## Used libraries and technologies
See the [version catalog](https://bitbucket.org/tymek313/glance-words/src/master/gradle/libs.versions.toml).