# Debug signing key

The PEM files in this folder (`debug.key.pem`, `debug.cert.pem`) are a
fixed, non-secret RSA keypair used ONLY to sign **debug** builds of this
app, so that every CI build produces the same app signature and you can
install a newer debug APK as an **update** over the old one instead of
having to uninstall first.

This is a debug-only convenience key. It provides no security and is not
used for the release build (`android-release.yml` currently produces an
unsigned release APK \u2014 see that workflow's comments for how to add a real
release signing key via GitHub Secrets before distributing beyond your own
device).

The CI build workflow (`.github/workflows/android-build.yml`) converts
these PEM files into a PKCS12 keystore at build time with:

```
openssl pkcs12 -export \
  -in ci/debug-signing/debug.cert.pem \
  -inkey ci/debug-signing/debug.key.pem \
  -out ci-debug.keystore.p12 \
  -name myai -passout pass:android
```

`app/build.gradle.kts` points the `debug` build type's `signingConfig` at
that generated file (store/key password `android`, alias `myai`).

Do not replace these files with your own production signing key \u2014 generate
a separate release keystore for that and keep it out of the repo (see the
release workflow notes).
