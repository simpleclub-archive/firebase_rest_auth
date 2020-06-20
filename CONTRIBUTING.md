# Contributing to firebase_rest_auth

_See also: [Flutter's code of conduct](https://flutter.io/design-principles/#code-of-conduct)_

## Things you will need

 * Linux, Mac OS X, or Windows.
 * git (used for source version control).
 * An ssh client (used to authenticate with GitHub).

## Getting the code and configuring your environment


 * Ensure all the dependencies described in the previous section are installed.
 * Fork `https://github.com/simpleclub/firebase_rest_auth` into your own GitHub account. If
   you already have a fork, and are now installing a development environment on
   a new machine, make sure you've updated your fork so that you don't use stale
   configuration options from long ago.
 * If you haven't configured your machine with an SSH key that's known to github, then
   follow [GitHub's directions](https://help.github.com/articles/generating-ssh-keys/)
   to generate an SSH key.
 * `git clone git@github.com:<your_name_here>/firebase_rest_auth.git`
 * `git remote add upstream git@github.com:simpleclub/firebase_rest_auth.git` (So that you
   fetch from the master repository, not your clone, when running `git fetch`
   et al.)

## Contributing code

We gladly accept contributions via GitHub pull requests.

Please peruse the
[Flutter style guide](https://github.com/flutter/flutter/wiki/Style-guide-for-Flutter-repo) and
[design principles](https://flutter.io/design-principles/) before
working on anything non-trivial. These guidelines are intended to
keep the code consistent and avoid common pitfalls.

To start working on a patch:

 * `git fetch upstream`
 * `git checkout upstream/master -b <name_of_your_branch>`
 * Hack away.
 * Verify changes with [flutter_plugin_tools](https://pub.dartlang.org/packages/flutter_plugin_tools)
```
pub global activate flutter_plugin_tools
pub global run flutter_plugin_tools format --plugins firebase_rest_auth
pub global run flutter_plugin_tools analyze --plugins firebase_rest_auth
pub global run flutter_plugin_tools test --plugins firebase_rest_auth
```
 * `git commit -a -m "<your informative commit message>"`
 * `git push origin <name_of_your_branch>`

To send us a pull request:

* `git pull-request` (if you are using [Hub](http://github.com/github/hub/)) or
  go to `https://github.com/simpleclub/firebase_rest_auth` and click the
  "Compare & pull request" button

Please make sure all your checkins have detailed commit messages explaining the patch.

Plugins tests are run automatically on contributions. However, due to
cost constraints, pull requests from non-committers may not run all the tests
automatically.

Once you've gotten an LGTM from a project maintainer and once your PR has received
the green light from all our automated testing, wait for one the package maintainers
to merge the pull request and `pub submit` any affected packages.