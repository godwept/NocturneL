# NocturneL Release Signing

Google Play App Signing owns the permanent app-signing key. NocturneL uses a separate, resettable upload key in CI.

## Create the upload key

Run this with a supported JDK from a private directory outside the repository:

~~~powershell
keytool -genkeypair -v -keystore nocturnel-upload.jks -storetype JKS -alias nocturnel-upload -keyalg RSA -keysize 4096 -validity 10000
~~~

Use distinct, randomly generated store and key passwords. Keep an encrypted offline backup of the keystore and keep its passwords separately. The repository ignores .jks and .keystore files, but never place the keystore in the workspace.

Export the public certificate and record its SHA-256 fingerprint:

~~~powershell
keytool -exportcert -rfc -keystore nocturnel-upload.jks -alias nocturnel-upload -file nocturnel-upload.pem
keytool -list -v -keystore nocturnel-upload.jks -alias nocturnel-upload
~~~

## Configure GitHub

Create a protected environment named play-release and add:

- NOCTURNEL_UPLOAD_KEYSTORE_BASE64
- NOCTURNEL_UPLOAD_KEY_ALIAS
- NOCTURNEL_UPLOAD_STORE_PASSWORD
- NOCTURNEL_UPLOAD_KEY_PASSWORD

Encode the keystore locally for the first secret:

~~~powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\private\nocturnel-upload.jks'))
~~~

Never put secret values in workflow YAML, command arguments, logs, artifacts, issues, or documentation.

## Recovery

If the upload key is lost or compromised, stop release work, remove/rotate the GitHub secrets, and request an upload key reset from Play Console's App integrity page. Play App Signing keeps the permanent app-signing key separate, so an upload-key reset does not change existing users' app identity.
