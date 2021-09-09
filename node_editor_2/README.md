## Installing Processing Core dependency:

To install processing:

```shell
$ mvn install:install-file -Dfile=<core.jar location> -DgroupId=org.processing -DartifactId=core -Dversion=<processing version> -Dpackaging=jar -DgeneratePom=true
```

Eg.

```shell
$ mvn install:install-file -Dfile=/usr/share/processing/core/library/core.jar -DgroupId=org.processing -DartifactId=core -Dversion=3.5.4 -Dpackaging=jar -DgeneratePom=true
```

Update the processing dependency in [pom.xml](pom.xml) to have a matching version.