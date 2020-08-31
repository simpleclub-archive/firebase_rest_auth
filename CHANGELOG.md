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