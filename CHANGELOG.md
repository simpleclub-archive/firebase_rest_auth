## 0.3.0
* New Basic functionality:
    * `signInWithEmailAndPassword({@required String email, @required String password})`

## 0.2.1+2

* Fix: Ensure Anonymous Users get correctly reported from `UserStorage` 


## 0.2.1+1

* Fix: Added `proguard-rules.pro` to disable unused file removal for the plugin in relase mode.

## 0.2.1

* Fix: Bug where the current user was not set before returning success.
* New Basic functionality:
    * `getIdToken()`

## 0.2.0

* Complete rewrite to support the new FlutterFire structure.
* New Basic functionality:
    * `signInAnonymously()`
    * `signInWithCustomToken({@required String token})`
    * `get onAuthStateChanged`
    * `signOut()`

## 0.1.0

* Initial public release
* Basic functionality:
    * `currentUser()`
    * `signInWithCustomToken({@required String token})`
    * `signInWithEmailAndPassword({@required String email, @required String password})`
    * `get onAuthStateChanged`
    * `signOut()`