# firebase_rest_auth

[![pub package](https://img.shields.io/pub/v/firebase_rest_auth.svg)](https://pub.dartlang.org/packages/firebase_rest_auth)

> Notice: The plugin is currently in development. See Limitations below for supported features. 
>
> You are more than welcome to contribute and extend the plugins functionality.

A Flutter plugin to use the [Firebase Authentication API](https://firebase.google.com/products/auth/) via REST.

Non [Google Mobile Services](https://www.android.com/gms/) (GMS) devices (such as Huawei or Amazon devices) 
can use the plugin to authenticate with Firebase, and can use plugins requiring user authentication such as 
* [cloud_firestore](https://pub.dev/packages/cloud_firestore)
* [firebase_storage](https://pub.dev/packages/firebase_storage)

without further modifications.  

The plugin supports REST authentication with Android only, but other platforms can be used through 
the standard [firebase_auth](https://pub.dev/packages/firebase_auth) implementation (see installation below).

___

### Disclaimer
This plugins also implements an `InternalAuthProvider` for many other firebase plugins to use.

Firebase Instance Id (FirebaseIid) is not implemented yet by this plugin, therefore some plugins
are reduced in functionality or might not work.

> **Note, you cannot also include the FirebaseAuth client library in your build.**

For more information about the implementation read [here](https://github.com/FirebaseExtended/auth-without-play-services/blob/master/README.md).

---

## Usage
The plugin utilizes the existing implementation and method channels of 
[firebase_auth](https://github.com/FirebaseExtended/flutterfire/tree/master/packages/firebase_auth/firebase_auth).

**All implemented methods of the standard dart library can be used** *(see limitations)*, 
as this plugin only overrides the Android part of the method channel implementation!

#### Example:

```
FirebaseAuth.instance.signInWithCustomToken(token);
```

To use this plugin, see installation below.

## Installation

To utilize the existing dart implementationa and method channels of `firebase_auth`, 
the installation comes with some unique requirements:

1. Fork [`flutterfire`](https://github.com/FirebaseExtended/flutterfire)
2. Edit `packages/firebase_auth/firebase_auth/pubspec.yaml`:
    1. Change the plugins section:
     ```
     flutter:
       plugin:
         platforms:
           android:
             default_package: firebase_rest_auth
           ios:
             pluginClass: FLTFirebaseAuthPlugin
           macos:
             pluginClass: FLTFirebaseAuthPlugin
           web:
             default_package: firebase_auth_web
     ```
    2. Add this plugin to your **forks `pubspec.yml`**
    ```
   dependencies:
     firebase_rest_auth:
       git: git@github.com:simpleclub/firebase_rest_auth.git
   ```
3. Commit the fork to a custom branch e.g. `firebase-rest-auth`
4. Add your fork to your **projects `pubspec.yml`**
```
firebase_auth:
    git:
      url: https://github.com/<your_name_here>/flutterfire
      path: packages/firebase_auth/firebase_auth
      ref: firebase-rest-auth
```

*A simple, pre-forked implementation is available at [simpleclub-extended/flutterfire](https://github.com/simpleclub-extended/flutterfire/tree/firebase-auth/firebase-rest-auth), branch `firebase-auth/firebase-rest-auth`*

## Limitations

The plugins Android implementation is inspired heavily by 
[FirebaseExtended/auth-without-play-services](https://github.com/FirebaseExtended/auth-without-play-services),
therefore it currently shares the same limitations as described in the disclaimer.

Implemented methods of `firebase_auth` are:

* `signInWithCustomToken({@required String token})`
* `get onAuthStateChanged`
* `signOut()`

A lot of methods such as `currentUser()` are internally implemented and need no custom override.

**You are more than welcome to contribute and extend the plugins functionality.**

## Contributing

If you wish to contribute a change in this repo,
please review our [contribution guide](https://github.com/simpleclub/firebase_rest_auth/blob/master/CONTRIBUTING.md),
and send a [pull request](https://github.com/simpleclub/firebase_rest_auth/pulls).
 