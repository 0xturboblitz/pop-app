To build react-native style with connected device:
```
npx expo run:[android|ios]
```

To create a expo development build, useful when modifying only TS for some time:
```
eas build --profile development --platform [ios|android]
```
Then install it, and run:
```
npx expo start
```