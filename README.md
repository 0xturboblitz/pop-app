To run:
```
yarn start
```

To create a development build, so that you can see native changes reflected:
```
eas build --profile development --platform ios
```
or
```
eas build --profile development --platform android
```
Pour le faire en local: https://docs.expo.dev/app-signing/local-credentials/

This also works and runs prebuild, and apparently opens simulator. Apparently not safe to do if native code is changed:
```
npx expo run:ios
```