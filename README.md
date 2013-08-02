# causeway

## Usage

If you are starting a new project: 

```bash
lein new causeway-template project-name
cd project-name 
lein ring server
```

If you want to use `causeway` in an already existing project, add this to leiningen dependencies:

```clojure
[causeway "0.1.1"]
```

## About
Simple library for rapid web development with Clojure - designed with the following qualities in mind:
* low interdependence - you should be able to take any one feature of this library and use it separately; or replace it with some other component. For instance, if you like hiccup, feel free to use it instead of the build in templating library.
* composability - you should be able to easily add or replace any parts of the web stack. For instance you prefer SASS over LESS? Let's disable LESS and enable SASS.
* flexibility - it is easy to change the project structure. If you generated a project from the template, enabling or disabling some feature should be as easy as commenting out a line of code (this is what I don't like about [Luminus](https://github.com/yogthos/luminus-template))
* performance - all the compiled assets and are cached (using soft references, so memory limit is no issue) in production (unless you disable that)
* simple code - if a feature takes more than 200 LoC, it should be decomposed into smaller parts.


## Features
Among others, it includes the following features:

* Assets and resources structure
* Caching and serving of compiled web assets (right now LESSCSS, CoffeeScript and minimizers are supported, thanks to wro4j, but create an issue if you need any other)
* Templates inspired by [clabango](https://github.com/danlarkin/clabango) (but improved) which in turn is inspired by Django templating library
* L10n, i18n and AB-testing supported through:
** variant resources (templates, compiled and static resources)
** `(loc "some string")` macro
* Project configuration:
** bootconfig: the most basic config (db address, server port, runtime mode, etc...) is in a file (the path is configurable through leiningen profiles)
** properties: properties work almost like vars, but are saved in the database (currently only MongoDB is supported) and persist between server resets. Admin panel to edit the properties is included.
* Forms support:
** composable and simple to use form validation
** simple forms HTML generation (compatibile with validation)

coming soon:
* separation of all the features into smaller packages
* basic login support
* database support (PostgreSQL and MongoDB)
* more examples and docs
* basic CSS and JS libraries (like Twitter Bootstrap and jQuery)


## License

Copyright © 2013 Marcin Skotniczny

Distributed under the Eclipse Public License, the same as Clojure.
