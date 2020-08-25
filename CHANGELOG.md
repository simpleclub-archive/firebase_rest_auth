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