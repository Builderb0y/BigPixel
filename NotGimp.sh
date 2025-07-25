#!/usr/bin/env bash

cd /home/builderb0y/Documents/Projects/NotGimp
/home/builderb0y/java/jdk-22.0.2+9/bin/java --enable-preview --add-modules jdk.incubator.vector,javafx.base,javafx.controls,javafx.graphics --module-path /home/builderb0y/Documents/Projects/NotGimp/dependencies --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED -classpath /home/builderb0y/Documents/Projects/NotGimp/out/production/classes:/home/builderb0y/Documents/Projects/NotGimp/out/production/resources builderb0y.notgimp.NotGimp "$@"